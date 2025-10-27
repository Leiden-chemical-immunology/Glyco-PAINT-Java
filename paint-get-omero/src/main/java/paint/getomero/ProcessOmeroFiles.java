package paint.getomero;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * The ProcessOmeroFiles class provides functionality to handle and reorganize
 * files stored in directories starting with the prefix "Fileset". This involves
 * extracting files from these directories and moving them into a root directory,
 * followed by the deletion of the now-empty subdirectories.
 */
public class ProcessOmeroFiles {

    /**
     * Processes a given root directory by identifying and handling subdirectories
     * whose names start with "Fileset". Extracts files from these subdirectories,
     * moves them to the root directory, and deletes the now-empty subdirectories.
     *
     * @param rootDir the root directory to process. It must be a valid directory and not null.
     * @throws IOException if the provided directory is invalid, no subdirectories
     *                     starting with "Fileset" are found, or file operations fail.
     */
    public static void process(File rootDir) throws IOException {
        if (rootDir == null || !rootDir.isDirectory()) {
            throw new IOException("Invalid root directory: " + rootDir);
        }

        // 🔹 Collect Fileset* directories immediately
        File[] filesetDirs = rootDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("Fileset"));
        if (filesetDirs == null || filesetDirs.length == 0) {
            throw new IOException("No Fileset directories found in: " + rootDir.getAbsolutePath());
        }

        // 🔹 Process each Fileset dir
        for (File fsDir : filesetDirs) {
            File[] files = fsDir.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
            if (files != null) {
                for (File f : files) {
                    Files.move(
                            f.toPath(),
                            new File(rootDir, f.getName()).toPath(),   // move into root dir
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
            // delete the now-empty Fileset directory
            Files.delete(fsDir.toPath());
        }
    }
}