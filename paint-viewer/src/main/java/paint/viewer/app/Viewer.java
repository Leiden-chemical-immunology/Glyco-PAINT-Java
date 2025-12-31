/*==============================================================================
 *  Class:        Viewer.java
 *  Package:      paint.viewer.app
 *
 *  PURPOSE:
 *    Serves as the entry point for the PAINT Viewer application, initializing
 *    preferences, logging, and configuration before launching the recording
 *    visualization interface.
 *
 *  DESCRIPTION:
 *    The {@code Viewer} class prepares the complete runtime environment for the
 *    PAINT Viewer. It loads user preferences, initializes application-wide
 *    configuration and logging systems, and displays the project specification
 *    dialog used to select a project directory.
 *
 *    When the user confirms the project settings, recordings are loaded via
 *    {@link paint.viewer.model.RecordingLoader}. If valid recordings are found,
 *    the {@link paint.viewer.ui.frames.ViewerFrame} is launched to provide the
 *    main visualization and interaction interface.
 *
 *  KEY FEATURES:
 *    • Loads last-used project root from user preferences.
 *    • Initializes PaintConfig and PaintLogger for the application session.
 *    • Presents a project selection dialog (viewer mode).
 *    • Loads and validates recordings for all selected experiments.
 *    • Launches the viewer interface when data is valid.
 *    • Displays clear diagnostic dialogs for missing or invalid data.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.app;

import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.dialogs.ProjectDialog.DialogMode;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;
import paint.viewer.model.RecordingEntry;
import paint.viewer.model.RecordingLoader;
import paint.viewer.ui.frames.ViewerFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Main entry point for the PAINT Viewer application.
 * <p>
 * This class sets up preferences, configuration, and logging, and then opens
 * the project selection dialog. Once the user confirms their selection, it
 * loads the corresponding recordings and launches the main viewer interface.
 * </p>
 */
public class Viewer {

    /**
     * Application entry point.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // --- Step 1: Load last-used project from preferences ---
            String lastProject = PaintPrefs.getString("Path", "Project Root", System.getProperty("user.home"));
            Path   projectPath = Paths.get(lastProject);

            // --- Step 2: Initialise logging and configuration ---
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Viewer");
            PaintLogger.setLevel(PaintPrefs.getString("Runtime", "Log Level", "INFO"));

            // --- Step 3: Show the project specification dialog ---
            ProjectDialog specificationDialog =
                    new ProjectDialog(null, projectPath, DialogMode.VIEWER);

            // Callback when user presses OK
            specificationDialog.setCalculationCallback(project -> {
                try {
                    List<RecordingEntry> recordingEntries =
                            RecordingLoader.loadFromProject(project);

                    if (recordingEntries.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                null,
                                "No valid recordings found in selected experiments.",
                                "No Recordings",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return false;
                    }

                    ViewerFrame viewer = new ViewerFrame(project, recordingEntries);
                    viewer.setVisible(true);
                    return true;

                } catch (Exception ex) {
                    PaintLogger.errorf("Viewer launch failed");
                    return false;
                }
            });

            // --- Step 4: Open dialog (blocking) ---
            specificationDialog.showDialog();
        });
    }
}