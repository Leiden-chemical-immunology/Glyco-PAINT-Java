package paint.shared.utils;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;

public class ValidProjectPath {

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
            needToAsk = true;
        }

        Path confPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        if (!Files.isRegularFile(confPath)) {
            JOptionPane.showMessageDialog(null,
                                          "There is no configured file:\n" + confPath +
                                                  "\n\nPlease select a valid project folder.",
                                          "Invalid Project Path",
                                          JOptionPane.WARNING_MESSAGE);
            projectPath = Paths.get(System.getProperty("user.home"));
            needToAsk = true;
        }

        if (needToAsk) {
            // macOS-friendly native folder picker
            FileDialog chooser = new FileDialog((Frame) null, "Select Project Folder", FileDialog.LOAD);
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            chooser.setVisible(true);
            System.clearProperty("apple.awt.fileDialogForDirectories");

            String dir = chooser.getDirectory();
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