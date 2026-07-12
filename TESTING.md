# What the tests check

This is a plain-language guide to the automated tests: what each one actually protects, and —
just as important — what it does not. Around 120 tests run on every build, and on every push via
GitHub Actions.

To run them all:

```bash
mvn clean test
```

The point of a test here is not to prove the science is right. It is to make sure that a change
made next month does not quietly break something that worked today. Most of the tests below exist
because something *did* break, or came close to it.

---

## The one that matters most: the golden master

`GenerateSquaresRegressionGateTest`

This runs the whole Generate Squares pipeline on a stored sample project and compares every number
it produces against a saved "known good" result. If a change alters a single tau, R², density or
visibility flag anywhere, this test fails and names the column and row that moved.

This is the safety net that lets everything else be changed with confidence. It answers the only
question that really matters: *did my change alter the scientific output?*

If a change is *supposed* to alter the output, the golden result has to be deliberately re-blessed
(`-Dpaint.updateGolden`) — which means such a change can never happen by accident or go unnoticed.

## What "the same" means: the comparison engine

`TableComparerTest`, `PaintStrictComparatorTest`, `CsvSourceTest` — 20 tests

The golden-master test is only as trustworthy as its notion of "identical", so the comparison
engine is itself tested. These pin down that:

- Rows in a different order are still the same data (order is not meaning).
- Two numbers that differ only in the last decimal place count as equal; a real difference does not.
- `NaN` matches `NaN`, an empty cell matches a zero, and `TRUE` matches `true`.
- Columns that change every run by design — timestamps, run durations — are ignored rather than
  reported as differences every single time.
- Quoted commas inside a CSV field are read as one field, not two.

Without these, the golden gate would either cry wolf constantly or, worse, stay silent when it
should not.

## The mathematics

`CalculateTauFitTest` (5), `SquareGeometryTest` (4), `GridSizeTest` (3)

The tau fit is checked against a curve whose answer is known in advance, and — equally important —
it is checked to *fail safely*: too few points, null input, or mismatched arrays return `NaN`
rather than a confident, wrong number.

The geometry tests confirm that the squares actually tile the image: their areas sum to the whole
image, and each square's size follows from the number of squares. `GridSizeTest` checks every grid
size offered in the dropdown (5×5 up to 40×40), because the Viewer once rejected perfectly valid
25×25 projects — the list of "allowed" layouts had fallen out of step with the list users could
actually choose from.

## Reading and writing the data files

`SquaresTableIoRoundTripTest`, `TracksTableIoRoundTripTest`, `ExperimentInfoTableIoRoundTripTest`,
`TableIoSchemaTest` — 8 tests

A "round trip" test writes an object out to CSV, reads it back, and checks that nothing changed on
the way. This catches the whole family of bugs where a column is written in one order and read in
another, and two values quietly swap places.

The schema tests fix the exact columns of Squares, Tracks, Recordings and Experiment Info. If
someone adds, removes or renames a column, they find out immediately rather than when an old
project fails to open. A file with the wrong header is rejected outright instead of being read as
nonsense.

## Configuration

`ConfigStoreTest` (4), `PaintConfigBackfillTest`, `PaintConfigNoWriteOnReadTest`,
`GenerateSquaresConfigDensityRatioTest` (2), `SweepConfigTest` (3)

These pin down how the application behaves when the configuration file is missing, damaged or
incomplete:

- No configuration file → one is created with sensible defaults, rather than the program failing.
- A **damaged** file → it is backed up before being replaced, so nothing the user wrote is lost.
- An empty file counts as damaged (this used to slip through and cause a confusing failure later).
- A missing setting → the default is used, and *simply reading* a setting never writes to disk.
  That last one matters: reading a config should not modify it.

## Checking the user's data before trusting it

`JsonValidatorTest` (5), `FileValidatorTest` (4), `ConditionConsistencyCheckerTest` (5),
`ValidationResultTest` (7)

Malformed JSON — a trailing comma, an unclosed brace, an empty file — is caught and reported
clearly, rather than causing an obscure crash further down the line.

The consistency checker verifies that recordings that claim to be the same experimental condition
really do agree with one another (same probe, same concentration). If two recordings disagree, the
user is told once, plainly — not the same complaint repeated for every row.

## The Viewer's overrides — 34 tests

`RecordingExcludeRoundTripTest` (12), `SquareOverrideRoundTripTest` (10),
`RecordingOverrideRoundTripTest` (8), `ApplyRecordingOverridesTest` (4)

When you exclude a recording in the Viewer, or assign a cell ID to a square, or change the
selection thresholds, the decision is stored in a CSV in the `Viewer` folder and applied to the
data on export. These tests write down the rules — which, until now, existed only in the code:

**The override file is the master record.** When the data is loaded or exported, the exclusions are
rebuilt *entirely* from `Recording Exclude.csv`. Anything marked excluded by other means is
cleared. This looks like data loss and is deliberate: it is what guarantees that what you see is
exactly what you chose. It was very nearly "corrected" as a bug — the tests now state plainly that
it is not one.

**Editing one thing does not disturb another.** Assigning a cell ID to one square leaves the other
squares' assignments alone. Changing thresholds for one recording leaves the other recordings
alone. Applying an override to recording A does not touch recording B's squares.

**Zero means "remove", not "cell zero".** Setting a square's cell ID to 0 deletes the override
rather than assigning it to a cell named 0.

**Names that are prefixes of other names stay separate.** Recording `…-A1-1` must never be confused
with `…-A1-10`. This is not hypothetical: two real bugs of exactly this kind were found in the
Viewer — one showed the squares of the wrong recording, another displayed the wrong image. Several
tests now guard it explicitly.

## Small things that are easy to get wrong

`BooleanUtilsTest` (5), `MiscellaneousTest` (4), `PaintLoggerThrowableTest` (2),
`ExperimentInfoRowTest` (3)

Reading `TRUE`, `true`, ` Yes ` and `1` from a CSV as the same thing. Formatting a duration. Making
sure that when an error is logged with an exception, the stack trace actually reaches the log file
— because a swallowed cause is the difference between a five-minute diagnosis and an afternoon.

---

## What is **not** covered

Being clear about the gaps is as useful as listing the coverage.

- **The user interface.** No test clicks a button. Everything Swing-related — dialogs, panels, the
  movie player, whether the window re-enables after playback — must still be checked by hand.
- **TrackMate runs.** The plugin is exercised only by running it. Nothing automated confirms that a
  TrackMate batch produces the expected tracks.
- **Omero download, the installers, and the release tooling.** Not covered at all.
- **Neighbour modes.** The override tests use the "Free" mode, where a square's visibility depends
  only on its own numbers. The "Relaxed" and "Strict" modes, where visibility depends on
  neighbouring squares, are exercised by the golden-master test but have no focused tests of their
  own.

## One thing that is not a test at all

`generatesquares/calc/CalculateTauTest` is **not** an automated test, despite its name and its
location in the test folder. It is a manual harness with a `main` method that draws the tau fit on
screen so it can be compared against Python by eye. It contains no assertions and nothing runs it
during a build. The real, automated tau tests are in `CalculateTauFitTest`.
