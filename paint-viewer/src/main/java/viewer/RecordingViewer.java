package viewer;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSpecificationDialog.DialogMode;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import viewer.utils.RecordingEntry;
import viewer.utils.RecordingLoader;

import javax.swing.*;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.List;

public class RecordingViewer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // --- Step 1: Project selection ---
            ProjectSelectionDialog selectionDialog = new ProjectSelectionDialog((Frame) null);
            Path projectPath = selectionDialog.showDialog();

            if (projectPath == null || selectionDialog.isCancelled()) {
                System.out.println("User cancelled selection.");
                System.exit(0);
            }
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Viewer");

            // --- Step 2: Project specification (force modal only here) ---
            ProjectSpecificationDialog specificationDialog =
                    new ProjectSpecificationDialog(null, projectPath, DialogMode.VIEWER);

            specificationDialog.getDialog().setModal(true); // ensure it blocks
            Project project = specificationDialog.showDialog();

            if (project == null || specificationDialog.isCancelled()) {
                System.out.println("User cancelled specification.");
                System.exit(0);
            }

            // --- Step 3: Show the main viewer ---
            List<RecordingEntry> entries = RecordingLoader.loadFromProject(project);

            if (entries.isEmpty()) {
                JOptionPane.showMessageDialog(null,
                                              "No valid recordings found in selected experiments.",
                                              "No Recordings",
                                              JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }

            RecordingViewerFrame viewer = new RecordingViewerFrame(project, entries);
            viewer.setVisible(true);

        });
    }
}