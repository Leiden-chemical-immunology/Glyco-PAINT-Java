/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.getomero.app;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Provides functionality to handle and reorganize files stored in directories
 * starting with the prefix {@code "Fileset"}. Extracts contained files and moves
 * them into the root directory, then deletes the empty subdirectories.
 */
public class ProcessOmeroFiles {

    /**
     * Processes the specified root directory by identifying and handling subdirectories
     * whose names start with {@code "Fileset"}. Extracts files from these subdirectories,
     * moves them into the root directory, and deletes the now-empty folders.
     *
     * @param rootDir the root directory to process; must be non-null and a valid directory
     * @throws IOException if the directory is invalid, no "Fileset" folders exist,
     *                     or if file operations fail during processing
     */
    public static void process(File rootDir) throws IOException {
        if (rootDir == null || !rootDir.isDirectory()) {
            throw new IOException("Invalid root directory: " + rootDir);
        }

        // Collect Fileset* directories
        File[] filesetDirs = rootDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("Fileset"));
        if (filesetDirs == null || filesetDirs.length == 0) {
            throw new IOException("No Fileset directories found in: " + rootDir.getAbsolutePath());
        }

        // Process each Fileset directory
        for (File fsDir : filesetDirs) {

            // Move the data files up into the root.
            File[] files = fsDir.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
            if (files != null) {
                for (File f : files) {
                    Files.move(
                            f.toPath(),
                            new File(rootDir, f.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }

            // Remove the hidden files the move deliberately skipped — .DS_Store and friends are
            // operating-system metadata, not data. They also make the folder non-empty, which is
            // why the delete below used to throw DirectoryNotEmptyException on virtually every
            // macOS import.
            File[] hidden = fsDir.listFiles(f -> f.isFile() && f.getName().startsWith("."));
            if (hidden != null) {
                for (File h : hidden) {
                    Files.deleteIfExists(h.toPath());
                }
            }

            // The Fileset folder should now be empty. If it is not, something unexpected is in
            // there (a nested folder, say) — do not delete it blindly, as that would destroy data.
            // Report it and leave it alone.
            try {
                Files.delete(fsDir.toPath());
            } catch (DirectoryNotEmptyException e) {
                String[] remaining = fsDir.list();
                throw new IOException(
                        "Fileset folder still contains entries after its files were moved, so it "
                                + "was left in place: " + fsDir.getAbsolutePath()
                                + " (contains: "
                                + (remaining == null ? "unreadable" : String.join(", ", remaining))
                                + ")", e);
            }
        }
    }
}