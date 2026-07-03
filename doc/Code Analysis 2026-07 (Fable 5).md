# GlycoPaint-Java — Code Analysis

*Analysis date: 2026-07-03 · Model: Claude Fable 5 · Version analysed: `paint-parent 0.0.138-SNAPSHOT` · 136 Java files, ~34.4k LOC, 13 Maven POMs (10 code modules + installer aggregator + 2 installer submodules) · git `7690e19e`*

*This is an independent re-analysis. Where it revises the earlier `Code Analysis 2026-07.md`, the revision is called out inline.*

## Executive summary

GlycoPaint-Java is a cleanly organised scientific-imaging pipeline — a Java rewrite of the Python Glyco-PAINT tooling, built on Fiji/TrackMate and OMERO. The architecture is a healthy hub-and-spoke: one foundation library (`paint-shared-utils`), no dependency cycles, and a schema-as-code design where each domain object owns the `Column` enum that *is* its CSV contract. Pipeline stages communicate only through CSV files, so they run and test independently. Maven version management is genuinely disciplined.

The debt concentrates in three places: **effectively zero automated test coverage over the scientific math**, a cluster of **correctness bugs in the viewer's override import/export path** (unguarded NPEs on null table reads, off-EDT Swing threading, a `-1` sentinel that pollutes statistics), and **infrastructure gaps** (docs-only CI with no build/test gate, an all-SNAPSHOT version, an EOL Java 8 toolchain hard-locked by the enforcer). Nothing blocks use today, but the absence of tests over complex numeric logic is the single largest risk to trusting results as the code changes.

Highest-value next moves, in order: (1) add JUnit assertions to the pure numeric methods, (2) fix the null-deref NPEs on the override export path, (3) add a `mvn -B verify` CI gate.

---

## 1. Architecture

### Module map

The reactor is a flat fan-out from one foundation library. `paint-shared-utils` (55 Java files, the largest module) is the base that 9 of the 11 code modules build on.

| Module | Role |
|---|---|
| **paint-shared-utils** | Foundation: domain objects, CSV/Tablesaw IO façade, config singleton, shared math (Tau, track filtering), validation, shared Swing dialogs, logging. |
| **paint-fiji-plugin** | Fiji/TrackMate integration; runs particle tracking on recordings/experiments and emits Tracks + Recordings CSVs. |
| **paint-generate-squares** | Tiles each recording into a grid of squares, assigns tracks, computes per-square Tau/density/variability. Emits Squares CSV. |
| **paint-viewer** | Interactive Swing/ImageJ desktop app; browse recordings/squares, TIFF playback, manual override editing and export. |
| **paint-create-experiment** | Standalone UI to scaffold an experiment directory / `Experiment Info.csv`. |
| **paint-get-omero** | Standalone OMERO downloader UI. Fully decoupled — depends only on `commons-csv` + `slf4j-nop`, zero `paint.*` imports. |
| **paint-launcher** | Tiny Swing menu that shells out to the other modules' JARs via `ProcessBuilder`. Zero dependencies. |
| **paint-regression** | CSV-to-CSV comparators (Java-vs-Python reference). |
| **paint-development-utils** | Build/release automation (GitHub release, format conversion). Not part of the runtime pipeline. |
| **paint-installer** (macos / windows) | Native installers packaging the app per OS. |

### Dependency graph

Read from each module's `<dependencies>` (groupId `com.github.jjabakker`):

```
paint-shared-utils   (depends on NO internal module — the foundation)
      ▲   ▲   ▲   ▲   ▲
      │   │   │   │   └── paint-installer-macos / -windows, paint-development-utils
      │   │   │   └────── paint-regression, paint-create-experiment
      │   │   └────────── paint-viewer            (reads CSVs only; no compute deps)
      │   └────────────── paint-generate-squares ◄── paint-fiji-plugin (compile)
      └────────────────── paint-fiji-plugin

paint-get-omero   → standalone (commons-csv + slf4j only; no internal deps)
paint-launcher    → zero dependencies (discovers app JARs by filename prefix at runtime)
```

The only module-to-module (non-shared) dependency is `paint-fiji-plugin → paint-generate-squares`, so the plugin can run square generation right after TrackMate. **No cycles** — verified `paint-shared-utils` imports no other paint module, and nothing outside `paint-fiji-plugin` imports `paint.fiji`. The viewer is cleanly decoupled: it imports neither the Fiji plugin nor generate-squares, integrating only through CSV files. This is a genuinely good design — stages communicate through files and can be exercised in isolation. The one structural caveat is that `paint-shared-utils` is a broad hub (domain + IO + config + math + UI dialogs), so it is a change-amplification point.

### Data flow

Domain hierarchy (all in `paint-shared-utils/.../objects/`): `Project` → `List<Experiment>` → `List<Recording>` → each holds `List<Square>` and `List<Track>`. Aggregation is by in-memory composition; children don't reference parents.

```
Raw microscopy + Experiment Info.csv
        │  [1] paint-fiji-plugin (TrackMate): detect spots → build tracks → per-track attributes
        ▼
   Tracks.csv + Recordings.csv
        │  [2] paint-generate-squares: grid → assign tracks → per-square Tau/R²/density/variability
        ▼
   Squares.csv (+ re-sorted Tracks/Recordings)
        │  [3] paint-viewer: inspect / select squares / assign cells → writes override files
        ▼
   [4] Analysis in R
```

Each domain entity declares a nested `public enum Column` carrying a header string and a Tablesaw `ColumnType` (e.g. `Square.java:767-805`, `Recording.java:635`). That enum **is** the CSV schema — there is no separate DDL. All IO flows through the static `MainIOInterface` façade, which reflects the enum into `String[] headers` + `ColumnType[] types` and delegates to package-private `*TableIO` classes over `BaseTableIO`. `BaseTableIO` reads every column as STRING first, then coerces and validates against the enum schema, and forces US-locale fixed-decimal output on write. Keeping the CSV contract in one place per object is a real strength.

### Architectural concerns

- **Global mutable singleton config.** `PaintConfig` is a `private static volatile INSTANCE` with double-checked locking (`PaintConfig.java:60,80-93`) and static accessors used pervasively (e.g. read at class-init time in `GenerateSquaresProcessor.java:84`). `reinitialise` nulls and rebuilds the instance on project switch — hidden global state mutated mid-run, a hazard for tests and concurrency and a source of initialisation-ordering coupling.
- **Static-façade IO with no seam.** `MainIOInterface` is entirely static with no interface/injection point, and callers use `import static`. Good for enforcing the single schema path, but IO can't be mocked — testing compute code requires real files on disk.
- **UI mixed into the data/foundation layer.** `ExperimentDataLoader` (an IO class in the foundation module) imports `javax.swing.*` and pops a `JOptionPane` on load failure (`ExperimentDataLoader.java:155-159`). A headless/CI run of generate-squares transitively pulls Swing error dialogs.
- **Large mutable domain objects.** `Square.java` = **811 lines** (~35 fields) and `Recording.java` = **678 lines** are wide, mutable state buckets mirroring wide CSV rows — both persistence schema and computation-result holder in one. Brittle to schema change. (For contrast `Project`=195, `Experiment`=126 are thin.)
- **Stringly-typed launcher.** `PaintLauncher` scans a hardcoded `jars` dir under `user.dir`, matches app JARs by filename **prefix**, takes `matches[0]`, and runs a bare `new ProcessBuilder("java","-jar",...)`. Fragile to CWD, PATH, JAR renames, and version ambiguity when multiple matches exist.
- **Scientific math split across modules.** `CalculateTau` and `SharedSquareUtils` (track filtering) live in shared-utils; `CalculateSquareAttributes` lives in generate-squares; per-track diffusion/speed math lives in the Fiji plugin (`TrackAttributeCalculations`). Computing one square's result spans two modules, and `paint-regression` re-encodes expected numeric behaviour a third time — worth watching for drift against the Python reference.
- **Minor: package-naming inconsistency.** Most modules root at `paint.*`, but `paint-create-experiment` uses `createexperiment.*` and `paint-development-utils` uses bare roots (`utils`, `release`, `github`).

---

## 2. Code quality & tech debt

### High priority

**Effectively zero automated test coverage.** There is exactly one file under any `src/test`: `paint-generate-squares/.../CalculateTauTest.java` (129 lines). It is **not a JUnit test** — it has a `main()`, builds a Swing plot, and only *prints* deltas against expected Python values; it never asserts. Repo-wide: **0** `@Test` annotations and **0** `assertEquals/assertTrue/assertThat/...`. JUnit isn't even declared in `paint-generate-squares/pom.xml`. The scientifically load-bearing code — `CalculateSquareAttributes` (402 LOC), `GenerateSquaresProcessor` (430 LOC), `CalculateTau` (379 LOC) — has no regression protection. *Start with the pure numeric methods: small fixed inputs, known outputs — highest value, lowest effort.*

**One very large method.** `runTrackMateOnRecording` (`RunTrackMateOnRecording.java:106-423`, ~318 lines) orchestrates image loading, TrackMate setup, execution, cancellation, and export in one block. Next largest: `assignTracksToSquares` (~125 lines) and `generateSquaresForExperiment` (~99 lines) in `GenerateSquaresProcessor`. Extract phases into private methods.

**Swallowed exceptions on a data-write path.** 29 empty/`ignored` catch blocks repo-wide (8 in the macOS installer, 5 in the Windows installer, 4 each in `JarInfoLogger` and the squares comparator, 2 in `TiffMoviePlayer`). The concerning one is production: `WriteSquareOverride.java:103` swallows a `Files.createDirectories` failure with `catch (IOException ignored) {}` in the constructor, so if the `Viewer/` dir can't be created, later override writes fail with no diagnostic. Route through `PaintLogger`.

**Hardcoded `/Users/hans/…` paths — 23 occurrences across 13 files.** Most are benign (dev/regression `main()` methods), but they make the entire `paint-regression` module unrunnable by anyone else or in CI: all three comparators hardcode `/Users/hans/Paint Test Project/…` and `/Users/hans/Downloads/validate/…` (`SquaresCsvComparatorPythonJava.java:76-77`, `TracksCsvComparatorPythonJava.java:879-880`, `CsvComparatorRegression.java:373-408`). Move inputs to test resources or accept them as CLI args.

### Medium priority

- **Regression module reinvents CSV parsing.** `SquaresCsvComparatorPythonJava.java` (1012 LOC) and `TracksCsvComparatorPythonJava.java` (900 LOC) hand-roll `BufferedReader` + `split(",")` (~10–12 raw splits each) — ~1900 LOC of bespoke, untested CSV logic that duplicates the solid Tablesaw-based `BaseTableIO`, which they never reference.
- **Duplicate installers.** `GlycoPaintInstallerMac.java` (751 LOC) and `GlycoPaintInstallerWindows.java` (646 LOC) are parallel implementations with no shared base class — Swing UI, extraction, and error handling duplicated per OS.
- **`printStackTrace()` in 13 production sites** bypasses `PaintLogger` (which is otherwise well adopted — 379 usages) and dumps to raw stderr; includes GUI/pipeline paths (`ViewerFrame.java:864`, `TiffMoviePlayer.java:126`, `TrackDataExporter.java:205`, `GenerateSquares.java:90`). Route through the logger.

### Low priority

- ~181 `System.out` + ~44 `System.err` calls, but the bulk are in dev/regression CLI tooling where stdout is the intended interface — defensible.
- Only **1** TODO/FIXME marker repo-wide (`PaintTiming.java:45`) and negligible commented-out code — a real positive.

> **Confirmed, not a bug:** the earlier suspicion that the code uses Java 16 `record` types while targeting Java 8 is **false**. A targeted search for `record` type declarations returns zero; every "record" token is either the domain word "recording" or a local variable of type `CSVRecord` (Apache Commons CSV), e.g. `for (CSVRecord record : parser)`. The source is genuinely Java-8-clean, so the Java 8 build config is consistent. (Java 8 being EOL is still a valid concern — see §4.)

---

## 3. Bugs & correctness

Only issues verified in source are listed. Highest-value fixes: the null-deref NPEs on the override export path (#1/#2), then the Swing threading hazard (#3), then the `-1` statistics pollution (#7).

> **Revision to the earlier report:** its Bug #1 described importers *silently returning partial data* because a `try/catch` sits outside the row loop. On re-verification this framing is imprecise: the override importers read via Tablesaw `Table.read().csv(...)` (`ImportSquareOverride.java:197`), which parses the entire table **before** the object-building loop — so a raw malformed CSV row aborts cleanly at parse time rather than truncating. A partial return is still *possible* only if per-row object construction throws mid-loop (Low severity). The genuinely High-severity issue in this path is the unguarded null-deref below (#1).

### High severity

1. **Unguarded NPE when a table read returns null (override export).** `ExportOverridesFromViewer.java:242,246` calls `squaresTable.stringColumn("Recording Name")` and `tracksTable.stringColumn("Recording Name")` with no null check. But `MainIOInterface.readSquaresTable/readTracksTable` catch their own exceptions internally and **return null**, so the surrounding `try/catch` here (lines 176-208) is dead and can never fire. A missing/malformed `Squares.csv` or `Tracks.csv` → raw NPE that aborts the export after in-memory squares were already filtered. The correct pattern exists at `WriteRecordingExclude.patchRecordingExcluded:80` (`if (table == null) return;`), confirming this is an oversight. *Fix: null-check each read immediately and fail explicitly.*

2. **Same swallowed-null pattern in `ExperimentDataLoader`.** `readRecordings`/`readTracksTable`/`readSquaresTable` return null while the surrounding `try/catch` can't trigger; dereferences at `ExperimentDataLoader.java:92` (`for (Recording r : recordings)`), `:111` (`tracksTable.rowCount()`), `:144` (`squaresTable.rowCount()`) → NPE on a failed load. *Fix: explicit null-check-and-return after each read.*

3. **Off-EDT Swing access + unsynchronised state in the TIFF player.** `TiffMoviePlayer.java:280-322` runs a raw `new Thread` whose loop reads `frame.isVisible()` and `speedSlider.getValue()` off the EDT and shares plain `boolean[] playing` / `int[] currentFrame` arrays (lines 243-244) with EDT listeners, without `volatile`/synchronisation. This violates Swing's single-thread rule and creates a visibility hazard (a pause may never be seen → thread never stops) plus a lost-update race on the frame index. `RecordingPlaybackController.java:70` does it correctly with `volatile boolean playing` — this class does not. *Fix: drive from a `javax.swing.Timer`, or use `AtomicBoolean`/`AtomicInteger` and snapshot the slider on the EDT.*

### Medium severity

4. **Missing null guard on `getRightImage()` in the display updater.** `RecordingDisplayUpdater.java:116` calls `entry.getRightImage().getImage().getScaledInstance(...)`, but `RecordingEntry.loadImage` returns null for a corrupt/unsupported brightfield image → NPE. *Fix: null-check and show a placeholder.*

5. **NPE on null neighbour-mode during override apply.** `ImportRecordingOverride.java:165` calls `oldNeighbourMode.equals(...)` where `oldNeighbourMode` (from `getNeighbourMode()`, `Recording.java:116`, default null) can be null — throwing *after* override values were already applied, leaving in-memory state half-updated. *Fix: `Objects.equals(...)`.*

6. **`PaintLogger` is not thread-safe** though called from worker threads (TrackMate runs, project dialogs). Static `writer`/`initialised`/`justPrintedRaw` are non-volatile and unsynchronised; concurrent writes can interleave. *Fix: synchronise the log methods; mark shared fields volatile.*

### Low severity

7. **Missing feature values written as literal `-1`, skewing statistics.** `TrackDataExporter.java:148-153` uses `roundOr(feature, n, -1)` for the four native TrackMate features (duration, displacement, max/median speed); a null feature becomes `-1`, a plausible-looking value. `CalculateSquareAttributes.java:217-229` then feeds those columns into `.median()/.max()/.sum()`, so `-1` enters the aggregates as if real. (The PAINT-computed attributes correctly use NaN, so this is confined to null native features — usually present on real tracks, hence latent.) *Fix: use `Double.NaN`; Tablesaw excludes NaN from stats.*

8. **Non-perfect-square grid count silently truncated.** `GenerateSquaresProcessor.java:207,256` and `CalculateSquareAttributes.java:308` compute `gridSize = (int) Math.sqrt(n)`, producing `gridSize²` real squares, while `Square.calculateSquareArea(n)` still divides by the original `n`. For a non-perfect-square config (e.g. 30 → 5 → 25 squares) density is wrong. Default 400 (=20²) is safe. *Fix: derive area from `gridSize²`, or reject non-perfect-square configs at load.*

9. **Hand-rolled override CSV is unquoted and asymmetric.** `WriteSquareOverride.java` / `WriteRecordingOverride.java` write via string concatenation and read via `split(",", n)` + `startsWith(prefix)` with no quoting/escaping — while the import side uses quoting-aware Tablesaw. A name containing a comma shifts fields and defeats prefix matching. Also `loadExistingRows` accepts the header with `equalsIgnoreCase` (`:181`) but `replaceSquareOverrides` requires exact `.equals` (`:293`) — a case-variant header is tolerated by one and triggers a rebuild/wipe in the other. *Fix: use a real CSV library both directions; make header checks consistent.*

### Verified *not* bugs

Override importers use quoting-aware Tablesaw (no raw-row truncation); `RecordingPlaybackController` threads correctly with `volatile` + `invokeLater`; `TrackAttributeCalculations` returns NaN (not −1) for insufficient data; the empty-filter path in `ViewerFrame` does not clear `recordingEntries`, so the unguarded `get(currentIndex)` calls are safe after a no-match filter (the only residual risk is a project loaded with zero recordings); density/area use double division; no `==` string-identity comparisons; `BaseTableIO.writeCsv` forces US locale.

---

## 4. Build & dependency health

**Version management is a strength.** All key versions are pinned in the parent `<properties>` + `<dependencyManagement>`; children mostly omit versions; no cross-module version conflicts. Minor leaks: a few children re-declare plugin versions inline (`maven-shade-plugin 3.5.3`, `launch4j 2.5.0` in 3–4 modules), and two dependencies are pinned inline outside parent management (`pdfbox-graphics2d 0.37`, `dd-plist 1.23` in `paint-shared-utils/pom.xml:70-78`).

The real risks are structural:

- **Docs-only CI, no build/test gate.** The single workflow `publish-javadoc.yml` runs only on tag push `v*.*.*` and manual dispatch — **not** on branch push or PR. Its one build step is `mvn -B -U -DskipTests site`, which generates Javadoc and deploys to gh-pages. Nothing compiles application bytecode or runs tests in CI. Broken compiles and failing tests can merge undetected. *This is the top infrastructure gap.*
- **EOL Java 8, hard-locked by the enforcer.** `maven.compiler.source/target = 1.8` (`pom.xml:86-87`) and `requireJavaVersion = [1.8,1.9)` (`pom.xml:366-368`) — a half-open range permitting only JDK 8; the build fails on JDK 9+. Reinforced downstream by `/usr/libexec/java_home -v1.8` launch scripts and `Info.plist JVMVersion 1.8*`. The lock is a deliberate Fiji/TrackMate constraint but actively blocks even *testing* a migration until relaxed.
- **All-SNAPSHOT version, no release path.** The whole reactor is `0.0.138-SNAPSHOT`; nothing has ever been cut as a stable release. No `maven-release-plugin`; `<distributionManagement>` declares only a `<site>` (gh-pages) — no artifact `<repository>`/`<snapshotRepository>`, so `mvn deploy` has nowhere to publish JARs. Top reproducibility issue.
- **Non-hermetic native packaging.** No `jpackage`; instead `maven-antrun` hand-builds macOS `.app` bundles and, during the `package` phase, copies them into `${user.home}/Applications/Glyco-PAINT` (`paint-viewer/pom.xml:249-260`, and 3 sibling modules). The Fiji plugin's `deploy-to-fiji` execution (bound to `install`) copies the shaded JAR into `/Applications/Fiji.app/plugins` (`paint-fiji-plugin/pom.xml:158-191`, skipped on CI via `${env.CI}`). Build-time writes outside `target/` — surprising and non-reproducible.
- **Two divergent fat-JAR mechanisms.** `maven-shade-plugin` (fiji-plugin, generate-squares, installer-windows) and `maven-assembly-plugin` `jar-with-dependencies` (get-omero, create-experiment, viewer, launcher, regression, installer-macos) coexist across sibling modules producing similar artifacts — different `META-INF/services`/manifest merge semantics; a maintenance and correctness hazard (note the two installers even differ from each other).
- **Repository config gaps.** `scijava.public` is declared in the parent *and* redundantly in ~5 children. The OME/OMERO repo (`ome.releases`) is **absent from the parent** and duplicated across the four Bio-Formats-dependent children — hoist it up. The Unidata/netCDF repo is absent; scijava mirrors most `edu.ucar` artifacts so builds resolve today, but it's a latent Bio-Formats resolution risk worth adding defensively.
- **No dependency/vuln scanning** — no OWASP dependency-check, no `.github/dependabot.yml`, no CVE step in CI.

**Dependencies to review for a version bump** (stale / CVE-prone lines; not asserting a live CVE): PDFBox **2.0.30** (2.x line is EOL; 3.x current), logback-classic **1.2.13** (legacy 1.2 branch, kept because Java 8 blocks 1.3+), Bio-Formats **6.13.0** (7.x exists), commons-compress **1.26.2** (1.27+ exists). ImageJ `ij` 1.54h, commons-csv 1.10.0, scijava-common 2.94.1, Guava 32.1.3, slf4j 1.7.36 are slightly stale but low-risk and pinned consistently with the Java 8 lock.

---

## 5. Recommended priorities

1. **Add JUnit assertions to the pure numeric methods** — `CalculateTau`, and density/variability/Tau in `CalculateSquareAttributes`. Highest value, lowest effort; protects the science.
2. **Fix the override-export null-deref NPEs** — #1 (`ExportOverridesFromViewer` squares/tracks tables) and #2 (`ExperimentDataLoader`). Small, high-impact.
3. **Add a build+test CI gate** — a workflow running `mvn -B verify` on push/PR, so compilation and tests are actually exercised.
4. **Log instead of ignore** on the override-write directory-creation failure (`WriteSquareOverride.java:103`).
5. **Fix the viewer threading hazard** — drive `TiffMoviePlayer` from a Swing `Timer`; synchronise `PaintLogger`.
6. **Make the regression module CI-runnable** — parameterise the hardcoded `/Users/hans/` paths.
7. **Longer term** — plan a Java 17/21 migration path (relax the enforcer), move to non-SNAPSHOT release versions with a real deploy repo, hoist the OME repo to the parent, converge on one fat-JAR plugin, and add OWASP dependency-check / Dependabot.
