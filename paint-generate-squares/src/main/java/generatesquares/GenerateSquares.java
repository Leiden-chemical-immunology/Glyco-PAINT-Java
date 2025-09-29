package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSelectionDialog;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static generatesquares.calc.GenerateSquareCalcs.generateSquaresForExperiment;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.CsvConcatenator.concatenateExperimentCsvFiles;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ValidationHandler.validateExperiments;

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
            ProjectSelectionDialog projectSelectionDialog = new ProjectSelectionDialog(null);
            Path projectPath = projectSelectionDialog.showDialog();

            // If the user selected Cancel, then return.
            if (projectPath == null) {
                PaintLogger.infof("User cancelled project selection.");
                return;
            }

            // The user pushed OK: we should have a valid project directory
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Generate Squares.log");

            PaintLogger.debugf("Starting Generate Squares...");

            JarInfo info = getJarInfo(GenerateSquares.class);
            if (info != null) {
                PaintLogger.infof("Compilation date: %s", info.implementationDate);
                PaintLogger.infof("Version: %s", info.implementationVersion);
            } else {
                PaintLogger.errorf("No manifest information found.");
                PaintLogger.infof();
            }
            PaintLogger.infof("Current time is: %s", LocalDateTime.now());
            PaintLogger.blankline();
            PaintLogger.blankline();

            // Use the project directory to display the experiment selection dialog.
            PaintLogger.debugf("User selected: " + projectPath);
            ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(null, projectPath, ProjectSpecificationDialog.DialogMode.GENERATE_SQUARES);

            // Wire the callback to perform calculations while keeping the dialog open
            dialog.setCalculationCallback(project -> {

                List<String> fileNames = Arrays.asList(
                        EXPERIMENT_INFO_CSV,
                        RECORDINGS_CSV
                );
                ValidationResult validateResult = validateExperiments(projectPath, project.experimentNames, fileNames);

                for (String line : validateResult.getReport().split("\n")) {
                    PaintLogger.errorf(line);
                }
                if (validateResult.hasErrors()) {
                    return false;
                 }
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