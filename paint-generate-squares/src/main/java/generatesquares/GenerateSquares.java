package generatesquares;

import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import io.SquareTableIO;
import paint.shared.utils.JarInfo;
import paint.shared.utils.JarInfoLogger;
import tech.tablesaw.api.Table;
import paint.shared.utils.AppLogger;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.ProjectSelectionDialog;

import static paint.shared.constants.PaintConstants.SQUARES_CSV;

import static generatesquares.calc.GenerateSquareCalcs.calculateSquaresForExperiment;
import static paint.shared.utils.CsvConcatenator.concatenateExperimentCsvFiles;
import static paint.shared.utils.JarInfoLogger.getJarInfo;

public class GenerateSquares {

    public static void main(String[] args) {
        AppLogger.init("Generate Squares.log");

        // Overrule the default, takes this out at some point
        AppLogger.setLevel("Info");

        AppLogger.debugf("Starting Generate Squares...");

        JarInfo info = getJarInfo(GenerateSquares.class);
        if (info != null) {
            AppLogger.infof("Compilation date: %s",info.implementationDate);
            AppLogger.infof("Version: %s",info.implementationVersion);
        } else {
            AppLogger.errorf("No manifest information found.");
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

            // Wire the callback to perform calculations while keeping the dialog open
            dialog.setCalculationCallback(project -> {
                for (String experimentName : project.experimentNames) {
                    calculateSquaresForExperiment(project, experimentName);
                }
                AppLogger.debugf("\n\nFinished calculating");

                // Create the squares table
                SquareTableIO squaresTableIO = new SquareTableIO();
                // Table allSquaresProjectTable = squaresTableIO.emptyTable();

                for (Experiment experiment: project.experiments) {

                    Table allSquaresExperimentTable = squaresTableIO.emptyTable();

                    for (Recording recording: experiment.getRecordings()) {
                        Table table = squaresTableIO.toTable(recording.getSquares());

                        // squaresTableIO.appendInPlace(allSquaresProjectTable, table);
                        squaresTableIO.appendInPlace(allSquaresExperimentTable, table);
                        AppLogger.debugf("Processing squares for experiment '%s'  - recording '%s'", experiment.getExperimentName(), recording.getRecordingName());
                    }

                    // Write the experiment squares file
                    Path squaresExperimentFilePath = projectPath.resolve(experiment.getExperimentName()).resolve(SQUARES_CSV);
                    try {
                        squaresTableIO.writeCsv(allSquaresExperimentTable, squaresExperimentFilePath);
                    } catch (Exception e) {
                        AppLogger.errorf(e.getMessage());
                    }
                }

                // Write the projects squares file
                try {
                    concatenateExperimentCsvFiles(projectPath, SQUARES_CSV, project.experimentNames);
                    AppLogger.infof("Generated squares info for the selected experiments and for the project,");
                }  catch (Exception e) {
                    AppLogger.errorf("Could not concatenate squares file - %s", e.getMessage());
                }
                return true;   //ToDo
            });

            // Show dialog (calculations will run after pressing OK)
            dialog.showDialog();
        });
    }

}