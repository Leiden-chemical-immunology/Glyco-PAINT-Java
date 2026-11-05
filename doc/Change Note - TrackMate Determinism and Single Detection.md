# Change Note: TrackMate determinism, and running spot detection only once

Date: 2026-11-05
Status: **implemented** (commits `d695a52b`, `d983744e`, `bf74de93`; see section 7).
Test data: experiment `221012`, recording `221012-Exp-1-A1-1` (Paint Test Project),
406 703 spots, about 14 850 filtered tracks.

## 1. Summary

1. **Spot detection runs twice per recording.** The plugin calls `execDetection()` to check
   the spot count against the maximum, and then `process()`, which starts with
   `execDetection()` again. Replacing `process()` by the steps that follow detection removes
   the second detection. **Tested: this gives the same results as `process()`.**
2. **TrackMate's linking was not reproducible.** Running TrackMate twice on the same recording
   with the same settings gave different tracks. The cause is multithreading in the tracker.
   **With `trackmate.setNumThreads(1)` the result is reproducible.** The only remaining
   differences are at about 1e-13, in the track position, and disappear in the 2-decimal
   rounding in `Tracks.csv`.

## 2. Background: how the plugin runs TrackMate

In `RunTrackMateOnRecording.runTrackMateOnRecording`:

1. `trackmate.execDetection()` finds the spots.
2. The spot count is checked against `Max Number of Spots in Image`. If it is too high, the
   recording is skipped. This gate is deliberate: tracking with very many spots takes a very
   long time.
3. `trackmate.process()` does the rest.

In TrackMate 7.14.0 (`paint-fiji-plugin/lib/TrackMate-7.14.0.jar`), `process()` is,
read from the compiled class:

```
execDetection()
execInitialSpotFiltering()
computeSpotFeatures(true)
execSpotFiltering(true)
execTracking()
computeEdgeFeatures(true)
computeTrackFeatures(true)
execTrackFiltering(true)
```

with a check for cancellation after each step. In that case it returns `true`, and it
returns `false` as soon as a step fails.

So detection runs twice. The gate in step 2 is needed, but the second detection is not. The
**new method** keeps the gate and calls the seven steps after `execDetection()` directly, with
the same arguments and the same return behaviour.

## 3. How the test was done

A temporary class, `ProcessComparison`, ran two extra TrackMate runs per recording, each on
a fresh `Model` with `settings.copyOn(imp)`. It was removed once the test was complete.

- **Control**: the old method again (`process()`). This measures how much two runs of the
  *same* method differ.
- **New**: `execDetection()` plus the seven steps.

Each is compared with the original run, the one the plugin exports:

- spot count and filtered track count
- tracks matched on their **first spot** (frame plus exact x and y), so that track IDs and list
  order play no role
- per matched track: `NUMBER_SPOTS`, `NUMBER_GAPS`, `LONGEST_GAP`, `TRACK_DURATION`,
  `TRACK_X_LOCATION`, `TRACK_Y_LOCATION`, `TRACK_DISPLACEMENT`, `TRACK_MAX_SPEED`,
  `TRACK_MEDIAN_SPEED`, compared exactly, with the largest difference per feature logged

(A first version compared sorted lists position by position. That was unreliable: one
differing track shifted all following rows, so the count of differing tracks was inflated.
It was replaced by matching on the first spot.)

## 4. Results

### 4.1 Default threading, as the plugin has always run

| | Spots | Filtered tracks | Matched | Differ | Only in one run |
|---|---|---|---|---|---|
| Control vs original | 406703 = 406703 | 14851 vs 14850 | 14847 | 6678 | 4 + 3 |
| New vs original | 406703 = 406703 | 14851 vs 14852 | 14849 | 6704 | 2 + 3 |

Largest differences (control): `NUMBER_SPOTS` 57, `NUMBER_GAPS` 3, `LONGEST_GAP` 2,
`TRACK_DURATION` 2.95 s, `TRACK_X_LOCATION` 0.26, `TRACK_MAX_SPEED` 9.07, and so on. The new
method shows the same pattern and the same order of magnitude.

The original run itself also varied: an earlier run of the same recording gave 14 852
filtered tracks, this run 14 851.

**Reading:** the spots are identical every time, so detection is reproducible. The linking of
spots into tracks is not: tracks are split or joined differently, which is visible in
`NUMBER_SPOTS` differing by up to 57. The old method differs from itself as much as the new
method differs from the old one. This shows there is a source of variation, but it cannot
prove the methods equal.

### 4.2 Single-threaded: `setNumThreads(1)` on all three runs

| | Spots | Filtered tracks | Matched | Differ | Only in one run |
|---|---|---|---|---|---|
| Control vs original | 406703 = 406703 | 14851 = 14851 | 14851 | 6864 | 0 + 0 |
| New vs original | 406703 = 406703 | 14851 = 14851 | 14851 | 6640 | 0 + 0 |

Largest differences: **only** `TRACK_X_LOCATION` (control 1.1e-13, new 9.2e-14) and
`TRACK_Y_LOCATION` (control 6.4e-14, new 7.1e-14). All other features are exactly equal for
every track.

## 5. Explanation

### 5.1 Why default threading is non-deterministic

TrackMate's tracker (`SparseLAPTrackerFactory`) links spots by solving assignment
problems: frame to frame, then gap closing. The work is split over threads. A `TrackMate`
object uses all processor cores unless told otherwise. When several links are equally good,
or nearly so, the result can depend on which thread finishes first. That changes which spots
end up in which track. Once one link differs, the tracks around it differ too, which explains
whole tracks splitting or merging.

The plugin already tried to force determinism with:

```java
System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
System.setProperty("trackmate.deterministic", "true");
```

These two lines are now commented out (section 7).

The first property only affects Java's shared thread pool, which TrackMate does not use for
this. Whether TrackMate reads the second at all has not been verified. The test shows that
neither prevents the variation. The TrackMate object's own thread count was never set.

Detection, by contrast, gives the same spots every time.

### 5.2 Why tiny differences remain single-threaded

`TRACK_X_LOCATION` and `TRACK_Y_LOCATION` are the mean position of a track's spots. TrackMate
holds a track's spots in a set, and the order in which a set is iterated is not fixed between
runs. Floating-point addition is not exactly associative: adding the same numbers in a
different order can change the last bits of the result. That gives differences of about
1e-13 µm.

This is consistent with what was measured:

- the differences occur only in the two features that are a sum over spots of real numbers
- they occur equally in the control (old vs old) and in the new method
- all integer features (spots, gaps, longest gap) and all other features are exactly equal

`TrackDataExporter` writes these positions rounded to 2 decimals (`roundOr(..., 2, -1)`).
A difference of 1e-13 can only change the written value when a position lies within 1e-13 of
a rounding boundary, which is practically never.

## 6. Why the new method is safe

1. **Same steps.** The new method calls exactly the steps of `process()` after detection, in
   the same order, with the same arguments (`true` for every `compute…`/`exec…Filtering`
   step), with the same cancellation and failure handling. Checked against the compiled
   `TrackMate` class in 7.14.0.
2. **Same input.** Detection is reproducible (identical spot counts in every run). The second
   detection inside `process()` therefore produced the same spots as the first, so skipping it
   changes nothing.
3. **Measured.** Single-threaded, so with the run-to-run variation switched off, the new method
   gives the same spots, the same tracks and the same feature values as the old method. The only
   differences are at 1e-13 in the track position, identical in size to those between two runs of
   the old method itself.
4. **The gate stays.** `execDetection()` and the spot-count check are unchanged.

**What this means in the default, multithreaded mode.** With several threads the results cannot
be shown equal, because the old method does not even reproduce itself (section 4.1). What the
test does show is that the new method varies from the old one no more than the old method varies
from itself: about 6 700 differing tracks in both cases, and track counts within one or two of
each other. So in normal multithreaded runs the replacement introduces no additional variation.
The variation itself comes from the threading, not from the method.

**Scope of the test.** The measurements were made on one recording, `221012-Exp-1-A1-1`
(406 703 spots, about 14 850 tracks). The argument does not depend on the recording: points 1
and 2 hold for any input, and point 3 confirms them on a large real recording.

**Benefit:** spot detection is done once per recording instead of twice.

**Caveat:** this has been verified for TrackMate 7.14.0. If TrackMate is upgraded, `process()`
may change, and the list of steps must be checked again.

## 7. What was decided and implemented

1. **New method implemented** (commit `d983744e`). `trackmate.process()` is replaced by
   `processAfterDetection(trackmate)` in `RunTrackMateOnRecording`, which runs the seven steps
   after detection. Spot detection now runs once per recording. `ProcessComparison` has been
   removed.
2. **Threading is a setting** (commit `d695a52b`): `Deterministic Tracking` in the `"TrackMate"`
   section of `Paint Configuration.json`.
   - `false`, or key absent (the default): multithreaded. Fast, but tracks vary slightly from
     run to run.
   - `true`: `trackmate.setNumThreads(1)`. Reproducible, but linking is slower. In the test, one
     recording took about 34 s for three runs with default threading, and about 2 min 1 s
     single-threaded.
   - The setting is read in `TrackMateConfig` (`isDeterministicTracking()`) and included in the
     configuration dump.
3. **The two `System.setProperty` lines** from 5.1 are commented out, with a note that they had
   no effect on linking (commit `d695a52b`). The locale lines above them stay.
4. **The mode is logged** (commit `bf74de93`). The track detection line shows it, for example
   `(14851 tracks detected, multithreaded)` or `(14851 tracks detected, deterministic)`.

### Still open

- Results produced earlier, with default threading, cannot be reproduced exactly.
- The regression references for the TrackMate stage should be regenerated with
  `Deterministic Tracking` set to `true`.
