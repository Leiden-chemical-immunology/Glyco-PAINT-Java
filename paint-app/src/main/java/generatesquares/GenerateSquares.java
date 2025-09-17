package generatesquares;

import javax.swing.*;
import java.nio.file.Path;

import objects.Experiment;
import objects.Recording;
import paint.io.SquareTableIO;
import tech.tablesaw.api.Table;
import utilities.AppLogger;
import dialogs.ExperimentDialog;
import dialogs.ProjectDialog;
import objects.Project;

import static constants.PaintConstants.SQUARES_CSV;
import static generatesquares.calc.GenerateSquareCalcs.calculateSquaresForExperiment;

public class GenerateSquares {

    public static void main(String[] args) {
        AppLogger.init("GenerateSquares.log");

        // Overrule the default, takes this out at some point
        AppLogger.setLevel("Info");

        AppLogger.debugf("Starting Generate Squares...");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {

            // Display the project directory selection dialog
            ProjectDialog projDlg = new ProjectDialog(null);
            Path projectPath = projDlg.showDialog();

            // If the user selected Cancel, then return.
            if (projectPath == null) {
                AppLogger.infof("User cancelled project selection.");
                return;
            }

            // Use the project directory to display the experiment selection dialog.
            AppLogger.debugf("User selected: " + projectPath);
            ExperimentDialog dialog = new ExperimentDialog(null, projectPath, ExperimentDialog.DialogMode.GENERATE_SQUARES);

            // Wire the callback to perform calculations while keeping the dialog open
            dialog.setCalculationCallback(project -> {
                for (String experimentName : project.experimentNames) {
                    calculateSquaresForExperiment(project, experimentName);
                }
                AppLogger.debugf("\n\nFinished calculating");

                // Create the squares table
                SquareTableIO squaresTableIO = new SquareTableIO();
                Table allSquaresProjectTable = squaresTableIO.emptyTable();

                for (Experiment experiment: project.experiments) {

                    Table allSquaresExperimentTable = squaresTableIO.emptyTable();

                    for (Recording recording: experiment.getRecordings()) {
                        Table table = squaresTableIO.toTable(recording.getSquares());

                        squaresTableIO.appendInPlace(allSquaresProjectTable, table);
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
                Path squaresProjectFilePath = projectPath.resolve(SQUARES_CSV);
                try {
                    squaresTableIO.writeCsv(allSquaresProjectTable, squaresProjectFilePath);
                } catch (Exception e) {
                    AppLogger.errorf(e.getMessage());
                }
            });

            // Show dialog (calculations will run after pressing OK)
            dialog.showDialog();
        });
    }
}