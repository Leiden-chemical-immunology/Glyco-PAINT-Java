# paint-development-utils

Developer-only utilities for the Glyco-PAINT project. Nothing here ships in the
end-user applications — these are command-line/IDE-run tools that support
**data migration, releasing, GitHub housekeeping, and project cleanup**. Several
are run by executing a class's `main()` directly (some contain hardcoded local
paths and are meant to be edited before use).

The module is organised into four directories:

| Directory | Purpose |
|-----------|---------|
| `convert/` | Migrate legacy (Python-era) experiment CSVs into the current Java pipeline format |
| `release/` | Automate the multi-module build, versioning, and packaging/release |
| `github/`  | Manage GitHub releases and CI runs via the `gh` CLI |
| `utils/`   | Miscellaneous project housekeeping (cleanup, POM reformatting) |

---

## `convert/` — legacy data conversion

One-off migration tools that convert the **old Python pipeline's CSV output**
into the **current Java pipeline's format** (renaming/reordering columns to the
current schema), then validate the result. Input files use the Python-era names
(e.g. `All Squares.csv`, `All Tracks.csv`, `All Recordings.csv`,
`Experiment Info - Python.csv`) and are written out under the current names
(`Squares.csv`, `Tracks.csv`, `Recordings.csv`, `Experiment Info.csv`).

| File | Role |
|------|------|
| `Converter.java` | Base class **and** the entry point (`main`) that runs all four converters over one experiment directory, then validates the output. Contains hardcoded local paths — edit before running. |
| `CsvIO.java` | Low-level CSV read/write helper (`readSimpleCsv` / write) used by the converters. |
| `SquaresConverter.java` | Converts `All Squares.csv` → `Squares.csv` (target column order + renames). |
| `TracksConverter.java` | Converts `All Tracks.csv` → `Tracks.csv`. |
| `RecordingsConverter.java` | Converts `All Recordings.csv` → `Recordings.csv`. |
| `ExperimentInfoConverter.java` | Converts `Experiment Info - Python.csv` → `Experiment Info.csv`. |

**Usage:** edit the paths in `Converter.main`, then run `Converter`. Each
converter can also be run/used individually via its `run()` method.

---

## `release/` — build and release automation

Automates the full multi-module build/release for macOS and Windows: version
bumping, compilation, packaging, and optional Git tagging and pushing.

Two entry points, plus supporting helpers in the `release.support` sub-package.

**Entry points (`release/`):**

| File | Role |
|------|------|
| `ReleaseNewVersion.java` | Orchestrates the **full** release: version bump, multi-module build, macOS/Windows packaging, assembling both installers, and optional Git tag & push. Options: `--bump-version` (next `-SNAPSHOT`, no tag) and `--release` (drop `-SNAPSHOT`, build, tag & push, then bump to next snapshot). See `doc/Release Process.md`. |
| `BuildSelector.java` | Standalone **Swing GUI** ("Deliverables Builder") for building **individual** deliverables à la carte: tick which macOS `.app`s, Windows `.exe`s, and/or the Fiji plugin to build, click Generate, and it builds `paint-shared-utils` first, then runs each selected module's Maven build, streaming output to a log pane. Unlike `ReleaseNewVersion`, it does **not** assemble installers or touch versions/Git — it's the "rebuild just this app to test a change" tool. |

**Support helpers (`release/support/`):**

| File | Role |
|------|------|
| `MavenSupport.java` | Runs Maven operations (install parent POM, build modules) during the release. |
| `GitUtils.java` | Git operations used by the release (tag, push, commit, status). |
| `PomUtils.java` | Reads and updates `<version>` in `pom.xml` files (version bumping). |
| `ProcessRunner.java` | Executes external system processes and captures their output (used to run Maven, Git, etc.); enforces a Java 8 environment where possible. |
| `FileOps.java` | File-system helpers for the release pipeline (payload zipping, file collection). |
| `PathsConfig.java` | Path constants / directory layout for the release pipeline. |
| `VersionInfo.java` | Small value object encapsulating a parsed version. |

**Usage:** run `ReleaseNewVersion` (optionally with `--bump-version` or
`--release`) for a full build/release, or run `BuildSelector` to rebuild specific
deliverables interactively.

---

## `github/` — GitHub release & CI housekeeping

`GitHubManager.java` wraps the **GitHub CLI (`gh`)** (invoked via
`ProcessBuilder`) to manage releases and workflow runs on the repository.
Supports a **dry-run** mode (prints the commands instead of executing them).

Capabilities include:

- List releases / list release tags only.
- Delete a release by tag; delete **all** releases; delete **pre-releases**;
  keep only the latest *N* releases.
- List tags.
- List and delete GitHub Actions workflow runs.

**Requires** the `gh` CLI to be installed and authenticated. Typically used from
the release process to clean up old releases and CI runs.

---

## `utils/` — project housekeeping

| File | Role |
|------|------|
| `CleanupUtility.java` | Removes generated/non-source files from a project tree. Takes a root directory and a **mode** (`TRACKMATE` or `GENERATE_SQUARES`) selecting which generated CSV outputs to target; supports `--dry-run` (show without deleting) and `--old` / `--all` (legacy vs. legacy+new CSV names). Also clears OS cruft (e.g. `.DS_Store`) and the generated image directories. |
| `ReformatPoms.java` | Reformats every `pom.xml` in the project consistently (4-space indentation, correct nesting) so POM formatting stays uniform. |

**Usage (CleanupUtility):**
```
java utils.CleanupUtility <root-directory> <mode> [--dry-run] [--old | --all]
  <mode>    : TRACKMATE | GENERATE_SQUARES
  --dry-run : show what would be deleted without deleting
  --old     : target legacy CSV names (without " Java")
  --all     : target both legacy and new CSV names
```

---

## Notes

- These tools are **not** covered by the automated test suite or the regression
  gate — they are developer aids run manually.
- Some entry points (notably `convert/Converter`) contain **hardcoded local
  paths** and must be edited before use.
- `release/` and `github/` shell out to external tools (`mvn`, `git`, `gh`),
  which must be installed and (for `gh`) authenticated.
