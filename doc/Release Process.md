# Release Process — `ReleaseNewVersion`

A record of what the `release.ReleaseNewVersion` tool
(`paint-development-utils`) does, what it produces, where the output goes, and
what you need to do afterwards.

---

## What it is

`ReleaseNewVersion` orchestrates the **full multi-module build** of the
Glyco-PAINT suite for **both macOS and Windows**, assembles the two installers,
and — for a full release — bumps the version, tags, and pushes to GitHub. It
drives Maven and Git via external processes.

It must be run with the project as the working directory (it derives paths from
`user.dir`): `BASE_PATH = <parent>/Glyco-PAINT-Java`, and output goes to a
sibling folder `<parent>/Glyco-PAINT-Builds`.

**Prerequisites:** `mvn`, `git`, and (for publishing) the `gh` CLI installed and
authenticated. The app modules must build cleanly.

---

## The three modes

Run with **no arguments** for an interactive menu, or pass a flag:

| Mode | How to invoke | Version | Git tag / push |
|------|---------------|---------|----------------|
| **1. Rebuild only** | no args → choose `1` (default) | unchanged | no |
| **2. Bump version only** | no args → choose `2`, or `--bump-version` | bumps (drops `-SNAPSHOT`), aligns all POMs | no |
| **3. Full release** | no args → choose `3`, or `--release` | bumps to release version | **yes** — tags `v<version>`, pushes, then bumps POMs to next `-SNAPSHOT` and commits |

All three modes build the same artifacts (below). Only mode 3 changes Git state.

> To just **produce installers without releasing** (e.g. for testing), use
> **mode 1 (Rebuild only)** — it regenerates a fresh `payload.zip` from the
> current build and builds both installers, with no version change and no
> Git activity.

---

## What it generates

For every app module (`paint-viewer`, `paint-generate-squares`,
`paint-get-omero`, `paint-create-experiment`, `paint-fiji-plugin`) and the Fiji
plugin, it builds and collects:

1. **macOS `.app` bundles** (via the `macos-appbundle` Maven profile).
2. **Windows `.exe`** launchers (via the `windows-exe` Maven profile).
3. The **Fiji plugin** fat JAR (`paint-fiji-plugin-*-jar-with-dependencies.jar`).
4. A fresh **`payload.zip`** for each installer (the platform's apps + the
   plugin), written into each installer module's `src/main/resources/`.
5. The two **installer fat JARs**, built from those payloads and renamed.

---

## Where the output is

Everything lands in a **sibling** folder of the repo (not inside it):

```
Glyco-PAINT-Builds/
└── Glyco-PAINT-<version>/
    ├── Windows/       # per-app .exe launchers
    ├── macOS/         # per-app .app bundles
    ├── Plugins/       # Fiji plugin fat JAR
    └── Installers/
        ├── Glyco-PAINT-Installer-macOS-<version>.jar
        └── Glyco-PAINT-Installer-Windows-<version>.jar
```

The two files in `Installers/` are the deliverables you hand to users. (Note:
the build also rewrites `payload.zip` inside each
`paint-installer/paint-installer-*/src/main/resources/` — those are generated,
git-ignored artifacts.)

---

## What you need to do afterwards

The tool builds (and, in mode 3, tags/pushes) — it does **not** publish the
installers to GitHub or install anything on user machines. After it finishes:

1. **Smoke-test the installers.** Run the macOS installer
   (`java -jar Glyco-PAINT-Installer-macOS-<version>.jar`) and confirm it
   installs the selected apps and drops the Fiji plugin into a valid Fiji
   `plugins` folder. Do the same for Windows on a Windows machine.
2. **Publish the release assets (mode 3).** Mode 3 pushes the tag `v<version>`,
   but uploading the installer JARs as **GitHub release assets is a separate,
   manual step** (e.g. via `gh release create`/`gh release upload`, or the
   `github.GitHubManager` helper, or the GitHub web UI).
3. **Distribute** the installer JARs from `Installers/` to end users, who then
   run them to install the apps and the Fiji plugin.

---

## How users launch the installer

The installer files are **executable JARs**, not native self-extracting
installers. Each has a `Main-Class` in its manifest, so it runs with:

```
java -jar Glyco-PAINT-Installer-macOS-<version>.jar
```

When it runs, the embedded `payload.zip` is extracted and the selected apps and
the Fiji plugin are installed — so it behaves like a self-contained installer,
but it needs a **JVM present** to run (unlike a native `.exe`/`.dmg`/`.pkg`).

Double-clicking works only under these conditions, which are worth telling users:

- **Java must be installed.** With no JRE, a double-click does nothing useful.
- **macOS Gatekeeper.** A JAR downloaded from GitHub is unsigned and quarantined,
  so a double-click is usually blocked ("unidentified developer"). The user must
  **right-click → Open** (or run `xattr -d com.apple.quarantine <installer>.jar`).
  Note: the installer strips quarantine from the *apps it installs*, but that
  does not help the *installer JAR itself* — the user still hits Gatekeeper on it.
- **Windows.** Double-clicking a `.jar` works only if the JRE installer set the
  `.jar` file association; otherwise the user runs `java -jar`.

For reliable launching (and for smoke-testing), `java -jar <installer>.jar` is
the sure path.

---

## Notes

- The installer build runs in **all** modes (including rebuild-only), despite a
  stale in-code comment suggesting otherwise.
- In rebuild-only mode the artifacts are named with the current `-SNAPSHOT`
  version.
- This tool is a developer aid; it is not covered by the test suite or the
  regression gate.
