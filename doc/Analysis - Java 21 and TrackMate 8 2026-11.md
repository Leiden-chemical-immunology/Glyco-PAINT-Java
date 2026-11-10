# Analysis — Moving to Java 21 and TrackMate 8

Date: 2026-11-10. Status: **exploration only — nothing decided, nothing changed in the code.**
First test results and the comparison plan are in sections 5 and 6.

Every statement about this repository below was checked against the code, the POMs or the built
JARs on this date. Statements about Fiji and TrackMate come from the sources at the end; where
something could not be verified, it says so.

---

## 1. Background: where Fiji and TrackMate are now

- **Fiji now comes in two distributions.** *Fiji Latest* runs on Java 21 and is the recommended
  download. *Fiji Stable* is the Java 8 version, offered "only if you need Java 8, an older
  operating system, or compatibility with older plugins".
- **Java 8 Fiji is being wound down.** A Fiji developer issue (September 2025) warns that Java 8
  installations will stop receiving updates: "even security and QoL bug-fixes will cease".
  Java 25 is planned as the next step for Fiji Latest.
- **TrackMate has moved to version 8.** The TrackMate 8.1 announcement (December 2025) says the
  new version is not shipped in the Java 8 Fiji. In practice TrackMate 8 means Fiji Latest and
  Java 21.
- This is not brand new: Java 21 builds were in community testing from about late 2024, and became
  the recommended download during 2025.

Glyco-PAINT is built for Java 8 and compiled against **TrackMate 7.14.0**.

**But in practice it already runs on Java 21 and TrackMate 8** (found 2026-11-10, see 5.1): the
normal Fiji on the Mac Studio (`/Applications/Fiji.app`) has Java 21.0.7 and TrackMate 8.1.6.
The Java 8 baseline therefore only really binds the build and the standalone apps.

---

## 2. Findings that matter regardless of any move

### 2.1 Java 8 is not actually enforced

The parent `pom.xml` contains a `maven-enforcer-plugin` rule `requireJavaVersion [1.8,1.9)`, but
only inside `<pluginManagement>`. No module activates it, so it never runs.

Evidence: the manifests of the locally built JARs (`paint-fiji-plugin`, `paint-viewer`) say
`Build-Jdk: 24.0.2`. The class files are still Java 8 bytecode (major version 52), because
`maven.compiler.source/target` are `1.8`. Only CI really builds on JDK 8
(`.github/workflows/build-and-test.yml`).

So the local builds already use a modern JDK, and only the bytecode level keeps Java 8
compatibility. Note: `CLAUDE.md` currently says the parent POM *enforces* `[1.8,1.9)` — that is
wrong and should be corrected.

### 2.2 The Fiji plugin JAR carries its own copy of ImageJ and other libraries

In `paint-fiji-plugin/pom.xml`, `net.imagej:ij` is a normal (compile-scope) dependency, so the
shade plugin copies it into the fat JAR. The built
`paint-fiji-plugin-0.0.138-SNAPSHOT-jar-with-dependencies.jar` contains roughly 500 `ij/…`
classes (ImageJ 1.54h), plus slf4j, Guava, fastutil, logback, pdfbox, ICU, Apache Commons and more.

That JAR is installed into `Fiji.app/plugins`, next to Fiji's own copies of the same libraries.
Which version wins depends on class-path order. This is a latent risk on Fiji Stable already, and
it grows on Fiji Latest, whose library versions are further away from ours.

The usual remedy is to mark everything Fiji provides as `provided` (TrackMate, scijava-common and
ome-common already are), so the plugin JAR only carries what Fiji does not have.

---

## 3. What a move would involve

### 3.1 Java code — little work

A search for things that disappeared or changed after Java 8 found nothing significant:

- no `sun.*` / `com.sun.*` APIs, JAXB, Nashorn, SecurityManager or `finalize()`;
- the only `setAccessible` call (`Miscellaneous.initialiseDoublesToNaN`) is on our own classes;
- the SciJava annotation processor is already named explicitly in `paint-fiji-plugin`, which
  JDK 23+ requires.

### 3.2 Build, launchers and tooling — mechanical, but in several places

| Place | Now | What changes |
|---|---|---|
| Parent `pom.xml` | `source/target 1.8`, inactive enforcer `[1.8,1.9)` | `release 21` (or keep 8 bytecode at first); decide whether to activate the enforcer |
| `.github/workflows/*.yml` | `java-version: 8` | 21 |
| macOS launch script (in the POM of each app module: viewer, generate-squares, create-experiment, get-omero) | `java_home -v1.8`, error "Java 8 not found" | look for 21 (or bundle a runtime) |
| macOS `Info.plist` (same POMs) | `JVMVersion 1.8*` | update |
| Windows launch4j | `minVersion 1.8.0` | fine as is, possibly raise to 21 |
| `PaintLauncher` | `java -jar` from the PATH | whatever is on the PATH must be 21 |
| `ReleaseNewVersion` / `ProcessRunner.enforceJava8` | forces JDK 8 for the release build | remove or retarget |
| `app.jvm.args` | `-XX:ReservedCodeCacheSize=512m -XX:-UseCodeCacheFlushing` (JDK 8 code-cache workaround) | probably unnecessary on 21; **not verified** whether `UseCodeCacheFlushing` is still accepted — test |
| `doc/development.md`, `README.md`, Manual | mention Java 8 | update |

### 3.3 TrackMate 7.14 → 8 — the real uncertainty

The plugin uses a small, mainstream part of TrackMate (all in
`paint-fiji-plugin/.../trackmate/RunTrackMateOnRecording.java`, plus `TrackDataExporter`,
`TrackAttributeCalculations` and the `ExcludeEdgeTracksTest`):

- `Settings`, `LogDetectorFactory`, `SparseLAPTrackerFactory`, configured with string keys
  (`RADIUS`, `THRESHOLD`, `LINKING_MAX_DISTANCE`, `MAX_FRAME_GAP`, …);
- `FeatureFilter`, `addAllAnalyzers`, `Model`, `TrackModel`, `Spot`, `FeatureModel`;
- `setNumThreads(1)` for the *Deterministic Tracking* setting;
- display/capture: `DisplaySettingsIO`, `HyperStackDisplayer`, `CaptureOverlayAction`.

The sensitive spot is **`processAfterDetection`**: a hand-made copy of the steps of TrackMate
7.14.0's `process()` without the detection step. Its own Javadoc says "Check this list again when
TrackMate is upgraded". If TrackMate 8 added, removed or reordered a step, this copy silently
diverges.

Not verified: what exactly changed in the TrackMate 8 API. The GitHub releases page could not be
read from this session. The reliable way to find out is to compile against TrackMate 8.

Related dependency points:

- `ij 1.54h`, `imglib2 7.1.5`, `bio-formats 6.13.0` are older than what Fiji Latest ships. The
  cleanest approach is to take versions from the `pom-scijava` BOM that matches the Fiji version,
  instead of pinning them by hand.
- `paint-fiji-plugin/lib/TrackMate-7.14.0.jar` is not referenced by any POM — apparently a leftover.

### 3.4 Installers

The installers look for Fiji at `~/Applications/Fiji.app` and `/Applications/Fiji.app` (macOS) and
`Fiji`, `Fiji-win64`, `Fiji-win32`, `Fiji-Windows` under the local app-data folder (Windows), then
ask the user.

Fiji Latest (macOS, `fiji-latest-macos-arm64-jdk.zip`) unpacks to a folder called **`Fiji`**, not
`Fiji.app` (seen 2026-11-10). The macOS installer only looks for `Fiji.app`, so it will not find a
Fiji Latest installation by itself; the user has to pick the folder. The Windows installer already
looks for `Fiji`. The `fiji.dir` property in `paint-fiji-plugin/pom.xml` (`/Applications/Fiji.app`)
has the same issue for development builds.

### 3.5 Which Java the user needs

- **Fiji plugin:** nothing to install. Fiji Latest (the `-jdk` download) brings its own Java — seen
  2026-11-10: Java 21.0.7 without any separate installation. The plugin runs on that.
- **Standalone apps** (Viewer, Generate Squares, Create Experiment, Get OMERO): as packaged now,
  they do **not** bring their own Java. The macOS launch script looks for an installed Java with
  `/usr/libexec/java_home`, the Windows launch4j wrapper looks for an installed runtime. Today the
  user must install Java 8 separately; after a move it would be Java 21.

Three ways to handle this (not decided):

1. **User installs JDK 21, as today with Java 8.** No extra work, apps stay small. One more step
   for the user, with room for mistakes (wrong version, several Javas side by side).
2. **Bundle a runtime with `jpackage`.** Part of JDK 14+, so only available once Java 8 is dropped.
   Produces a `.app` / `.exe` with its own Java inside; the user installs nothing. Costs roughly
   50–100 MB per app — four apps add up unless they share one runtime, which `jpackage` does not do
   by itself. Replaces the hand-made `.app` construction in the POMs and the launch4j setup: a
   substantial but one-off packaging change. Apps remain unsigned, as now (the installer already
   removes the macOS quarantine flag).
3. **Use Fiji's Java.** Every user has Fiji Latest anyway, so the launcher could look for the Java
   inside the Fiji folder. Nothing extra to install or download, but fragile: it depends on where
   Fiji is installed (and under which folder name — `Fiji`, not `Fiji.app`), and a future Fiji on
   Java 25 would silently change the apps' Java as well.

Option 2 is the cleanest for users but is real packaging work; option 1 changes nothing compared
with today.

### 3.6 The scientific output — the biggest item

The golden-master test (`GenerateSquaresRegressionGateTest`) starts from the stored `Tracks.csv`
in `paint-generate-squares/src/test/resources/reference-project/221012/`. **It does not cover the
TrackMate step at all.** If TrackMate 8 detects spots or links tracks slightly differently, no
existing test will notice.

A switch therefore needs its own validation: the same recordings, same settings, tracked with 7.14
and with 8, and the resulting tracks compared (spot count, track count, durations, positions), then
pushed through Generate Squares to see the effect on Tau, density and the other square attributes.
`paint-compare` can serve as the comparison engine. A behaviour change of this kind deserves a
change note in `doc/`, like the earlier TrackMate notes.

---

## 4. Ways to try it without risking the current project

### A. A second copy of the repository

Clone into a separate folder (e.g. `/Users/hans/JavaPaintProjects/Glyco-PAINT-Java-21`) on its own
branch and migrate that copy.

- Tests everything: code, build, launchers, jar clashes, installers.
- **Trap:** both copies have version `0.0.138-SNAPSHOT`, and `mvn install` writes *outside* the
  project folder — into `~/.m2`, into `/Applications/Fiji.app/plugins`, and into
  `~/Applications/Glyco-PAINT`. Run in the copy, it overwrites the working Java 8 installation.
  In the copy, use only `mvn test` / `mvn package`, or first give it a different version and a
  different `fiji.dir`.

### B. A git worktree

Same as A, but the second folder shares the one `.git`, so changes are easy to bring across later.
Same install trap.

### C. A small, separate test project

A minimal Maven project that depends only on TrackMate 8 (via `pom-scijava`). It opens one
recording, runs LoG detection and SparseLAP tracking with the current Glyco-PAINT settings, and
writes the tracks to CSV. That file is compared with the `Tracks.csv` Glyco-PAINT produces with
TrackMate 7.14 for the same recording.

- Answers the two biggest unknowns: does the API still work as the plugin uses it, and do the
  results change?
- Leaves Glyco-PAINT completely untouched.
- Does not test packaging, installers or jar clashes. It is new code, but throwaway.

### D. No project at all — the current plugin in Fiji Latest

Java 8 bytecode runs on Java 21, so the existing plugin may already work in Fiji Latest, as long
as TrackMate 8 is compatible with the calls it makes.

1. Download Fiji Latest and unpack it in a **separate** location, not over
   `/Applications/Fiji.app`.
2. Copy `paint-fiji-plugin-…-jar-with-dependencies.jar` into its `plugins` folder.
3. Use a **copy** of a small test project, because a run overwrites `Tracks.csv` and the other
   output files.
4. Run one recording and watch the console and the Paint log.

Caveats: Fiji (`IJ_Prefs.txt`) and Glyco-PAINT (`PaintPrefs`) keep their preferences in the user
profile, which both Fiji installations share. If it fails, it may not be clear whether the cause
is TrackMate 8 or a library clash (see 2.2).

### Possible order

D first (about half an hour, no code), then C for hard numbers on the tracking. A or B only once
there is a decision to go ahead. Independently, fixing the plugin dependencies (2.2) is useful on
Java 8 already and makes any later move cleaner.

---

## 5. First test — current plugin on Fiji Latest (2026-11-10)

Setup: Fiji Latest unpacked in `/Users/hans/Downloads/Fiji`, updated with *Help ▸ Update…*.
*Help ▸ About ImageJ* shows **ImageJ 1.54p, Java 21.0.7**; the `jars` folder contains
**`TrackMate-8.1.6.jar`**. The existing `paint-fiji-plugin-0.0.138-SNAPSHOT-jar-with-dependencies.jar`
(Java 8 bytecode, compiled against TrackMate 7.14.0) was copied unchanged into `Fiji/plugins`.

Results:

- **The plugin loads and runs.** A recording was opened and processed through TrackMate without
  errors. None of the TrackMate calls the plugin makes broke between 7.14 and 8.1.6.
- **Bio-Formats progress messages are no longer silenced.** The Console fills with
  `Parsing block 'ImageDataSeq' NN%` lines while an `.nd2` file is read. On Fiji Stable these are
  suppressed by `DebugTools.setRootLevel("OFF")` in `RunTrackMateOnRecording` (line 76). Likely
  cause, **not verified**: the plugin JAR carries its own slf4j 1.7 / logback 1.2, while Fiji Latest
  uses newer versions, so the call may silence a different logger than the one Bio-Formats writes
  to (see 2.2). Cosmetic, though thousands of console lines may slow a run slightly.
- **Two ImageJ versions side by side:** Fiji Latest has ImageJ 1.54p; the plugin JAR carries
  ImageJ 1.54h (see 2.2). No problem observed yet.

### 5.1 The normal Fiji is already on Java 21 and TrackMate 8

Checked the same day on the regular installation `/Applications/Fiji.app`:

- `jars/TrackMate-8.1.6.jar` (file date 3 Feb 2026);
- `java/macos-arm64/zulu21.42.19-ca-jdk21.0.7-macosx_aarch64` — Java 21.0.7.

So the regular Fiji is effectively the same setup as the Fiji Latest in `Downloads`. Presumably an
update migrated it from Java 8 to Java 21 (the Fiji updater offers such a migration); not verified.

Consequences:

- **The test above compared less than intended.** Both Fijis have TrackMate 8.1.6; the only
  difference was the surrounding Fiji. That the plugin works on Java 21 and TrackMate 8 was already
  true in daily use.
- **Since when?** The ImageJ updater normally gives a downloaded file the timestamp it has on the
  update site, so 3 Feb 2026 is most likely the *publication* date of TrackMate 8.1.6, not the
  installation date (updater behaviour not verified). Runs on this Mac since some date on or after
  3 Feb 2026 used TrackMate 8.
- **Results produced since then** were made with TrackMate 8. Whether that changed anything compared
  with 7.14 is exactly what the comparison in section 6 must show.
- **Golden master:** it is not known which TrackMate version produced the reference `Tracks.csv`
  (`paint-generate-squares/src/test/resources/reference-project/221012/`). The gate does not run
  TrackMate, so this does not affect the gate itself, but it matters when comparing with it.
- **`processAfterDetection`** has in practice been running on TrackMate 8.1.6, while its comment
  says it matches `process()` in 7.14.0. Checking it against the 8.1.6 source is now more urgent.
- **Other users:** unknown which Fiji (Stable/Java 8 or Latest/Java 21) the other users in Leiden
  run.

### 5.2 Not yet established

- whether the **results** are the same — see the comparison plan in section 6;
- whether `processAfterDetection` still matches `process()` in TrackMate 8.1.6 — it runs, but that
  does not prove it does the same thing. To check against the TrackMate 8.1.6 source.

---

## 6. Comparison experiment — old setup versus new setup

Goal: find out whether TrackMate 8.1.6 gives the same scientific output as TrackMate 7.14, with
everything else held equal.

**Changed after 5.1:** the normal Fiji is itself the new setup (Java 21, TrackMate 8.1.6). The
"old" side therefore needs a **separate Fiji Stable (Java 8) with TrackMate 7.14**: downloaded as
the Java 8 version, unpacked in its own folder, and **not updated** (an update may migrate it).
Check its `jars` folder for `TrackMate-7.*.jar` before use. In the steps below, "old Fiji" means
that installation and "new Fiji" means `/Applications/Fiji.app` (or the identical Fiji Latest in
`Downloads`).

### Preparation

1. **Pick one small experiment** — a handful of recordings is enough. Deterministic Tracking runs
   single-threaded and is slow.
2. **Make two copies of the project folder**, e.g. `… Compare-Old` and `… Compare-New`. A run
   overwrites `Tracks.csv` and the other output files, so the original must not be used.
3. **Switch on *Deterministic Tracking* in both copies.** Without it, multithreaded linking already
   differs from run to run, and a TrackMate 8 effect cannot be told apart from chance. All other
   settings (`Paint Configuration.json`) identical — ideally the second copy is an exact copy of
   the first.
4. **Use the same plugin JAR in both Fijis.** Check that
   the `plugins/paint*.jar` in the old and the new Fiji have the same date and size; if not, copy
   the newer one into both.

### Runs

5. Run Glyco-PAINT from the old Fiji (Java 8, TrackMate 7.14) on `Compare-Old`.
6. Run it from the new Fiji (`/Applications/Fiji.app`) on `Compare-New`. One after the other,
   not simultaneously — both Fijis share the same preferences.
7. Run Generate Squares on both copies with the same settings.

### Comparison

- Spot and track counts per recording, from `Tracks.csv` and the logs.
- Whether the tracks themselves match: positions, durations, spots per track.
- `Squares.csv` and `Recordings.csv`: Tau, density, R² and the other attributes, using the same
  comparison engine as the golden-master test (`paint-compare`).

If an existing check cannot do part of this, discuss before building anything new. The outcome —
identical, or different and by how much — goes into this note, and a real behaviour difference
gets its own change note.

---

## 7. Open questions

- What exactly changed in the TrackMate 8 API and in LoG / SparseLAP behaviour?
- Does `processAfterDetection` still match `process()` in TrackMate 8?
- Under which folder name does Fiji Latest install on Windows? (macOS: `Fiji` — answered in 3.4.)
- Which Fiji (Java 8 or Java 21) do the other users run?
- Since when exactly has the normal Fiji on the Mac Studio run TrackMate 8?
- Is `-XX:-UseCodeCacheFlushing` still accepted on Java 21, and is the code-cache workaround still
  needed there?
- Should the standalone apps (Viewer, Generate Squares, …) move to Java 21 together with the
  plugin, or can they stay on 8 for a while? (Technically they are independent: they run in their
  own JVM.)
- Should the apps bundle their own Java runtime, rather than relying on one being installed?
  (Options in 3.5.)

---

## Sources

- [Fiji Downloads](https://imagej.net/software/fiji/downloads)
- [fiji/fiji issue #413 — How stable should Fiji-Stable be?](https://github.com/fiji/fiji/issues/413)
- [A new version of TrackMate… (FocalPlane, December 2025)](https://focalplane.biologists.com/2025/12/12/a-new-version-of-trackmate-with-improved-integration-of-key-ai-algorithms-and-track-edition-improvements/)
- [TrackMate on GitHub](https://github.com/trackmate-sc/TrackMate)
- [Fiji-Latest Java 21 bundles ready for community testing (image.sc)](https://forum.image.sc/t/fiji-latest-java-21-bundles-ready-for-community-testing/112417)
