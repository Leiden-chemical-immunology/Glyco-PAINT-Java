package viewer;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.dialogs.ProjectDialog.DialogMode;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.PaintLogger;
import viewer.utils.RecordingEntry;
import viewer.utils.RecordingLoader;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The RecordingViewer class serves as the entry point for the viewer application.
 * It initializes application preferences, logging, and configuration before
 * launching the recording viewer interface.
 *
 * Responsibilities:
 * - Load the last used project root directory using PaintPrefs.
 * - Initialize application-specific configuration and logging using PaintConfig and PaintLogger.
 * - Launch a project specification dialog for user interaction and project selection.
 * - Display recordings in a viewer frame if valid recordings are detected in the selected project.
 *
 * Behavior:
 * - If no valid recordings are found in the selected project, it notifies the user via a dialog.
 * - Provides error handling for failures during the launch of the viewer.
 *
 * Key Components:
 * - PaintPrefs: Used to load application preferences, such as the last used project directory.
 * - PaintConfig: Handles initialization of application-specific configurations.
 * - PaintLogger: Manages logging for the viewer application.
 * - ProjectDialog: Displays a dialog for project selection and captures user input.
 * - RecordingLoader: Loads recordings from the selected project and provides a list of entries.
 * - RecordingViewerFrame: Presents the user interface for viewing the loaded recordings.
 *
 * This class utilizes Swing for its graphical user interface components and ensures
 * the application launches on the Event Dispatch Thread (EDT) for thread safety.
 */
public class RecordingViewer {

    /**
     * The main method serves as the entry point for the application. It initializes
     * application settings, logging, and other configurations. Upon startup, it attempts
     * to load the last used project root, configures the logging system, and opens the
     * project specification dialog. The dialog enables users to select projects to
     * proceed with, and on confirmation, launches the recording viewer if valid recordings
     * are found.
     *
     * @param args Command-line arguments passed to the application. These are not used
     *             in the current implementation but exist to comply with the standard
     *             main method signature.
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
                    List<RecordingEntry> entries = RecordingLoader.loadFromProject(project);
                    if (entries.isEmpty()) {
                        JOptionPane.showMessageDialog(null,
                                                      "No valid recordings found in selected experiments.",
                                                      "No Recordings",
                                                      JOptionPane.WARNING_MESSAGE);
                        return false;
                    }

                    RecordingViewerFrame viewer = new RecordingViewerFrame(project, entries);
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