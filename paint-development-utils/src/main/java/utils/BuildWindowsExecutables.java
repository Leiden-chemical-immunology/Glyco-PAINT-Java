package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BuildWindowsExecutables {

    private static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment"
    );

    public static void main(String[] args) throws IOException, InterruptedException {
        Path basePath = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");
        Path buildPath = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Builds/build/Windows");
        Files.createDirectories(buildPath);

        System.out.println("=== Building Windows executables for PAINT desktop apps ===");

        for (String module : MODULES) {
            Path moduleDir = basePath.resolve(module);
            System.out.println("\n🏗️  Building " + module + " ...");

            ProcessBuilder pb = new ProcessBuilder("mvn", "-q", "clean", "package", "-Pwindows-exe");
            pb.directory(moduleDir.toFile());
            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("❌ Build failed for " + module);
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleDir.resolve("target"), "*.exe")) {
                for (Path exe : stream) {
                    Files.copy(exe, buildPath.resolve(exe.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("✅ Copied " + exe.getFileName() + " to " + buildPath);
                }
            }
        }

        System.out.println("\n🎉 All builds complete.");
        System.out.println("Executables available in: " + buildPath);
    }
}