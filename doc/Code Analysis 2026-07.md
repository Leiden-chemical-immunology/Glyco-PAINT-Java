# GlycoPaint-Java — Code Analysis

*Analysis date: 2026-07-03 · Version analysed: `paint-parent 0.0.138-SNAPSHOT` · ~34k LOC, 136 Java files, 11 Maven modules*

## Executive summary

GlycoPaint-Java is a well-organised scientific imaging pipeline — a Java rewrite of the original Python Glyco-PAINT tooling, built on Fiji/TrackMate and OMERO. The architecture is clean: a single shared foundation library, minimal module-to-module coupling, no dependency cycles, and a schema-as-code approach that keeps the CSV data contract centralised. Version management in Maven is genuinely good.

The debt is concentrated in three places: **near-zero automated test coverage over the scientific calculations**, a handful of **correctness bugs in the viewer's override import/export paths** (silent partial-data loss, unguarded NPEs, threading hazards), and **infrastructure gaps** (docs-only CI, an all-SNAPSHOT version, an EOL Java 8 toolchain). None of these block use today, but the lack of tests over complex numeric logic is the single largest risk to trusting results as the code evolves.

The most valuable next moves, in order: (1) add JUnit assertions to the pure numeric methods, (2) fix the silent data-loss and NPE bugs in the override export path, (3) add a build+test CI gate.

---

## 1. Architecture

### Module map

The reactor is a flat fan-out from one foundation library. `paint-shared-utils` (12.7k LOC) is the base that every functional module builds on.

| Module | Role |
|---|---|
| **paint-shared-utils** | Foundation: domain objects, CSV/table IO, config, validation, shared Swing dialogs, logging, Tau math. |
| **paint-fiji-plugin** | TrackMate integration; SciJava `Command` plugins (`Plugins → Glyco-PAINT → Run Single / Run Batch`). Produces Tracks + Recordings CSVs. |
| **paint-generate-squares** | Grid/square analysis: divides recordings into square grids, assigns tracks, computes per-square Tau/density/variability. Produces Squares CSV. |
| **paint-viewer** | Interactive Swing viewer; square selection, cell assignment, playback, override export. |
| **paint-create-experiment** | Builds the `Experiment Info.csv` skeleton from image filenames. |
| **paint-get-omero** | Flattens OMERO download folders into an experiment directory. |
| **paint-launcher** | GUI front door that launches the app JARs as child processes. |
| **paint-regression** | CSV-to-CSV comparators (Java-vs-gold-standard, Python-vs-Java). |
| **paint-development-utils** | Build/release automation (not part of the runtime pipeline). |
| **paint-installer** (macos / windows) | Native installers that copy JARs and deploy the Fiji plugin. |

### Dependency graph

```
paint-shared-utils   (depends on NO internal module — the base)
      ▲   ▲   ▲   ▲   ▲
      │   │   │   │   └── paint-installer-macos / -windows, paint-development-utils
      │   │   │   └────── paint-regression, paint-create-experiment
      │   │   └────────── paint-viewer          (reads CSVs only; no compute deps)
      │   └────────────── paint-generate-squares ◄── paint-fiji-plugin (compile)
      └────────────────── paint-fiji-plugin

paint-get-omero   → standalone (only commons-csv)
paint-launcher    → zero compile coupling (discovers app JARs by filename at runtime)
```

The only module-to-module (non-shared) dependency is `paint-fiji-plugin → paint-generate-squares`, so the plugin can optionally run square generation right after TrackMate. The viewer is cleanly decoupled — it imports neither the Fiji plugin nor generate-squares, and integrates only through the CSV files. This is a healthy design: pipeline stages communicate through files and can be run and tested independently.

### Data flow

The data model is four CSV tables managed with Tablesaw dataframes, mirroring the object hierarchy `Project → Experiment → Recording → Square → Track`:

```
Raw microscopy + Experiment Info.csv
        │  [1] paint-fiji-plugin (TrackMate): detect spots → build tracks → per-track attributes
        ▼
   Tracks.csv + Recordings.csv
        │  [2] paint-generate-squares: grid → assign tracks → per-square Tau/R²/Density/Variability
        ▼
   Squares.csv
        │  [3] paint-viewer: inspect / select squares / assign cells → writes override files
        ▼
   [4] Analysis in R
```

Each domain entity owns a nested `Column` enum (header + Tablesaw type) that **is** the CSV schema, driving read/write/validation through the static `MainIOInterface` façade. This keeps the CSV contract in one place rather than scattered — a real strength.

### Architectural concerns

- **Global mutable singleton config.** `PaintConfig` is a `volatile INSTANCE` with static accessors, read from ~10+ classes across modules (including field initialisers). This is hidden global state: it couples compute logic to a static context, complicates testing, and creates ordering hazards (must be initialised before any static read).
- **Static-façade IO with no seam.** `MainIOInterface` and `ExperimentDataLoader` are all-static, so IO can't be mocked; testing compute code requires real files on disk.
- **UI mixed into the data layer.** `ExperimentDataLoader` (an IO class) pops a Swing `JOptionPane` on an invalid squares layout — presentation logic in the loading layer hurts headless/batch use.
- **Large mutable domain objects.** `Square` (811 LOC, ~35 fields) and `Recording` (679 LOC, ~64 methods) are wide, mutable state buckets mirroring wide CSV rows. Not behaviour God-classes, but brittle to schema change and easy to leave in inconsistent states (e.g. `Square` holds both a `List<Track>` and a `tracksTable`).
- **Stringly-typed launcher.** `PaintLauncher` finds apps by JAR filename prefix and shells out with a bare `"java"` — fragile to renames, PATH, and Java-version issues.
- **Scientific math split across modules.** Per-track diffusion/speed math lives in the Fiji plugin (`TrackAttributeCalculations`) while Tau lives in shared-utils (`CalculateTau`) — worth watching for drift against the Python reference.

---

## 2. Code quality & tech debt

### High priority

**Effectively zero automated test coverage.** There is exactly one file under any `src/test` — `CalculateTauTest.java` — and it is *not* a test: it has a `public static void main()` and builds a `JFrame`; it is a manual visualisation harness with no JUnit import and no assertions. Repo-wide there are **0** occurrences of `assertEquals/assertTrue/assertThat`. The core calculations (`CalculateSquareAttributes`, `CalculateTau`, `GenerateSquaresProcessor`, density/variability/tau metrics) have no regression protection. *Start with the pure numeric methods — small fixed inputs, known outputs — they are the highest-value, lowest-effort tests.*

**One very large method.** `runTrackMateOnRecording` (`RunTrackMateOnRecording.java`, ~281 lines, lines 106–431) orchestrates image loading, TrackMate setup, execution, cancellation, and export in a single block. Extract the phases into private methods.

**Swallowed exceptions on data-write paths.** 18 empty/`ignored` catch blocks exist; the concerning ones are in production writes — e.g. `WriteSquareOverride.java:103` swallows a `Files.createDirectories` failure with `catch (IOException ignored) {}`, so a later CSV write fails confusingly with no diagnostic. Route these through the existing `PaintLogger` at minimum.

**Hardcoded `/Users/hans/…` paths.** ~22 occurrences. Most are benign (inside `main()` methods of dev/regression tooling and Javadoc), but they make the entire `paint-regression` module unrunnable by anyone else or in CI. Move regression inputs to test resources or accept them as CLI args.

### Medium priority

- **Duplicate installers.** `GlycoPaintInstallerMac.java` (752 lines) and `GlycoPaintInstallerWindows.java` (646 lines) are parallel implementations sharing structure; extract a common base.
- **Regression module reinvents CSV parsing.** The comparators (`SquaresCsvComparatorPythonJava.java` ~1012 lines, `TracksCsvComparatorPythonJava.java` ~900 lines) hand-roll `split(",")`-based parsing and duplicate helpers, instead of reusing the solid shared `BaseTableIO`.
- **Other oversized methods** worth decomposing, prioritising the science path: `assignTracksToSquares` (~125 lines) and `generateSquaresForExperiment` (~99 lines) in `GenerateSquaresProcessor`; large constructors/paint routines in `SquareGridPanel` and `ViewerFrame`.
- **`printStackTrace()` in ~13 production sites** bypasses the proper `PaintLogger` (used in ~45 files) and dumps to raw stderr. Route through the logger.

### Low priority

- Large but mechanical domain classes (`Square`, `Recording`) with hand-written ~100-line `toString` methods.
- ~225 `System.out/err` calls, but ~200 are in dev/regression CLI tooling where stdout is the intended interface.
- Only 4 TODO/FIXME markers repo-wide and no large blocks of commented-out code — a genuine positive.

> **Correction to note:** an earlier pass flagged that the code uses Java `record` types (Java 16+) while the build targets Java 8. On verification this is **false** — there are no `record` type declarations; the matches were variables named `record` and `CSVRecord`. The code uses only Java 8 features (lambdas/streams, no `var`), so the Java 8 build configuration is **consistent**. (Java 8 being EOL is still a valid concern — see §4.)

---

## 3. Bugs & correctness

Only issues verified in the source are listed. The highest-value fixes are #1 and #2 (silent/partial data loss and a hard NPE in the override export path), then the threading hazards (#3/#4) and the `-1` sentinel polluting statistics (#5).

### High severity

1. **Importers silently return partial data on a parse error.** In `ImportRecordingExclude.java:197`, `ImportSquareOverride.java:212`, and `ImportRecordingOverride.java:205`, the `catch (Exception)` sits *outside* the row loop and the method then returns the list built so far. A single malformed row throws mid-loop, and rows accumulated up to that point are returned as if complete. Callers in `ExportOverridesFromViewer` only check `!isEmpty()` and apply the truncated set — silent data loss with no signal. *Fix: move try/catch inside the loop (skip+warn per row) or treat any failure as fatal.*

2. **Unguarded NPE when `readTracksTable` returns null.** `ExportOverridesFromViewer.java:246` calls `tracksTable.stringColumn("Recording Name")`, but `MainIOInterface.readTracksTable` catches all exceptions and returns `null`. `tracksTable` is used with no null check (unlike the sibling recordings-table path). A missing/failed `Tracks.csv` → NPE that aborts the export after in-memory squares were already filtered. *Fix: null-check immediately after the read and fail explicitly.*

3. **Off-EDT Swing access + unsynchronised state in the TIFF player.** `TiffMoviePlayer.java` playback loop (a raw `new Thread`) reads `speedSlider.getValue()` and `frame.isVisible()` off the EDT, and shares plain `boolean[] playing` / `int[] currentFrame` arrays with EDT listeners without `volatile`/synchronisation. This violates Swing's single-thread rule and creates a visibility hazard (pause may never be seen → thread never stops) plus a lost-update race on the frame index. Note `RecordingPlaybackController` already uses `volatile boolean playing` correctly — this class does not. *Fix: use a `javax.swing.Timer`, or make the flags `volatile`/atomic and snapshot the slider on the EDT.*

### Medium severity

4. **`PaintLogger` is not thread-safe** despite being called from worker threads (TrackMate runs and project dialogs spawn background threads that all log). Static `writer` / `initialised` / `justPrintedRaw` are non-volatile and unsynchronised; concurrent writes can interleave or corrupt log lines. *Fix: synchronise the log methods; mark `writer`/`initialised` volatile.*

5. **Missing values written as the literal `-1`, skewing statistics.** `TrackDataExporter.java:148–159` uses `roundOr(feature, n, -1)` — a null feature (e.g. a missing diffusion coefficient) is written as `-1`, a plausible-looking value. `CalculateSquareAttributes.java:214–228` then feeds these columns into `.median()/.max()/.sum()`, so `-1` sentinels enter the aggregates as if real data. (The sibling `roundTo` correctly returns NaN for null.) *Fix: use NaN/missing so Tablesaw excludes absent features from stats.*

6. **Non-perfect-square grid count silently truncated.** `GenerateSquaresProcessor.java:207,256` and `CalculateSquareAttributes.java:308` compute `gridSize = (int) Math.sqrt(numberOfSquaresInRecording)`. A non-perfect-square config (e.g. 30 → 5 → 25 squares) silently uses fewer squares, while `calculateSquareArea(n)` still divides by the original `n` — inconsistent per-square area. *Fix: validate perfect-square at config load, or derive from `gridSize²`.*

7. **Hand-rolled override CSV is unquoted and asymmetric.** `WriteSquareOverride.java` / `WriteRecordingOverride.java` write via string concatenation and read via `split(",", n)` with no quoting/escaping. A name containing a comma shifts fields → wrong key or `NumberFormatException` (caught by #1 → dropped row). Also `hasOverridesFor`/`replaceSquareOverrides` use `startsWith(prefix)` (not field-anchored, can over-match/over-delete), and the header check in `replaceSquareOverrides` is case-sensitive `.equals` while `loadExistingRows` uses `equalsIgnoreCase` — a case-mismatched header triggers a `lines.clear()`, wiping all existing overrides. *Fix: use a real CSV library both directions; compare by exact field equality; make header checks consistent.*

8. **Right-image NPE in the display updater.** `RecordingDisplayUpdater.java:116` calls `entry.getRightImage().getImage().getScaledInstance(...)` with no null guard, but `RecordingEntry.loadImage()` returns `null` for a corrupt/unsupported brightfield image. *Fix: null-check and show a placeholder.*

9. **Missing bounds guards in three `ViewerFrame` handlers.** `ViewerFrame.java:551` (and `onApplySquareControl` ~727, `handleCellAssignment` ~634) call `recordingEntries.get(currentIndex)` without the `isEmpty()/bounds` guard the other handlers have. After a filter that matches nothing, these buttons stay enabled → `IndexOutOfBoundsException`. *Fix: apply the same guard or disable the buttons.*

10. **NPE on null neighbour-mode during override apply.** `ImportRecordingOverride.java:165` calls `oldNeighbourMode.equals(...)` where `oldNeighbourMode` can be null, throwing *after* override values were already applied — leaving in-memory state half-updated. *Fix: `Objects.equals(...)`.*

### Low severity

- `CsvUtils.addCase` can leave orphan `.tmp` files when it `continue`s past an empty-header check after opening the temp writer.
- `Math.round(NaN)` returns 0, so `Miscellaneous.round` / `roundTo` would silently turn a NaN into `0.0` (latent — callers currently set NaN explicitly).
- Regression comparators use naive `split(",")` (dev tooling only).

### Verified *not* bugs

Density and area calculations use double division (not integer division); no `==` string-identity comparisons anywhere; override *imports* use header-name column lookup (robust to reordering); `BaseTableIO.writeCsv` correctly forces US locale to avoid decimal-separator issues.

---

## 4. Build & dependency health

**Version management is a strength.** All key versions are pinned in the parent `<properties>` + `<dependencyManagement>`; child modules omit versions almost everywhere, and there are no cross-module version conflicts. Plugin versions are pinned too.

**The real risks are structural:**

- **All-SNAPSHOT version.** The entire reactor is `0.0.138-SNAPSHOT` — no reproducible released coordinate; every build of "138" can differ. This is the top reproducibility issue. There is also no release pipeline (no `maven-release-plugin`, no artifact `distributionManagement`).
- **Docs-only CI.** The single GitHub workflow (`publish-javadoc.yml`) builds the Maven site with `-DskipTests` and deploys Javadoc to gh-pages. Nothing compiles, tests, or produces release artifacts on push/PR. There is no build/test gate.
- **EOL Java 8 toolchain, hard-locked.** `maven.compiler.source/target = 1.8` and the enforcer requires JVM `[1.8,1.9)` — builds fail on anything but Java 8. This is a deliberate Fiji-ecosystem constraint, but Java 8 is past end of free public updates and the enforcer actively blocks any migration until relaxed. (The code itself is genuinely Java-8-clean — see the correction in §2.)
- **Non-hermetic native packaging.** No `jpackage`; instead antrun scripts hand-build a macOS `.app` and copy it into `~/Applications/Glyco-PAINT`, and the Fiji plugin copies JARs into `/Applications/Fiji.app/plugins` during the build — build-time side effects that write into user/system dirs.
- **Two divergent fat-JAR mechanisms** (`maven-shade-plugin` for some modules, `maven-assembly-plugin` for others) — a mild smell with different merge semantics for `META-INF/services`.
- **Repository config gaps.** The OME/OMERO repo is duplicated across four modules instead of being in the parent; the Unidata (netCDF) repo is absent, which is a latent Bio-Formats resolution risk.
- **No dependency/vuln scanning** (no OWASP dependency-check, no Dependabot).

**Dependencies to review for a version bump** (CVE-prone lines; not asserting a live CVE): Bio-Formats **6.13.0** (7.x exists), PDFBox **2.0.30** (3.x current, 2.x has CVE history), commons-compress **1.26.2** (1.27+ exists), logback-classic **1.2.13** (legacy 1.2 line, kept only because Java 8 blocks 1.3+). ImageJ `ij` 1.54h, commons-csv 1.10.0, and scijava-common 2.94.1 are slightly stale but low-risk.

---

## 5. Recommended priorities

1. **Add JUnit assertions to the pure numeric methods** — `CalculateTau`, density/variability/tau in `CalculateSquareAttributes`. Highest value, lowest effort; protects the science.
2. **Fix the override export bugs** — #1 (silent partial data), #2 (tracksTable NPE), #7 (unquoted CSV / header-clear). These risk silent, hard-to-notice data corruption.
3. **Add a build+test CI gate** — a workflow that runs `mvn -B verify` on push/PR, so tests and compilation are actually exercised.
4. **Log instead of ignore** on the override-write directory-creation failures (#3 in code-quality / H3).
5. **Fix the viewer threading hazards** — drive `TiffMoviePlayer` from a Swing `Timer`; synchronise `PaintLogger`.
6. **Make the regression module CI-runnable** — parameterise the hardcoded `/Users/hans/` paths.
7. **Longer term** — plan a Java 17/21 migration path (relax the enforcer), move to non-SNAPSHOT release versions, and review the flagged dependency bumps.
