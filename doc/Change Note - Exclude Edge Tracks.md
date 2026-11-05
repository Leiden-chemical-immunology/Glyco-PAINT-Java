# Change Note: Exclude tracks that touch the first or last frame

Status: **implemented: option A, variant A2** (see section 6).
Date: 2026-11-04

## 1. Why

A track that is already present in the first frame of a recording may have started before
the recording began. A track that is still present in the last frame may continue after the
recording ended. For both, the measured `Track Duration` is only a lower bound, not the real
duration.

These tracks bias durations, and therefore Tau, towards shorter values.

**Principle:** such tracks are invalid. They are excluded from everything: square and
recording Tau, R², density, density ratio, background and track counts. As far as the
analysis is concerned, they do not exist.

**Definition.** With frames numbered 0-based, as TrackMate does, a track is an *edge track*
if its first spot is in frame 0, or its last spot is in frame `Number of Frames − 1`.
`Number of Frames` is per recording (`imp.getNFrames()` in the Fiji plugin), not the fixed
`PaintTiming.FRAMES`. With 2000 frames, that is frame 0 or frame 1999.

**Common to both options:** existing experiments only get the correction after TrackMate (the
Fiji plugin) has been run on them again. The `paint-regression` reference files must be
regenerated after the reference case has been run again.

---

## 2. Option A: the Fiji plugin does not write edge tracks

### 2.1 Idea

The plugin already removes tracks before writing `Tracks.csv`. A TrackMate track filter on
`NUMBER_SPOTS` (minimum number of spots per track) is set in
`RunTrackMateOnRecording` (around line 201), and only the tracks that pass it are written
(`TrackDataExporter.writeTracksCsv(..., visibleOnly = true)`). Edge tracks are removed
in the same way, so they never reach `Tracks.csv`.

### 2.2 Result

- Not in `Tracks.csv`
- Not in the "tracks detected" count (`numberOfFilteredTracks`, which counts filtered tracks
  only)
- Not drawn in the TrackMate overlay image (`TrackMate Images/<recording>.jpg`)
- Generate Squares, Viewer, Compare and R scripts see fewer tracks, but nothing changes
  in them

`Number of Tracks` in `Recordings.csv` (`nTracks(false)`) remains the raw TrackMate count,
which already includes the too-short tracks, and now includes the edge tracks as well.

### 2.3 How to detect them: two variants

**A1: TrackMate track filters on `TRACK_START` and `TRACK_STOP`**

Two extra `FeatureFilter`s next to the existing `NUMBER_SPOTS` filter:

- keep tracks with `TRACK_START` above ½ frame
- keep tracks with `TRACK_STOP` below `(Number of Frames − 1.5)` frames

Points to note:

- `TRACK_START` and `TRACK_STOP` are expressed in time units from the image calibration
  (seconds if the frame interval is set, frames if not). So the thresholds are computed from
  `imp.getCalibration().frameInterval`, falling back to 1 if it is 0.
- The half-frame margin makes the result independent of rounding, and of whether TrackMate's
  filter compares with `>` or `>=`.
- Fully inside TrackMate's own mechanism, next to the existing filter.

**A2: explicit frame check after tracking**

After `trackmate.process()`, loop over the visible tracks. For each one, take the lowest and
highest `Spot.FRAME` of its spots. If it is 0 or `Number of Frames − 1`, hide the track with
`trackModel.setVisibility(trackId, false)`.

Points to note:

- Works directly in frames, independent of calibration.
- About ten lines of new code in `RunTrackMateOnRecording`, outside TrackMate's filter
  mechanism.
- Must happen before the overlay is rendered and before `numberOfFilteredTracks` is
  counted, so that both exclude edge tracks.

### 2.4 Code changes

| File | Change |
|------|--------|
| `paint-fiji-plugin/.../trackmate/RunTrackMateOnRecording.java` | A1: two extra `addTrackFilter` calls. A2: the frame check after `process()` |
| `paint-shared-utils/.../config/TrackMateConfig.java` | Only if a setting is wanted (see 4) |

Nothing else changes. The `Tracks.csv` format stays the same, so old and new files are read
identically.

### 2.5 Pros and cons

- \+ Smallest change: one file, no format change, nothing downstream changes
- \+ Matches the principle directly: edge tracks do not exist anywhere, including the overlay
  image and the plugin's own counts
- \+ Extends an existing mechanism (the track filter)
- − No switch downstream: comparing with and without means running TrackMate twice
- − The output does not show whether the filter was applied. A new `Tracks.csv` looks the same
  as an old one

---

## 3. Option B: the plugin records start and end frame; Generate Squares excludes

### 3.1 Idea

The plugin writes all tracks as before, plus two new columns. Generate Squares removes the
edge tracks.

### 3.2 Fiji plugin

New columns in `Tracks.csv`, directly after `Track Duration`:

| Column              | Type    | Meaning                                       |
|---------------------|---------|-----------------------------------------------|
| `Track Start Frame` | INTEGER | Frame of the first spot (0-based)             |
| `Track End Frame`   | INTEGER | Frame of the last spot (0-based)              |

These come from `TrackAttributeCalculations`, which already sorts each track's spots by
`Spot.FRAME`. 0-based matches the other identifiers in the output files (Track Id,
Square Number, Row, Column, Label Number).

### 3.3 Reading old and new files

`Tracks.csv` is currently read with a strict schema (`readTracksTable` calls
`readCsvWithSchema(..., allowSuperset = false)`): count, names and order must match
`Track.Column` exactly. The reader has to accept both versions: with and without the two
columns. For old files the frame values are left empty.

### 3.4 Generate Squares

- In `GenerateSquaresProcessor.assignTracksToSquares`, before assignment: if the frame columns
  are present, edge tracks are not assigned to any square. If they are absent (old file), no
  correction is applied, and a line in the log says so.
- Generate Squares rewrites `Tracks.csv` at the end of every run
  (`writeTracks(experimentPath, compileAllTracks(...))`), replacing the plugin's file. What
  happens to the edge tracks in that rewrite has two variants:

  **B1: drop them.** Edge tracks are left out of the rewritten `Tracks.csv`, consistent with
  "they do not exist". Consequence: after the first run they are gone from the file. A later
  run with the filter switched off cannot bring them back without running TrackMate again.

  **B2: keep them, marked as not in any square.** Edge tracks stay in the rewritten
  `Tracks.csv` with `Square Number = -1` and `Label Number = -1`, the values the Fiji plugin
  writes before squares are assigned. They take part in no calculation, but remain in the
  file. Consequence: the switch keeps working on every run, so you can compare with and
  without. But the file then contains tracks that "do not exist", and every other reader of
  `Tracks.csv` (Viewer, Compare, R scripts) must ignore tracks with `Square Number = -1`, or
  it will count them.
- Optional: a column `Number of Edge Tracks Excluded` in `Recordings.csv`.

### 3.5 Code changes

| File | Change |
|------|--------|
| `paint-shared-utils/.../objects/Track.java` | Two fields, getters and setters, two `Column` entries |
| `paint-shared-utils/.../io/internal/TracksTableIO.java` | Write and read the columns |
| `paint-shared-utils/.../io/MainIOInterface.java`, `BaseTableIO.java` | Accept the old format without the two columns |
| `paint-shared-utils/.../validate/TracksValidator.java` | Accept both formats |
| `paint-fiji-plugin/.../tracks/TrackAttributeCalculations.java`, `TrackDataExporter.java` | Determine and write start and end frame |
| `paint-generate-squares/.../calc/GenerateSquaresProcessor.java` | Exclude edge tracks |
| `paint-shared-utils/.../objects/Recording.java`, `RecordingsTableIO.java` | Only if the reporting column is wanted |
| Tests in `paint-shared-utils` and `paint-generate-squares` | New columns, both file formats, exclusion |

### 3.6 Pros and cons

- \+ The plugin's `Tracks.csv` shows when every track starts and ends
- \+ Old files remain readable
- − Many more files touched, and a second file format to support permanently
- − With B1, the "switch downstream" advantage disappears after the first run, because
  Generate Squares rewrites `Tracks.csv` without the edge tracks. Getting them back means
  running TrackMate again, as in option A
- ± With B2 the switch survives, but `Tracks.csv` keeps the edge tracks, and every reader of
  it has to recognise and ignore them
- − Edge tracks still exist in the plugin's overlay image and its "tracks detected" count, so
  the principle "they do not exist" is only partly met

---

## 4. Open points at the time of the proposal

1. **Option A or B.**
2. **If A: variant A1** (TrackMate filters on `TRACK_START` / `TRACK_STOP`) **or A2**
   (explicit frame check).
3. **Setting**, for example `Exclude Edge Tracks` (on/off) in the TrackMate section (A) or the
   Generate Squares section (B), or always on?
4. **If B: variant B1** (drop edge tracks from the rewritten `Tracks.csv`) **or B2** (keep
   them with `Square Number = -1`).
5. **If B: reporting column** `Number of Edge Tracks Excluded` in `Recordings.csv`: yes or no?

## 5. Not part of this change

- `RECORDING_DURATION` (used for density) is a fixed 2000 × 0.05 s, not the recording's actual
  `Number of Frames`. That is a separate issue.
- User Manual / help text: updated only after the code change has been reviewed.

## 6. What was decided and implemented

1. **Option A, variant A2.** The Fiji plugin does not write edge tracks. They are detected by an
   explicit frame check after tracking: `excludeEdgeTracks(model, settings.tstart,
   settings.tend)` in `RunTrackMateOnRecording`. It hides, with `model.setTrackVisibility`,
   every filtered track that has a spot in the first or the last analysed frame (0-based, the
   range TrackMate itself analysed). It runs directly after tracking and before the track
   count, so the overlay image, the "tracks detected" count and `Tracks.csv` all skip edge
   tracks. The `Tracks.csv` format and everything downstream are unchanged.
2. **Setting:** `Exclude Edge Tracks` in the `"TrackMate"` section of `Paint Configuration.json`.
   The default is `true`, also when the key is absent, so edge tracks are excluded. `false`
   keeps them. It is read in `TrackMateConfig` (`isExcludeEdgeTracks()`) and included in the
   configuration dump.
3. **Logging per recording**, on the track detection line, for example
   `(14812 tracks detected, 39 edge tracks dismissed, multithreaded)`, or `edge tracks kept`
   when the setting is off.
4. **Unit test:** `ExcludeEdgeTracksTest` in `paint-fiji-plugin` (the module's first test; JUnit
   added as a test dependency). It builds a small TrackMate model by hand and checks that tracks
   touching the first or last frame are hidden, that tracks in between stay, and that tracks
   already hidden by TrackMate's filters are not counted.

Existing experiments get the correction only after TrackMate has been run on them again.

