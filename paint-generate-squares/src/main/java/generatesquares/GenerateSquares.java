/**
 * ============================================================================
 *  GenerateSquares.java
 *  Part of the "Generate Squares" module.
 *
 *  Purpose:
 *    Acts as the main entry point for the Generate Squares workflow.
 *    Provides a GUI-driven process to:
 *      - Select a project directory
 *      - Configure analysis parameters
 *      - Run square-based calculations on TrackMate output
 *      - Export histogram summaries and consolidated CSVs
 *
 *  Notes:
 *    This class contains only orchestration logic and user interface flow.
 *    All computational work is delegated to GenerateSquareCalcs.
 *
 *  Author: Herr Doctor
 *  Version: 1.0
 *  Module: paint-generate-squares
 * ============================================================================
 */

package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.dialogs.RootSelectionDialog;
import paint.shared.objects.Experiment;
import paint.shared.utils.HistogramPdfExporter;
import paint.shared.utils.JarInfo;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.ValidationResult;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static generatesquares.calc.GenerateSquareCalcs.generateSquaresForExperiment;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.io.ProjectDataLoader.loadExperiment;
import static paint.shared.utils.CsvConcatenator.concatenateNamedCsvFiles;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.Miscellaneous.formatDuration;
import static paint.shared.validate.ValidationHandler.validateExperiments;

/**
 * Entry point for the "Generate Squares" module.
 * <p>
 * This standalone application allows the user to:
 * <ul>
 *     <li>Select a project directory containing experimental data</li>
 *     <li>Configure parameters for generating square grids over recordings</li>
 *     <li>Run square-based calculations (Tau, Variability, Density, etc.)</li>
 *     <li>Export histogram data to PDF summaries</li>
 * </ul>
 * The application uses a Swing-based GUI and logs detailed progress to
 * {@code Generate Squares.log} in the selected project directory.
 * </p>
 *
 * <p><b>Usage:</b></p>
 * <pre>{@code
 * java -jar paint-generate-squares.jar
 * }</pre>
 *
 * <p>This application is typically launched via the Automator app
 * {@code Generate Squares.app} on macOS.</p>
 *
 * @author  Paint Project
 * @version 1.0.0
 * @since   1.0.0
 */
public class GenerateSquares {

    /**
     * Main entry point for the Generate Squares module.
     * <p>
     * Initializes the Swing look and feel, opens project selection dialogs,
     * and executes square-based analysis for selected experiments.
     * </p>
     *
     * @param args not used
     */
    public static void main(String[] args) {

        try {
            // Use native OS look and feel (Aqua on macOS, system L&F elsewhere)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // macOS integration hints for menu bar and font rendering
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("swing.aatext", "true");
            System.setProperty("swing.useSystemFontSettings", "true");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {

            // --- Step 1: Select project directory ---
            RootSelectionDialog selectionDialog = new RootSelectionDialog(null, RootSelectionDialog.Mode.PROJECT);

            Path projectPath = selectionDialog.showDialog();

            // User pressed Cancel
            if (projectPath == null) {
                PaintLogger.infof("User cancelled project selection.");
                return;
            }

            // --- Step 2: Initialize configuration and logging ---
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Generate Squares.log");
            String debugLevel = PaintConfig.getString("Paint", "Log Level", "INFO");
            PaintLogger.setLevel(debugLevel);
            PaintLogger.debugf("Starting Generate Squares...");

            JarInfo info = getJarInfo(GenerateSquares.class);
            if (info != null) {
                PaintLogger.infof("Compilation date: %s", info.implementationDate);
                PaintLogger.infof("Version: %s", info.implementationVersion);
            } else {
                PaintLogger.errorf("No manifest information found.");
                PaintLogger.infof();
            }

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = now.format(fmt);
            PaintLogger.infof("Current time is: %s", formattedTime);
            PaintLogger.blankline();
            PaintLogger.blankline();

            // --- Step 3: Open the experiment configuration dialog ---
            PaintLogger.debugf("User selected: " + projectPath);
            ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                    null,
                    projectPath,
                    ProjectSpecificationDialog.DialogMode.GENERATE_SQUARES
            );

            // --- Step 3.5: Create console and tie its lifecycle to the dialog ---
            PaintConsoleWindow.createConsoleFor("Generate Squares");
            PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

            // --- Step 4: Define what happens when user presses OK ---
            dialog.setCalculationCallback(project -> {

                // Validate the input experiment data
                ValidationResult validateResult = validateExperiments(
                        projectPath,
                        project.experimentNames,
                        Arrays.asList(EXPERIMENT_INFO_CSV,
                                      RECORDING_CSV,
                                      TRACK_CSV)
                );

                if (!validateResult.isValid()) {
                    for (String line : validateResult.getReport().split("\n")) {
                        PaintLogger.errorf(line);
                    }
                    return false;
                }

                LocalDateTime start = LocalDateTime.now();

                // --- Step 5: Process all selected experiments ---
                for (String experimentName : project.experimentNames) {
                    generateSquaresForExperiment(project, experimentName);

                    // Export histograms to a PDF summary
                    try {
                        Experiment experiment = loadExperiment(project.projectRootPath, experimentName, true);
                        Path pdfOut = project.projectRootPath
                                .resolve(experimentName)
                                .resolve("Output")
                                .resolve("Background.pdf");

                        Files.createDirectories(pdfOut.getParent());
                        HistogramPdfExporter.exportExperimentHistogramsToPdf(experiment, pdfOut);

                    } catch (Exception e) {
                        PaintLogger.errorf("Failed to export histograms to PDF: %s", e.getMessage());
                    }
                }

                PaintLogger.debugf("\n\nFinished calculating.");

                // --- Step 6: Concatenate experiment CSVs into a project-level summary ---
                try {
                    PaintLogger.infof("Creating project level All Squares");
                    concatenateNamedCsvFiles(projectPath, SQUARE_CSV, project.experimentNames);

                    PaintLogger.infof("Creating project level All Recordings");
                    concatenateNamedCsvFiles(projectPath, RECORDING_CSV, project.experimentNames);

                    PaintLogger.infof("Creating project level Experiment Info");
                    concatenateNamedCsvFiles(projectPath, EXPERIMENT_INFO_CSV, project.experimentNames);

                    PaintLogger.infof("Creating project level All Tracks");
                    concatenateNamedCsvFiles(projectPath, TRACK_CSV, project.experimentNames);

                    Duration duration = Duration.between(start, LocalDateTime.now());
                    PaintLogger.infof("Generated squares info for all selected experiments in %s", formatDuration(duration));
                } catch (Exception e) {
                    PaintLogger.errorf("Could not concatenate squares file - %s", e.getMessage());
                }

                return true;
            });

            // --- Step 7: Show the configuration dialog ---
            dialog.showDialog();
        });
    }
}