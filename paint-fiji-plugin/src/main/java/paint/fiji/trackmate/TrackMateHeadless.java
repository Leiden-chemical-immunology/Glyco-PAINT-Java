/******************************************************************************
 *  Class:        TrackMateHeadless.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Provides headless (non-interactive) execution of the TrackMate analysis
 *    pipeline for Paint projects. Acts as the main command entry point for
 *    Fiji/ImageJ environments or standalone execution.
 *
 *  DESCRIPTION:
 *    • Reads the Paint project root and configuration files.
 *    • Initializes PaintConfig and PaintLogger.
 *    • Identifies selected experiments from configuration.
 *    • Executes TrackMate on the selected experiments using
 *      {@link RunTrackMateOnProject}.
 *    • Logs all progress, warnings, and errors.
 *
 *  RESPONSIBILITIES:
 *    • Serve as the main entry point for automated TrackMate runs.
 *    • Integrate user preferences (PaintPrefs) and configuration (PaintConfig).
 *    • Ensure safe initialization and cleanup in a headless environment.
 *    • Provide meaningful user feedback via PaintLogger.
 *
 *  USAGE EXAMPLE:
 *    Command cmd = new TrackMateHeadless();
 *    cmd.run();  // Executes configured TrackMate experiments headlessly
 *
 *  DEPENDENCIES:
 *    – paint.shared.config.PaintConfig
 *    – paint.shared.utils.PaintPrefs
 *    – paint.shared.utils.PaintLogger
 *    – paint.shared.utils.PaintConsoleWindow
 *    – paint.fiji.trackmate.RunTrackMateOnProject
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.fiji.trackmate;

import org.scijava.command.Command;
import paint.shared.config.PaintConfig;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;
import static paint.shared.utils.ValidProjectPath.getValidProjectPath;

/**
 * Executes the Paint TrackMate analysis in a headless (non-interactive)
 * Fiji or ImageJ session. This class implements {@link Command} and can
 * be invoked either manually or via plugin registration.
 *
 * <p>Responsibilities include:</p>
 * <ul>
 *   <li>Validating the project root path.</li>
 *   <li>Initializing PaintConfig and PaintLogger.</li>
 *   <li>Identifying active experiments.</li>
 *   <li>Executing the TrackMate pipeline on selected datasets.</li>
 * </ul>
 */
public class TrackMateHeadless implements Command {

    /**
     * Main command entry point — executes the TrackMate pipeline for all
     * experiments marked as active in the Paint configuration file.
     *
     * <p>Execution steps:</p>
     * <ol>
     *   <li>Retrieve and validate project root path.</li>
     *   <li>Initialize configuration and logging systems.</li>
     *   <li>Identify all active experiments.</li>
     *   <li>Run the TrackMate workflow using {@link RunTrackMateOnProject}.</li>
     * </ol>
     *
     * <p>Logs all progress and exceptions via {@link PaintLogger}.</p>
     */
    @Override
    public void run() {
        try {
            // -----------------------------------------------------------------
            // Step 1 — Validate project path
            // -----------------------------------------------------------------
            Path projectPath = getValidProjectPath();
            if (projectPath == null) {
                PaintLogger.errorf("No valid Project Root found.");
                return;
            }

            // -----------------------------------------------------------------
            // Step 2 — Initialize configuration
            // -----------------------------------------------------------------
            Path jsonPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
            if (!jsonPath.toFile().exists()) {
                PaintLogger.errorf("Invalid or missing configuration file: %s", jsonPath);
                return;
            }
            PaintConfig.initialise(projectPath);
            PaintConfig.reinitialise(projectPath);  // ensures consistent reload

            Path imagesPath = Paths.get(PaintPrefs.getString("Path", "Images Root", ""));

            // -----------------------------------------------------------------
            // Step 3 — Initialize console and logging
            // -----------------------------------------------------------------
            PaintConsoleWindow.createConsoleFor("TrackMate Headless");
            PaintLogger.initialise(projectPath, "TrackMateHeadless");

            // -----------------------------------------------------------------
            // Step 4 — Identify active experiments
            // -----------------------------------------------------------------
            List<String> experiments = getSelectedExperiments();
            if (experiments.isEmpty()) {
                PaintLogger.warnf("No experiments marked as true in configuration. Nothing to process.");
                return;
            }

            PaintLogger.infof("Running TrackMate in headless mode...");
            PaintLogger.infof("Project path: %s", projectPath);
            PaintLogger.infof("Images root:  %s", imagesPath);
            PaintLogger.infof("Experiments:  %s", experiments);

            // -----------------------------------------------------------------
            // Step 5 — Execute TrackMate on project
            // -----------------------------------------------------------------
            boolean success = RunTrackMateOnProject.runProject(projectPath, imagesPath, experiments, null, null);
            if (success) {
                PaintLogger.infof("TrackMate completed successfully.");
            } else {
                PaintLogger.errorf("TrackMate encountered errors.");
            }

        } catch (Exception e) {
            PaintLogger.errorf("TrackMate headless execution failed: %s", e.getMessage());
        }
    }

    /**
     * Retrieves all experiment names from the Paint configuration that are
     * explicitly marked as <code>true</code> in the "Experiments" section.
     *
     * <p>If no valid section is found, or if all values are false, a warning is logged.</p>
     *
     * @return a list of selected experiment names; empty if none found
     */
    private static List<String> getSelectedExperiments() {
        List<String> selected = new ArrayList<>();
        JsonObject experiments = PaintConfig.instance().getSection("Experiments");

        if (experiments == null) {
            PaintLogger.warnf("No 'Experiments' section found in configuration.");
            return selected;
        }

        for (String key : experiments.keySet()) {
            try {
                if (experiments.get(key).getAsBoolean()) {
                    selected.add(key);
                }
            } catch (Exception ignored) {
                // Ignore malformed entries (non-boolean)
            }
        }
        return selected;
    }
}