/*=============================================================================
 *  Class:        ProcessOmeroFiles.java
 *  Package:      paint.getomero.app
 *
 *  PURPOSE:
 *    Handles reorganization of Omero-exported directories by extracting files
 *    from "Fileset" subdirectories and moving them to the root directory.
 *
 *  DESCRIPTION:
 *    This utility processes directories that contain nested "Fileset*" folders
 *    (typically produced by Omero exports). It moves all valid files from
 *    these subdirectories into the root directory and removes the now-empty
 *    "Fileset" folders. It is used internally by {@link GetOmeroUI} and can
 *    also be invoked headlessly for batch cleanup operations.
 *
 *  KEY FEATURES:
 *    • Detects and processes subdirectories beginning with "Fileset".
 *    • Moves contained files directly into the parent directory.
 *    • Deletes empty "Fileset" folders after transfer.
 *    • Supports safe overwrite using StandardCopyOption.REPLACE_EXISTING.
 *    • Designed for batch automation and GUI invocation.
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  MODULE:
 *    paint-get-omero
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *    Licensed under the MIT License.
=============================================================================*/

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