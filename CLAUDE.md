# CLAUDE.md — Glyco-PAINT-Java

Guidance for Claude Code when working in this repository.

## What this is

Java rewrite of the Glyco-PAINT processing pipeline (Leiden chemical immunology group). It turns
single-molecule super-resolution recordings (Fiji + TrackMate) into tracks, squares and per-square
statistics (Tau, density, R², variability, …) that end up in published research
(Steuten et al., Nat Commun 2026). **Changing the scientific output by accident is the worst
possible bug here.**

Repo: `Leiden-chemical-immunology/Glyco-PAINT-Java` on GitHub. Work happens on `main` only
(`develop` is gone; `gh-pages` holds the published Maven site/Javadoc).

## Build and test

Java 21 baseline (Fiji Latest ships Java 21). The parent POM compiles with `release 21` and the
enforcer requires JDK `[21,)`. TrackMate is 8.1.6; the ij, imglib2, Bio-Formats, scijava-common and
ome-common pins in the parent POM follow what TrackMate 8.1.6 is built against (its parent
`pom-scijava`), so move them together when TrackMate is upgraded. See
`doc/Analysis - Java 21 and TrackMate 8 2026-11.md`.

```bash
mvn clean test                              # all unit tests + golden-master gate
mvn -B -Djava.awt.headless=true test        # what CI runs (.github/workflows/build-and-test.yml)
mvn clean install                           # full build incl. packaging (see warning below)
mvn -pl paint-viewer -am test               # one module plus its dependencies
```

- `package`/`install` also build `Glyco-PAINT.app` (`paint-app`, jlink + jpackage, so they take
  longer); `install`/`verify` also build macOS .app bundles, Windows .exe wrappers, and
  **copy the Fiji plugin into the local Fiji's `plugins` folder** (found automatically on macOS and
  Windows, or `-Dfiji.dir=<folder>`; see `paint-fiji-plugin/pom.xml`). Use `test` unless packaging is
  actually needed.
- Tests must pass headless. `paint-shared-utils` must stay UI-free; the logger only writes to a UI
  sink if a GUI app registered one.
- Bio-Formats 8 pulls `edu.ucar:cdm-core` from Unidata's repository; IntelliJ may ask to trust
  its certificate. Rejecting is fine once the command-line build has cached the jar in `~/.m2`.
- The golden master starts from a stored `Tracks.csv`, so it does not test TrackMate itself;
  `paint-fiji-plugin/.../trackmate/TrackMateRegressionGateTest` does (see below). Whether
  TrackMate 8 tracks the same as 7.14 is still open (section 6 of the analysis note).

## The golden master — read before touching any calculation

`paint-generate-squares/.../regression/GenerateSquaresRegressionGateTest` runs the whole Generate
Squares pipeline on `paint-generate-squares/src/test/resources/reference-project` and compares
every number with stored known-good output.

- If it fails, assume the change is wrong until proven otherwise.
- Re-blessing (`-Dpaint.updateGolden=squares`, or `=trackmate` for the TrackMate gate; `true` is
  refused) is only for changes that are *meant* to alter the
  output, and only after Hans has agreed. Never re-bless to make a test go green.
- The gate is not omniscient: the comparison engine lives in `paint-compare`, and its notion of
  "equal" (rounding, NaN handling, ignored columns) can hide real differences. See `REVIEW.md`.
- The TrackMate stage has its own gate, `TrackMateRegressionGateTest` (paint-fiji-plugin): the
  plugin's TrackMate step (`RunTrackMateOnRecording.track`) on a small committed crop of
  `221012-Exp-1-A1-1` (`src/test/resources/trackmate-reference`), with *Deterministic Tracking*
  on, compared with a reference `Tracks.csv` (numbers within 0.01, one unit in the last written
  decimal; counts exact). Same rules for re-blessing.

## Modules

| Module | Role |
|---|---|
| `paint-shared-utils` | Core: objects, CSV/table IO (Tablesaw), config, calculations (Tau fit, density, squares). No UI. |
| `paint-ui-common` | Shared Swing components/dialogs |
| `paint-fiji-plugin` | Fiji/TrackMate plugin: run TrackMate on recordings, produce tracks (TrackMate jar in `lib/`) |
| `paint-generate-squares` | Generate Squares pipeline; holds the golden-master test |
| `paint-create-experiment` | Create experiment / `Experiment Info.csv` |
| `paint-viewer` | Viewer for recordings, squares, include/exclude, cell assignment |
| `paint-get-omero` | Download recordings from OMERO |
| `paint-launcher` | Launcher for the applications |
| `paint-app` | Glyco-PAINT as one app (Launcher + the four apps) with built-in Java (the building JDK, 21 or newer), via jlink/jpackage. Part of the normal build; output in `paint-app/target/bundle`, nothing installed. macOS only so far |
| `packaging` | The installers: macOS `.pkg` and Windows installer (NSIS: Glyco-PAINT and/or the separate apps), both installing the Fiji plugin, both with a fixed built-in Java (Temurin 21.0.13), both built on a Mac. Built by the release tool, or by hand with `-Ppackaging`; output in `packaging/target/dist`, plus `Glyco-PAINT.app` in `~/Applications/Glyco-PAINT`. Needs `brew install makensis` |
| `paint-compare` | CSV comparison engine used by the regression gate |
| `paint-development-utils` | Dev tools, incl. `release.ReleaseNewVersion` (see `doc/Release Process.md`) |
| `paint-installer` (+ `-macos`, `-windows`) | Old installers (executable JARs), kept as a backup. Only in the build with `-Pold-installers`; the release tool builds them only with `--old-installers` |

## Documentation

- `README.md`, `doc/Manual.md`, `doc/Features.md` — user-facing.
- `TESTING.md` — what the tests check, in plain language. Update it when adding meaningful tests.
- `TROUBLESHOOTING.md` — external-drive permissions on macOS, JIT stall, log location.
- `REVIEW.md` — verified work list from July 2026; check each item against the code, some may be done.
- `doc/Change Note - *.md`, `doc/Analysis - *.md` — design notes for behaviour changes. A change to
  scientific behaviour gets a change note in `doc/`.
- `HANDOVER.md`, `REFACTORING_PROGRESS.md`, `CHANGELOG.md` — partly **stale** (e.g. HANDOVER still
  talks about `develop`). Context only.

**None of these documents, nor code comments or Javadoc, are ground truth.** Verify every claim
against the actual code before stating or acting on it.

## Working rules (Hans)

- **Ask before coding.** Discuss and propose first; start writing code only after an explicit OK.
  Do not introduce new components, behaviour or design rules unasked. Hans decides when a point is
  settled.
- When offering choices, explain each one: background, what it means in practice, consequences.
- If Hans makes an observation, say explicitly whether it is right and why, before or alongside any fix.
  If he asks something mid-task, answer it first.
- Fix general causes, not special cases. No duplication — extend existing mechanisms before adding
  new ones; more code is fine when it makes the structure clearer and less error-prone.
- Don't attribute earlier changes to "someone" — check `git log`/`git blame`.
- For a bug: write a failing test first, then fix.
- **Git:** never commit or push unless asked. When asked to commit: commit all outstanding changes
  (except work from another session), in small themed commits, messages in English, via heredoc
  (zsh chokes on quotes/em-dashes in `-m`):

  ```bash
  git commit -F - <<'EOF'
  Subject line

  Body.
  EOF
  ```

- Commands for Hans to run: plain command blocks, no comments inside, each starting with an
  absolute `cd`, e.g. `cd /Users/hans/JavaPaintProjects/Glyco-PAINT-Java`.
- Keep him posted with short progress updates during long work.
- On a mounted/FUSE filesystem (Cowork), do not edit with `sed -i`/`perl -i` — it leaves
  `.fuse_hidden*` files that can be swept into commits.
