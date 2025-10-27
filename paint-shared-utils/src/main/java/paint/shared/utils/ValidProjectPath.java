/******************************************************************************
 *  Class:        ValidProjectPath.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Validates and returns a usable project root folder path for the Paint
 *    application. It checks that the configured path exists and contains the
 *    required configuration file. If not, it prompts the user to select a valid
 *    folder via a native dialog.
 *
 *  DESCRIPTION:
 *    • Retrieves the stored “Project Root” path from preferences.
 *    • Verifies the folder exists and contains the expected configuration file
 *      (`PAINT_CONFIGURATION_JSON`).
 *    • If validation fails, prompts the user with a warning message and displays
 *      a folder chooser for manual selection.
 *    • Updates preferences if a new valid path is selected.
 *    • Returns the validated project path or `null` if the user cancels.
 *
 *  RESPONSIBILITIES:
 *    • Ensure the project folder is valid and ready for use by the application.
 *    • Interact with the user (via dialog) when validation fails.
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
 *    – paint.shared.utils.PaintPrefs (for preference access)
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
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
import java.nio.file.Paths;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;

public class ValidProjectPath {

    /**
     * Retrieves and validates the configured project root directory.
     * <p>
     * It first reads the stored project path from preferences. If the path does not exist
     * or does not contain the expected configuration JSON file, the user is prompted
     * via a warning dialog and a native folder chooser to select a valid project folder.
     * The preference is updated if a new folder is selected.
     * <p>
     * If the user cancels during folder selection, this method returns {@code null} indicating
     * the startup should be aborted.
     *
     * @return a valid {@link Path} to the project root directory, or {@code null} if the user cancelled the selection
     */
    public static Path getValidProjectPath() {

        boolean needToAsk = false;
        Path projectPath  = Paths.get(PaintPrefs.getString("Path", "Project Root", System.getProperty("user.home")));

        if (!Files.isDirectory(projectPath)) {
            JOptionPane.showMessageDialog(null,
                                          "The configured project path is invalid:\n" + projectPath +
                                                  "\n\nPlease select a valid project folder.",
                                          "Invalid Project Path",
                                          JOptionPane.WARNING_MESSAGE);
            projectPath = Paths.get(System.getProperty("user.home"));
            needToAsk   = true;
        }

        Path confPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        if (!Files.isRegularFile(confPath)) {
            JOptionPane.showMessageDialog(null,
                                          "There is no configured file:\n" + confPath +
                                                  "\n\nPlease select a valid project folder.",
                                          "Invalid Project Path",
                                          JOptionPane.WARNING_MESSAGE);
            projectPath = Paths.get(System.getProperty("user.home"));
            needToAsk   = true;
        }

        if (needToAsk) {
            // macOS-friendly native folder picker
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
                return null; // abort startup
            }
        }

        return projectPath;
    }
}