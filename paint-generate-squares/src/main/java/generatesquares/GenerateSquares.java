package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import static generatesquares.calc.GenerateSquareCalcs.generateSquaresForExperiment;
import static paint.shared.constants.PaintConstants.SQUARES_CSV;
import static paint.shared.utils.CsvConcatenator.concatenateExperimentCsvFiles;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.Miscellaneous.formatDuration;

public class GenerateSquares {

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
        }

        SwingUtilities.invokeLater(() -> {

            // Display the project directory selection dialog
            ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
            Path projectPath = projDlg.showDialog();

            // If the user selected Cancel, then return.
            if (projectPath == null) {
                PaintLogger.infof("User cancelled project selection.");
                return;
            }

            // The user pushed OK: we should have a valid project directory
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Generate Squares.log", "Info");

            PaintLogger.debugf("Starting Generate Squares...");

            JarInfo info = getJarInfo(GenerateSquares.class);
            if (info != null) {
                PaintLogger.infof("Compilation date: %s", info.implementationDate);
                PaintLogger.infof("Version: %s", info.implementationVersion);
            } else {
                PaintLogger.errorf("No manifest information found.");
                PaintLogger.infof();
            }

            // Use the project directory to display the experiment selection dialog.
            PaintLogger.debugf("User selected: " + projectPath);
            ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, projectPath, ProjectSpecificationDialog.DialogMode.GENERATE_SQUARES);

            // Wire the callback to perform calculations while keeping the dialog open
            dialog.setCalculationCallback(project -> {

                LocalDateTime start = LocalDateTime.now();
                for (String experimentName : project.experimentNames) {
                    generateSquaresForExperiment(project, experimentName);
                }
                PaintLogger.debugf("\n\nFinished calculating");

                // Write the projects squares file
                try {
                    concatenateExperimentCsvFiles(projectPath, SQUARES_CSV, project.experimentNames);
                    Duration duration = Duration.between(start, LocalDateTime.now());
                    PaintLogger.infof("Generated squares info for the selected experiments and for the project in %s", formatDuration(duration));
                } catch (Exception e) {
                    PaintLogger.errorf("Could not concatenate squares file - %s", e.getMessage());
                }
                return true;   //ToDo
            });

            // Show dialog (calculations will run after pressing OK)
            dialog.showDialog();
        });
    }

}