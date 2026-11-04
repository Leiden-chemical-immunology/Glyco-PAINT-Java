# paint-regression — What each file does and how to use it

*2026-07-03 · Reference guide to the five source files in `paint-regression/src/main/java/paint/regression/`.*

## Purpose of the module

The module compares two CSV files produced by the PAINT pipeline and reports where they differ. It is used for two distinct jobs:

- **Regression checking** — compare a freshly generated Java output against a previously saved Java "gold standard," to confirm a code change did **not** alter the results. (`CsvComparatorRegression` + `RegressionRules` + `RegressionLogger`.)
- **Cross-implementation validation** — compare the **Python** PAINT output against the **Java** output, to confirm the Java rewrite reproduces the original. (`SquaresCsvComparatorPythonJava`, `TracksCsvComparatorPythonJava`.)

All five files are currently **run as `main()` programs from the IDE**, and the input/output paths are **hardcoded** (mostly under `/Users/hans/...` and `~/Downloads/Validate/...`). None of them assert pass/fail — they print reports and difference counts that you read by eye. (See "Current limitations" at the end.)

| File | Role | Runnable? | Reads | Writes |
|---|---|---|---|---|
| `CsvComparatorRegression` | Java-vs-Java regression comparator | Yes (`main`) | 2 CSVs (baseline, test) | console + timestamped log |
| `RegressionRules` | Tolerance/ignore configuration | No (library) | — | — |
| `RegressionLogger` | Dual console+file logging | No (library) | log dir | log file |
| `SquaresCsvComparatorPythonJava` | Python-vs-Java squares validation | Yes (`main`) | Python + Java squares CSV | 6 CSVs in `~/Downloads/Validate/Squares` |
| `TracksCsvComparatorPythonJava` | Python-vs-Java tracks validation | Yes (`main`/`run`) | Python + Java tracks CSV | 5 CSVs in `~/Downloads/Validate/Tracks` |

---

## 1. `RegressionRules.java` — the comparison rulebook

A configuration/helper class (no `main`, not run directly). It defines **how** `CsvComparatorRegression` decides whether two values are "the same." Everything here comes in two profiles, **STRICT** and **RELAXED**, selected by a boolean.

What it holds:

- **Ignored columns** (`IGNORE_COLUMNS_STRICT` / `_RELAXED`) — columns never compared. Both profiles ignore `Run Time` and `Time Stamp` (these legitimately vary run to run). RELAXED additionally ignores `Row/Column/Label Number`, several speed columns, and `Density Ratio`.
- **Ignore-case fields** (`IGNORE_CASE_FIELDS`) — `Visible`, `Square Manually Excluded`, `Image Excluded` are compared case-insensitively.
- **Numeric tolerances** (`RELATIVE_TOLERANCE_STRICT` / `_RELAXED`) — per-column allowed relative error. STRICT is tight (`Tau` 0.0001), RELAXED is loose (`Tau` 0.01, i.e. 1%).
- **Rounding** (`ROUNDING_STRICT` / `_RELAXED`) — per-column decimal places used before comparing.
- **Equality helpers** — `valuesEqual` (rounds to 3 decimals), `numericEqualWithTolerance` (rounding then relative tolerance), plus three "leniency" rules: `emptyAndZeroEquiv` (treats empty ≡ `0`/`-1`/`-2`/`-3`), `numericMissingSkipDifference` (a value present on one side and missing on the other is **not** reported), and `correctedValueIfTrackDependent` (rescales `Density`/`Density Ratio Ori` by the old/new track-count ratio).

**How you use it:** you don't run it — you *edit* it. To change what counts as a difference (add a column to ignore, tighten a tolerance), change the sets/maps here. Note the three leniency rules apply even in STRICT mode, so STRICT is not as strict as the name suggests (see the Regression Module Review for why that hides some real changes).

## 2. `RegressionLogger.java` — logging to console and file

A small helper (no `main`). `CsvComparatorRegression` uses it to write every line of a run both to the console and to a timestamped log file `paint-regression-YYYYMMDD-HHmmss.log` in a directory you choose. Its one clever extra is `compareLatestLogWithPrevious(logDir)`: it finds the two most recent log files in a directory and diffs them line by line, so you can see whether **this** regression run produced a different report from the **previous** one.

**How you use it:** indirectly. `CsvComparatorRegression.main` calls `enableDualLogging(logPath)` to turn on the file log and `compareLatestLogWithPrevious(logPath)` at the end. You only interact with it by choosing the log directory in the comparator's `main`.

## 3. `CsvComparatorRegression.java` — the Java-vs-Java regression comparator

This is the one that matters for your "did my change alter the results?" question. It compares a **baseline** CSV against a **test** CSV, key by key, using `RegressionRules` for tolerances and `RegressionLogger` for output.

How it works:

- Reads both files into rows, then groups rows by a **key = `Recording Name` + `Square Number`** (`buildKey` / `toMultiMap`).
- For each key it compares the matching rows field by field, skipping ignored columns and applying the tolerance rules. It reports `NUMERIC DIFFERENCE`, `TEXT DIFFERENCE`, `Missing in NEW`, or `Extra in NEW`, then prints a summary grouped by recording and square, and a total difference count.
- `main` runs a **STRICT** comparison of two hardcoded pairs — `Squares.csv` and `Recordings.csv` — comparing `/Users/hans/Paint Test Project/221012/…` (the freshly generated files) against `paint-regression/src/main/resources/221012 reference/…` (the saved gold standard), logging to `/Users/hans/Downloads/validate/logs strict`.

**How you use it:** in `main`, set `baseline` to your saved-good CSV and `testfile` to the newly generated CSV, choose STRICT (`setRelaxedComparison(false)`) for regression, run the class, and read the difference count (0 = no regression). Works well for **Squares** and **Recordings** because their key is unique per row. **Do not** use it for **Tracks** — a square holds many tracks, so within a key it compares tracks *positionally*, and any change in row order shows up as differences (this is the ordering-vs-real-difference problem; the fix is to key tracks on the `Unique Key` column).

## 4. `SquaresCsvComparatorPythonJava.java` — Python-vs-Java squares validation

A one-shot **diagnostic** (has `main`) for checking that the Java squares output matches the original Python output. Because the two come from different implementations with different schemas, it does more than compare — it **aligns** them first.

Pipeline inside `main`:

1. Reads the Python `All Squares.csv` and the Java `Squares.csv` (hardcoded paths).
2. Normalizes each into a shared schema (`normalizeOld` / `normalizeNew`).
3. Computes a **shared numeric precision** per field (so it compares at the coarser of the two files' precisions).
4. Writes the normalized versions back out, re-reads them (to lock in that precision), then compares.
5. Produces a set of report CSVs in `~/Downloads/Validate/Squares/`: `Old Normalized`, `New Normalized`, `Comparison`, `Detailed`, `Selected Overview`, and `Tolerance Optimization` (which suggests how tight a tolerance the data could pass at).

**How you use it:** edit `oldCsv` (Python) and `newCsv` (Java) at the top of `main`, run the class, and inspect the CSVs in `~/Downloads/Validate/Squares`. It's for understanding *how close* the two implementations are and *what tolerance* is realistic — not a pass/fail gate.

## 5. `TracksCsvComparatorPythonJava.java` — Python-vs-Java tracks validation

The most elaborate file. It validates Python-vs-Java **tracks**, where there is **no stable track identity** across implementations, so it can't just line up rows — it **matches** them by their attributes.

How it works (`run(oldCsv, newCsv)`):

1. Reads both files, normalizes recording names (strips a `-threshold-N` suffix) and column-name variants, sorts by recording + track id.
2. Maps each old recording to the closest new recording by name.
3. **Phase 1 — strict matching** (`findMatches`): within a recording, for each old track it finds new tracks whose integer fields (`Square Nr`, `Nr Spots`, `Nr Gaps`, `Longest Gap`) match **exactly** and whose numeric fields (duration, displacement, speed, distance, X/Y, confinement) are within fixed absolute tolerances. A single clean match is "perfect"; already-used new tracks are excluded (greedy).
4. **Phase 2 — diagnostic scoring** (`findBestCandidate`): for tracks left unmatched, it computes an RMS "distance" to every candidate and reports the best one under a score cap — so you can see *how far off* the closest track was.
5. Runs a tolerance sweep, then writes 5 report CSVs to `~/Downloads/Validate/Tracks/`: `Old Normalised`, `New Normalised`, `Comparison`, `Comparison Diagnostics`, `Comparison Summary`.

**How you use it:** call `run(oldCsv, newCsv)` (from its `main`) with the Python and Java tracks files, run it, and read the summary + diagnostics CSVs to see how many tracks matched perfectly, matched reasonably, or couldn't be matched. It's a **diagnostic report**, not a pass/fail gate. Known rough edges: the greedy assignment can leave tracks falsely unmatched, and the exact-integer gate on `Nr Spots`/`Nr Gaps` means a single spot difference (common with TrackMate's nondeterminism) breaks an otherwise-good match.

---

## How to run them

All four runnable entry points are `public static void main` classes. From IntelliJ, open the class and Run it. There is currently no Maven `exec` wiring, so command-line running would require adding the `exec-maven-plugin` to `paint-regression/pom.xml` (e.g. `mvn -pl paint-regression exec:java -Dexec.mainClass=paint.regression.CsvComparatorRegression`).

Before running any of them you must edit the hardcoded input/output paths near the top of each `main` to point at your own files.

## Current limitations (and where they're addressed)

- **Hardcoded `/Users/hans/…` and `~/Downloads/Validate/…` paths** — only runnable on the original author's machine without editing.
- **No assertions** — they print reports; "acceptable or not" is a human judgement each run.
- **Track comparison ordering** — `CsvComparatorRegression` keys tracks on `Recording + Square`, which isn't unique per track, so it compares tracks positionally and produces ordering false-positives. The `Tracks.csv` already has a `Unique Key` column that fixes this.
- **STRICT mode leniency** — `emptyAndZeroEquiv`, `numericMissingSkipDifference`, and `correctedValueIfTrackDependent` mask some real changes even in STRICT mode.

These, and a proposed unified redesign, are detailed in `doc/Regression Module Review 2026-07.md`.
