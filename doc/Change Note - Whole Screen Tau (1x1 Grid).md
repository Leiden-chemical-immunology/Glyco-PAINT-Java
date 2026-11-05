# Change Note: Whole-screen Tau with a 1x1 grid

Date: 2026-11-05
Status: **implemented** (commits `53457a77`, `231cf92c`).

## 1. Why

The square method computes Tau per square and selects squares by comparing them with the
background of the rest of the cell (density ratio), by their variability and by their
neighbours. Sometimes a single Tau over **all tracks in the image** is wanted instead: the
whole-screen Tau.

The simplest way to get it with the existing machinery is a grid of one square covering the
whole image. Generate Squares then calculates the Tau of that square, and the recording Tau
over the selected squares, which is the same set of tracks.

## 2. What did not work before

1. **The dialog offered no 1x1.** The grid dropdown in `SquaresParamsPanel` only had `5x5` to
   `40x40`. When you press OK, the dialog writes its values back to `Paint Configuration.json`
   (`persistTo`), so a `1` entered by hand in the configuration file was overwritten.
2. **The selection criteria cannot apply to one square.**
   - **Density ratio:** the background estimate (`SquareUtils.calculateBackgroundDensity`) is the
     average over squares, so with one square the background is that square itself. Its
     density ratio is always exactly 1, and it never passes the default threshold of 2.0. The
     square is not selected, and the recording gets no Tau.
   - **Neighbour mode:** `Strict` and `Relaxed` keep only squares with a selected neighbour
     (`SharedSquareUtils.applyVisibilityFilterOnRecording`). A single square has none.
   - **Variability:** it is now measured over the whole image, empty areas included, so it is
     not comparable with that of a normal square, and the normal threshold makes no sense.

## 3. What was changed

### 3.1 The dialog (`paint-ui-common`, `SquaresParamsPanel`)

- `1x1` is the first option in the grid dropdown.
- With `1x1` selected, **Min Required Density Ratio** and **Max Allowable Variability** are
  greyed out (field and label), because they do not apply. Choosing another grid enables them
  again. This works together with the existing greying-out in TrackMate mode ("Run Generate
  Squares after TrackMate").
- Four lines in `setEnabled` that set the label colours a second time, directly after
  `setSquaresEnabled`, were removed. They would have overwritten the 1x1 greying.

### 3.2 Fixed selection criteria (`paint-shared-utils`, `GenerateSquaresConfig`)

- New method `isWholeScreen()`: `true` when the grid size is 1.
- In that case three settings are **not read** from the configuration file but fixed, as named
  constants:

| Setting | Value at 1x1 | Constant |
|---|---|---|
| `Min Required Density Ratio` | 0 | `WHOLE_SCREEN_MIN_REQUIRED_DENSITY_RATIO` |
| `Neighbour Mode` | `Free` | `WHOLE_SCREEN_NEIGHBOUR_MODE` |
| `Max Allowable Variability` | 1.0E9 (no limit in practice) | `WHOLE_SCREEN_MAX_ALLOWABLE_VARIABILITY` |

- `Min Required R Squared`, `Min Tracks to Calculate Tau` and all other settings are still read
  from the configuration. A recording with a poor fit therefore still gets NaN.
- Because the override happens inside the configuration object, the values Generate Squares
  stamps on every recording (`SquareGenerationService.computeExperiment`) are the 1x1 values.
  The Viewer uses those stamped values, so it applies the same selection.

**Why 1.0E9 and not infinity.** The first version used `Double.POSITIVE_INFINITY`. The CSV
writer (`BaseTableIO.writeCsv`, `DecimalFormat("0.000")`) formats infinity as the symbol `∞`,
which the reader cannot parse back. The Viewer then reported "No valid recordings found", and
Generate Squares refused to start on the existing file. A large finite value is written as
`1000000000.000`, which every reader (Java, R, Excel) handles. A coefficient of variation never
comes near that value.

### 3.3 Warning (`paint-generate-squares`, `GenerateSquaresHeadless`)

Every route into Generate Squares (the dialog, after TrackMate, the sweep) goes through
`GenerateSquaresHeadless.run`, where the configuration is created once. With a 1x1 grid a
warning is logged there, once per run:

```
Grid is 1x1 (whole screen): Min Required Density Ratio set to 0.0, Neighbour Mode to Free and
Max Allowable Variability to 1000000000 (no limit); the values in the configuration file are ignored.
```

### 3.4 Viewer (`paint-viewer`)

Since November 2025 (commit `5d870447`) the Viewer does not show Tau, because recalculating the
recording Tau needs all tracks, which is too slow. The stored values are available without
tracks, so with a 1x1 grid they are now shown:

- **Attribute list** (`RecordingAttributesPanel`): rows **Tau** (in ms) and **R²** below
  *Number of Tracks*, from `Recordings.csv`. Also shown in the preview of the Square Control
  dialog.
- **Square popup** (`SquareGridPanel`, left-click on the image): a **Tau** line, from
  `Squares.csv`.

With a normal grid both are unchanged.

## 4. Test

Experiment `221012` (Paint Test Project), 1x1 grid:

| Recording | Tau (ms) | R² | Tracks |
|---|---|---|---|
| 221012-Exp-1-A1-1 | 215 | 0.949 | 14583 |
| 221012-Exp-1-A1-2 | 232 | 0.952 | 13417 |
| 221012-Exp-1-A1-3 | 227 | 0.947 | 14981 |

The square is selected (`Visible: true`), its Tau equals the recording Tau, and the Viewer shows
the values in the attribute list and the popup.

## 5. Points to be aware of

- **Meaning.** A whole-screen Tau mixes binding on the cell with non-specific binding on the
  glass beside it. The square method separates these through the density ratio. So the
  whole-screen Tau answers a different question from the normal recording Tau.
- **Results are overwritten.** Generate Squares writes `Squares.csv`, `Recordings.csv` and
  `Tracks.csv` in the experiment folders, and the dialog saves the grid size in
  `Paint Configuration.json`. A 1x1 run therefore replaces the normal square results. To keep
  both, run 1x1 in a copy of the project, or run Generate Squares again with the normal grid
  afterwards.
- **The Viewer takes the grid size from the configuration**, not from the results files
  (`ViewerFrame`). After changing the grid without running Generate Squares again, the Viewer
  shows the old results on the wrong grid. This was already the case before this change.
- **Square Control dialog in the Viewer.** Its variability slider cannot represent 1.0E9 and sits
  at its maximum for a 1x1 recording. Pressing Apply there sets variability to that maximum.
  With a single square this makes little difference. Not changed.
- **Edge tracks.** Tracks present in the first or last frame are excluded by the Fiji plugin
  (`Exclude Edge Tracks`, default on), so they are not part of the whole-screen Tau either.
