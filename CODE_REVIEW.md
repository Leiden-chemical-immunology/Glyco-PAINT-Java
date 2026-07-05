# Glyco-PAINT-Java — Code Review

**Date:** 2026-07-05
**Scope:** All modules — architecture, robustness, maintainability, simplification, tests
**Size:** ~34,000 LOC across 12 Maven modules, Java 8 baseline

---

## Executive summary

This is a well-organized multi-module project with a genuinely correct dependency direction (everything depends on `paint-shared-utils`; nothing depends "up"), consistent use of `PaintLogger`, and solid CSV schema validation in `BaseTableIO`. The build is centralized cleanly through the parent POM.

The three things holding it back are structural rather than superficial:

1. **A god-module.** `paint-shared-utils` has accreted domain objects, I/O, config, validation **and a full Swing dialog subsystem**. Because every module depends on it, headless consumers (generate-squares, regression, the Fiji plugin) transitively drag in UI code — and the I/O layer itself pops modal dialogs.
2. **A global mutable config singleton whose getters write to disk.** `PaintConfig` is process-wide state, and reading a missing key silently persists a default back to the JSON file. This makes behavior order-dependent and effectively untestable.
3. **Essentially zero automated tests.** A repo-wide search for `@Test` returns nothing. The one file under `src/test` is a `main()` with a Swing plot and no assertions. The 2,800-LOC `paint-regression` module is a golden-file comparison *app*, not a test suite, and never runs during `mvn test`.

None of this is a crisis — the science works and the code is readable. But the combination of (2) and (3) means there is currently no safety net under the numerical core (τ fitting, square generation), which for a research pipeline producing published results is the highest-priority gap to close.

The highest-value work, in order: **add a real JUnit suite over the pure numerical kernels**, then **break the UI out of `shared-utils`**, then **make config injectable and side-effect-free on read**.

---

## 1. Architecture

### A1 — HIGH — Swing UI lives inside the foundational module
`paint-shared-utils/.../dialogs/` (ProjectDialog, ProjectDialogController, 6 panels) and `utils/PaintConsoleWindow.java` — 11 files import `javax.swing`. The lowest layer every module depends on carries a full dialog subsystem, so CLI/headless consumers transitively bundle UI.
**Fix:** extract `dialogs/` and `PaintConsoleWindow` into a new `paint-ui-common` module (or fold into `paint-viewer`). Keep `shared-utils` UI-free.

### A2 — HIGH — The I/O layer calls Swing directly
`paint-shared-utils/.../io/ExperimentDataLoader.java:155` calls `JOptionPane.showMessageDialog(...)` on a bad squares layout. This same method runs from `GenerateSquaresHeadless` and worker threads, where it will hang or throw `HeadlessException`, and it couples persistence to presentation. (Verified in source.)
**Fix:** return the error via `ValidationResult` / throw a typed exception; let the UI layer decide whether to show a dialog. Same pattern in `ProjectPathResolver.java:107,127,167`.

### A3 — HIGH — `PaintConfig` is a global mutable singleton
`config/paintconfig/PaintConfig.java:60` — `static volatile INSTANCE` exposed through static `getString/getInt/getDouble/getBoolean`, used in 19 files. `instance()` lazily initializes against `user.home`. Process-wide state that is order-dependent, non-injectable, and impossible to isolate in a test.
**Fix:** introduce a `Config` interface, pass an instance via constructor/parameter, and keep the singleton only as a thin default provider.

### A4 — HIGH — Config getters have a write side-effect (read mutates disk)
`PaintConfig.getIntValue/getDoubleValue/getStringValue/getBooleanValue` call `setXxxValue(..., /*autoSave=*/true)` when a key is missing or invalid, persisting a default back to the JSON file during what looks like a pure read. (Verified in source, lines 135–211.) A getter that writes to disk is surprising, creates reentrancy/concurrency hazards, and makes reads non-idempotent.
**Fix:** separate "read with fallback" from "persist default." Never write during a get.

### A5 — MEDIUM — Core pipeline is static methods interleaving load → compute → write
`paint-generate-squares/.../calc/GenerateSquaresProcessor.java:95` (`generateSquaresForExperiment`) loads, computes, then writes — all `static`, reaching straight to the filesystem and `PaintConfig`. None of the orchestration is unit-testable without a real project directory.
**Fix:** split into a pure `SquareGenerationService` (takes loaded domain objects, returns tables) with thin I/O adapters at the edges.

### A6 — MEDIUM — Config flag read in a static initializer
`GenerateSquaresProcessor.java:84`: `static final boolean debug... = PaintConfig.getBoolean("Debug", ...)` is evaluated once at class load, before any project config is set — it silently reads/creates the default config in `user.home` and freezes the flag for the JVM lifetime.
**Fix:** read the flag at method-call time.

### A7 — MEDIUM — Domain objects coupled to the Tablesaw persistence library
`objects/Square.java:49,767` imports `tech.tablesaw.api.ColumnType` and embeds a `Column` enum carrying CSV headers + Tablesaw types. The domain model can't be used without Tablesaw on the classpath. Pragmatic (single source of truth for schema), but it belongs in the I/O layer.
**Fix:** move the schema enum into `SquaresTableIO` so `objects/` stays persistence-agnostic. Otherwise `Square` is a clean POJO — good.

### A8 — MEDIUM — ~26 `main()` entry points, several in library classes
`main()` appears in 26 non-test files, including `shared-utils` utilities (`CsvUtils`, `Miscellaneous`, `JarInfoLogger`, `JsonValidator`, `ValidationHandler`). `paint-launcher` has *no* module dependencies and launches apps out-of-process, so there is no in-code composition root.
**Fix:** remove `main()` from library classes (move ad-hoc probes to `paint-development-utils`); document the launcher's out-of-process model.

### A9 — LOW — Stale package/dependency banners in file headers
`CalculateTau.java:3` claims package `generatesquares.calc` but it's `paint.shared.utils`; `CsvComparatorRegression.java:3` / `RegressionRules.java:3` say `paint.regression.clean` but are `paint.regression`; a `GenerateSquaresProcessor` javadoc references `MainDataInterface` but the code uses `MainIOInterface`. Hand-maintained banners have drifted from reality.
**Fix:** delete the banners or generate them.

### A10 — LOW — `paint-generate-squares` pulls heavyweight imaging deps
Its POM pulls TrackMate, imglib2, bio-formats, ImageJ, though the square math needs none of them — bloating the fat JAR and blurring responsibility.
**Fix:** verify usage; if only the Fiji plugin needs them, move them there.

---

## 2. Robustness & error handling

### R1 — CRITICAL — Real TrackMate failure cause is swallowed in the core pipeline
`paint-fiji-plugin/.../trackmate/RunTrackMateOnExperiment.java:304`
```java
} catch (Exception e) {
    // Swallow exceptions here; outer code will detect failure via trackMateResults[0]
}
```
The genuine cause (OOM, corrupt image, config error) is discarded; the outer code only logs a generic "failed or timed out," making a real failure indistinguishable from a timeout with zero diagnostics.
**Fix:** capture the exception into a shared holder (`Throwable[] err`) and log `err[0]` with message/stack in the outer branch.

### R2 — HIGH — `PaintLogger.log` writes an unsynchronized shared `BufferedWriter`
`paint-shared-utils/.../utils/PaintLogger.java:199` writes to the static `writer` with no synchronization, yet `GenerateSquaresProcessor` runs on worker threads and the Fiji plugin logs from watchdog threads. Concurrent `write()/newLine()/flush()` can interleave, corrupt output, or throw.
**Fix:** make `log()` / `initialise()` `synchronized`, or use a thread-safe sink.

### R3 — HIGH — `ExperimentInfo(Map)` swallows parse errors, returns a half-built object
`objects/ExperimentInfo.java:109` uses raw `Integer.parseInt`/`Double.parseDouble`; on a malformed/empty field it catches, logs, and continues — leaving `concentration`/`threshold` at `0.0` and later fields unset. The caller (`RunTrackMateOnExperiment.java:280`) gets no signal and processes garbage.
**Fix:** parse per-field with explicit null/empty handling; throw a checked parse exception or return a status the caller checks.

### R4 — HIGH — Platform-default charset in CSV readers (encoding corruption)
`validate/ConditionConsistencyChecker.java:97` (`new FileReader(file)`) and `config/SweepConfig.java:80` use the JVM default charset, inconsistent with `AbstractFileValidator.java:115` which correctly forces UTF-8. Imaging metadata routinely contains non-ASCII (µ, greek probe names) — silent mis-decode on Windows.
**Fix:** `Files.newBufferedReader(path, StandardCharsets.UTF_8)`.

### R5 — MEDIUM — Modal dialog from headless/off-EDT code
`io/ExperimentDataLoader.java:155` (see A2) and `ProjectPathResolver.java:107,127,167` pop `JOptionPane` directly (not via `invokeLater`) from headless/worker contexts.
**Fix:** as A2; if a dialog is unavoidable, guard with `GraphicsEnvironment.isHeadless()` and wrap in `SwingUtilities.invokeLater`.

### R6 — MEDIUM — Unvalidated `getAsBoolean()` / `getAsJsonObject()` on external JSON
`config/SweepConfig.java:81,111` — `parseReader(...).getAsJsonObject()` throws if the file is a JSON array/primitive, and `entry.getValue().getAsBoolean()` throws if a sweep flag isn't a boolean.
**Fix:** check `isJsonObject()` and `isJsonPrimitive() && isBoolean()` before coercing; skip/report invalid entries.

### R7 — MEDIUM — Silently swallowed `Files.createDirectories` failure
`paint-viewer/.../override/square_override/WriteSquareOverride.java:101`
```java
try { Files.createDirectories(viewerPath); } catch (IOException ignored) {}
```
A permissions/read-only failure is invisible; the subsequent write fails with a confusing downstream error.
**Fix:** log or propagate.

### R8 — MEDIUM — `printStackTrace` instead of the logger (output lost in packaged app)
`GenerateSquares.java:90`, `TrackDataExporter.java:205`, `ViewerFrame.java:864`, `TiffMoviePlayer.java:126` write to `System.err`, bypassing `PaintLogger`'s file/console sink — often invisible in a bundled Fiji/app.
**Fix:** route through `PaintLogger` (ideally add an overload accepting a `Throwable`).

### R9 — MEDIUM — Global `System.setOut` redirect from a background thread
`paint-viewer/.../io/TiffMoviePlayer.java:106` replaces process-wide `System.out` to silence ImageJ and restores it after `IJ.openImage`. Any other thread printing in that window is swallowed; if loading throws before the restore line, stdout is never restored.
**Fix:** restore in a `finally` block; prefer ImageJ's own log redirection; avoid mutating global state from a worker thread.

### R10 — LOW — `System.exit` inside viewer library code
`paint-viewer/.../override/ExportOverridesFromViewer.java:104,162` hard-kills the JVM (and Fiji, if embedded) rather than throwing.
**Fix:** throw and let the top-level `main` translate to an exit code. (The `System.exit` calls in dev-utils, regression, and installers are genuine CLI entry points — fine.)

**Solid, not findings:** `BaseTableIO.readCsvWithSchema` does proper existence + header validation with descriptive `IOException`s; `ConfigStore.ensureLoaded` validates JSON, backs up invalid files, and regenerates defaults; most file I/O correctly uses try-with-resources; `loadExperiment` returning `null` is consistently null-checked by all callers.

---

## 3. Maintainability & simplification

### M1 — HIGH — Duplicated config defaults that disagree (latent bug)
`DefaultConfigLoader.java:45` seeds `MIN_REQUIRED_DENSITY_RATIO = 2.0`, but the corresponding `PaintConfig.getX(section, key, default)` call site in `GenerateSquaresConfig.java` passes a different inline fallback (`0.1`). Defaults live in two places — the JSON seeder and every accessor call site — and here they diverge.
**Fix:** make `DefaultConfigLoader` the single source of truth; have typed accessors read the default from it (or a shared constants map) rather than inline literals. Removes ~15 duplicated literals and a whole class of bug.

### M2 — HIGH — `getColumnHeaders()`/`getColumnTypes()` copy-pasted into all 4 TableIO subclasses
`SquaresTableIO.java:51`, `TracksTableIO`, `RecordingsTableIO`, `ExperimentInfoTableIO` — byte-identical modulo the entity enum name.
**Fix:** add a generic `headersOf(Class<E>, Function<E,String>)` helper (or a `SchemaColumn` interface implemented by each `.Column` enum) to `BaseTableIO`. Each subclass drops ~18 lines.

### M3 — HIGH — The two installer classes are ~90% duplicated
`GlycoPaintInstallerMac.java` (751 lines) and `GlycoPaintInstallerWindows.java` (646 lines) share near-identical `detectVersion`, `runInstaller`, `installFijiPlugin`, `writeZipEntry`, `copyDirectory`, `findPluginJar`, `askUserForFijiFolder`, `log`. Real differences are cosmetic plus macOS quarantine removal.
**Fix:** extract `AbstractGlycoPaintInstaller` (Swing UI + zip/copy/jar plumbing) with 2–3 abstract hooks. Eliminates ~500 lines.

### M4 — HIGH — The two CSV comparators duplicate their infrastructure
`SquaresCsvComparatorPythonJava.java` (1012) and `TracksCsvComparatorPythonJava.java` (900) duplicate `readCsv`, `escapeCsv`, `percentWithin` (identical) and the `optimizeTolerances` sweep loop. A third, `CsvComparatorRegression.java`, likely overlaps too.
**Fix:** pull the plumbing into a shared `CsvComparatorSupport` helper; keep only the genuinely different domain comparison per class.

### M5 — MEDIUM — Accessor-bloated data classes
`Square.java` (811) = 38 fields + ~72 getters/setters + embedded enum + big `toString`, one real method (`calculateSquareArea`). `Recording.java` (678) mirrors it (61 accessors). Not mixed-responsibility god classes — just boilerplate.
**Fix:** Lombok `@Getter/@Setter/@ToString`, or group the mutable numeric metrics into a small `SquareMetrics` value object. Lower priority (mechanical, low-risk).

### M6 — MEDIUM — `ViewerFrame.java` (881) is a genuine god UI class
34 methods spanning navigation, import/override, cell assignment, filtering, and enable/disable, with inline anonymous `WindowListener`s.
**Fix:** extract `RecordingNavigator`, `CellAssignmentController`, `ImportOverrideController`; ViewerFrame becomes thin wiring.

### M7 — MEDIUM — Hardcoded machine-specific paths in production `main()` drivers
`SweepFlattener.java:196` and `RunTrackMateOnProjectSweep.java:272` embed `/Users/hans/Paint Test Project` and `/Volumes/Extreme Pro/Omero` in production classes; ~23 `/Users/hans/...` literals project-wide.
**Fix:** move debug drivers to `src/test` or a `dev-drivers` sourceset; read paths from args/env.

### M8 — MEDIUM — Magic strings/thresholds in comparators instead of `constants/` + config
`SquaresCsvComparatorPythonJava.java` repeats `"Selected"`, `"Density Ratio"`, `"Tau"`, etc. 28+ times and re-implements selection thresholds (`dr >= 2.0 && var < 10.0 && r2 > 0.1`) that already exist in config constants.
**Fix:** reference `PaintStringConstants` and the config constants so validation stays in lockstep with production selection logic.

### M9 — MEDIUM — `System.out.println` in the production `validate/` package
`JsonValidator.java:174`, `ValidationHandler.java:222`, `SweepConfig.java:105`, `Miscellaneous.java:69`. The core modules (viewer/generate/fiji/omero) are clean, so this package is the outlier.
**Fix:** route through `PaintLogger`. (Regression/dev-utils `System.out` is fine — CLI tools.)

### M10 — LOW — Minor items
Config split across `config/` and `config/paintconfig/` adds navigation friction (L1); redundant `List` + `HashSet` of the same fields in the comparator (L2); an O(n²) lookup in `writeSelectedOverview` (`SquaresCsvComparatorPythonJava.java:756`) that should use a map (L3); a `Thread.sleep(150)` "file flush" hack (L4); `PaintTiming.java:45` `// TODO these should be JSON parameters` (L5).

---

## 4. Tests

### T1 — CRITICAL — The project has effectively zero automated tests
Repo-wide search for `@Test` / `org.junit` / `Assertions.` returns nothing. The only file under any `src/test` is `CalculateTauTest.java` — a `public static void main` with a Swing plot and console `printf`, no assertions, requires a human to eyeball a graph. This is the #1 quality gap.

### T2 — HIGH — The single "test" cannot fail and cannot run in CI
`CalculateTauTest.java:17` prints `Δ vs. Python` but never asserts it, and opens a `JFrame` (blocks/`HeadlessException` on CI). Test 2 even passes `expectedTau = Double.NaN`, disabling comparison.
**Fix:** convert to `@Test` with `assertEquals(expected, tau, 1e-6)`; move plotting to a manual `main`.

### T3 — HIGH — `paint-generate-squares` has no JUnit dependency
`junit-jupiter` is declared only in root `dependencyManagement` and `paint-regression`. Generate-squares' POM has no junit entry — which is *why* the "test" is a `main()`.
**Fix:** add `junit-jupiter` (test scope) to generate-squares and shared-utils POMs.

### T4 — HIGH — `paint-regression` (2793 LOC) is an app, not a test suite
All five classes are in `src/main` with `main()` methods; the assembly manifest points at `paint.regression.CompareAllSquares`, **a class that doesn't exist** (stale reference). It compares Java CSV output against pre-generated Python "gold" CSVs, logging diffs to `System.out`; it never runs during `mvn test` and asserts nothing.
**Fix:** keep it as a golden-file harness but wrap comparisons in JUnit assertions (fail the build on diff); fix the stale manifest mainClass.

### T5 — HIGH — Regression harness depends on out-of-band baselines and mutable global mode
`CsvComparatorRegression.java:44` `static boolean relaxedComparison = true` plus `RegressionRules` ignore-lists mean results depend on hidden global state and Python baselines not in the repo. The RELAXED set *ignores* scientifically meaningful columns ("Density Ratio", speed medians), so comparisons can silently pass while real numbers diverge.
**Fix:** make mode an explicit parameter, commit small baseline fixtures under `src/test/resources`, and don't ignore result-bearing columns by default.

### T6 — HIGH — The τ exponential-decay fitter is the highest-value untested logic
`utils/CalculateTau.java` — `fit()` (Levenberg–Marquardt), `initialGuess()`, `computeRSquared()`, `createFrequencyDistribution()` are pure, deterministic, and the single most important scientific kernel.
**Fix:** JUnit tests for (a) known synthetic exponential → τ within tolerance (values already in the harness: τ≈997.088, R²≈0.99954), (b) <2 distinct points → `TAU_NO_FIT`, (c) non-finite input → NaN handling, (d) R² below threshold → `TAU_RSQUARED_TOO_LOW`.

### T7 — HIGH — Square-grid generation is pure and untested
`GenerateSquaresProcessor.generateSquaresForRecording(...)` and `Square.calculateSquareArea(int)` are deterministic.
**Fix:** assert grid count = n, contiguous non-overlapping coverage of the image, correct row/col/number sequencing, and the non-perfect-square `n` edge case (`(int)Math.sqrt` silently truncates today — worth pinning).

### T8 — MEDIUM — Validators are untested
The 10 classes in `validate/` return `ValidationResult` from input — ideal unit targets.
**Fix:** table-driven tests feeding malformed/valid fixture CSV/JSON, asserting `ValidationResult`.

### T9 — MEDIUM — CSV/table round-trip I/O is untested
The `io/internal` TableIO classes map domain ↔ Tablesaw ↔ CSV — classic characterization-test targets, and free of the Swing coupling that makes `ExperimentDataLoader` hard to test.
**Fix:** round-trip tests (`Square` → table → CSV → table → `Square`) against `@TempDir`.

### T10 — MEDIUM — Config system untestable in isolation
The `PaintConfig` singleton and read-writes-disk behavior (A3/A4) pollute global state and the filesystem, blocking parallel tests. `ConfigStore` is the testable piece (it takes a `Path` + `Gson` in its constructor).
**Fix:** after introducing a `Config` interface, unit-test `ConfigStore` against `@TempDir`, covering the invalid-file backup path.

### T11 — LOW — No test convention or CI gate
The lone test's package (`generatesquares.calc`) diverges from the production `paint.` prefix. GitHub Actions is configured but runs no tests.
**Fix:** mirror production packages under `src/test/java`; add a surefire-backed `mvn test` gate.

---

## Recommended action plan (highest value first)

**Quick wins (low risk, high payoff):**
1. Fix M1 (disagreeing config defaults — a live bug) and R4 (UTF-8 in the two `FileReader` sites).
2. Add `junit-jupiter` to `shared-utils` + `generate-squares` (T3), then convert `CalculateTauTest` to asserting JUnit tests using its own known values (T2/T6). Cheapest, highest scientific payoff.
3. Synchronize `PaintLogger.log` (R2) and unswallow the TrackMate exception (R1).

**Structural (schedule deliberately):**
4. Test the pure kernels: square grid math (T7), `ConfigStore` against `@TempDir` (T10), validators (T8), TableIO round-trip (T9).
5. Extract UI out of `shared-utils` (A1) and remove the Swing call from the I/O layer (A2).
6. Introduce a `Config` interface; stop getters from writing to disk (A3/A4).
7. Split `GenerateSquaresProcessor` into a pure service + I/O adapters (A5) — this unlocks most remaining testability.
8. Wrap `paint-regression` in JUnit assertions with committed fixtures and fix its stale manifest (T4/T5).

**Deduplication (mechanical):**
9. Collapse the two installers (M3, ~500 lines), the 4 TableIO header/type methods (M2), and the CSV comparator plumbing (M4).

---

*Line numbers reference the state of the repo on 2026-07-05. Key claims (config getters writing defaults to disk, the headless Swing dialog, the absence of `@Test` in the codebase) were verified directly against source.*
