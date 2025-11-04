package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;

/**
 * BuildAllExecutables
 *
 * PURPOSE:
 *   Builds both Windows (.exe) and macOS (.app) Glyco-PAINT desktop applications
 *   and gathers them into:
 *
 *       /Users/hans/JavaPaintProjects/Glyco-PAINT-<version>/{Windows,macOS}
 *
 * BEHAVIOR:
 *   • Windows → copies .exe files from /target/
 *   • macOS → copies the full .app bundle (not the .zip)
 */
public class BuildAllExecutables {

    // === CONFIGURATION ===
    private static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment"
    );

    private static final Path BASE_PATH = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");

    public static void main(String[] args) {
        try {
            new BuildAllExecutables().run();
        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // === MAIN WORKFLOW ===
    private void run() throws Exception {
        System.out.println("=== Building Glyco-PAINT apps for macOS and Windows ===");

        // Determine version from parent pom.xml
        String version = getVersionFromPom(BASE_PATH.resolve("pom.xml"));
        if (version == null || version.isEmpty()) {
            throw new IllegalStateException("Could not determine version from parent pom.xml");
        }

        // Build target directories
        Path buildPath   = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Builds", "Glyco-PAINT-" + version);
        Path windowsPath = buildPath.resolve("Windows");
        Path macOSPath   = buildPath.resolve("macOS");

        Files.createDirectories(windowsPath);
        Files.createDirectories(macOSPath);

        System.out.println("📦 Output base: " + buildPath.toAbsolutePath());

        for (String module : MODULES) {
            Path moduleDir = BASE_PATH.resolve(module);
            System.out.println("\n---------------------------------------------");
            System.out.println("🏗️  Module: " + module);
            System.out.println("---------------------------------------------");

            // --- Windows build ---
            buildAndCollect(moduleDir, "-Pwindows-exe", "*.exe", windowsPath);

            // --- macOS build ---
            buildAndCollectMacApp(moduleDir, "-Pmacos-appbundle", macOSPath);
        }

        System.out.println("\n🎉 All builds complete!");
        System.out.println("✅ Output directory: " + buildPath.toAbsolutePath());
    }

    // ---------------------------------------------------------------------
    // Build and collect for Windows
    // ---------------------------------------------------------------------
    private void buildAndCollect(Path moduleDir, String profile, String glob, Path destDir)
            throws IOException, InterruptedException {

        System.out.println("🛠️  Running Maven build " + profile + " in " + moduleDir.getFileName());
        ProcessBuilder pb = new ProcessBuilder("mvn", "-q", "clean", "package", profile);
        pb.directory(moduleDir.toFile());
        pb.inheritIO();

        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            System.err.println("❌ Build failed for " + moduleDir.getFileName() + " (" + profile + ")");
            return;
        }

        copyMatchingFiles(moduleDir.resolve("target"), glob, destDir);
    }

    // ---------------------------------------------------------------------
    // Build and collect for macOS (copy .app directly)
    // ---------------------------------------------------------------------
    private void buildAndCollectMacApp(Path moduleDir, String profile, Path destDir)
            throws IOException, InterruptedException {

        System.out.println("🍎  Running Maven build " + profile + " in " + moduleDir.getFileName());
        ProcessBuilder pb = new ProcessBuilder("mvn", "-q", "clean", "package", profile);
        pb.directory(moduleDir.toFile());
        pb.inheritIO();

        Process process = pb.start();
        int exit = process.waitFor();
        if (exit != 0) {
            System.err.println("❌ macOS build failed for " + moduleDir.getFileName());
            return;
        }

        // Copy any generated .app bundles directly
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleDir.resolve("target"), "*.app")) {
            for (Path appBundle : stream) {
                Path dest = destDir.resolve(appBundle.getFileName());
                System.out.println("📦 Copying " + appBundle.getFileName() + " → " + destDir);
                copyDirectory(appBundle, dest);
                System.out.println("✅ Copied " + appBundle.getFileName());
            }
        }
    }

    // ---------------------------------------------------------------------
    // File utilities
    // ---------------------------------------------------------------------
    private void copyMatchingFiles(Path fromDir, String glob, Path destDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fromDir, glob)) {
            for (Path file : stream) {
                Path dest = destDir.resolve(file.getFileName());
                Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Copied " + file.getFileName() + " → " + destDir.getFileName());
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path relative = source.relativize(path);
                Path dest = target.resolve(relative);
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

    private String getVersionFromPom(Path pomPath) {
        try (InputStream in = Files.newInputStream(pomPath)) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder        = factory.newDocumentBuilder();
            org.w3c.dom.Document doc       = builder.parse(in);
            org.w3c.dom.NodeList list      = doc.getElementsByTagName("version");
            if (list.getLength() > 0) {
                return list.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not read version from " + pomPath + ": " + e.getMessage());
        }
        return null;
    }
}