/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import java.io.*;
import java.nio.file.*;

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

    /**
     * Resolves exactly one expected build artifact, by name.
     * <p>
     * This deliberately replaces an earlier "newest file matching a loose pattern" lookup. A
     * Maven target directory does not hold one candidate: {@code maven-shade-plugin} leaves the
     * pre-shade jar behind as {@code original-<name>.jar} alongside the real, shaded one. A
     * pattern such as {@code .*jar$} matches both, and picking the most recently modified one is
     * a race — shade writes them within milliseconds of each other. Losing that race means
     * shipping a thin, non-runnable jar under the official release name.
     * <p>
     * Artifact names are deterministic, so ask for the one we mean and fail loudly if it is not
     * there.
     *
     * @param dir      the directory to look in (typically a module's {@code target})
     * @param fileName the exact file name expected, e.g. {@code paint-installer-macos-1.2.3.jar}
     * @return the path to that artifact
     * @throws IOException if it does not exist, listing what the directory does contain
     */
    public static Path requireArtifact(Path dir, String fileName) throws IOException {
        Path artifact = dir.resolve(fileName);
        if (Files.isRegularFile(artifact)) {
            return artifact;
        }

        StringBuilder present = new StringBuilder();
        if (Files.isDirectory(dir)) {
            try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
                stream.map(p -> p.getFileName().toString())
                      .sorted()
                      .forEach(name -> present.append("\n    ").append(name));
            }
        } else {
            present.append("\n    (directory does not exist)");
        }

        throw new IOException("Expected build artifact not found: " + artifact
                + "\n  Directory contains:" + present);
    }
}