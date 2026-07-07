# Glyco-PAINT-Java — Refactoring Progress

**Date:** 2026-07-07
**Branch:** `develop` (19 commits ahead of `main`; `main` untouched at tagged `stable-baseline-0.0.138`)
**Build status:** `mvn clean install` green across all 13 modules.

This document tracks what has been done in the refactoring effort and what
remains. It is a companion to `CODE_REVIEW.md` (the original findings).

---

## Guiding approach

Every change followed the same low-risk loop: **build a safety net first, then
change code under it, and verify with a green build.** Behaviour-changing edits
were pinned with red→green tests where possible; changes to code the tests can't
reach (e.g. the TrackMate/Fiji flow) were kept strictly additive and reasoned
about explicitly. Each change is a small, individually revertible commit.

Two levels of protection now exist:

- **Unit tests** over the pure logic of `paint-shared-utils`.
- An **end-to-end regression gate** that runs the real Generate Squares pipeline
  on a committed golden master and asserts every numeric and boolean field of
  every square is reproduced exactly.

---

## Done

### Safety net (tests)
- Unit tests for `paint-shared-utils` pure logic: `CalculateTau` fitter, `Square`
  geometry, `BooleanUtils`, `ValidationResult`. *(6c6b3e64)*
- CSV round-trip + schema tests for all four TableIO classes. *(fbab411f)*
- **End-to-end regression gate**: runs the real pipeline on a committed
  golden-master project (`reference-project/221012`) and compares output to the
  golden `Squares.csv`. *(ed15148a, b21a6495)* Later expanded to compare **every**
  numeric column (27) plus all three boolean flags, keyed by Unique Key.
  *(93bacf1b)*

### Bugs fixed
- **Diverging density-ratio default**: `GenerateSquaresConfig` fell back to `0.1`
  while `DefaultConfigLoader` seeded `2.0`. Aligned to `2.0`, pinned by a
  red→green test. *(3867787d)*
- **Divergent TrackMate defaults**: radius, splitting/merging distance, linking
  cost, subpixel — call-site fallbacks disagreed with the (correct) seeds. Aligned
  all five. *(3b1079bb)*

### Configuration robustness (the "A4 / Option 2" work)
- Reads no longer write to disk (removed the surprising getter side-effect and
  its concurrency hazard). *(637a9b19)*
- Config now **self-completes** at load via `backfillMissing` — any absent
  default key is added (existing values preserved), so a config file can't
  silently miss a known tunable. Defaults live in one `buildDefaults()` source of
  truth. *(637a9b19)*
- Seeded a real but previously-unseeded tunable (`Min Tracks to Calculate`) and
  removed dead/remnant config: two never-read debug seeds and their constants, and
  stale keys trimmed from the committed reference config. *(46e42732, c7e5bfec,
  f01631d7)*
- Dropped two **non-functional sweep parameters** (`Fraction of Squares to
  Determine Background`, `Exclude zero DC tracks from Tau Calculation`) that the
  pipeline never read, plus their dead constants. Named the fixed background
  fraction (`BACKGROUND_SQUARES_FRACTION = 0.1`) instead of a magic literal.
  *(9332384d, 3673908f)*

### Maintainability
- Deduplicated the four copy-pasted TableIO schema helpers into one generic
  `BaseTableIO.newEmptyTable(...)`. *(eac48f2f)*

### Robustness (R1–R4 from the review)
- **R4** — `ConditionConsistencyChecker` and `SweepConfig` now read with explicit
  UTF-8 instead of the platform default charset. *(7595aa55)*
- **R2** — `PaintLogger` file writes are now thread-safe (single synchronized
  helper; the shared writer can no longer be corrupted by concurrent threads).
  *(8e40ab0e)*
- **R3** — `ExperimentInfo(Map)` fails fast on a malformed row instead of leaving
  a half-built object; the sole caller already skips bad records. Netted by a new
  test. *(717ed1c0)*
- **R1** — TrackMate failures now log the real cause instead of swallowing the
  exception behind a generic "failed or timed out". *(3fc18e47)*

---

## Remaining (from `CODE_REVIEW.md`, not yet done)

### Consciously deferred
- **A3 — config singleton.** `PaintConfig` is a global static singleton used in
  ~19 files. Reviewed and **deliberately left as-is**: its harmful behaviours
  (read-writes-disk, testability) are already addressed, config is testable via
  `reinitialise(tempDir)`, and full dependency injection is a large, invasive
  change for a smell that isn't causing bugs. Revisit only if a concrete need
  appears (concurrent projects, parallel test isolation).

### Architecture
- **A1 / A2** — Swing UI lives inside `paint-shared-utils`, and the I/O layer
  (`ExperimentDataLoader`, `ProjectPathResolver`) pops `JOptionPane` directly,
  which is unsafe from headless/worker contexts. Extract UI out of the base
  module; return errors instead of showing dialogs from I/O.
- **A5** — the Generate Squares pipeline is static and interleaves load → compute
  → write, making the core hard to unit-test in isolation (the gate covers it
  end-to-end, but not in pieces). Split into a pure service + I/O adapters.
- **A6–A10** (minor) — config flag read in a static initializer; domain objects
  coupled to Tablesaw; many `main()` entry points in library classes; stale
  package/dependency banners in file headers; `paint-generate-squares` pulling
  heavyweight imaging deps it may not need.

### Robustness (R5–R10)
- Modal dialogs from headless/off-EDT code (overlaps A2).
- Unvalidated JSON coercion in `SweepConfig` (`getAsBoolean` / `getAsJsonObject`
  can throw on malformed input).
- Swallowed `Files.createDirectories` failure in `WriteSquareOverride`.
- `printStackTrace`/`System.err` instead of `PaintLogger` in several places
  (R8) — output can be lost in a packaged app. Consider a `PaintLogger` overload
  that accepts a `Throwable`.
- Global `System.setOut` redirect from a worker thread (`TiffMoviePlayer`, R9) —
  restore in a `finally`.
- `System.exit` inside viewer library code (`ExportOverridesFromViewer`, R10).

### Maintainability (M3–M9)
- **M3** — the macOS and Windows installers are ~90% duplicated (~500 lines).
- **M4** — the two large CSV comparators in `paint-regression` duplicate their
  plumbing; also carry hardcoded `/Users/hans/...` paths.
- **M5** — `Square` (811 lines) and `Recording` (678) are accessor-bloated;
  consider Lombok or grouping metrics into value objects.
- **M6** — `ViewerFrame` (881 lines) is a god UI class; extract navigation,
  cell-assignment, and import-override controllers.
- **M7** — hardcoded machine-specific paths in production `main()` drivers.
- **M9** — `System.out.println` in the production `validate/` package.

### Tests still worth adding
- Validators (`validate/` package), `ConfigStore` against `@TempDir`, and TableIO
  wiring beyond round-trips.
- A tracks/recordings regression comparison (the gate currently covers squares).

### Known smells (noted, low priority)
- `BACKGROUND_PLOTS` / `TAU_FITTING_PLOTS` constants do double duty as both a
  config-flag key and a plot-output directory name — fragile coupling.
- `Density Ratio Ori` is computed and written but never used in Java logic (it
  exists for Python output parity, validated in `paint-regression`).

---

## Housekeeping
- `develop` is local-only. Back it up with `git push -u origin develop` when
  ready.
- `main` remains at `stable-baseline-0.0.138` — a clean fallback point.
