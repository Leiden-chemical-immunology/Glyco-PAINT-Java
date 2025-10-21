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
 *  Author: Herr Doctor
 *  Version: 1.0
 *  Module: paint-generate-squares
 * ============================================================================
 */

package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectSpecificationDialog;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static paint.shared.utils.JarInfoLogger.getJarInfo;

public class GenerateSquares {

    /**
     * Main entry point for the Generate Squares module.
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
            String lastProjectDir = paint.shared.prefs.PaintPrefs.getString(
                    "Project Root",
                    System.getProperty("user.home")
            );
            Path projectPath = Paths.get(lastProjectDir);

            // --- Step 2: Initialise config and logger early (in case project was known) ---
            PaintConfig.initialise(projectPath);
            PaintLogger.initialise(projectPath, "Generate Squares.log");

            JarInfoLogger.JarInfo info = getJarInfo(GenerateSquares.class);
            if (info != null) {
                PaintLogger.infof("Version: %s", info.implementationVersion);
                PaintLogger.infof("Compiled: %s", info.implementationDate);
            }

            LocalDateTime now = LocalDateTime.now();
            String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            PaintLogger.infof("Current time is: %s", formattedTime);
            PaintLogger.blankline();

            // --- Step 3: Show the integrated configuration dialog ---
            ProjectSpecificationDialog dialog = new ProjectSpecificationDialog(
                    null,
                    projectPath,
                    ProjectSpecificationDialog.DialogMode.GENERATE_SQUARES
            );

            // --- Step 4: Create console and tie it to dialog lifecycle ---
            PaintConsoleWindow.createConsoleFor("Generate Squares");
            PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

            // --- Step 5: Run calculations when user presses OK ---
            dialog.setCalculationCallback(project -> {
                try {
                    GenerateSquaresHeadless.run(project.getProjectRootPath(), project.getExperimentNames());
                    return true;
                } catch (Exception e) {
                    PaintLogger.errorf("Generate Squares failed: %s", e.getMessage());
                    return false;
                }
            });

            // --- Step 6: Show dialog ---
            dialog.showDialog();
        });
    }

}