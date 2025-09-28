package getomero;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ProcessOmeroFiles {

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