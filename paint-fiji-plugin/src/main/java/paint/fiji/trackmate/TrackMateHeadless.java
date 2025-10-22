package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.PaintConfig;
import paint.shared.prefs.PaintPrefs;
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
 * ============================================================================
 *  TrackMateHeadless.java
 *  Part of the Glyco-PAINT Fiji plugin.
 *
 *  <p><b>Purpose:</b><br>
 *  Runs TrackMate fully headless using Paint Configuration.json.
 *  No dialogs are shown; configuration and experiment selection
 *  are read directly from JSON.
 *  </p>
 *
 *  <p><b>Menu:</b><br>
 *  Plugins ▸ Glyco-PAINT ▸ Run TrackMate (Headless)
 *  </p>
 * ============================================================================
 */
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run (Headless)")
public class TrackMateHeadless implements Command {

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
            Path imagesPath = Paths.get(PaintPrefs.getString("Images Root", ""));

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

            boolean success = RunTrackMate.run(projectPath, imagesPath, experiments);
            if (success)
                PaintLogger.infof("✅ TrackMate completed successfully.");
            else
                PaintLogger.errorf("⚠️ TrackMate encountered errors.");

        } catch (Exception e) {
            PaintLogger.errorf("TrackMate headless execution failed: %s", e.getMessage());
        }
    }

    /** Reads the "Experiments" section and collects all entries set to true. */
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