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
 * The {@code TrackMateHeadless} class provides a way to execute the TrackMate functionality
 * in headless mode. It integrates with the PaintConfig and PaintPrefs configuration frameworks
 * to determine execution settings, logs progress and errors using PaintLogger, and processes
 * experiments based on the defined configuration.
 *
 * This class implements the {@link Command} interface, enabling its usage as a plugin
 * in an ImageJ or Fiji environment.
 *
 * Key steps performed in the {@code run()} method:
 * 1. Reads and validates the project path from preferences.
 * 2. Initializes the PaintConfig using the project path and configuration file.
 * 3. Sets up logging for tracking progress and diagnostics.
 * 4. Gathers experiments marked as active in the configuration.
 * 5. Executes the TrackMate functionality in a headless environment.
 *
 * If no valid configuration or experiments are found, the execution is gracefully exited
 * with appropriate logging messages. Errors during execution are also logged.
 */
//@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run (Headless)")
public class TrackMateHeadless implements Command {

    /**
     * Executes the main logic for running TrackMate in headless mode.
     *
     * This method performs the following operations in sequence:
     * 1. Reads the project path from preferences and validates it. If the path
     *    is invalid, the execution stops and logs an error message.
     * 2. Initializes the PaintConfig using the project path and validates the
     *    existence of the configuration file (`paint-configuration.json`). If the
     *    file is missing or invalid, the execution stops and logs an error message.
     * 3. Sets up logging configuration, including creating a console window for
     *    logging information and initializing PaintLogger for the session.
     * 4. Retrieves a list of experiments marked as "selected" in the configuration
     *    file and logs their details. If no experiments are selected, the
     *    execution stops and logs a warning message.
     * 5. Executes the TrackMate operation on the selected project, experiments,
     *    and image path. Logs the success or failure status of the execution.
     *
     * In case of an exception during the execution, an error message is logged with
     * the exception details.
     */
    @Override
    public void run() {
        try {

            //  --- Step 1: read the project path from Prefs ---
            Path projectPath = getValidProjectPath();
            if (projectPath == null) {
                PaintLogger.errorf("No valid Project Root found.");
                return;
            }

            // --- Step 2: initialise PaintConfig  ---
            // Check if the file exists
            Path jsonPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
            if (!jsonPath.toFile().exists()) {
                PaintLogger.errorf("Invalid or missing configuration file: %s. ", jsonPath );
                return;
            }
            PaintConfig.initialise(projectPath);

            // Reinitialise PaintConfig with the real project path
            PaintConfig.reinitialise(projectPath);
            Path imagesPath = Paths.get(PaintPrefs.getString("Path", "Images Root", ""));

            // --- Step 3: setup logging ---
            PaintConsoleWindow.createConsoleFor("TrackMate Headless");
            PaintLogger.initialise(projectPath, "TrackMateHeadless");

            // --- Step 4: find experiments marked as true ---
            List<String> experiments = getSelectedExperiments();
            if (experiments.isEmpty()) {
                PaintLogger.warnf("No experiments marked as true in configuration. Nothing to process.");
                return;
            }

            PaintLogger.infof("Running TrackMate in headless mode...");
            PaintLogger.infof("Project path: %s", projectPath);
            PaintLogger.infof("Images root:  %s", imagesPath);
            PaintLogger.infof("Experiments:  %s", experiments);

            boolean success = RunTrackMateOnProject.runProject(projectPath, imagesPath, experiments, null, null);
            if (success)
                PaintLogger.infof("✅ TrackMate completed successfully.");
            else
                PaintLogger.errorf("⚠️ TrackMate encountered errors.");

        } catch (Exception e) {
            PaintLogger.errorf("TrackMate headless execution failed: %s", e.getMessage());
        }
    }

    /**
     * Retrieves a list of experiments that are marked as "selected" in the configuration.
     * The method reads the "Experiments" section from the configuration file,
     * checks each key for a boolean value of `true`, and adds the corresponding key
     * to the list of selected experiments.
     * If the "Experiments" section is not found, a warning is logged.
     *
     * @return a list of selected experiment names, or an empty list if none are selected
     *         or the "Experiments" section is missing or invalid.
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
            } catch (Exception ignored) {}
        }
        return selected;
    }
}