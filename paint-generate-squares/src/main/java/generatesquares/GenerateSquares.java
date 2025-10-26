/******************************************************************************
 *  Class:        GenerateSquares.java
 *  Package:      generatesquares
 *
 *  PURPOSE:
 *    Acts as the main entry point for the “Generate Squares” module,
 *    providing a GUI-driven workflow to configure and execute square-based
 *    analyses on TrackMate output data.
 *
 *  DESCRIPTION:
 *    This class handles orchestration and user interaction. It initializes
 *    project configurations, sets up logging and runtime parameters, and
 *    launches a Swing-based interface allowing users to:
 *      • Select a project directory
 *      • Configure analysis parameters
 *      • Run square-level calculations and data exports
 *    Computational work is delegated to {@link generatesquares.GenerateSquaresHeadless}.
 *
 *  RESPONSIBILITIES:
 *    • Initialize PaintConfig, logging, and runtime environment
 *    • Provide GUI interaction for project and experiment selection
 *    • Launch headless processing via GenerateSquaresHeadless
 *
 *  USAGE EXAMPLE:
 *    $ java -jar paint-generate-squares.jar
 *
 *  DEPENDENCIES:
 *    - paint.shared.config.PaintConfig
 *    - paint.shared.dialogs.ProjectDialog
 *    - paint.shared.utils.{PaintLogger, PaintConsoleWindow, PaintRuntime, JarInfoLogger}
 *    - javax.swing.*
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-23
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package generatesquares;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.utils.JarInfoLogger;
import paint.shared.utils.PaintConsoleWindow;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;

import javax.swing.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.shared.utils.ValidProjectPath.getValidProjectPath;

/**
 * The GenerateSquares class is the main entry point for running the "Generate Squares" GUI application.
 * This application initializes required configurations, manages project directories, and performs
 * square generation calculations in response to user inputs.
 */
public class GenerateSquares {

    /**
     * The main entry point for the "Generate Squares" application. This method initializes the
     * application's environment, configuration, logging, and graphical user interface (GUI)
     * components to manage project directories and perform square generation calculations
     * based on user input.
     *
     * @param args command-line arguments (not used by this application)
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
            PaintLogger.infof("Verbose mode is %s", PaintRuntime.isVerbose() ? "enabled" : "disabled");
            String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            PaintLogger.infof("Current time is: %s", formattedTime);
            PaintLogger.blankline();

            // --- Step 3: Show the integrated configuration dialog ---
            ProjectDialog dialog = new ProjectDialog(
                    null,
                    projectPath,
                    ProjectDialog.DialogMode.GENERATE_SQUARES
            );
            PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

            // --- Step 4: Run calculations when the user presses OK ---
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