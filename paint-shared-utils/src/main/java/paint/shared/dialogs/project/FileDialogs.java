package paint.shared.dialogs.project;

import javax.swing.*;
import java.awt.*;
import java.io.File;

final class FileDialogs {
    private FileDialogs() {}

    static File chooseDirectory(Component parent, String title, String initialDir) {
        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        File start = (initialDir != null && !initialDir.trim().isEmpty()) ? new File(initialDir) : new File(System.getProperty("user.home"));
        if (!start.isDirectory()) start = new File(System.getProperty("user.home"));

        if (isMac) {
            FileDialog fd = new FileDialog((Frame) null, title, FileDialog.LOAD);
            fd.setDirectory(start.getAbsolutePath());
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            fd.setVisible(true);
            System.clearProperty("apple.awt.fileDialogForDirectories");
            String dir = fd.getDirectory();
            String name= fd.getFile();
            if (dir != null && name != null) {
                File chosen = new File(dir, name);
                return chosen.isDirectory() ? chosen : null;
            }
            return null;
        } else {
            JFileChooser chooser = new JFileChooser(start);
            chooser.setDialogTitle("Select directory for: " + title);
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int res = chooser.showOpenDialog(parent);
            return (res == JFileChooser.APPROVE_OPTION) ? chooser.getSelectedFile() : null;
        }
    }
}