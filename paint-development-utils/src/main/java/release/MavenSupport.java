package release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class MavenSupport {
    private MavenSupport() {
    }

    static void rebuildSharedUtils() throws IOException, InterruptedException {
        Path utilsDir = PathsConfig.BASE_PATH.resolve("paint-shared-utils");
        System.out.println("\n🧱 Building paint-shared-utils...");
        if (!Files.exists(utilsDir.resolve("pom.xml"))) {
            throw new IOException("Missing pom.xml in " + utilsDir);
        }

        String localRepo = System.getProperty("user.home") + "/.m2/repository";
        List<String> cmd = Arrays.asList(
                "mvn", "-q", "-U", "clean", "install",
                "-DskipTests",
                "-Dmaven.repo.local=" + localRepo
        );
        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(utilsDir.toFile());
        Process process = ProcessRunner.startAndFilterOutput(pb, "paint-shared-utils");
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Failed to install paint-shared-utils. Exit code: " + exit);
        }
        System.out.println("✅ paint-shared-utils installed successfully (refreshed local repo).");
    }

    static void buildAndCollect(Path moduleDir, String profile, String glob, Path destDir)
            throws IOException, InterruptedException {

        String localRepo = System.getProperty("user.home") + "/.m2/repository";

        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("mvn", "-U", "-q", "clean", "package"));
        if (profile != null && profile.trim().length() > 0) {
            cmd.add(profile.trim());
        }
        cmd.addAll(Arrays.asList(
                "-Dmaven.repo.local=" + localRepo,
                "-Dmaven.artifact.threads=1"
        ));

        System.out.println("🔧 Running: " + String.join(" ", cmd) + " (in " + moduleDir.getFileName() + ")");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(moduleDir.toFile());
        Process process = ProcessRunner.startAndFilterOutput(pb, moduleDir.getFileName().toString());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Build failed for " + moduleDir.getFileName() + " (" + profile + ")");
        }

        FileOps.copyMatchingFiles(moduleDir.resolve("target"), glob, destDir);
        Thread.sleep(2000);
    }

    static void buildAndCollectMacApp(Path moduleDir, String profile, Path destDir)
            throws IOException, InterruptedException {

        String localRepo = System.getProperty("user.home") + "/.m2/repository";
        List<String> cmd = Arrays.asList(
                "mvn", "-U", "-q", "clean", "package",
                profile,
                "-Dmaven.repo.local=" + localRepo,
                "-Dmaven.artifact.threads=1"
        );

        System.out.println("🔧 Running: " + String.join(" ", cmd) + " (in " + moduleDir.getFileName() + ")");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(moduleDir.toFile());
        Process process = ProcessRunner.startAndFilterOutput(pb, moduleDir.getFileName().toString());
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ macOS build failed for " + moduleDir.getFileName());
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleDir.resolve("target"), "*.app")) {
            for (Path appBundle : stream) {
                Path dest = destDir.resolve(appBundle.getFileName());
                System.out.println("📦 Copying " + appBundle.getFileName() + " → " + destDir);
                FileOps.copyDirectory(appBundle, dest);
                System.out.println("✅ Copied .app bundle");
            }
        }
        Thread.sleep(2000);
    }

    static void installParentPom() throws IOException, InterruptedException {
        Path parentPom = PathsConfig.BASE_PATH.resolve("pom.xml");
        System.out.println("\n🧩 Installing parent POM locally...");
        if (!Files.exists(parentPom)) {
            throw new IOException("Parent POM not found at " + parentPom);
        }

        List<String> cmd = Arrays.asList(
                "mvn", "-q", "-U", "install", "-N", "-DskipTests",
                "-Dmaven.repo.local=" + System.getProperty("user.home") + "/.m2/repository"
        );
        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(PathsConfig.BASE_PATH.toFile());
        Process process = ProcessRunner.startAndFilterOutput(pb, "paint-parent");
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Failed to install paint-parent. Exit code: " + exit);
        }

        System.out.println("✅ Parent POM installed locally.");
    }

    static void installParentPomAsRelease(String releaseVersion) throws IOException, InterruptedException {
        Path parentPom = PathsConfig.BASE_PATH.resolve("pom.xml");
        if (!Files.exists(parentPom)) {
            System.out.println("⚠️  Parent POM not found — skipping release install.");
            return;
        }

        System.out.println("\n🧩 Installing parent POM as release " + releaseVersion + "...");
        Path tmpPom = Files.createTempFile("parent-release-", ".xml");
        Files.copy(parentPom, tmpPom, StandardCopyOption.REPLACE_EXISTING);

        String content = new String(Files.readAllBytes(tmpPom), StandardCharsets.UTF_8)
                .replaceAll("<version>.*?</version>", "<version>" + releaseVersion + "</version>");
        Files.write(tmpPom, content.getBytes(StandardCharsets.UTF_8));

        List<String> cmd = Arrays.asList(
                "mvn", "-q", "install:install-file",
                "-Dfile=" + tmpPom.toAbsolutePath(),
                "-DgroupId=com.github.jjabakker",
                "-DartifactId=paint-parent",
                "-Dversion=" + releaseVersion,
                "-Dpackaging=pom"
        );

        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(PathsConfig.BASE_PATH.toFile());
        pb.inheritIO();
        ProcessRunner.enforceJava8(pb);
        Process process = pb.start();
        int exit = process.waitFor();

        Files.deleteIfExists(tmpPom);

        Path localPom = Paths.get(System.getProperty("user.home"),
                                  ".m2", "repository", "com", "github", "jjabakker", "paint-parent", releaseVersion,
                                  "paint-parent-" + releaseVersion + ".pom");

        if (exit == 0 && Files.exists(localPom)) {
            System.out.println("✅ Installed paint-parent " + releaseVersion + " locally at:");
            System.out.println("   " + localPom.toAbsolutePath());
        } else {
            System.err.println("❌ Failed to install parent POM release version " + releaseVersion);
            if (!Files.exists(localPom)) {
                System.err.println("   ⚠️  No POM found at expected path: " + localPom.toAbsolutePath());
            }
            throw new RuntimeException("Failed to install paint-parent release version " + releaseVersion);
        }
    }

    static void runMavenModule(String module, String version) throws Exception {
        List<String> cmd = Arrays.asList(
                "mvn", "-q", "-U", "clean", "package",
                "-pl", module, "-am",
                "-DskipTests",
                "-Dproject.version=" + version
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(PathsConfig.BASE_PATH.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Maven build failed for module: " + module);
        }
    }

    static void alignAllPomVersions(String newVersion) throws IOException, InterruptedException {
        System.out.println("🔄 Aligning all POM versions to " + newVersion + " using Maven Versions Plugin...");

        Path projectRoot = PathsConfig.BASE_PATH.toAbsolutePath();

        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "versions:set",
                "-DnewVersion=" + newVersion,
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:set");

        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "-N", "install", "-DskipTests"
        ), PathsConfig.BASE_PATH, "install paint-parent");

        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "-N", "install:install-file",
                "-Dfile=paint-installer/pom.xml",
                "-DgroupId=com.github.jjabakker",
                "-DartifactId=paint-installer",
                "-Dversion=" + newVersion,
                "-Dpackaging=pom"
        ), PathsConfig.BASE_PATH, "install paint-installer");

        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "versions:update-child-modules",
                "-DforceVersion=true",
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:update-child-modules");

        System.out.println("✅ All modules (including nested) now aligned to version " + newVersion);
    }

    static void runMaven(List<String> cmd, Path dir, String label) throws IOException, InterruptedException {
        System.out.println("🔧 Running (" + label + "): " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Maven command failed: " + label);
        }
    }
}