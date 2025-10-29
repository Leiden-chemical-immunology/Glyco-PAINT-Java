/******************************************************************************
 *  Class:        RecordingViewer.java
 *  Package:      paint.viewer
 *
 *  PURPOSE:
 *    Serves as the entry point for the PAINT Viewer application, initializing
 *    preferences, logging, and configuration before launching the recording
 *    visualization interface.
 *
 *  DESCRIPTION:
 *    The {@code RecordingViewer} initializes application components such as
 *    {@link paint.shared.utils.PaintPrefs}, {@link paint.shared.config.PaintConfig},
 *    and {@link paint.shared.utils.PaintLogger}, and presents the user with a
 *    {@link paint.shared.dialogs.ProjectDialog} for project selection.
 *
 *    Upon confirmation, it loads experiment data via
 *    {@link paint.viewer.utils.RecordingLoader}, and if valid recordings are found,
 *    launches the {@link paint.viewer.RecordingViewerFrame} interface.
 *
 *  KEY FEATURES:
 *    • Loads the last used project directory from user preferences.
 *    • Initializes configuration and logging systems.
 *    • Presents a project selection dialog for the user.
 *    • Loads and validates recordings for display.
 *    • Launches the main viewer interface if data is valid.
 *    • Displays error or warning dialogs when necessary.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.viewer;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.dialogs.ProjectDialog.DialogMode;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;
import paint.viewer.utils.RecordingEntry;
import paint.viewer.utils.RecordingLoader;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Entry point for the PAINT Viewer application.
 * <p>
 * Initializes preferences, logging, and configuration before launching the
 * recording viewer interface. The viewer enables users to inspect and
 * interact with experiment recordings loaded from a project.
 * </p>
 *
 * <p><strong>Responsibilities:</strong></p>
 * <ul>
 *   <li>Load the last used project root directory via {@link PaintPrefs}.</li>
 *   <li>Initialize configuration and logging via {@link PaintConfig} and {@link PaintLogger}.</li>
 *   <li>Display a project specification dialog for user selection.</li>
 *   <li>Load recordings from the selected project using {@link RecordingLoader}.</li>
 *   <li>Launch the {@link RecordingViewerFrame} interface if valid recordings are found.</li>
 * </ul>
 */
public class RecordingViewer {

    /**
     * Main entry point for the PAINT Viewer application.
     * <p>
     * This method initializes preferences, configuration, and logging, then
     * opens the project dialog for user input. If valid recordings are loaded,
     * it launches the viewer frame for visualization.
     * </p>
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // --- Step 1: Load the last used project root from preferences ---
            String lastProject = PaintPrefs.getString("Path", "Project Root", System.getProperty("user.home"));
            Path projectPath = Paths.get(lastProject);

            // --- Step 2: Initialise logging/config ---
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Viewer");

            // --- Step 3: Open the Project Specification dialog directly ---
            ProjectDialog specificationDialog =
                    new ProjectDialog(null, projectPath, DialogMode.VIEWER);

            // ✅ Callback for the OK button — launches the viewer
            specificationDialog.setCalculationCallback(project -> {
                try {
                    // Load the data for experiments (images, squares, etc.)
                    List<RecordingEntry> recordingEntries = RecordingLoader.loadFromProject(project);
                    if (recordingEntries.isEmpty()) {
                        JOptionPane.showMessageDialog(null,
                                                      "No valid recordings found in selected experiments.",
                                                      "No Recordings",
                                                      JOptionPane.WARNING_MESSAGE);
                        return false;
                    }

                    // With the recordingEntries info available, the viewer can be started
                    RecordingViewerFrame viewer = new RecordingViewerFrame(project, recordingEntries);
                    viewer.setVisible(true);
                    return true;
                } catch (Exception ex) {
                    PaintLogger.errorf("Viewer launch failed: %s", ex.getMessage());
                    JOptionPane.showMessageDialog(null,
                                                  "Viewer launch failed:\n" + ex.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            });

            // --- Step 4: Show dialog and keep it open ---
            specificationDialog.showDialog();
        });
    }
}