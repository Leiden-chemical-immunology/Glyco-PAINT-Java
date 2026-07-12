/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.ui.dialogs;

import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;

/**
 * Validates and retrieves a usable project root folder path for the PAINT application.
 * <p>
 * If the stored path is invalid or missing the configuration file, the user is prompted
 * with a warning dialog and a native macOS folder chooser to select a valid directory.
 * The chosen path is stored persistently via {@link PaintPrefs}.
 * </p>
 */
public final class ProjectPathResolver {

    /**
     * Private constructor to prevent instantiation.
     */
    private ProjectPathResolver() {
    }

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
     * @return a valid {@link Path} to the project root directory, or {@code null} if the user canceled
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
        if (!validateProjectFolder(projectPath)) {
            projectPath = Paths.get(System.getProperty("user.home"));
            needToAsk   = true;
        }

        // Ask user to select a valid folder if required
        if (needToAsk) {
            Path chosen = chooseProjectFolder(projectPath);

            if (chosen == null) {
                JOptionPane.showMessageDialog(
                        null,
                        "No project folder selected.\nExiting.",
                        "Operation Cancelled",
                        JOptionPane.ERROR_MESSAGE
                );
                return null;
            }

            projectPath = chosen;
            PaintPrefs.putString("Path", "Project Root", projectPath.toString());
        }
        return projectPath;
    }

    private static boolean validateProjectFolder(Path projectPath) {

        if (projectPath == null) {
            return false;
        }

        // Normalize Windows weirdness: trim whitespace, resolve canonical path
        try {
            projectPath = projectPath.toRealPath().normalize();
        } catch (Exception ignored) {
            projectPath = projectPath.normalize();
        }

        // Build expected Experiment Info path
        Path confPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);

        PaintLogger.debugf("=== Project Folder Validation ===");
        PaintLogger.debugf("projectPath = [" + projectPath + "]");
        PaintLogger.debugf("confPath    = [" + confPath + "]");
        PaintLogger.debugf("Exists      = " + Files.exists(confPath));
        PaintLogger.debugf("IsRegular   = " + Files.isRegularFile(confPath));
        PaintLogger.debugf("=================================");

        // A missing configuration file is no longer fatal: the folder is accepted
        // and the config is created with default values on first load (ConfigStore
        // logs a warning when it does so). Only warn here; do not block the user.
        if (!Files.exists(confPath) || !Files.isRegularFile(confPath)) {
            PaintLogger.warnf("No configuration file at %s; it will be created with default values.", confPath);
        }

        return true;
    }

    private static Path chooseProjectFolder(Path currentDefault) {

        String os = System.getProperty("os.name").toLowerCase();

        // macOS: use native folder picker
        if (os.contains("mac")) {
            FileDialog chooser = new FileDialog((Frame) null, "Select Project Folder", FileDialog.LOAD);

            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            chooser.setDirectory(currentDefault.toString());
            chooser.setVisible(true);
            System.clearProperty("apple.awt.fileDialogForDirectories");

            String dir = chooser.getDirectory();
            String file = chooser.getFile();

            // macOS returns folder name in "file"
            if (dir != null && file != null) {
                return Paths.get(dir, file);
            }

            // user cancelled
            return null;
        }

        // Windows/Linux: **use JFileChooser**, because FileDialog cannot select directories
        JFileChooser chooser = new JFileChooser(currentDefault.toFile());
        chooser.setDialogTitle("Select Project Folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (selected != null && selected.isDirectory()) {
                return selected.toPath();
            }
        }

        // user cancelled
        return null;
    }
}