# Review — architecture, code quality, documentation

Date: 2026-07-12. Every finding below was verified by reading the code; nothing here is
speculation. Findings are ranked by what they cost you, not by how they look.

This review supersedes `CODE_REVIEW.md` and `DOCUMENTATION_REVIEW.md`, both of which are now
substantially false and should be deleted (see the end).

---

## The one to fix first: NaN silently becomes 0.0 in the output

**`Miscellaneous.round` turns NaN into zero.** `Math.round(double)` returns a `long`, and its
contract says: *"If the argument is NaN, the result is 0."*

```java
// Miscellaneous.java:113
double scale = Math.pow(10, decimals);
return Math.round(value * scale) / scale;     // round(NaN, 5) == 0.0
```

That would be harmless if NaN never reached it. But the guard meant to stop it has a hole:

```java
// SharedSquareUtils.java:337
if (area <= 0 || time <= 0 || concentration <= 0) {   // NaN <= 0 is FALSE
    throw new IllegalArgumentException("Area, time, and concentration must be positive");
}
```

`NaN <= 0` evaluates to `false`, so a NaN concentration passes the guard and
`calculateDensity` returns NaN. It is then rounded — to **0.0**.

**How it happens in practice:** a blank Concentration cell in `Experiment Info.csv`. Tablesaw's
missing-value sentinel for a double column is NaN, the value is copied through to `Recordings.csv`,
and `CalculateSquareAttributes` (lines 183 and 256) rounds it into the Squares and Recordings
files.

**What the user sees:** `Density = 0.0` for every square and every recording of that experiment.
Not an error. Not a NaN. A plausible number that is wrong.

**And the regression gate cannot catch it.** `paint-compare` carries its own private copy of the
same function (`RegressionRules.java:275`), and its NaN short-circuit only runs in *relaxed* mode:

```java
// RegressionRules.java:246
if (relaxed) {
    if (Double.isNaN(a) || Double.isNaN(b)) { return true; }
}
```

In **strict** mode, an old NaN and a new 0.0 both round to 0.0 and compare equal — "no difference".
`Density`, `Tau`, `R Squared`, `Density Ratio` and `Variability` are all in the strict rounding set.
So the golden master, the thing we rely on to prove the science has not changed, is blind to
precisely this corruption.

**Fix:** `if (!Double.isFinite(value)) return value;` at the top of *both* `round()` methods, and
make the `calculateDensity` guard NaN-aware (`!(concentration > 0)` rather than
`concentration <= 0`). Then add a test.

This is the only finding in this review that can silently change your published numbers.

---

## High

### 2. An uncaught-exception handler that discards every crash

```java
// TrackMateUIBatch.java:65  (identical copy at TrackMateUISingle.java:67)
Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
        PaintLogger.debugf("AWT complained: %s", throwable.getMessage()));
```

This replaces the JVM-wide default handler for the whole Fiji process. Every uncaught exception in
every thread — including the TrackMate worker thread — is reduced to its message, with no stack
trace. And it logs at **DEBUG**, which is below the default level of INFO, so
`PaintLogger.log` discards it entirely.

The net effect: an uncaught exception during a TrackMate run produces **no output at all**. The run
stops or hangs and the log is silent. For an NPE, `getMessage()` is null anyway.

This will cost you days the next time something breaks. Use `PaintLogger.error(msg, throwable)`,
which exists for exactly this, and log at ERROR.

### 3. Generate Squares reports success after an experiment failed

```java
// GenerateSquaresHeadless.java:102
} catch (Exception e) {
    PaintLogger.errorf("Error processing experiment %s: %s", experimentName, e.getMessage());
    continue;
}
```

The cause is discarded, the loop continues, the project-level concatenation runs regardless, and
the run ends with *"Finished Generate Squares for all experiments"*. The user gets a project
`Squares.csv` missing an entire experiment, and a log that says it worked. Combined with finding 1:
a `Concentration` of exactly `0` (a no-ligand control) throws here, and the message contains no
recording name, no square number, and no stack trace.

### 4. Null as an error channel has already produced one live NPE

`MainIOInterface`'s five read methods log and return `null` instead of throwing
(lines 148, 185, 199, 239, 292). `ExperimentDataLoader` then handles this **three different ways in
one method**:

- Tracks — guarded (we fixed this yesterday).
- **Squares — not guarded.** `ExperimentDataLoader.java:122`: `int numberOfSquares = squaresTable.rowCount();`
  A missing or bad-header `Squares.csv` makes `readSquaresTable` return null, and this throws an
  uncaught NPE out of `loadExperiment`.
- Recordings — "works" only because the NPE is caught by an enclosing `catch (Exception e)` and
  reported as a read failure. NPE as control flow.

Same user error, three different behaviours: a clean message, a clean message, and a stack trace.
Yesterday I fixed the tracks case one at a time; that is what `null` returns cost. Make these
methods throw and the compiler will find every remaining site.

### 5. The Viewer freezes on close, doing whole-project IO on the UI thread

`ViewerFrame.java:836` — the close handler shows a dialog that *warns the user it may take a
while*, then does the work inline on the event dispatch thread. `exportOverrides` reads and
rewrites the squares and recordings tables for the entire project. The window is frozen and
unrepaintable throughout; macOS will show "application not responding". Same pattern at
`ViewerFrame.java:429`.

There is no `SwingWorker` anywhere in the codebase, yet raw `new Thread(...)` appears in three
other places — so this is an inconsistency, not a deliberate choice.

### 6. "Merge or replace?" is asked at most once per session

`CellAssignmentManager.firstAssignmentForRecording` is initialised `true` and set to `false` at
`ViewerFrame.java:643`. It is **never set back to true** — `setFirstAssignmentForRecording(true)`
appears nowhere, and `clear()` (called on every recording change) does not reset it.

So after the user's first cell assignment anywhere in the session, the prompt at
`ViewerFrame.java:625` never fires again. A user who moves to another recording that already has
assignments and wants to **replace** them is never asked: the code silently merges. Their old
assignments survive into `Square Override.csv` with no visible symptom.

---

## Medium

### 7. `Label Number` means two different things in two files

In `Tracks.csv` it is written as the square's ordinal (`GenerateSquaresProcessor.java:233`), which
equals the Square Number. In `Squares.csv` it is later overwritten with the square's rank **among
visible squares only** (`CalculateSquareAttributes.java:90`). Consequences: joining the two files
on `Label Number` returns the wrong rows, and every filtered-out square in `Squares.csv` has
`Label Number = 0` — as does the first visible square.

### 8. The regression gate compares neither speed column

All four rule maps in `RegressionRules` key on `"Max Mean Speed"` (lines 81, 102, 123, 146). The
actual column is `"Max Median Speed"` (`Square.java:760`). The entries can never match. Both
spellings also sit in the ignore lists, so the strict gate silently compares **no** speed column at
all.

### 9. `PaintConfig.initialise()` on a different project warns and carries on

`PaintConfig.java:80` — if the singleton is already bound elsewhere it only logs a warning. Fiji
keeps one JVM alive across plugin invocations, so running a sweep and then TrackMate on a different
project leaves the config bound to the previous one. It should rebind or throw; warn-and-continue
is the worst of both.

Related, in the same class: `instance()` lazily binds to `~/` if any read happens before
`initialise()`, permanently.

### 10. Duplicated grid rule, only one copy live

`SharedSquareUtils.filterTracksInSquare` is dead — no caller passes both `loadSquares` and
`loadTracks` as true, so `ExperimentDataLoader.java:144` is unreachable. The live rule is
`GenerateSquaresProcessor.assignTracksToSquares`, and the two have **drifted**: the dead one drops
tracks outside the image, the live one clamps them into the border square. Anyone reading the dead
one — with its careful boundary comment — would reasonably believe it is the rule. Delete it.

### 11. `WriteRecordingOverride` is quadratic at Project scope

`WriteRecordingOverride.java:125` — `writeOrUpdate` does a full read-modify-write of the CSV **per
recording**. Applying square-control settings at Project scope on 200 recordings performs 200 full
rewrites. The same shape as the `ExportOverridesFromViewer` problem fixed yesterday; this copy was
missed.

---

## Lower, but worth knowing

- **Thread safety in `TiffMoviePlayer`.** `playing[0]` and `currentFrame[0]` are plain array
  elements shared between the EDT and the playback thread with no `volatile` — the pause may never
  be observed. The playback thread also reads Swing components (`frame.isVisible()`,
  `speedSlider.getValue()`) off the EDT, and both threads call `imp.getStack().getProcessor(...)`
  concurrently, which ImageJ does not guarantee is safe.
- **`PaintPrefs` is macOS-only** (`Library/Preferences/…plist`, hardcoded at line 43) while the repo
  ships a Windows installer. It also reads the real file at class-load and **writes on a cache
  miss** — so any test touching it writes to your actual `~/Library/Preferences`.
- **Dead code**, all verified unused: nine locals in `CalculateSquareAttributes` (60–70) that
  `CalcParams` recomputes; the unused `config` parameter at `CalculateSquareAttributes.java:131`;
  `SquareGridPanel.dragSelectedSquares` (written, never read); `outPath` in
  `GenerateSquaresHeadless` (118).
- **`PaintLogger` still imports `java.awt`** and bakes `Color` into its `Level` enum and `Sink`
  interface, despite the class comment promising the module stays free of UI.

---

## Architecture: what is actually fine

Worth stating, because it stops us re-litigating it.

**No dependency cycles.** Verified across every pom. Nothing depends upward on an application
module. The shared computation that *could* have been copy-pasted — visibility filtering, tau,
density — genuinely lives in `SharedSquareUtils` and `CalculateTau` and is called from both the
Viewer and Generate Squares. That is right, and it is the part that matters most.

One edge to reconsider: `paint-fiji-plugin` depends on `paint-generate-squares`, which depends on
`paint-ui-common` — so the ImageJ plugin transitively drags the Swing project dialog onto its
classpath. The headless pipeline entry point wants its own UI-free module.

**The object/table duality is the real structural weakness.** `Recording` holds `List<Square>`,
`List<Track>` *and* `Table tracksTable`; `Square` holds `List<Track>` *and* `Table tracksTable`.
Both are populated. So track data for one recording exists simultaneously as a whole-experiment
table, a per-recording table slice, a list, a map, and a list-plus-table on every one of N squares.
The square schema is declared in five places (the `Column` enum, the getters/setters, and three
methods in `SquaresTableIO`). Adding one metric means five coordinated edits — and if you miss the
`toTable` line, the column appears in the CSV, is always empty, and nothing fails at compile time
or run time. You find out when a downstream script produces nonsense.

This is not worth a rewrite. It *is* worth the rule: tables for I/O and bulk column work, objects
for per-square computation, and never both on the same object.

---

## Documentation

The mechanical quality is genuinely good: across all of `src/main` there are only **two**
`@param`/`@return` mismatches, **zero** dead `{@link}` targets, and five lines of commented-out
code in the whole repo. Inline comments explain *why* rather than restating *what*. That is a
better state than most codebases reach.

The problem is concentrated in the class docs promoted from the old banners yesterday — and the
sharpest part is that **the promotion carried known-false text into Javadoc**, where it now looks
authoritative:

- **`FileHelper.java:22`** claims *"Exporting Swing components as high-resolution PNG images"*. The
  class has one public method and exports nothing; PNG export is in `PanelExporter`. The old
  documentation review had already flagged this exact falsehood — and it was promoted anyway.
- **`GenerateSquaresProcessor.java:40`** says the method *"applies geometric segmentation, assigns
  tracks… and calculates attributes"*. It does none of those any more — it delegates all of it to
  `SquareGenerationService`. It also has two silent early returns that are undocumented, and the
  class has no class-level Javadoc at all, despite being the pipeline's entry point.
- **`PaintPrefs.java:26`** advertises *"Reload capability for external modifications"*. There is no
  reload method; the plist is read once in a static block.
- **`SquareUtils.java:19`** inventories three methods that live in a different class
  (`SharedSquareUtils`) and omits the two it actually has.
- **`TrackMateConfig.java:38`** claims a file-writing method that was deleted — the commented-out
  `java.nio.file` imports are still sitting at lines 12–15.

### Markdown at the repo root

| File | Verdict |
|---|---|
| `CODE_REVIEW.md` | **Delete.** Its summary is now flatly false: *"essentially zero automated tests… a search for `@Test` returns nothing"* (there are 119, run by CI), *"11 files import javax.swing"* in shared-utils (there are 0), and it describes a `paint-regression` module that no longer exists. Its "highest-value work" list is three-for-three already done. Anyone reading it would conclude the project is untested. |
| `DOCUMENTATION_REVIEW.md` | **Delete**, but fix the FileHelper and GenerateSquaresProcessor Javadoc first — it flagged both, and they were promoted regardless. |
| `REFACTORING_PROGRESS.md` | **Update.** Four "remaining" items are already done, and it says ~60 tests while the README says ~120. A progress document that lists finished work as outstanding is how work gets done twice. |
| `CHANGELOG.md` | **Stale.** Top entry is v0.0.134; the pom is 0.0.138-SNAPSHOT. Misses tests, CI, the MIT licence and `paint-ui-common` entirely. |
| `README.md` | One real error: *"The pipeline is a collection of Python and R scripts"* — there are zero `.py` files, and three paragraphs later the README says this is a Java rewrite of the Python pipeline. Copied from the old repo. |
| `TESTING.md`, `TROUBLESHOOTING.md` | Keep. |

---

## Suggested order

1. **`round()` and the density guard** (finding 1), with a test. This is the only one that can
   silently change published numbers, and the golden master cannot see it.
2. **The uncaught-exception handler** (2) and **Generate Squares' false success** (3). Neither is a
   bug in itself; both are what will make the *next* bug expensive.
3. **Make `MainIOInterface` throw** (4) and let the compiler find `ExperimentDataLoader:122`.
4. The two Viewer bugs: the frozen close (5) and the never-repeated prompt (6).
5. Delete `CODE_REVIEW.md` and `DOCUMENTATION_REVIEW.md`; fix the five lying Javadocs; drop the
   "Python and R scripts" sentence.
