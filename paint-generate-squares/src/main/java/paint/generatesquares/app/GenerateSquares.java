/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.generatesquares.app;

import paint.shared.config.paintconfig.PaintConfig;
import paint.ui.dialogs.ProjectDialog;
import paint.shared.objects.Project;
import paint.shared.utils.*;

import javax.swing.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.utils.JarInfoLogger.getJarInfo;
import static paint.ui.dialogs.ProjectPathResolver.getValidProjectPath;

/**
 * The GenerateSquares class serves as the main entry point for launching the
 * GENERATE_SQUARES GUI application. It initializes project configuration,
 * logging, and the runtime environment, and provides an interactive interface
 * for selecting projects and triggering square-generation workflows.
 * <p>
 * Square computation and data export tasks are performed by
 * {@link paint.generatesquares.app.GenerateSquaresHeadless}.
 */
public class GenerateSquares {

    /**
     * The main entry point for the GENERATE_SQUARES application. This method sets
     * up the environment, applies system look-and-feel settings, and initializes
     * configuration, logging, and GUI components used for selecting project
     * directories and executing square-generation workflows.
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

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                // --- Step 1: Determine the last used valid project directory ---
                Path projectPath = getValidProjectPath();
                if (projectPath == null) {
                    return;
                }

                // --- Step 2: Create console, initialize config and logger early ---
                paint.ui.console.PaintConsoleWindow.createConsoleFor(GENERATE_SQUARES);
                PaintLogger.initialise(projectPath, "Generate Squares.log");
                PaintLogger.setLevel(PaintPrefs.getString("Runtime", "Log Level", "INFO"));
                PaintConfig.initialise(projectPath);

                JarInfoLogger.JarInfo info = getJarInfo(GenerateSquares.class);
                if (info != null) {
                    PaintLogger.infof("Version: %s", info.implementationVersion);
                    PaintLogger.infof("Compiled: %s", info.implementationDate);
                }
                PaintLogger.infof("Verbose mode is %s",
                                  PaintRuntime.isVerbose() ? "enabled" : "disabled");

                String formattedTime = LocalDateTime.now()
                                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                PaintLogger.infof("Current time is: %s", formattedTime);
                PaintLogger.infof("Logging level is %s", PaintLogger.getLevelName());
                PaintLogger.blankline();

                // --- Step 3: Show the integrated configuration dialog ---
                ProjectDialog dialog = new ProjectDialog(
                        null,
                        projectPath,
                        ProjectDialog.DialogMode.GENERATE_SQUARES
                );
                paint.ui.console.PaintConsoleWindow.closeOnDialogDispose(dialog.getDialog());

                // --- Step 4: Run calculations when the user presses OK ---
                dialog.setCalculationCallback(this::runGenerateSquares);

                // --- Step 5: Show dialog ---
                dialog.showDialog();
            }

            private boolean runGenerateSquares(Project project) {
                try {
                    GenerateSquaresHeadless.run(
                            project.getProjectRootPath(),
                            project.getExperimentNames()
                    );
                    return true;

                } catch (Exception e) {
                    PaintLogger.errorf("Generate Squares failed: %s", e.getMessage());
                    return false;
                }
            }
        });
    }

}