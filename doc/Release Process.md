# Release Process — `ReleaseNewVersion`

A record of what the `release.ReleaseNewVersion` tool
(`paint-development-utils`) does, what it produces, where the output goes, and
what you need to do afterwards.

---

## What it is

`ReleaseNewVersion` orchestrates the **full multi-module build** of the
Glyco-PAINT suite for **both macOS and Windows**, builds the two installers (a
macOS `.pkg` and a Windows `setup.exe`, both with a built-in Java), and — for a
full release — bumps the version, tags, and pushes to GitHub. It
drives Maven and Git via external processes.

It must be run with the project as the working directory (it derives paths from
`user.dir`): `BASE_PATH = <parent>/Glyco-PAINT-Java`, and output goes to a
sibling folder `<parent>/Glyco-PAINT-Builds`.

**Prerequisites:** `mvn`, `git`, `makensis` (for the Windows installer:
`brew install makensis`; the tool stops at the start if it is missing), and (for
publishing) the `gh` CLI installed and authenticated. The app modules must build cleanly. For a full release (mode 2) the
working tree must be clean and the tag `v<version>` must not exist yet; otherwise
the tool stops before changing anything.

Before building the apps one by one, the tool installs them together with every
project module they depend on (`mvn install -pl <apps> -am`), so a new library
module needs no change to the tool.

---

## The two modes

Run with **no arguments** for an interactive menu, or pass a flag:

| Mode | How to invoke | Version | Git tag / push |
|------|---------------|---------|----------------|
| **1. Rebuild only** | no args → choose `1` (default) | unchanged | no |
| **2. Full release** | no args → choose `2`, or `--release` | bumps to release version | **yes** — commits the POMs as `Release <version>` and tags that commit `v<version>`, then bumps the POMs to the next `-SNAPSHOT` and commits, then pushes the tag. Only tracked `pom.xml` files are committed. The branch itself is not pushed. |

Both modes build the same artifacts (below). Only mode 2 changes Git state.
(There used to be a mode "bump version only"; it left the POMs on the release
version without a commit or tag, and was removed.)

The option `--old-installers` (on its own it still shows the menu, or combined
with `--release`) also builds the old installers; see
[Old installers (backup)](#old-installers-backup).

> To just **produce installers without releasing** (e.g. for testing), use
> **mode 1 (Rebuild only)** — it builds both installers from the current code,
> with no version change and no Git activity.

---

## What it generates

For every app module (`paint-viewer`, `paint-generate-squares`,
`paint-get-omero`, `paint-create-experiment`, `paint-fiji-plugin`) and the Fiji
plugin, it builds and collects:

1. **macOS `.app` bundles** (via the `macos-appbundle` Maven profile).
2. **Windows `.exe`** launchers (via the `windows-exe` Maven profile).
3. The **Fiji plugin** fat JAR (`paint-fiji-plugin-*-jar-with-dependencies.jar`).
3a. **`Glyco-PAINT.app`** (module `paint-app`): the Launcher and the four apps as one
   application with a built-in Java runtime (jlink + jpackage, from the JDK 21 or newer that
   the toolchains plugin selects). Built once from the project root with `-pl paint-app -am`
   and copied into `macOS/`, next to the separate apps. macOS only: jpackage builds for the
   system it runs on.
4. The two **installers**, built from the module `packaging` with
   `mvn package -Ppackaging -pl packaging -am -DskipTests`:
   - `Glyco-PAINT-<version>-macos.pkg`: Glyco-PAINT and/or the separate apps, and the Fiji plugin.
   - `Glyco-PAINT-<version>-windows-setup.exe`: the same for Windows (NSIS).

   Both have a fixed built-in Java (Temurin 21), so users do not install Java. See
   `packaging/pom.xml` and the Manual. The packaging build also puts a copy of
   `Glyco-PAINT.app` in `~/Applications/Glyco-PAINT`.

The version commands (`versions:set`, `versions:update-child-modules`) run with
`-Ppackaging,old-installers`, so the POMs of `packaging` and the old installers,
which are not in the normal build, get the new version too.

---

## Where the output is

Everything lands in a **sibling** folder of the repo (not inside it):

```
Glyco-PAINT-Builds/
└── Glyco-PAINT-<version>/
    ├── Windows/       # per-app .exe launchers
    ├── macOS/         # per-app .app bundles, plus Glyco-PAINT.app
    ├── Plugins/       # Fiji plugin fat JAR
    └── Installers/
        ├── Glyco-PAINT-<version>-macos.pkg            (~640 MB)
        └── Glyco-PAINT-<version>-windows-setup.exe    (~290 MB)
```

The two files in `Installers/` are the deliverables you hand to users.

---

## What you need to do afterwards

The tool builds (and, in mode 2, tags/pushes) — it does **not** publish the
installers to GitHub or install anything on user machines. After it finishes:

1. **Smoke-test the installers.** Double-click the `.pkg`, choose the
   components, and confirm that the apps land in `/Applications/Glyco-PAINT`
   and the Fiji plugin in Fiji's `plugins` folder (Installer: Window → Installer
   Log shows what the plugin step did). Start `Glyco-PAINT.app` and check that
   the Launcher starts all four apps. Do the same with the `setup.exe` on
   Windows (Parallels): the Fiji folder page, the Start menu, and removal via
   Settings → Apps.
2. **Publish the release assets (mode 2).** Mode 2 pushes the tag `v<version>`,
   but uploading the installers as **GitHub release assets is a separate,
   manual step** (e.g. via `gh release create`/`gh release upload`, or the
   `github.GitHubManager` helper, or the GitHub web UI).
3. **Distribute** the installers from `Installers/` to end users. How users
   install is described in the Manual (Installing the Pipeline).

---

## Old installers (backup)

Before the `.pkg` and `setup.exe`, the installers were executable JARs (modules
`paint-installer`, `paint-installer-macos`, `paint-installer-windows`). They are
kept as a backup but are **not built by default**:

- The modules are only in the Maven build with the profile `old-installers`, so
  `mvn install` does not build them.
- The release tool only builds them with `--old-installers`. It then also writes
  a fresh `payload.zip` (the platform's apps + the plugin) into each installer
  module's `src/main/resources/` (generated, git-ignored), builds the two fat
  JARs and puts them in `Installers/` as
  `Glyco-PAINT-Installer-macOS-<version>.jar` and
  `Glyco-PAINT-Installer-Windows-<version>.jar`, next to the new installers.

### How users launch an old installer

The old installer files are **executable JARs**, not native self-extracting
installers. Each has a `Main-Class` in its manifest, so it runs with:

```
java -jar Glyco-PAINT-Installer-macOS-<version>.jar
```

When it runs, the embedded `payload.zip` is extracted and the selected apps and
the Fiji plugin are installed — so it behaves like a self-contained installer,
but it needs a **JVM present** to run (unlike a native `.exe`/`.dmg`/`.pkg`).

Double-clicking works only under these conditions, which are worth telling users:

- **Java 21 or newer must be installed.** With no JRE, or an older one, a double-click does
  nothing useful.
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

- The installer build runs in **all** modes (including rebuild-only).
- In rebuild-only mode the artifacts are named with the current `-SNAPSHOT`
  version.
- In mode 2, once both installers are built, a `Glyco-PAINT-<version>-SNAPSHOT`
  folder left by an earlier rebuild of the same version is deleted: the release
  build supersedes it.
- This tool is a developer aid. Parts of it have unit tests (`GitUtilsTest`,
  `MavenSupportTest`, `FileOpsTest`); the full build is not tested automatically.
