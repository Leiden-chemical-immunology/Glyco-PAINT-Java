package viewer;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.dialogs.ProjectDialog.DialogMode;
import paint.shared.prefs.PaintPrefs;
import paint.shared.utils.PaintLogger;
import viewer.utils.RecordingEntry;
import viewer.utils.RecordingLoader;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RecordingViewer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // --- Step 1: Load last used project root from preferences ---
            String lastProject = PaintPrefs.getString("Project Root", System.getProperty("user.home"));
            Path projectPath = Paths.get(lastProject);

            // --- Step 2: Initialise logging/config ---
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Viewer");

            // --- Step 3: Open Project Specification dialog directly ---
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