package paint.fiji.trackmate;

import paint.shared.config.PaintConfig;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * ============================================================================
 *  RunTrackMate.java
 *  Part of the Glyco-PAINT Fiji plugin.
 *
 *  <p><b>Purpose:</b><br>
 *  Provides a headless entry point for TrackMate processing.
 *  Detects sweep configuration automatically and executes
 *  either {@link RunTrackMateOnProjectSweep} or
 *  {@link RunTrackMateOnProject}.
 *  </p>
 *
 *  <p><b>Usage:</b><br>
 *  This class can be invoked from:
 *  <ul>
 *      <li>{@link TrackMateUI} (the Fiji GUI plugin)</li>
 *      <li>a command-line launcher</li>
 *      <li>an automated pipeline or testing framework</li>
 *  </ul>
 *  </p>
 *
 *  <p><b>Author:</b> Herr Doctor<br>
 *  <b>Version:</b> 2.0<br>
 *  <b>Module:</b> paint-fiji-plugin
 *  </p>
 * ============================================================================
 */
public final class RunTrackMate {

    private RunTrackMate() {}

    /**
     * Executes TrackMate processing for the given project.
     * <p>
     * This is the core logic used in both GUI and headless contexts.
     *
     * @param projectPath      project root directory
     * @param imagesPath       root directory containing experiment images
     * @param experimentNames  list of experiment names to process
     * @return {@code true} if all runs completed successfully
     */
    public static boolean run(Path projectPath, Path imagesPath, List<String> experimentNames) {

        boolean debug = PaintConfig.getBoolean("Debug", "Debug RunTrackMateOnProject", false);

        if (debug) {
            PaintLogger.debugf("TrackMate processing started (headless mode).");
            PaintLogger.debugf("Experiments: %s", experimentNames);
        }

        // Sweep detection
        Path sweepFile = projectPath.resolve("Sweep Config.json");
        if (Files.exists(sweepFile)) {
            PaintLogger.infof("Sweep configuration detected at %s", sweepFile);
            try {
                return RunTrackMateOnProjectSweep.runWithSweep(projectPath, imagesPath, experimentNames);
            } catch (IOException e) {
                PaintLogger.errorf("Sweep execution failed: %s", e.getMessage());
                return false;
            }
        }

        // Normal execution
        return RunTrackMateOnProject.runProject(projectPath, imagesPath, experimentNames, null, null);
    }

    /**
     * Optional CLI entry point for headless execution without Fiji.
     * Example:
     * <pre>
     * java -cp paint-fiji-plugin.jar paint.fiji.trackmate.RunTrackMate /path/to/project /path/to/images Exp1 Exp2
     * </pre>
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: RunTrackMate <projectPath> <imagesPath> <experiment1> [experiment2 ...]");
            System.exit(1);
        }

        Path projectPath = Paths.get(args[0]);
        Path imagesPath = Paths.get(args[1]);
        List<String> experiments = Arrays.asList(args).subList(2, args.length);

        PaintConfig.initialise(projectPath);
        PaintLogger.initialise(projectPath, "TrackMateHeadless");

        boolean success = run(projectPath, imagesPath, experiments);
        if (!success) {
            System.exit(2);
        }
    }
}