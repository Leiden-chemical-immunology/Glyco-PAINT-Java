# Glyco-PAINT-Java — Documentation Review

**Date:** 2026-07-12
**Scope:** every `src/main/java` file in all 12 modules (~140 files)
**Question asked:** are the inline comments adequate and relevant, are the headers correct, is the Javadoc up to date?

---

## Executive summary

Three answers, in order of how much they matter.

**1. The audit found roughly twenty real bugs.** Checking documentation against code means reading both and asking "is this true?", and that turns out to be an efficient way to find defects. Several are user-visible today. One of them — the Viewer opening the *wrong recording's* data — was dormant until this morning, because it is only reachable once the recording filter works, and the filter had been broken since December. Fixing the filter armed it. These are listed first, below, and they deserve attention before a single comment is touched.

**2. The Javadoc is structurally fine but semantically drifting.** The `/** ... */` blocks written recently are good — several are model documentation. Older ones describe behaviour the code no longer has: `PaintLogger`'s method Javadoc still says it writes to the console (it writes to a file and forwards to an optional sink); `GenerateSquaresProcessor` still claims to segment and compute (it now orchestrates load → compute → write). The most important public façades — `MainIOInterface`, `PaintConfig`'s instance API, `RegressionRules` — have **no Javadoc at all**, so the two facts a new reader most needs (*what does this return when it fails?* and *does calling a getter mutate state?*) are recorded nowhere.

**3. The file header banners are the actual problem, and they should go.** They are the first thing a reader sees and the most confidently worded text in the codebase, and they are majority-false.

---

## Why the banners rot: a structural cause, not carelessness

Every banner opens with `/*=====`, not `/**`.

That single character means the block is **not Javadoc**. It is never compiled, never linted, never rendered, and never link-checked. The banners are full of `{@link ...}` and `{@code ...}` tags that *look* official and are validated by nothing.

This is not a hypothesis. The banners currently reference these classes, **none of which exist anywhere in the repository**:

| Phantom class | Named in |
|---|---|
| `MainDataInterface` | `BaseTableIO`, `GenerateSquaresProcessor`, `TrackDataExporter` (twice) |
| `ExperimentInfoSchema`, `RecordingSchema`, `SquareSchema`, `TrackSchema` | `ValidationHandler`, `TrackDataExporter` |
| `PaintColumnNames` | `GenerateSquaresConfig` |
| `HelperIO` | `ImageRootValidator` |
| `ValidProjectPath` | `ProjectPathResolver` (as its own `Class:` name) |
| `CsvComparatorRegression` | `PaintStrictComparator`, `ComparisonResult` |

A `{@link}` to a non-existent class in real Javadoc fails the build. In a banner it sits there for months.

The evidence is consistent across all four modules audited:

- **paint-shared-utils** — of 12 banners sampled, **8 contain factually false statements**. Four `USAGE EXAMPLE` blocks **would not compile** (`Recording`'s 11-arg constructor example — the only constructor is no-arg; `PaintPrefs.reload()` — no such method; `GenerateSquaresConfig.from()` — no such method; `ValidationHandler.validate(...)` — the method is `validateExperiments`).
- **paint-fiji-plugin** — two banners cite classes/packages that do not exist; two `USAGE EXAMPLE`s would not compile; two advertise **"headless operation"** for code that calls `imp.show()` and `IJ.wait(2000)` and cannot run without a display; both plugin banners document the same wrong menu path.
- **paint-ui-common** — the packages and `MODULE:` fields were mechanically updated during the refactor, but nobody re-read the prose: `BottomBarPanel`'s banner invents a *"Save and Recalculate"* toggle that has never existed, while omitting all three checkboxes that do — including the Save Experiments preference added five lines below it. `PaintConsoleWindow`'s banner claims *"no external dependencies"*, which is now precisely the inverse of its design (it registers itself into `PaintLogger` — that dependency is the whole point).
- **paint-viewer and friends** — `FileHelper`'s banner describes a high-resolution PNG exporter; the class has exactly one method and exports nothing. `RecordingFilterDialog`'s banner promises "blue borders for active filters"; no border code exists. Both installers' banners say `Package: paint.installer`; **both classes are in the default package.**

Meanwhile: **the best-documented classes in the codebase have no banner at all.** `TableComparer`, `FieldComparator`, `ComparisonResult`, `SquareGenerationService` and the three `package-info.java` files carry real `/** */` Javadoc that explains *why*, and they are accurate. Five of six files in `paint-compare` have no banner. That is the strongest available argument for retiring the convention.

### Recommendation

**Delete the banner blocks.** Fold anything genuinely unique into the class Javadoc, where the compiler and `javadoc` keep it honest. The banner fields break down as:

- `PURPOSE` / `DESCRIPTION` / `KEY FEATURES` — duplicated by the class Javadoc five lines below, and the duplicate is the one that rots.
- `DEPENDENCIES` — a hand-maintained copy of the import list. **19 of 26 are already wrong.** The import list is authoritative and free.
- `USAGE EXAMPLE` — four of them do not compile. A test is a usage example that cannot lie.
- `MODULE` / `Package` — derivable from the file's location. Ten `paint-fiji-plugin` files omit `MODULE` entirely; nothing noticed.
- `UPDATED` — every file says `2025-12-31`, including files rewritten today. Decorative.
- `AUTHOR` / `COPYRIGHT` — fine, but one line, not fifteen. (Two files carry *"All rights reserved. Licensed under the MIT License"*, which is self-contradictory.)

If the banners must stay, then **change `/*=====` to `/**`** so `javadoc` validates them. The next `MainDataInterface` then breaks the build instead of misleading a reader for six months. Be aware this will surface every stale link at once.

---

## Real bugs found (fix these first)

### User-visible today

**1. The Viewer opens the wrong recording's squares data when a filter is active.**
`ViewerFrame.java:809-813` reads `allRecordingEntries.get(currentIndex)`, but `currentIndex` is an index into `recordingEntries` — the *filtered* list. Every other handler uses `recordingEntries`. With any filter applied, "Show Squares Data" silently opens a different recording's CSV. **This was unreachable until today**: applying a filter did nothing (the Apply button was wired to the cancel path since 19 December), so `recordingEntries` never diverged from `allRecordingEntries`. Fixing the filter armed this bug.

**2. `PaintConsoleWindow.closeIfVisible()` does not detach the log sink.**
`close()` calls `PaintLogger.clearSink()`; `closeIfVisible()` does not — and `closeIfVisible()` is the path `ProjectDialogController:256` actually uses. The logger is left holding a sink pointing at a disposed frame, so the next log line re-enters `ensureConsoleCreated()` and pops a fresh console window. *This is a defect in today's A1 sink-inversion work.*

**3. The Verbose checkbox has no effect on TrackMate runs.**
`RunTrackMateOnExperiment.java:100-103` — `static final boolean verbose = PaintRuntime.isVerbose();` is captured **once at class load**. Toggling Verbose calls `PaintRuntime.setVerbose(...)`, which this never re-reads. Same species as finding A6 (config flag frozen in a static initializer), in a place A6 did not look.

**4. The sweep does not restore the original TrackMate parameter.**
`RunTrackMateOnProjectSweep.java:246-248` writes the original value back *after* `PaintConfig.reinitialise(sweepPath)` (line 196) has repointed the config at the sweep directory — so it restores into the sweep copy, not the project config. The Javadoc at line 88 documents an intent the code does not achieve.

**5. Omero import fails on any folder containing a `.DS_Store`.**
`ProcessOmeroFiles.java:73-84` moves only non-hidden files, then calls `Files.delete(fsDir)`, which requires the directory to be empty. On macOS — the primary platform — a `.DS_Store` guarantees `DirectoryNotEmptyException`.

**6. Movie playback never restores the UI, and hangs 10 seconds first.**
`RecordingPlaybackController.java:145` waits for a window titled `<filename>`; `TiffMoviePlayer.java:179` creates one titled `"Movie Player - " + filename`. The titles can never match, so `waitForWindow` polls for ~10 s, returns `null`, and the close listeners are never attached. The `finally` block then re-enables the UI immediately, so the class's central documented guarantee — "UI is disabled during playback and restored afterward" — is false. The banner still describes an external *Fiji* window; playback is an in-process Swing frame. That stale mental model is the root of the bug.

**7. Exporting overrides silently clears pre-existing exclusions.**
`ExportOverridesFromViewer.java:443-445` resets every `Exclude` flag in Recordings.csv to `false` before re-applying only what is in the viewer's override file. Any exclusion set elsewhere is destroyed. Undocumented.

**8. The Viewer never writes recording exclusions back to Recordings.csv.**
`WriteRecordingExclude.patchRecordingExcluded` is **dead code** (no callers anywhere). `ViewerFrame:453` and `:458` both call `setExcluded(...)` on the in-memory object — the same call twice — and nothing patches the CSV.

**9. The Viewer shows a half-built window on an invalid square layout.**
`ViewerFrame.java:172-175` `return`s from the constructor, leaving every panel `null`; `Viewer.java:105` then calls `setVisible(true)`. Result: an empty window that NPEs on interaction. It should throw. The error message is also a mangled format string — `errorf("Invalid square layout (d x d)")` prints no values.

### Latent / lower severity

**10.** `ExperimentDataLoader.java:110` — `tracksTable.rowCount()` sits *outside* the `try` block, so a missing `Tracks.csv` throws an uncaught NPE rather than returning `null` as the Javadoc promises. (`MainIOInterface`'s read methods return `null` on error — a contract documented nowhere.)
**11.** `FileHelper.java:106` — `line.contains(recordingName)` matches any column and prefix-colliding names (`A4-1` matches `A4-10`). Same class of bug at `RecordingLoader.java:151`.
**12.** `CellAssignmentManager.java:81-83` — with nothing selected, returns the *existing* assignments rather than an empty map, so `ViewerFrame`'s `isEmpty()` guard fails and Assign re-writes the override file.
**13.** `RecordingDisplayUpdater.java:115` — unguarded `getRightImage().getImage()`; `loadImage` is documented to return `null` and does.
**14.** `Viewer.java:109-112` — `catch (Exception ex)` logs a bare message and discards the exception; any startup failure is undiagnosable.
**15.** `CalculateSquareAttributes.java:319` — `double height = IMAGE_WIDTH / dimension;` uses `IMAGE_WIDTH` for the height. Harmless only because the image is square; `IMAGE_HEIGHT` exists and is used correctly elsewhere.
**16.** `ValidationResult` — `infos`, `getInfos()`, `hasInfos()` and the `INFO:` block in `toString()` are permanently dead: there is **no `addInfo()`**. Both the banner and the class Javadoc advertise informational messages.
**17.** `ProjectPathResolver` — `validateProjectFolder` now unconditionally returns `true` (we made a missing config non-fatal today), so the `if (!validateProjectFolder(...))` branch at line 120 is dead code.
**18.** `ProjectDialogController.java:168-169` — the two debug log lines have their labels swapped (project path logs `imagesRootText()` and vice versa).
**19.** `TrackMateUISingle.java:278` — logs `infos.get(0).getRecordingName()` instead of the selected recording; always names the first.
**20.** `RunTrackMateOnExperiment.java:150` — `numberOfInterrupts++; if (numberOfInterrupts >= 1)` is always true; the counter is inert and counts seconds, not interrupts.

### Dead weight

- `PaintTrackFeatureAnalyzer.java` — DELETED. (This entry originally claimed the class body was commented out. That was wrong: it was a complete, compiling `TrackAnalyzer` computing diffusion coefficient, extended diffusion coefficient, total distance and confinement ratio. But it carried no `@Plugin` annotation and nothing referenced it, so SciJava never discovered it and TrackMate never ran it. Dormant, unreachable code — removed on Hans's instruction.)
- Unused imports left by the deleted demo drivers: `java.util.Arrays` in `SweepFlattener:53` and `RunTrackMateOnProjectSweep:58`.
- `RunTrackMateOnProjectSweep` — `summaryRows` is built row by row and never read (its only consumer was the removed `main()`).
- Dangling Javadoc for deleted `main()` methods: `Miscellaneous:61-65`, `JarInfoLogger:227-237` (both still carry `@param args`).
- Commented-out `logLevel` in `PaintRuntime:85` — the source of a documented-but-nonexistent feature.
- Commented-out imports in `TrackMateConfig:36-39`, residue of a "write config to file" method the banner still promises.
- Dead locals: `GenerateSquaresProcessor:151, 200`; `CalculateSquareAttributes:96-106` (ten locals, all superseded by `params.*`); `ExportOverridesFromViewer:346, 362`; `SquareGridPanel:106`; `RecordingFilterDialog:67`.
- Five surviving `main()` demo drivers with hardcoded paths, all in `paint-development-utils` and `TiffMoviePlayer:335`.

---

## Javadoc: what's wrong

**Stale — describes the old behaviour**

- `PaintLogger` L235, L269, L381 — still says "writes to both file and GUI console". The class banner (correctly) says the opposite; the two contradict each other *inside the same file*. Note `raw()` with no sink registered now prints **nowhere**, not even to the file.
- `GenerateSquaresProcessor:83-91` — "applies geometric segmentation, assigns tracks, calculates attributes". It does none of those now; it loads, delegates, writes.
- `SquareGenerationService:23` — refers to "the `Debug` flag". That config section was deleted; the switch is `-Dpaint.debug.dumpTrackAssignmentCsv`. This is the last surviving pointer to a dead config key.
- `SquareGenerationService:17` vs `:22` — says "writes no output files", then two lines later lists the files it writes. Same contradiction in `computeExperiment` (`reads nothing and writes nothing` vs `@throws IOException … if optional debug output … fails`).
- `TrackMateConfig:90` — "using the parameters provided in the given `PaintConfig` instance"; the constructor takes **no arguments**.
- `TrackMateUISingle:80-91` — promises a sweep path and a Generate-Squares trigger. Neither exists in that class (it is a copy-paste of the batch Javadoc).
- `ProjectPathResolver:91` — says it checks the config file exists. It stopped doing that today.
- `RecordingFilterDialog:315-320` — the Javadoc and the line directly beneath it state opposite things about which list is filtered.

**Wrong signature / missing tags**

- `AbstractFileValidator:232` — `rowMatchesTypes` has 5 params, 4 documented; `@param rowNumber` missing.
- `GenerateSquaresProcessor:92` and `:195` — no `@throws IOException`, which both declare.
- `ExperimentInfoWriter:82` — `@return` says `File`; the method returns `Path`.
- `ViewerFrame:733` — `@param scope` documents `"Preview" or "Apply"`; the enum is PREVIEW / RECORDING / EXPERIMENT / PROJECT.
- `PaintPrefs:165, 181` — `putBoolean` / `putString` have Javadoc with zero `@param` tags.
- `CalculateTau:71` — `@param tracks` ends with the orphaned fragment `"points status."`, debris from a deleted parameter.
- `ViewerFrame:476` — `showExclusiveDialog` is documented as "modal"; two of its three callers pass **modeless** dialogs. (Mine — worth correcting.)

**Absent where it matters most**

- `MainIOInterface` — the advertised public I/O façade. Class and ~20 public methods: **zero Javadoc**. Nothing records that every `read*` returns `null` on failure, which is the direct cause of bug 10.
- `PaintConfig`'s entire instance API — undocumented, including the genuinely surprising fact that a `get*` call **mutates** the in-memory tree.
- `RegressionRules` — seven public methods, no Javadoc, and they encode the whole comparison contract. Two are dangerous in a way nobody could guess: `valuesEqual()` silently rounds **both sides to 3 decimals**, so "strict" is not strict below 1e-3; and `correctedValueIfTrackDependent()` rescales density by the track-count ratio, which can mask a genuine regression.
- `Square.Column` — the CSV schema enum, 34 constants, no Javadoc (`Recording.Column` has it).

**Noise**

~150 accessor Javadoc blocks that restate their own identifier (`/** @return the top-left X coordinate. */ public double getX0()`). Worse than useless in one respect: `Square`'s setters silently **round to 2 decimals**, and no Javadoc anywhere says so.

---

## Inline comments: better than the Javadoc

**There are no `TODO`/`FIXME`/`HACK` markers anywhere in the codebase** — one `// TODO These should be JSON parameters` in `PaintTiming:45`, and that one is legitimate. This is not the health signal it appears to be: the debt is recorded as *confidently wrong prose* rather than as admitted uncertainty, which is the more dangerous form.

**The best documentation in the codebase is its inline comments**, and they are recent:

- `PaintLogger:287` — *"File first: the persistent log must never be lost because a UI sink misbehaved."*
- `PaintLogger:117-125` — why `Sink` exists: *"a structural guarantee rather than a runtime isHeadless() check"*.
- `ExperimentDataLoader:151` — *"do NOT show a dialog here. This I/O method runs headless and on worker threads, where a modal dialog would hang."* Prevents a plausible, damaging "improvement".
- `RunTrackMateOnExperiment:145` — *"DO NOT CALL thread.interrupt() — this causes FutureTask.get() to throw InterruptedException."*
- `JsonValidator:103` — *"Gson returns null — without throwing — for input that contains no JSON at all."*
- `GenerateSquaresProcessor:208` — why the debug dump is a system property and not a config key.
- `FileOps:88-105` — the shade-plugin `original-*.jar` race, explained precisely.
- `ViewerFrame:476` — why the dialog lifecycle is centralised: *"if one copy forgets to re-enable the buttons … the viewer locks up for good."*
- `PaintGeometry:38` — *"Pixel width in µm (specified by Nikon)."* Provenance for a magic number. Exemplary.
- `CalculateSquareAttributes:74` — `BACKGROUND_SQUARES_FRACTION` is a fixed methodological constant of the original Python method, deliberately not a user knob.

These explain **why**. They are the model. There are perhaps 20 of them.

**Against that:** a long tail of `// --- Draw background ---`, `// Get the x,y coordinates`, `// Step 1 … Step 5`, emoji (`// 🔥 DEBUG CSV`), second-person notes-to-self (*"adapt to your actual getter/setter"*, *"Build TrackMate config exactly how you do it in batch"*), and refactor-diary comments frozen into the source (*"(REPLACED WITH METHOD REFERENCES)"*, *"(UNCHANGED)"*, *"(UPDATED FOR EMBEDDED SCHEMA)"* — unchanged relative to *what*?).

Several comments are simply false: `CalculateSquareAttributes:211` says values are written with 3 decimals, four lines above constants specifying 2, 3, 4 and 5; `RunTrackMateOnExperiment:131` says *"no interrupts sent"* when `ProjectDialog:359` sends one; `ProjectDialogController:68` says a field is *"null in VIEWER mode"* when it is also null in TRACKMATE_SINGLE.

---

## Verdict

The code is in better shape than its documentation, and the documentation is in worse shape than it looks.

The pattern is sharp and consistent: **anything written as a `/** */` block or as a WHY-comment during a recent fix is good. Anything inside a `/*=====` banner is unmaintained and, more often than not, false.** That is not a discipline failure — it is a direct consequence of the banners being invisible to every tool in the build. Text that nothing checks will drift, and text that drifts while remaining confidently worded is worse than no text at all: a reader who trusts the banners is actively misled, and four of them contain examples that would not compile.

The recommendation is therefore structural rather than editorial: **retire the banner convention**, keep the ~20 excellent WHY-comments, document the three public façades that have no Javadoc at all (`MainIOInterface`, `PaintConfig`'s instance API, `RegressionRules`), and let the compiler enforce the rest.

But do the bugs first. The documentation has been wrong for months and hurt no one. The Viewer is opening the wrong recording's data today.
