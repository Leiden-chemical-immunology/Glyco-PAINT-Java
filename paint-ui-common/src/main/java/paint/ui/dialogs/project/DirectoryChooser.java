/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.ui.dialogs.project;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Utility class providing directory selection dialogs with platform-specific
 * behavior. macOS uses the native {@link FileDialog}, while other platforms
 * fall back to a {@link JFileChooser} configured for directory-only selection.
 *
 * <p>This class is final and cannot be instantiated.</p>
 */
final class DirectoryChooser {

    /** Private constructor to prevent instantiation of this utility class. */
    private DirectoryChooser() {
    }

    /**
     * Opens a directory chooser dialog, using the native macOS dialog where
     * available and {@link JFileChooser} otherwise.
     *
     * @param parent     Optional parent component for Swing dialogs.
     * @param title      The title displayed in the dialog.
     * @param initialDir Starting directory, falling back to user home if invalid.
     * @return The selected directory, or {@code null} if no selection was made.
     */
    static File chooseDirectory(Component parent, String title, String initialDir) {

        // Determine whether the system is macOS
        boolean isMac = System.getProperty("os.name")
                              .toLowerCase()
                              .contains("mac");

        // Resolve starting directory; fallback to user home if invalid
        File start = (initialDir != null && !initialDir.trim().isEmpty())
                ? new File(initialDir)
                : new File(System.getProperty("user.home"));

        if (!start.isDirectory()) {
            start = new File(System.getProperty("user.home"));
        }

        if (isMac) {
            // --- macOS native directory selection via AWT FileDialog ---

            FileDialog fd = new FileDialog((Frame) null, title, FileDialog.LOAD);
            fd.setDirectory(start.getAbsolutePath());

            // Enable directory-only mode on macOS
            System.setProperty("apple.awt.fileDialogForDirectories", "true");

            fd.setVisible(true);

            // Clear property after use to avoid side effects
            System.clearProperty("apple.awt.fileDialogForDirectories");

            String dir  = fd.getDirectory();
            String name = fd.getFile();

            if (dir != null && name != null) {
                File chosen = new File(dir, name);
                return chosen.isDirectory() ? chosen : null;
            }
            return null;

        } else {
            // --- Cross-platform Swing directory chooser ---

            JFileChooser chooser = new JFileChooser(start);
            chooser.setDialogTitle("Select directory for: " + title);
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            int res = chooser.showOpenDialog(parent);
            return (res == JFileChooser.APPROVE_OPTION)
                    ? chooser.getSelectedFile()
                    : null;
        }
    }
}