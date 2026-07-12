# Glyco-PAINT-Java — Refactoring Progress

**Date:** 2026-07-12
**Branch:** `develop` (`main` untouched at tagged `stable-baseline-0.0.138`)
**Build status:** `mvn clean install` green across all 13 modules; `mvn test` green
(~60 unit tests + the end-to-end regression gate).

This document tracks what has been done in the refactoring effort and what
remains. It is a companion to `CODE_REVIEW.md` (the original findings).

---

## Guiding approach

Every change followed the same low-risk loop: **build a safety net first, then
change code under it, and verify with a green build.** Behaviour-changing edits
were pinned with red→green tests where possible; changes to code the tests can't
reach (e.g. the TrackMate/Fiji flow) were kept strictly additive and reasoned
about explicitly. Each change is a small, individually revertible commit.

Three levels of protection now exist:

- **Unit tests** over the pure logic of `paint-shared-utils`.
- An **end-to-end regression gate** that runs the real Generate Squares pipeline
  on a committed golden master and asserts every numeric and boolean field of
  every square (and every recording) is reproduced exactly.
- **CI** that actually runs both on every push and pull request.

---

## Done

### Safety net (tests + CI)
- Unit tests for `paint-shared-utils` pure logic: `CalculateTau` fitter, `Square`
  geometry, `BooleanUtils`, `ValidationResult`, config backfill / no-write-on-read.
- CSV round-trip + schema tests for all four TableIO classes.
- **End-to-end regression gate**: runs the real pipeline on a committed
  golden-master project (`reference-project/221012`) and compares **Squares.csv
  and Recordings.csv** field by field, keyed by Unique Key. Exact by default;
  `-Dpaint.rules=relaxed` for tolerance rules; `-Dpaint.updateGolden=true` to
  accept an intentional change (with a timestamped backup of the previous golden).
- The gate **pins the run to the committed `Paint Configuration.json`**, so the
  golden master is reproducible instead of silently inheriting whatever
  `DefaultConfigLoader` currently seeds.
- **CI (`.github/workflows/build-and-test.yml`)** runs `mvn test` headless on
  every push/PR. Before this, ~60 tests and the gate existed but nothing ever ran
  them — the safety net was not load-bearing.

### Bugs fixed
- **Headless logging crash (found immediately by the new CI).** `PaintLogger` →
  `PaintConsoleWindow` unconditionally constructed a Swing `JFrame`, so *any*
  headless run (CI, server, headless Fiji) threw `HeadlessException` on the first
  log line — and because the console call precedes `writeLineToFile`, the file log
  was lost too. The console is now a no-op when `GraphicsEnvironment.isHeadless()`.
- **Diverging density-ratio default**: `GenerateSquaresConfig` fell back to `0.1`
  while `DefaultConfigLoader` seeded `2.0`. Aligned, pinned by a red→green test.
- **Divergent TrackMate defaults**: radius, splitting/merging distance, linking
  cost, subpixel — call-site fallbacks disagreed with the (correct) seeds.
- Windows installer: a swallowed `copyDirectory` failure reported false success;
  a `FileSystemView.getDefaultDirectory` crash.

### Configuration
- Reads no longer write to disk (removed the getter side-effect and its
  concurrency hazard); pinned by `PaintConfigNoWriteOnReadTest`.
- Config **self-completes** at load via `backfillMissing`; defaults live in one
  `buildDefaults()` source of truth.
- A missing `Paint Configuration.json` is created with defaults and a logged
  warning, instead of aborting with an "Invalid Project Path" dialog.
- Plot flags (`TAU_FITTING_PLOTS`, `BACKGROUND_PLOTS`) default to **false** and are
  homed in `GenerateSquaresConfig`. `Run Generate Squares After` defaults to
  **false** and is homed in `TrackMateConfig`.
- Dropped non-functional sweep parameters and dead constants.

### Robustness (R1–R10 from the review — complete)
- **R1** — TrackMate failures log the real cause with a stack trace.
- **R2** — `PaintLogger` file writes are thread-safe.
- **R3** — `ExperimentInfo(Map)` fails fast on a malformed row.
- **R4** — CSV readers use explicit UTF-8, not the platform default charset.
- **R5 / A2** — the data loader no longer pops a modal dialog (which hung the
  headless pipeline).
- **R6** — `SweepConfig` parses defensively.
- **R7** — a failed `Viewer` directory creation is logged, not swallowed.
- **R8** — `PaintLogger.error(String, Throwable)`; app-code `printStackTrace()`
  sites route through it.
- **R9** — `TiffMoviePlayer` restores redirected stdout in a `finally`.
- **R10** — GUI-called `exportOverrides` throws instead of `System.exit`.

### Architecture / maintainability
- **A1 — the base layer is now UI-free.** All Swing was extracted from
  `paint-shared-utils` into a new **`paint-ui-common`** module: `PaintConsoleWindow`
  (→ `paint.ui.console`), `ProjectDialog` and `ProjectPathResolver`
  (→ `paint.ui.dialogs`), and the seven dialog panels (→ `paint.ui.dialogs.project`).
  `javax.swing` imports in `paint-shared-utils`: **10 → 0**.

  The one piece of core code that reached into the UI — `PaintLogger` calling
  `PaintConsoleWindow` — was fixed by **inverting the dependency**: `PaintLogger` now
  exposes a `Sink` interface and knows nothing about Swing; the console *registers
  itself* as a sink when a GUI app creates it. A headless run never registers one, so
  no UI class is loaded on that path at all — the headless crash is now structurally
  impossible rather than guarded against. The file write was also moved *ahead* of the
  sink call, so a misbehaving UI can no longer cost us the log.

  Only the three modules that actually show an interface (`paint-fiji-plugin`,
  `paint-viewer`, `paint-generate-squares`) depend on `paint-ui-common`. Headless
  consumers — the pipeline, the regression gate, `paint-compare`, CI — no longer carry
  Swing at all.
- **A6** — the config flag read in a static initializer is gone. The track-assignment
  CSV dump is now a `-Dpaint.debug.dumpTrackAssignmentCsv` switch, read at call
  time; the whole `Debug` config section was removed (no debug flag gets a JSON key).
- **A5 (partial)** — the pure per-recording compute is extracted into
  `SquareGenerationService`, with an isolated unit test.
- **M4 — done by deletion.** The Python-vs-Java comparators were dropped
  (~1,900 lines of hand-rolled CSV parsing and duplicated plumbing), along with
  their hardcoded `/Users/hans` paths and the orphaned 5.7 MB `reference-case`
  data. `paint-regression` was renamed **`paint-compare`** and is now purely the
  tested comparison engine behind the gate; its CLI was removed.
- Deduplicated the four copy-pasted TableIO schema helpers.
- Build hygiene: all shade/assembly overlap, compiler and javadoc warnings
  eliminated. `paint-generate-squares` now publishes a thin jar with the runnable
  fat jar attached as `-standalone`, so downstream fat jars stop re-bundling it.

---

## Remaining

### Consciously deferred
- **A3 — config singleton.** `PaintConfig` is a global static singleton. Reviewed
  and deliberately left as-is: its harmful behaviours (read-writes-disk,
  testability) are addressed, and config is testable via `reinitialise(tempDir)`.
  Full DI is a large, invasive change for a smell that isn't causing bugs.

### Architecture
- **A5 (remainder)** — the pipeline still interleaves load → compute → write in
  static methods outside the extracted service.
- **A7–A10** (minor) — domain objects coupled to Tablesaw; `main()` in library
  classes (5 in shared-utils); stale package/dependency banners; `paint-generate-squares`
  pulling heavyweight imaging deps it may not need.

### Robustness / correctness
- **M9** — 6 `System.out.println` remain in the production `validate/` package,
  bypassing `PaintLogger` (invisible in a packaged app).
- **M7** — hardcoded `/Users/hans` paths remain in main code, including production
  `paint-fiji-plugin` (`SweepFlattener`, `RunTrackMateOnProjectSweep`).
- **Fragile release-jar selection.** `ReleaseNewVersion` picks the installer with
  `contains("installer") && endsWith(".jar")` — a loose filter that would grab the
  wrong jar if a second match ever appeared. This is why `installer-macos` keeps
  `appendAssemblyId=false` (and its one harmless shade warning).
- **Stale exec `mainClass`** in `paint-development-utils`: `utils.BuildWindowsExecutables`
  no longer exists.

### Tests still worth adding
- The 10 `validate/` classes (pure `ValidationResult` in → out; ideal targets).
- `ConfigStore` against `@TempDir`.
- The grid generator's non-perfect-square `n` edge case — `(int)Math.sqrt` silently
  truncates today; worth pinning.

### Maintainability (larger)
- **M3** — the macOS and Windows installers are ~90% duplicated (~1,400 lines).
- **M6** — `ViewerFrame` (881 lines) is a god UI class.
- **M5** — `Square` / `Recording` are accessor-bloated.

### Known smells (noted, low priority)
- `BACKGROUND_PLOTS` / `TAU_FITTING_PLOTS` constants do double duty as both a
  config-flag key and a plot-output directory name — fragile coupling.
- `Density Ratio Ori` is computed and written but never used in Java logic.
- `paint-compare`'s package nesting is `paint.compare.compare` — worth flattening.

---

## Housekeeping
- `main` remains at `stable-baseline-0.0.138` — a clean fallback point.
- `.idea/` is untracked; `paint-launcher/README.md` is still untracked.
