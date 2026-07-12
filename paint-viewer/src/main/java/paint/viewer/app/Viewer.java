/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.app;

import paint.shared.config.paintconfig.PaintConfig;
import paint.ui.dialogs.ProjectDialog;
import paint.ui.dialogs.ProjectDialog.DialogMode;
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
                    // Log the cause, not just the fact. This used to discard `ex` entirely, which
                    // made every startup failure undiagnosable — and it silently hid the case where
                    // ViewerFrame could not build its grid, leaving the user with nothing at all.
                    PaintLogger.error("Viewer launch failed", ex);
                    JOptionPane.showMessageDialog(
                            null,
                            "The Viewer could not be opened:\n\n" + ex.getMessage(),
                            "Viewer failed to start",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return false;
                }
            });

            // --- Step 4: Open dialog (blocking) ---
            specificationDialog.showDialog();
        });
    }
}