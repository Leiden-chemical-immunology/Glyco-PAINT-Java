/*=============================================================================
 *  Class:        FileOps.java
 *  Package:      release
 *
 *  PURPOSE:
 *    Utility methods for file system operations during the release process.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-development-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package release.support;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.function.Predicate;

public final class FileOps {

    private FileOps() {

    }

    public static void copyMatchingFiles(Path fromDir, String glob, Path destDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fromDir, glob)) {
            for (Path file : stream) {
                Files.copy(file, destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Copied " + file.getFileName() + " → " + destDir.getFileName());
            }
        }
    }

    public static void copyDirectory(Path source, Path target) throws IOException {
        try (java.util.stream.Stream<Path> paths = Files.walk(source)) {
            paths.forEach(path -> {
                try {
                    Path dest = target.resolve(source.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    public static void zipPayload(Path appDir, Path pluginDir, Path outputZip) throws Exception {
        // zip -qry outputZip .
        ProcessBuilder pb = new ProcessBuilder("zip", "-qry", outputZip.toString(), ".");
        pb.directory(appDir.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Failed to zip payload at " + appDir);
        }

        if (Files.exists(pluginDir)) {
            try (java.util.stream.Stream<Path> s = Files.list(pluginDir)) {
                if (s.findAny().isPresent()) {

                    String cmdStr = String.format(
                            "cd \"%s\" && mkdir -p ../_plugin_tmp && cp -R . ../_plugin_tmp/plugin && " +
                                    "cd ../_plugin_tmp && zip -qry \"%s\" plugin && cd .. && rm -rf _plugin_tmp",
                            pluginDir.toAbsolutePath(), outputZip.toAbsolutePath()
                    );

                    ProcessBuilder addPb = new ProcessBuilder("bash", "-c", cmdStr);
                    addPb.inheritIO();

                    if (addPb.start().waitFor() != 0) {
                        throw new RuntimeException("❌ Failed to append plugin to " + outputZip);
                    }
                }
            }
        }
    }

    public static Path latestMatching(Path dir, Predicate<String> fileNamePredicate) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> fileNamePredicate.test(p.getFileName().toString()))
                    .max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toMillis();
                        } catch (IOException e) {
                            return Long.MIN_VALUE;
                        }
                    }))
                    .orElseThrow(IOException::new);
        }
    }
}