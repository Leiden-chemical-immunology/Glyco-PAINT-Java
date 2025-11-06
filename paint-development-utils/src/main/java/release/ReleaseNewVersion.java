// =================================================================================================
//  PURPOSE    : Automate full version bumping and multi-platform builds for all Glyco-PAINT modules.
//
//  DESCRIPTION:
//     This utility coordinates the end-to-end build pipeline for all Glyco-PAINT Java modules.
//     It performs version management, dependency installation, Git commits, and packaging
//     for both Windows and macOS executables in one run.
//
//  EXECUTION FLOW SUMMARY:
//     1. Read current parent POM version (must end with -SNAPSHOT).
//     2. Compute release and next development versions based on the bump flag.
//     3. Build and install current SNAPSHOT versions of shared and parent modules.
//     4. Install the parent POM also as a local release (for dependency resolution).
//     5. Bump all POM versions across modules and commit to Git.
//     6. Reinstall the bumped parent POM and rebuild shared-utils.
//     7. Build each module for both platforms, fail-fast on errors.
//     8. Copy built executables or app bundles into the organized build directory.
//
//  KEY FEATURES:
//     • Full automated version bumping and tagging
//     • Local parent POM release injection for offline builds
//     • Fail-fast build execution with clear per-module reporting
//     • Distinct build directories for macOS (.app bundles) and Windows (.exe)
//     • Git commit integration for version control synchronization
//
//  COMMAND-LINE FLAGS:
//     -bump <mode>     : Defines how to increment the version number.
//                        Supported values:
//                           0.0.x → increment patch (e.g., 0.0.26 → 0.0.27)
//                           0.x.0 → increment minor (e.g., 0.2.9 → 0.3.0)
//                           x.0.0 → increment major (e.g., 1.9.5 → 2.0.0)
//
//     --release        : Performs a full release sequence:
//                           • Converts SNAPSHOT to release version
//                           • Builds all modules for both platforms
//                           • Creates and pushes Git tag (vX.Y.Z)
//                           • Bumps back to next SNAPSHOT version
//
//     Example usage:
//         java utils.BuildAllExecutables -bump 0.0.x --release
//         java utils.BuildAllExecutables -bump 0.x.0
//
//  AUTHOR     : J.J. Bakker
//  MODULE     : paint-development-utils
//  UPDATED    : 2025-11-04
//  COPYRIGHT  : (c) 2025 J.J. Bakker. All rights reserved.
// ===============================================================================================

package release;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;

public class ReleaseNewVersion {

    private static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment",
            "paint-fiji-plugin"
    );

    private static final Path BASE_PATH   = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");
    private static final Path BUILDS_PATH = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Builds");

    /**
     * Ensures Maven runs with Java 8, even if this program itself runs on a newer JDK.
     */
    private static void enforceJava8(ProcessBuilder pb) {
        try {
            Process proc = new ProcessBuilder("/usr/libexec/java_home", "-v", "1.8").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String java8Home = reader.readLine();
                proc.waitFor();
                if (java8Home != null && !java8Home.isEmpty()) {
                    Map<String, String> env = pb.environment();
                    env.put("JAVA_HOME", java8Home);
                    env.put("PATH", java8Home + "/bin:" + env.get("PATH"));
                } else {
                    System.err.println("⚠️  Java 8 not found; Maven may build with a newer JDK.");
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not enforce Java 8 environment: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            String bumpFlag = "0.0.x";
            boolean doRelease = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-bump") && i + 1 < args.length) {
                    bumpFlag = args[i + 1];
                    i++;
                } else if (args[i].equalsIgnoreCase("--release")) {
                    doRelease = true;
                }
            }

            new ReleaseNewVersion().run(bumpFlag, doRelease);

        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run(String bumpFlag, boolean doRelease) throws Exception {
        System.out.println("=== Building Glyco-PAINT apps for macOS and Windows ===");

        VersionInfo versionInfo = null;  // ✅ Declare it here, before try

        try {
            Path parentPom = BASE_PATH.resolve("pom.xml");
            String currentVersion = getVersionFromPom(parentPom);
            if (currentVersion == null) {
                throw new IllegalStateException("Could not determine version from parent pom.xml");
            }

            if (!currentVersion.endsWith("-SNAPSHOT")) {
                System.out.println("⚠️  Parent POM is not a SNAPSHOT (" + currentVersion + ") — converting to snapshot for continued development.");
                currentVersion = currentVersion + "-SNAPSHOT";
            }

            versionInfo = computeVersions(currentVersion, bumpFlag);
            System.out.println("🔢  Current:  " + currentVersion);
            System.out.println("🏷️  Release: " + versionInfo.releaseVersion);
            System.out.println("🚀 Next dev: " + versionInfo.nextDevVersion);

            // --- Prepare environment ---
            installParentPom();
            rebuildSharedUtils();

            // --- Release preparation ---
            if (doRelease) {
                System.out.println("\n🎯 Preparing release " + versionInfo.releaseVersion);

                // ✅ Install parent release POM first so children resolve it
                installParentPomAsRelease(versionInfo.releaseVersion);

                // ✅ Align all POMs, remove -SNAPSHOT
                alignAllPomVersions(versionInfo.releaseVersion);
                removeSnapshotFromAllPoms();

                // ❌ Skip committing version bump — we'll tag directly instead
                System.out.println("ℹ️  Skipping commit of release version (tag only).");

                // ✅ Reinstall updated parent and rebuild shared-utils
                installParentPom();
                rebuildSharedUtils();
            } else {
                installParentPomAsRelease(versionInfo.releaseVersion);
            }

            // --- Prepare output directories ---
            Path buildRoot     = BUILDS_PATH.resolve("Glyco-PAINT-" + versionInfo.releaseVersion);
            Path windowsPath   = buildRoot.resolve("Windows");
            Path macOSPath     = buildRoot.resolve("macOS");
            Path pluginPath    = buildRoot.resolve("Plugins");
            Path installerPath = buildRoot.resolve("Installers");
            Files.createDirectories(installerPath);
            Files.createDirectories(windowsPath);
            Files.createDirectories(macOSPath);
            Files.createDirectories(pluginPath);
            System.out.println("📦 Output base: " + buildRoot);

            // --- Build all application modules ---
            for (String module : MODULES) {
                Path moduleDir = BASE_PATH.resolve(module);
                System.out.println("\n---------------------------------------------");
                System.out.println("🏗️  Module: " + module);
                System.out.println("---------------------------------------------");

                buildAndCollect(moduleDir, "-Pwindows-exe", "*.exe", windowsPath);
                buildAndCollectMacApp(moduleDir, "-Pmacos-appbundle", macOSPath);

                System.out.println("📦 Installing " + module + " into local Maven repo...");
                List<String> installCmd = Arrays.asList(
                        "mvn", "-q", "install", "-DskipTests",
                        "-Dmaven.repo.local=" + System.getProperty("user.home") + "/.m2/repository"
                );
                ProcessBuilder installPb = new ProcessBuilder(installCmd);
                installPb.directory(moduleDir.toFile());
                Process installProc = startAndFilterOutput(installPb, module);
                if (installProc.waitFor() != 0) {
                    throw new RuntimeException("❌ Failed to install " + module + " into local repo.");
                }
                System.out.println("✅ Installed " + module + " locally.");
            }

            // --- Build the Fiji plugin ---
            Path pluginDir = BASE_PATH.resolve("paint-fiji-plugin");
            if (Files.exists(pluginDir.resolve("pom.xml"))) {
                System.out.println("\n---------------------------------------------");
                System.out.println("🔬 Module: paint-fiji-plugin");
                System.out.println("---------------------------------------------");

                buildAndCollect(pluginDir, "", "*-jar-with-dependencies.jar", pluginPath);
            } else {
                System.out.println("⚠️  paint-fiji-plugin not found — skipping plugin build.");
            }

            // --- Tag release if needed ---
            if (doRelease) {
                createAndPushTag(versionInfo.releaseVersion);
            }

            // --- Bump back to next development version (local only) ---
            System.out.println("\n🔄 Restoring development version (" + versionInfo.nextDevVersion + ")...");
            alignAllPomVersions(versionInfo.nextDevVersion);

            // --- Skip committing; we'll roll back later ---
            System.out.println("ℹ️  Version bump staged locally (no commit will be kept).");

            // --- Ensure updated parent is reinstalled ---
            installParentPom();
            rebuildSharedUtils();

            // ======================================================================
            // 🔹 Build Installer Payloads (macOS + Windows)
            // ======================================================================
            System.out.println("\n---------------------------------------------");
            System.out.println("📦 Building macOS and Windows installer payloads");
            System.out.println("---------------------------------------------");

            Path macInstallerResources = BASE_PATH.resolve("paint-installer/paint-installer-macos/src/main/resources");
            Path winInstallerResources = BASE_PATH.resolve("paint-installer/paint-installer-windows/src/main/resources");
            Files.createDirectories(macInstallerResources);
            Files.createDirectories(winInstallerResources);

            Path macPayloadZip = macInstallerResources.resolve("payload.zip");
            Path winPayloadZip = winInstallerResources.resolve("payload.zip");
            Files.deleteIfExists(macPayloadZip);
            Files.deleteIfExists(winPayloadZip);

            zipPayload(macOSPath, pluginPath, macPayloadZip);
            zipPayload(windowsPath, pluginPath, winPayloadZip);

            System.out.println("✅ macOS payload → " + macPayloadZip);
            System.out.println("✅ Windows payload → " + winPayloadZip);

            // ======================================================================
            // 🔹 Build Installer JARs and Collect Results
            // ======================================================================
            System.out.println("\n---------------------------------------------");
            System.out.println("🛠️  Building installer packages");
            System.out.println("---------------------------------------------");

            runMavenModule("paint-installer/paint-installer-macos", versionInfo.releaseVersion);
            runMavenModule("paint-installer/paint-installer-windows", versionInfo.releaseVersion);

            Path macTarget = BASE_PATH.resolve("paint-installer/paint-installer-macos/target");
            Path winTarget = BASE_PATH.resolve("paint-installer/paint-installer-windows/target");
            Path macBuilt = Files.list(macTarget)
                    .filter(p -> p.getFileName().toString().toLowerCase().contains("installer") && p.toString().endsWith(".jar"))
                    .max(Comparator.comparingLong(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis(); }
                        catch (IOException e) { return Long.MIN_VALUE; }
                    }))
                    .orElseThrow(() -> new FileNotFoundException("❌ macOS installer not found"));

            Path winBuilt = Files.list(winTarget)
                    .filter(p -> p.getFileName().toString().toLowerCase().matches(".*(exe|jar|shaded\\.jar)$"))
                    .max(Comparator.comparingLong(p -> {
                        try { return Files.getLastModifiedTime(p).toMillis(); }
                        catch (IOException e) { return Long.MIN_VALUE; }
                    }))
                    .orElseThrow(() -> new FileNotFoundException("❌ Windows installer not found"));

            Path macFinal = installerPath.resolve("Glyco-PAINT-Installer-" + versionInfo.releaseVersion + ".jar");
            Path winFinal = installerPath.resolve("Glyco-PAINT-Installer-" + versionInfo.releaseVersion + ".exe");
            Files.copy(macBuilt, macFinal, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(winBuilt, winFinal, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ macOS installer → " + macFinal);
            System.out.println("✅ Windows installer → " + winFinal);

            System.out.println("\n🎉 All builds complete for " + versionInfo.releaseVersion);
            System.out.println("✅ Output directory: " + buildRoot.toAbsolutePath());

        } finally {
            // 🔄 Always restore all POMs to pre-build state
            rollbackPomChanges();

            // --- Commit updated parent POM version for next development cycle (release builds only) ---
            if (doRelease) {
                Path parentPom = BASE_PATH.resolve("pom.xml");
                String nextVersion = versionInfo.nextDevVersion;

                // 🔹 Re-apply the parent bump after rollback
                System.out.println("🔄 Setting parent POM version to " + nextVersion + "...");
                alignAllPomVersions(nextVersion);

                System.out.println("📝 Committing parent POM bump to " + nextVersion + " for continued development...");
                ProcessBuilder addParent = new ProcessBuilder("git", "add", parentPom.toString());
                addParent.directory(BASE_PATH.toFile());
                addParent.inheritIO();
                addParent.start().waitFor();

                String msg = "Bump parent POM to " + nextVersion + " for next development cycle";
                ProcessBuilder commitParent = new ProcessBuilder("git", "commit", "-m", msg);
                commitParent.directory(BASE_PATH.toFile());
                commitParent.inheritIO();
                if (commitParent.start().waitFor() == 0) {
                    System.out.println("✅ Parent POM committed for new development version: " + nextVersion);
                } else {
                    System.err.println("⚠️  No parent POM changes to commit.");
                }
            }
        }
    }

    /**
     * Builds and installs {@code paint-shared-utils} locally to ensure all dependencies are current.
     */
    private void rebuildSharedUtils() throws IOException, InterruptedException {
        Path utilsDir = BASE_PATH.resolve("paint-shared-utils");
        System.out.println("\n🧱 Building paint-shared-utils...");
        if (!Files.exists(utilsDir.resolve("pom.xml"))) {
            throw new IOException("Missing pom.xml in " + utilsDir);
        }

        String localRepo = System.getProperty("user.home") + "/.m2/repository";
        List<String> cmd = Arrays.asList(
                "mvn", "-U", "clean", "install",
                "-DskipTests",
                "-Dmaven.repo.local=" + localRepo
        );
        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(utilsDir.toFile());
        Process process = startAndFilterOutput(pb, "paint-shared-utils");
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Failed to install paint-shared-utils. Exit code: " + exit);
        }
        System.out.println("✅ paint-shared-utils installed successfully (refreshed local repo).");
    }

    // ======================================================================
    // 🔹 Helper classes and methods
    // ======================================================================

    private static class VersionInfo {
        final String releaseVersion, nextDevVersion;
        VersionInfo(String release, String next) {
            this.releaseVersion = release;
            this.nextDevVersion = next;
        }
    }

    private VersionInfo computeVersions(String currentVersion, String bumpFlag) {
        String base = currentVersion.replace("-SNAPSHOT", "").trim();

        // Extract last numeric segment
        String[] parts = base.split("\\.");
        int lastNum = Integer.parseInt(parts[parts.length - 1]);

        int next = lastNum + 1;

        String prefix = "";
        if (parts.length > 1) {
            prefix = String.join(".", Arrays.copyOf(parts, parts.length - 1)) + ".";
        }

        String releaseVersion = prefix + lastNum;
        String nextDevVersion = prefix + next + "-SNAPSHOT";

        return new VersionInfo(releaseVersion, nextDevVersion);
    }
    // ======================================================================
    // 🔹 Build Execution and Artifact Collection
    // ======================================================================

    /**
     * Builds a module using a specified Maven profile,
     * then copies matching artifacts into the output directory.
     */
    private void buildAndCollect(Path moduleDir, String profile, String glob, Path destDir)
            throws IOException, InterruptedException {

        // ✅ Use the global Maven repository (~/.m2/repository)
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
        Process process = startAndFilterOutput(pb, moduleDir.getFileName().toString());
        int exit = process.waitFor();

        if (exit != 0) {
            throw new RuntimeException("❌ Build failed for " + moduleDir.getFileName() + " (" + profile + ")");
        }

        copyMatchingFiles(moduleDir.resolve("target"), glob, destDir);
        Thread.sleep(2000); // allow file locks to clear
    }

    /**
     * Builds the macOS `.app` bundle version of a module and copies results to destination.
     */
    private void buildAndCollectMacApp(Path moduleDir, String profile, Path destDir)
            throws IOException, InterruptedException {

        // ✅ Use the global Maven repository (~/.m2/repository)
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
        Process process = startAndFilterOutput(pb, moduleDir.getFileName().toString());
        int exit = process.waitFor();

        if (exit != 0) {
            throw new RuntimeException("❌ macOS build failed for " + moduleDir.getFileName());
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleDir.resolve("target"), "*.app")) {
            for (Path appBundle : stream) {
                Path dest = destDir.resolve(appBundle.getFileName());
                System.out.println("📦 Copying " + appBundle.getFileName() + " → " + destDir);
                copyDirectory(appBundle, dest);
                System.out.println("✅ Copied .app bundle");
            }
        }

        Thread.sleep(2000);
    }

    // ======================================================================
    // 🔹 File Utilities
    // ======================================================================

    /** Copies files from source to destination matching a given glob pattern. */
    private void copyMatchingFiles(Path fromDir, String glob, Path destDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fromDir, glob)) {
            for (Path file : stream) {
                Files.copy(file, destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Copied " + file.getFileName() + " → " + destDir.getFileName());
            }
        }
    }

    /** Recursively copies a directory tree. */
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
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

    // ======================================================================
    // 🔹 POM and Git Utilities
    // ======================================================================

    private void alignAllPomVersions(String newVersion) throws IOException, InterruptedException {
        System.out.println("🔄 Aligning all POM versions to " + newVersion + " using Maven Versions Plugin...");

        Path projectRoot = BASE_PATH.toAbsolutePath();

        // 1️⃣ Set the new version recursively across all modules (even nested)
        runMaven(Arrays.asList(
                "mvn", "-B", "versions:set",
                "-DnewVersion=" + newVersion,
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:set");

        // 2️⃣ Install parent POMs locally so submodules can resolve them
        // Install top-level parent (paint-parent)
        runMaven(Arrays.asList(
                "mvn", "-B", "-N", "install", "-DskipTests"
        ), BASE_PATH, "install paint-parent");

        // Install installer aggregator (paint-installer)
        runMaven(Arrays.asList(
                "mvn", "-B", "-N", "install:install-file",
                "-Dfile=paint-installer/pom.xml",
                "-DgroupId=com.github.jjabakker",
                "-DartifactId=paint-installer",
                "-Dversion=" + newVersion,
                "-Dpackaging=pom"
        ), BASE_PATH, "install paint-installer");

        // 3️⃣ Update child module <parent> references to the new version
        runMaven(Arrays.asList(
                "mvn", "-B", "versions:update-child-modules",
                "-DforceVersion=true",
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:update-child-modules");

        System.out.println("✅ All modules (including nested) now aligned to version " + newVersion);
    }

    private void runMaven(List<String> cmd, Path dir, String label) throws IOException, InterruptedException {
        System.out.println("🔧 Running (" + label + "): " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Maven command failed: " + label);
        }
    }

    /** Extracts the version number from the given pom.xml. */
    private String getVersionFromPom(Path pomPath) {
        try (InputStream    in   = Files.newInputStream(pomPath)) {
            DocumentBuilder b    = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document        doc  = b.parse(in);
            NodeList        list = doc.getElementsByTagName("version");
            if (list.getLength() > 0) {
                return list.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not read version from " + pomPath + ": " + e.getMessage());
        }
        return null;
    }

    /** Commits only updated pom.xml files to Git. */
    private void commitVersionBump(String oldVersion, String newVersion) throws IOException, InterruptedException {
        Path repoDir = BASE_PATH;
        if (!Files.exists(repoDir.resolve(".git"))) {
            System.out.println("⚠️  No Git repository found — skipping commit.");
            return;
        }

        String message = String.format("Bump version: %s → %s", oldVersion, newVersion);

        // Use bash -c so globs (**/pom.xml) are expanded by the shell
        String addCommand = "bash -c 'shopt -s globstar; git add pom.xml **/pom.xml'";
        List<String[]> commands = Arrays.asList(
                new String[]{"bash", "-c", addCommand},
                new String[]{"git", "diff", "--cached", "--quiet"},
                new String[]{"git", "commit", "-m", message}
        );

        // --- 1️⃣ Add all pom.xml files
        System.out.println("🔧 Running: " + addCommand);
        ProcessBuilder addPb = new ProcessBuilder("bash", "-c", addCommand);
        addPb.directory(repoDir.toFile());
        addPb.inheritIO();
        Process addProc = addPb.start();
        if (addProc.waitFor() != 0) {
            System.err.println("⚠️  git add failed — check repository status.");
            return;
        }

        // --- 2️⃣ Check if anything was staged
        ProcessBuilder diffPb = new ProcessBuilder("git", "diff", "--cached", "--quiet");
        diffPb.directory(repoDir.toFile());
        Process diffProc = diffPb.start();
        int diffExit = diffProc.waitFor();

        if (diffExit == 0) {
            System.out.println("ℹ️  No pom.xml changes to commit.");
            return;
        }

        // --- 3️⃣ Commit staged pom.xml files
        System.out.println("🔧 Running: git commit -m \"" + message + "\"");
        ProcessBuilder commitPb = new ProcessBuilder("git", "commit", "-m", message);
        commitPb.directory(repoDir.toFile());
        commitPb.inheritIO();
        Process commitProc = commitPb.start();
        int commitExit = commitProc.waitFor();

        if (commitExit == 0) {
            System.out.println("✅ Committed pom.xml version bump: " + message);
        } else {
            System.err.println("⚠️  git commit failed (nothing staged or error).");
        }
    }
    // ======================================================================
    // 🔹 Maven Process Helpers
    // ======================================================================

    /** Starts a Maven process and filters its output to suppress noisy warnings. */
    private static Process startAndFilterOutput(ProcessBuilder pb, String moduleName) throws IOException {
        pb.redirectErrorStream(true);
        enforceJava8(pb);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("sun.misc.Unsafe") || line.contains("HiddenClassDefiner")) {
                    continue;
                }
                System.out.println("[" + moduleName + "] " + line);
            }
        }
        return process;
    }

    /** Installs the current parent POM locally. */
    private void installParentPom() throws IOException, InterruptedException {
        Path parentPom = BASE_PATH.resolve("pom.xml");
        System.out.println("\n🧩 Installing parent POM locally...");
        if (!Files.exists(parentPom)) {
            throw new IOException("Parent POM not found at " + parentPom);
        }

        List<String> cmd = Arrays.asList(
                "mvn", "-U", "install", "-N", "-DskipTests",
                "-Dmaven.repo.local=" + System.getProperty("user.home") + "/.m2/repository"
        );
        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(BASE_PATH.toFile());
        Process process = startAndFilterOutput(pb, "paint-parent");
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Failed to install paint-parent. Exit code: " + exit);
        }

        System.out.println("✅ Parent POM installed locally.");
    }

    /**
     * Installs the parent POM as a local release version (e.g., 0.0.31)
     * so that dependent modules can resolve it even when Maven runs offline.
     * Includes verification and clear logging.
     */
    private void installParentPomAsRelease(String releaseVersion) throws IOException, InterruptedException {
        Path parentPom = BASE_PATH.resolve("pom.xml");
        if (!Files.exists(parentPom)) {
            System.out.println("⚠️  Parent POM not found — skipping release install.");
            return;
        }

        System.out.println("\n🧩 Installing parent POM as release " + releaseVersion + "...");

        Path tmpPom = Files.createTempFile("parent-release-", ".xml");
        Files.copy(parentPom, tmpPom, StandardCopyOption.REPLACE_EXISTING);

        // Replace only the <version> tag in the temporary copy
        String content = new String(Files.readAllBytes(tmpPom), "UTF-8")
                .replaceAll("<version>.*?</version>", "<version>" + releaseVersion + "</version>");
        Files.write(tmpPom, content.getBytes("UTF-8"));

        List<String> cmd = Arrays.asList(
                "mvn", "install:install-file",
                "-Dfile=" + tmpPom.toAbsolutePath(),
                "-DgroupId=com.github.jjabakker",
                "-DartifactId=paint-parent",
                "-Dversion=" + releaseVersion,
                "-Dpackaging=pom"
        );

        System.out.println("🔧 Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(BASE_PATH.toFile());
        pb.inheritIO(); // show Maven output directly
        enforceJava8(pb);
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
    /**
     * Creates and pushes a Git tag for the specified release version.
     * Automatically commits any outstanding pom.xml version bumps,
     * including the root pom.xml, before tagging.
     */
    private void createAndPushTag(String version) throws IOException, InterruptedException {
        Path repoDir = BASE_PATH;
        if (!Files.exists(repoDir.resolve(".git"))) {
            System.out.println("⚠️  No Git repository found — skipping tagging.");
            return;
        }

        String tagName = "v" + version;

        // --- Check for uncommitted changes
        ProcessBuilder statusCheck = new ProcessBuilder("git", "status", "--porcelain");
        statusCheck.directory(repoDir.toFile());
        Process status = statusCheck.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(status.getInputStream()));
        boolean dirty = reader.lines().anyMatch(line -> !line.trim().isEmpty());
        status.waitFor();

        if (dirty) {
            System.out.println("⚠️  Working tree has uncommitted changes — auto-committing before tagging...");

            // ✅ Explicitly include root and submodule POMs
            String addCommand = "bash -c 'shopt -s globstar; git add pom.xml **/pom.xml'";
            ProcessBuilder addPb = new ProcessBuilder("bash", "-c", addCommand);
            addPb.directory(repoDir.toFile());
            addPb.inheritIO();
            Process addProc = addPb.start();
            addProc.waitFor();

            // Commit the version bump
            String message = "Auto-commit before tagging release v" + version;
            ProcessBuilder commitPb = new ProcessBuilder("git", "commit", "-m", message);
            commitPb.directory(repoDir.toFile());
            commitPb.inheritIO();
            Process commitProc = commitPb.start();
            commitProc.waitFor();

            // Verify it's now clean
            ProcessBuilder verifyClean = new ProcessBuilder("git", "status", "--porcelain");
            verifyClean.directory(repoDir.toFile());
            Process        verifyProc   = verifyClean.start();
            BufferedReader verifyReader = new BufferedReader(new InputStreamReader(verifyProc.getInputStream()));
            boolean        stillDirty   = verifyReader.lines().anyMatch(line -> !line.trim().isEmpty());
            verifyProc.waitFor();

            if (stillDirty) {
                System.out.println("⚠️  Working tree still has uncommitted files after auto-commit.");
                System.out.println("─────────────────────────────────────────────────────────────");
                System.out.println("Your build artifacts (.app and .exe) were created successfully!");
                System.out.println("However, Git tagging was skipped because additional files are dirty.\n");
                System.out.println("👉 Please review and commit manually:");
                System.out.println("   cd " + repoDir);
                System.out.println("   git status");
                System.out.println("   git add pom.xml **/pom.xml");
                System.out.println("   git commit -m \"Finalize v" + version + " release\"");
                System.out.println("   git tag -a v" + version + " -m \"Release v" + version + "\"");
                System.out.println("   git push origin main --tags");
                System.out.println("─────────────────────────────────────────────────────────────");
                return;
            }

            System.out.println("✅ Auto-committed all pom.xml files. Continuing with tagging...");
        }

        // --- Ensure tag doesn’t already exist
        ProcessBuilder checkTag = new ProcessBuilder("git", "tag", "--list", tagName);
        checkTag.directory(repoDir.toFile());
        Process check = checkTag.start();
        BufferedReader tagReader = new BufferedReader(new InputStreamReader(check.getInputStream()));
        boolean exists = tagReader.lines().anyMatch(line -> line.trim().equals(tagName));
        check.waitFor();
        if (exists) {
            System.out.println("⚠️  Tag " + tagName + " already exists — skipping tag creation.");
            System.out.println("✅ Continuing build without creating duplicate tag.");
            return;
        }

        // --- Create and push the tag
        List<String[]> commands = Arrays.asList(
                new String[]{"git", "tag", "-a", tagName, "-m", "Release " + tagName},
                new String[]{"git", "push", "origin", "main"},
                new String[]{"git", "push", "origin", tagName}
        );

        for (String[] cmd : commands) {
            System.out.println("🔧 Running: " + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(repoDir.toFile());
            pb.inheritIO();
            enforceJava8(pb);
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("❌ Git command failed: " + String.join(" ", cmd));
            }
        }



        System.out.println("✅ Tagged and pushed " + tagName + " successfully!");
    }

    private void zipPayload(Path appDir, Path pluginDir, Path outputZip) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("zip", "-qry", outputZip.toString(), "."));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(appDir.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Failed to zip payload at " + appDir);
        }

        if (Files.exists(pluginDir) && Files.list(pluginDir).findAny().isPresent()) {
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

    private void runMavenModule(String module, String version) throws Exception {
        List<String> cmd = Arrays.asList(
                "mvn", "-U", "clean", "package",
                "-pl", module, "-am",
                "-DskipTests",
                "-Dproject.version=" + version
        );
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(BASE_PATH.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Maven build failed for module: " + module);
        }
    }
    /** Ensures no POM files still contain "-SNAPSHOT" before release builds. */
    private void removeSnapshotFromAllPoms() throws IOException {
        try (java.util.stream.Stream<Path> files = Files.walk(BASE_PATH)) {
            files.filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .forEach(p -> {
                        try {
                            String text = new String(Files.readAllBytes(p), "UTF-8");
                            String cleaned = text.replaceAll("-SNAPSHOT", "");
                            if (!text.equals(cleaned)) {
                                Files.write(p, cleaned.getBytes("UTF-8"));
                                System.out.println("🧹 Cleaned -SNAPSHOT from " + p);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }

    }

    private void rollbackPomChanges() throws IOException, InterruptedException {
        System.out.println("🧹 Rolling back all POM version changes to committed state...");
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c", "git ls-files '**/pom.xml' | xargs git checkout --"
        );
        pb.directory(BASE_PATH.toFile());
        pb.inheritIO();
        Process proc = pb.start();
        int exit = proc.waitFor();
        if (exit == 0) {
            System.out.println("✅ Restored all pom.xml files to last committed version.");
        } else {
            System.err.println("⚠️  Rollback failed — please check Git status manually.");
        }
    }

}