package viewer;

import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSpecificationDialog.DialogMode;
import paint.shared.objects.Project;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

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
            RecordingViewerFrame viewer = new RecordingViewerFrame(project);
            viewer.setVisible(true);
        });
    }
}