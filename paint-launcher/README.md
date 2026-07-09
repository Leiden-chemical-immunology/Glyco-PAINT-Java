# paint-launcher

A small **standalone GUI launcher** (`PaintLauncher`) that presents the
Glyco-PAINT workflow as a single window and lets the user start the individual
pipeline apps from one place.

> **Status:** a standalone utility, **not** wired into the build/release/install
> pipeline — it isn't built by `ReleaseNewVersion`, isn't in `BuildSelector`,
> and isn't bundled in either installer. Nothing depends on it. It is retained
> for possible future use; it is compiled as part of a full project build (it's
> listed as a module in the parent `pom.xml`) but is not distributed.

---

## What it does

Opens a fixed-size window ("Glyco-PAINT Launcher") showing the workflow top to
bottom, with `↓` arrows between steps. Each step is either a **launch button**
(for an app) or a **visual-only label** (for a manual step):

| Step | Type | Launches |
|------|------|----------|
| Get Omero | button | `paint-get-omero` |
| Create Experiment | button | `paint-create-experiment` |
| Run TrackMate | label (manual step) | — |
| Generate Squares | button | `paint-generate-squares` |
| Viewer | button | `paint-viewer` |
| Analyse Results | label (manual step) | — |

Clicking a button launches that app **out-of-process**.

## How launching works

When a button is clicked, the launcher:

1. Looks in a `jars/` subdirectory of the **current working directory**
   (`<user.dir>/jars`).
2. Finds a JAR whose filename **starts with the module prefix** (e.g.
   `paint-viewer…​.jar`) and ends in `.jar`.
3. Runs it as a new process: `java -jar <that.jar>` (inheriting I/O).

If the `jars/` directory is missing, or no matching JAR is found, or the process
fails to start, it shows an error dialog.

**Requirement:** for the launcher to actually start anything, a `jars/` folder
must sit next to the working directory containing the app fat JARs named with
the module prefixes (`paint-get-omero*.jar`, `paint-create-experiment*.jar`,
`paint-generate-squares*.jar`, `paint-viewer*.jar`).

## Running it

```
java -jar paint-launcher-<version>-jar-with-dependencies.jar
```

(run from a directory that has the `jars/` folder alongside it).

---

## Note

Because it is not part of the release pipeline, the launcher can drift from the
rest of the suite (e.g. the app JAR naming it expects). If it is revived for
distribution, wire it into `ReleaseNewVersion`/the installers and confirm the
`jars/` layout matches what the release produces. If it is eventually retired,
it can be removed from the parent `pom.xml` modules and deleted.
