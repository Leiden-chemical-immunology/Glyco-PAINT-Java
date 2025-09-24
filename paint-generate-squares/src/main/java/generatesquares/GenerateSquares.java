package generatesquares;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import paint.shared.config.PaintConfig;
import paint.shared.utils.AppLogger;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSelectionDialog;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;
import static paint.shared.constants.PaintConstants.SQUARES_CSV;

import paint.shared.utils.JarInfo;

import static paint.shared.utils.CsvConcatenator.concatenateExperimentCsvFiles;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.Miscellaneous.formatDuration;

import static generatesquares.calc.GenerateSquareCalcs.generateSquaresForExperiment;

public class GenerateSquares {

    public static void main(String[] args) {
        AppLogger.init("Generate Squares.log");

        // Overrule the default, takes this out at some point
        AppLogger.setLevel("Info");

        AppLogger.debugf("Starting Generate Squares...");

        JarInfo info = getJarInfo(GenerateSquares.class);
        if (info != null) {
            AppLogger.infof("Compilation date: %s", info.implementationDate);
            AppLogger.infof("Version: %s", info.implementationVersion);
        } else {
            AppLogger.errorf("No manifest information found.");
            AppLogger.infof();
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw);
        }

        SwingUtilities.invokeLater(() -> {

            // Display the project directory selection dialog
            ProjectSelectionDialog projDlg = new ProjectSelectionDialog(null);
            Path projectPath = projDlg.showDialog();

            // If the user selected Cancel, then return.
            if (projectPath == null) {
                AppLogger.infof("User cancelled project selection.");
                return;
            }

            // Use the project directory to display the experiment selection dialog.
            AppLogger.debugf("User selected: " + projectPath);
            ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, projectPath, ProjectSpecificationDialog.DialogMode.GENERATE_SQUARES);

            // Initialise the config file
            // PaintConfig.initialise(projectPath.resolve(PAINT_CONFIGURATION_JSON));

            // Wire the callback to perform calculations while keeping the dialog open
            dialog.setCalculationCallback(project -> {

                LocalDateTime start = LocalDateTime.now();
                for (String experimentName : project.experimentNames) {
                    generateSquaresForExperiment(project, experimentName);
                }
                AppLogger.debugf("\n\nFinished calculating");

                // Write the projects squares file
                try {
                    concatenateExperimentCsvFiles(projectPath, SQUARES_CSV, project.experimentNames);
                    Duration duration = Duration.between(start, LocalDateTime.now());
                    AppLogger.infof("Generated squares info for the selected experiments and for the project in %s", formatDuration(duration));
                } catch (Exception e) {
                    AppLogger.errorf("Could not concatenate squares file - %s", e.getMessage());
                }
                return true;   //ToDo
            });

            // Show dialog (calculations will run after pressing OK)
            dialog.showDialog();
        });
    }

}