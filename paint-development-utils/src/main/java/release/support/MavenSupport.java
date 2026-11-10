/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides support for Maven-related operations during the release process.
 */
public final class MavenSupport {
    private MavenSupport() {
    }

    /**
     * Installs the given modules into the local repository together with every project module
     * they depend on (the parent POM, paint-shared-utils, paint-ui-common, ...).
     * <p>
     * The modules are later built one by one from their own folder, which only works if what
     * they depend on is already installed. Maven works out that list itself ({@code -am}), so a
     * new library module does not have to be added here by hand.
     */
    public static void installModulesAndDependencies(List<String> modules) throws IOException, InterruptedException {
        List<String> cmd = installModulesAndDependenciesCommand(modules);
        System.out.println("\n🧱 Installing the modules and everything they depend on...");
        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(PathsConfig.BASE_PATH.toFile());
        Process process = ProcessRunner.startAndFilterOutput(pb, "dependencies");
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Failed to install the modules and their dependencies. Exit code: " + exit);
        }
        System.out.println("✅ Modules and their dependencies installed (refreshed local repo).");
    }

    static List<String> installModulesAndDependenciesCommand(List<String> modules) {
        return Arrays.asList(
                "mvn", "-q", "-U", "install",
                "-DskipTests",
                "-pl", String.join(",", modules),
                "-am",
                "-Dmaven.repo.local=" + System.getProperty("user.home") + "/.m2/repository"
        );
    }

    public static void buildAndCollect(Path moduleDir, String profile, String glob, Path destDir)
            throws IOException, InterruptedException {

        String localRepo = System.getProperty("user.home") + "/.m2/repository";

        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("mvn", "-U", "-q", "clean", "package"));
        if (profile != null && !profile.trim().isEmpty()) {
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

    @SuppressWarnings("SameParameterValue")
    public static void buildAndCollectMacApp(Path moduleDir, String profile, Path destDir)
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

    public static void installParentPomAsRelease(String releaseVersion) throws IOException, InterruptedException {
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
        ProcessRunner.enforceJava21(pb);
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

    public static void runMavenModule(String module, String version) throws Exception {
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

    public static void alignAllPomVersions(String newVersion) throws IOException, InterruptedException {
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

    public static void runMaven(List<String> cmd, Path dir, String label) throws IOException, InterruptedException {
        System.out.println("🔧 Running (" + label + "): " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Maven command failed: " + label);
        }
    }
}