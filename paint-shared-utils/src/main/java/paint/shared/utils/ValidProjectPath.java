/******************************************************************************
 *  Class:        ValidProjectPath.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Validates and returns a usable project root folder path for the PAINT
 *    application. Checks that the configured path exists and contains the
 *    required configuration file. If validation fails, prompts the user to
 *    select a valid folder via a native macOS file dialog.
 *
 *  DESCRIPTION:
 *    • Retrieves the stored “Project Root” path from preferences.
 *    • Verifies that the folder exists and contains the expected configuration file
 *      ({@code PAINT_CONFIGURATION_JSON}).
 *    • If validation fails, shows a warning message and invokes a native folder chooser.
 *    • Updates preferences if a new valid path is selected.
 *    • Returns the validated project path or {@code null} if the user cancels.
 *
 *  RESPONSIBILITIES:
 *    • Ensure that a valid project directory is available at runtime.
 *    • Interact with the user when reconfiguration is required.
 *
 *  USAGE EXAMPLE:
 *    Path projectRoot = ValidProjectPath.getValidProjectPath();
 *    if (projectRoot == null) {
 *        // handle cancellation or abort startup
 *    } else {
 *        // proceed with project using projectRoot
 *    }
 *
 *  DEPENDENCIES:
 *    – javax.swing.JOptionPane
 *    – java.awt.FileDialog
 *    – java.nio.file.{Path, Files}
 *    – paint.shared.constants.PaintConstants
 *    – paint.shared.utils.PaintPrefs
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.utils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;

/**
 * Validates and retrieves a usable project root folder path for the PAINT application.
 * <p>
 * If the stored path is invalid or missing the configuration file, the user is prompted
 * with a warning dialog and a native macOS folder chooser to select a valid directory.
 * The chosen path is stored persistently via {@link PaintPrefs}.
 * </p>
 */
public final class ValidProjectPath {

    /** Private constructor to prevent instantiation. */
    private ValidProjectPath() {}

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN VALIDATION LOGIC
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Retrieves and validates the configured project root directory.
     * <p>
     * Steps performed:
     * <ol>
     *   <li>Reads the stored project path from {@link PaintPrefs}.</li>
     *   <li>Checks that the directory exists and contains the required
     *       {@code PAINT_CONFIGURATION_JSON} file.</li>
     *   <li>If invalid, prompts the user with warnings and a native folder chooser.</li>
     *   <li>Updates preferences with the newly selected valid folder.</li>
     * </ol>
     * <p>
     * Returns {@code null} if the user cancels folder selection.
     * </p>
     *
     * @return a valid {@link Path} to the project root directory, or {@code null} if the user cancelled
     */
    public static Path getValidProjectPath() {

        boolean needToAsk = false;
        Path projectPath = Paths.get(PaintPrefs.getString("Path", "Project Root",
                                                          System.getProperty("user.home")));

        // Validate existence of the configured path
        if (!Files.isDirectory(projectPath)) {
            JOptionPane.showMessageDialog(null,
                                          "The configured project path is invalid:\n" + projectPath +
                                                  "\n\nPlease select a valid project folder.",
                                          "Invalid Project Path",
                                          JOptionPane.WARNING_MESSAGE);
            projectPath = Paths.get(System.getProperty("user.home"));
            needToAsk   = true;
        }

        // Validate configuration file presence
        Path confPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        if (!Files.isRegularFile(confPath)) {
            JOptionPane.showMessageDialog(null,
                                          "The project folder does not contain:\n" + confPath +
                                                  "\n\nPlease select a valid project folder.",
                                          "Invalid Project Path",
                                          JOptionPane.WARNING_MESSAGE);
            projectPath = Paths.get(System.getProperty("user.home"));
            needToAsk   = true;
        }

        // Ask user to select a valid folder if required
        if (needToAsk) {
            FileDialog chooser = new FileDialog((Frame) null, "Select Project Folder", FileDialog.LOAD);
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            chooser.setVisible(true);
            System.clearProperty("apple.awt.fileDialogForDirectories");

            String dir  = chooser.getDirectory();
            String file = chooser.getFile();

            if (dir != null && file != null) {
                projectPath = Paths.get(dir, file);
                PaintPrefs.putString("Path", "Project Root", projectPath.toString());
            } else {
                JOptionPane.showMessageDialog(null,
                                              "No project folder selected.\nExiting.",
                                              "Operation Cancelled",
                                              JOptionPane.ERROR_MESSAGE);
                return null; // Abort startup
            }
        }

        return projectPath;
    }
}