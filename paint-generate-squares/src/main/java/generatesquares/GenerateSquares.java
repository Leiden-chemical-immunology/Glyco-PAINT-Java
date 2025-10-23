/**
 * ============================================================================
 *  GenerateSquares.java
 *  Part of the "Generate Squares" module.
 * <p>
 *  Purpose:
 *    Acts as the main entry point for the Generate Squares workflow.
 *    Provides a GUI-driven process to:
 *      - Select a project directory
 *      - Configure analysis parameters
 *      - Run square-based calculations on TrackMate output
 *      - Export histogram summaries and consolidated CSVs
 * <p>
 *  Notes:
 *    This class contains only orchestration logic and user interface flow.
 *    All computational work is delegated to GenerateSquaresRunner.
 * <p>
 *  Author: Hans Bakker
 *  Version: 1.0
 *  Module: paint-generate-squares
 * ============================================================================
 */

package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.ValidProjectPath.getValidProjectPath;

public class GenerateSquares {

    /**
     * Application entry point for the Generate Squares GUI.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {

        try {
            // Use native OS look and feel (Aqua on macOS, system L&F elsewhere)
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("swing.aatext", "true");
            System.setProperty("swing.useSystemFontSettings", "true");
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {

            // --- Step 1: Determine last used project directory ---
            Path projectPath = getValidProjectPath();
            if (projectPath == null) {
                return;
            }

            // --- Step 2: Create console,initialise config and logger early ---
            PaintConsoleWindow.createConsoleFor("Generate Squares");
            PaintLogger.initialise(projectPath, "Generate Squares.log");
            PaintConfig.initialise(projectPath);

            JarInfoLogger.JarInfo info = getJarInfo(GenerateSquares.class);
            if (info != null) {
                PaintLogger.infof("Version: %s", info.implementationVersion);
                PaintLogger.infof("Compiled: %s", info.implementationDate);
            }
            String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            PaintLogger.infof("Current time is: %s", formattedTime);
            PaintLogger.blankline();

            PaintLogger.doc("Generate Squares", Arrays.asList(
                    "Line 1 of commentary",
                    "Line 2 of commentary",
                    "Line 3 of commentary"
            ));

            // --- Step 3: Show the integrated configuration dialog ---
            ProjectDialog dialog = new ProjectDialog(
                    null,
                    projectPath,
                    ProjectDialog.DialogMode.GENERATE_SQUARES
            );
            PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

            // --- Step 4: Run calculations when user presses OK ---
            dialog.setCalculationCallback(project -> {
                try {
                    GenerateSquaresHeadless.run(project.getProjectRootPath(), project.getExperimentNames());
                    return true;
                } catch (Exception e) {
                    PaintLogger.errorf("Generate Squares failed: %s", e.getMessage());
                    return false;
                }
            });

            // --- Step 5: Show dialog ---
            dialog.showDialog();
        });
    }
}