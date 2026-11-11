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
     * The profiles whose modules are not in the normal build: the installers (packaging), the
     * old installers kept as a backup (old-installers) and Glyco-PAINT.app (paint-app, active on
     * macOS by itself). The version commands activate them, so the versions of those modules move
     * along with the rest.
     */
    static final String VERSION_PROFILES = "-Ppackaging,old-installers,paint-app";

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

        Path localPom = Path.of(System.getProperty("user.home"),
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

    /**
     * Builds one of the old installer modules. They are only in the build with the
     * old-installers profile.
     */
    public static void runMavenModule(String module, String version) throws Exception {
        List<String> cmd = Arrays.asList(
                "mvn", "-q", "-U", "clean", "package",
                "-Pold-installers",
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

        runMaven(versionsSetCommand(newVersion, true), projectRoot, "versions:set");

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
                VERSION_PROFILES,
                "-DforceVersion=true",
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:update-child-modules");

        System.out.println("✅ All modules (including nested) now aligned to version " + newVersion);
    }

    /**
     * The Maven command that sets the project version. With {@code allModules} false only the
     * parent POM gets the new version (and the modules' references to it).
     */
    public static List<String> versionsSetCommand(String newVersion, boolean allModules) {
        return Arrays.asList(
                "mvn", "-q", "-B", "versions:set",
                VERSION_PROFILES,
                "-DnewVersion=" + newVersion,
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=" + allModules
        );
    }

    /**
     * Builds the installers (module packaging: the macOS .pkg and the Windows setup.exe) and
     * copies them into {@code destDir}. Both are asked for by their exact name, so a missing or
     * stale installer stops the release.
     */
    public static void buildInstallers(String version, Path destDir) throws IOException, InterruptedException {
        runMaven(installersCommand(), PathsConfig.BASE_PATH, "installers");

        Path dist = PathsConfig.BASE_PATH.resolve("packaging/target/dist");
        for (String name : installerNames(version)) {
            Path built = FileOps.requireArtifact(dist, name);
            Files.copy(built, destDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ " + name + " → " + destDir);
        }
    }

    static List<String> installersCommand() {
        return Arrays.asList(
                "mvn", "-q", "-B", "package",
                "-Ppackaging",
                "-pl", "packaging", "-am",
                "-DskipTests"
        );
    }

    static List<String> installerNames(String version) {
        return Arrays.asList(
                "Glyco-PAINT-" + version + "-macos.pkg",
                "Glyco-PAINT-" + version + "-windows-setup.exe"
        );
    }

    /**
     * Stops before anything is changed if makensis (needed for the Windows installer) is not
     * installed. Looks where packaging/pom.xml looks: the Homebrew folders and the PATH.
     */
    public static void requireMakensis() {
        if (findMakensis(System.getenv("PATH")) == null) {
            throw new IllegalStateException("makensis (needed for the Windows installer) was not found. "
                                                     + "Install it with: brew install makensis");
        }
    }

    static Path findMakensis(String pathVariable) {
        List<Path> candidates = new ArrayList<>(List.of(
                Path.of("/opt/homebrew/bin/makensis"),
                Path.of("/usr/local/bin/makensis")));
        if (pathVariable != null) {
            for (String dir : pathVariable.split(java.io.File.pathSeparator)) {
                if (!dir.isBlank()) {
                    candidates.add(Path.of(dir, "makensis"));
                }
            }
        }
        for (Path candidate : candidates) {
            if (Files.isExecutable(candidate)) {
                return candidate;
            }
        }
        return null;
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