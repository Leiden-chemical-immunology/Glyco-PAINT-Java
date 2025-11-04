package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

public class BuildAllExecutables {

    private static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment"
    );

    private static final Path BASE_PATH   = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");
    private static final Path BUILDS_PATH = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Builds");

    public static void main(String[] args) {
        try {
            String bumpFlag = "0.0.x"; // default bump type
            if (args.length >= 2 && args[0].equalsIgnoreCase("-bump")) {
                bumpFlag = args[1];
            }

            new BuildAllExecutables().run(bumpFlag);
        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void run(String bumpFlag) throws Exception {
        System.out.println("=== Building Glyco-PAINT apps for macOS and Windows ===");

        Path parentPom = BASE_PATH.resolve("pom.xml");
        String currentVersion = getVersionFromPom(parentPom);
        if (currentVersion == null || !currentVersion.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("Expected SNAPSHOT version in parent pom.xml, found: " + currentVersion);
        }

        VersionInfo versionInfo = computeVersions(currentVersion, bumpFlag);
        System.out.println("🔢 Current: " + currentVersion);
        System.out.println("🏷️  Release: " + versionInfo.releaseVersion);
        System.out.println("🚀 Next dev: " + versionInfo.nextDevVersion);

        rebuildSharedUtils();
        bumpAllPomVersions(currentVersion, versionInfo.nextDevVersion);
        commitVersionBump(currentVersion, versionInfo.nextDevVersion);
        
        Path buildRoot   = BUILDS_PATH.resolve("Glyco-PAINT-" + versionInfo.releaseVersion);
        Path windowsPath = buildRoot.resolve("Windows");
        Path macOSPath   = buildRoot.resolve("macOS");
        Files.createDirectories(windowsPath);
        Files.createDirectories(macOSPath);

        System.out.println("📦 Output base: " + buildRoot);

        for (String module : MODULES) {
            Path moduleDir = BASE_PATH.resolve(module);
            System.out.println("\n---------------------------------------------");
            System.out.println("🏗️  Module: " + module);
            System.out.println("---------------------------------------------");

            buildAndCollect(moduleDir, "-Pwindows-exe", "*.exe", windowsPath);
            buildAndCollectMacApp(moduleDir, "-Pmacos-appbundle", macOSPath);
        }

        System.out.println("\n🎉 All builds complete!");
        System.out.println("✅ Output directory: " + buildRoot.toAbsolutePath());
    }

    // ===============================================================
    // 🔹 Build paint-shared-utils first
    // ===============================================================
    private void rebuildSharedUtils() throws IOException, InterruptedException {
        Path utilsDir = BASE_PATH.resolve("paint-shared-utils");
        System.out.println("\n🧱 Building paint-shared-utils first...");
        if (!Files.exists(utilsDir.resolve("pom.xml"))) {
            throw new IOException("Missing pom.xml in " + utilsDir);
        }

        List<String> cmd = Arrays.asList("mvn", "-U", "clean", "install", "-DskipTests");
        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(utilsDir.toFile());
        pb.inheritIO();
        Process process = pb.start();

        int exit = process.waitFor();
        if (exit != 0) throw new RuntimeException("❌ Failed to install paint-shared-utils. Exit code: " + exit);

        System.out.println("✅ paint-shared-utils installed successfully.");
    }

    // ===============================================================
    // Version bumping logic
    // ===============================================================
    private static class VersionInfo {
        final String releaseVersion, nextDevVersion;
        VersionInfo(String release, String next) { this.releaseVersion = release; this.nextDevVersion = next; }
    }

    private VersionInfo computeVersions(String currentVersion, String bumpFlag) {
        String base = currentVersion.replace("-SNAPSHOT", "");
        String[] parts = base.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);

        switch (bumpFlag) {
            case "0.0.x": patch++; break;
            case "0.x.0": minor++; patch = 0; break;
            case "x.0.0": major++; minor = 0; patch = 0; break;
            default: throw new IllegalArgumentException("Unknown bump flag: " + bumpFlag);
        }

        String releaseVersion = String.format("%d.%d.%d", major, minor, patch);
        return new VersionInfo(releaseVersion, releaseVersion + "-SNAPSHOT");
    }

    // ===============================================================
    // Build logic with visible commands
    // ===============================================================
    private void buildAndCollect(Path moduleDir, String profile, String glob, Path destDir)
            throws IOException, InterruptedException {

        List<String> cmd = Arrays.asList("mvn", "-U", "-q", "clean", "package", profile);
        System.out.println("🔧 Running: " + String.join(" ", cmd) + " (in " + moduleDir.getFileName() + ")");

        ProcessBuilder pb = new ProcessBuilder(cmd);
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

    private void buildAndCollectMacApp(Path moduleDir, String profile, Path destDir)
            throws IOException, InterruptedException {

        List<String> cmd = Arrays.asList("mvn", "-U", "-q", "clean", "package", profile);
        System.out.println("🔧 Running: " + String.join(" ", cmd) + " (in " + moduleDir.getFileName() + ")");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(moduleDir.toFile());
        pb.inheritIO();
        Process process = pb.start();

        int exit = process.waitFor();
        if (exit != 0) {
            System.err.println("❌ macOS build failed for " + moduleDir.getFileName());
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleDir.resolve("target"), "*.app*")) {
            for (Path appBundle : stream) {
                Path dest = destDir.resolve(appBundle.getFileName());
                System.out.println("📦 Copying " + appBundle.getFileName() + " → " + destDir);
                if (appBundle.toString().endsWith(".zip"))
                    Files.copy(appBundle, dest, StandardCopyOption.REPLACE_EXISTING);
                else
                    copyDirectory(appBundle, dest);
                System.out.println("✅ Copied " + appBundle.getFileName());
            }
        }
    }

    private void copyMatchingFiles(Path fromDir, String glob, Path destDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fromDir, glob)) {
            for (Path file : stream) {
                Files.copy(file, destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Copied " + file.getFileName() + " → " + destDir.getFileName());
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path dest = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) Files.createDirectories(dest);
                else Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    // ===============================================================
    // POM version utilities
    // ===============================================================
    private void bumpAllPomVersions(String oldVersion, String newVersion) throws Exception {
        Files.walk(BASE_PATH)
                .filter(p -> p.getFileName().toString().equals("pom.xml"))
                .forEach(p -> {
                    try { updatePomVersion(p, oldVersion, newVersion); }
                    catch (Exception e) { throw new RuntimeException(e); }
                });
        System.out.println("✅ Updated all pom.xml files to " + newVersion);
    }

    private void updatePomVersion(Path pom, String oldVersion, String newVersion) throws Exception {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse(Files.newInputStream(pom));
        NodeList list = doc.getElementsByTagName("version");
        for (int i = 0; i < list.getLength(); i++) {
            Node node = list.item(i);
            if (node.getTextContent().trim().equals(oldVersion)) node.setTextContent(newVersion);
        }
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.transform(new DOMSource(doc), new StreamResult(Files.newOutputStream(pom)));
    }

    private String getVersionFromPom(Path pomPath) {
        try (InputStream in = Files.newInputStream(pomPath)) {
            DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = b.parse(in);
            NodeList list = doc.getElementsByTagName("version");
            if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        } catch (Exception e) {
            System.err.println("⚠️  Could not read version from " + pomPath + ": " + e.getMessage());
        }
        return null;
    }

    // ===============================================================
    // 🔹 Commit updated pom.xml files to Git
    // ===============================================================
    private void commitVersionBump(String oldVersion, String newVersion) throws IOException, InterruptedException {
        Path repoDir = BASE_PATH;
        if (!Files.exists(repoDir.resolve(".git"))) {
            System.out.println("⚠️  No Git repository found — skipping commit.");
            return;
        }

        String message = String.format("Bump version: %s → %s", oldVersion, newVersion);

        // Stage all pom.xml changes
        List<String[]> commands = Arrays.asList(
                new String[]{"git", "add", "-u"},
                new String[]{"git", "commit", "-m", message}
        );

        for (String[] cmd : commands) {
            System.out.println("🔧 Running: " + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(repoDir.toFile());
            pb.inheritIO();
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit != 0) {
                System.err.println("⚠️  Git command failed: " + String.join(" ", cmd));
                return;
            }
        }

        System.out.println("✅ Committed version bump: " + message);
    }
}