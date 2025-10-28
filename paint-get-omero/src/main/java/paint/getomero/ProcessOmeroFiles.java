/******************************************************************************
 *  Class:        ProcessOmeroFiles.java
 *  Package:      paint.getomero
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
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *    Licensed under the MIT License.
 ******************************************************************************/

package paint.getomero;

import java.io.File;
import java.io.IOException;
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
            // Delete the now-empty Fileset directory
            Files.delete(fsDir.toPath());
        }
    }
}