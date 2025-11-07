// =================================================================================================
//  PURPOSE    : Orchestrate full multi-module Glyco-PAINT builds across macOS and Windows.
//               Handles version bumping, rebuilding, artifact collection, payload/installer creation,
//               Fiji plugin packaging, and optional Git tag push for a consistent release output.
//
//  DESCRIPTION:
//     ReleaseNewVersion is the single entry-point for building and releasing all Java modules in
//     the Glyco-PAINT toolchain. It:
//       • Computes release and next development versions from the parent POM
//       • Aligns all child modules to a single version
//       • Rebuilds shared-utils to refresh the local Maven repository
//       • Builds Windows (.exe) and macOS (.app) deliverables per module
//       • Packages the Fiji plugin shaded JAR
//       • Produces installer payload.zip per platform and builds installer JARs
//       • Bumps back to next -SNAPSHOT and commits
//       • Creates a local Git tag (and optionally pushes it)
//
//     Two modes are supported:
//       1) Full release (default): complete end-to-end flow described above
//       2) No-release (with --no-release): quick iteration without version/tag/installer steps
//
//  KEY FEATURES:
//     • Automatic version computation and bumping
//     • Multi-module Maven build orchestration
//     • Windows .exe and macOS .app packaging
//     • Fiji plugin shaded JAR packaging
//     • Installer payload + installer JAR generation
//     • Optional tag push via --push-tag
//     • Java 8 enforced for Maven execution to ensure compatibility
//
//  COMMAND-LINE FLAGS:
//     -bump <mode>     : Version increment pattern (0.0.x → patch | 0.x.0 → minor | x.0.0 → major)
//     --no-release     : Skip version bumping/installer builds/tag creation
//     --push-tag       : Push the created local release tag to origin
//
//  EXAMPLES:
//     java release.ReleaseNewVersion -bump 0.0.x
//     java release.ReleaseNewVersion -bump 0.x.0 --push-tag
//     java release.ReleaseNewVersion -bump x.0.0 --no-release
//
//  AUTHOR     : J.J. Bakker
//  MODULE     : paint-development-utils
//  UPDATED    : 2025-11-04
//  COPYRIGHT  : (c) 2025 J.J. Bakker. All rights reserved.
// =================================================================================================
package release;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;

/**
 * Orchestrates the complete Glyco-PAINT release process.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Reads version from the parent POM and computes release/next-dev versions</li>
 *   <li>Aligns module versions and reinstalls parents so children can resolve them</li>
 *   <li>Builds Windows/macOS artifacts and the Fiji plugin shaded JAR</li>
 *   <li>Creates installer payloads and installer JARs</li>
 *   <li>Bumps back to next development version and commits</li>
 *   <li>Creates a local Git tag and optionally pushes it</li>
 * </ul>
 * This class is Java 8 compatible by design.
 */
public class ReleaseNewVersion {

    // List of modules that produce standalone application artifacts.
    // (Installer and parent modules are handled separately.)
    private static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment",
            "paint-fiji-plugin"
    );

    // Resolve project root dynamically based on execution directory.
    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();

    // Base source tree containing all Glyco-PAINT Java modules.
    private static final Path BASE_PATH = PROJECT_ROOT.resolve("Glyco-PAINT-Java");

    // Target directory where final artifacts (.exe, .app, installers) are assembled.
    private static final Path BUILDS_PATH = PROJECT_ROOT.resolve("Glyco-PAINT-Builds");
    /**
     * Ensures Maven executions run under Java 8 by exporting JAVA_HOME and PATH for the spawned process.
     * Non-fatal if Java 8 is not available; Maven may then use the default JDK.
     * @param pb process builder to receive JAVA_HOME/PATH updates
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

    /**
     * CLI entry point. Parses flags, reports chosen mode, and invokes the release pipeline.
     * Recognized flags: -bump <mode>, --no-release, --push-tag.
     */
    public static void main(String[] args) {
        try {
            String  bumpFlag  = "0.0.x";
            boolean doRelease = true;
            boolean pushTag   = false;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-bump") && i + 1 < args.length) {
                    bumpFlag = args[i + 1];
                    i++;
                } else if (args[i].equalsIgnoreCase("--no-release")) {
                    doRelease = false;
                } else if (args[i].equalsIgnoreCase("--push-tag")) {
                    pushTag = true;
                }
            }

            if (pushTag) {
                System.out.println("🚀 Tags will be pushed to GitHub");
            }
            if (doRelease) {
                System.out.println("🚀 A release will be made");
            }

            new ReleaseNewVersion().run(bumpFlag, doRelease, pushTag);

        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Runs the full or partial release workflow based on flags.
     * @param bumpFlag version increment pattern (e.g., 0.0.x)
     * @param doRelease if false, skips release steps (versions/installer/tag)
     * @param pushTag if true, pushes the release tag after creation
     * @throws Exception on any failing sub-step
     */
    private void run(String bumpFlag, boolean doRelease, boolean pushTag) throws Exception {
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


            // --- Bump back to next development version (local only) ---
            System.out.println("\n🔄 Restoring development version (" + versionInfo.nextDevVersion + ")...");
            alignAllPomVersions(versionInfo.nextDevVersion);

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

            Path macFinal = installerPath.resolve("Glyco-PAINT-Installer-macOS-" + versionInfo.releaseVersion + ".jar");
            Path winFinal = installerPath.resolve("Glyco-PAINT-Installer-Windows" + versionInfo.releaseVersion + ".jar");
            Files.copy(macBuilt, macFinal, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(winBuilt, winFinal, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ macOS installer → " + macFinal);
            System.out.println("✅ Windows installer → " + winFinal);

            System.out.println("\n🎉 All builds complete for " + versionInfo.releaseVersion);
            System.out.println("✅ Output directory: " + buildRoot.toAbsolutePath());

        } finally {

            // --- Commit updated parent POM version for next development cycle (release builds only) ---
            if (doRelease) {
                System.out.println("\n🚀 Preparing next development iteration...");

                try {

                    // 2️⃣ Compute next snapshot version
                    String nextSnapshotVersion = versionInfo.nextDevVersion;
                    System.out.println("🔧 Updating parent POM to next development version: " + nextSnapshotVersion);

                    // 3️⃣ Apply version bump ONLY to the parent POM
                    runMaven(Arrays.asList(
                            "mvn", "-q",  "-B", "versions:set",
                            "-DnewVersion=" + nextSnapshotVersion,
                            "-DgenerateBackupPoms=false",
                            "-DprocessAllModules=false" // only parent
                    ), BASE_PATH, "versions:set (parent only)");

                    // ✅ Commit all updated POMs (parent + modules)
                    System.out.println("💾 Committing full project version bump to " + nextSnapshotVersion + "...");

                    // Add ALL pom.xml files
                    runCommand(Arrays.asList("bash", "-c", "shopt -s globstar; git add pom.xml **/pom.xml"), BASE_PATH);

                    // Commit
                    runCommand(Arrays.asList("git", "commit", "-m",
                                             "Bump project to " + nextSnapshotVersion + " for next development cycle"), BASE_PATH);

                    System.out.println("✅ Committed all POMs to next development version.");
                    System.out.println("\n✅ Release process complete!");
                    System.out.println("   🎯 Tagged release: " + versionInfo.releaseVersion);
                    System.out.println("   🚀 Next development version: " + nextSnapshotVersion);

                    String tagName = "v" + versionInfo.releaseVersion;

                    // ✅ Check if tag already exists locally
                    ProcessBuilder checkPb = new ProcessBuilder("git", "tag", "--list", tagName);
                    checkPb.directory(BASE_PATH.toFile());
                    Process checkProc = checkPb.start();
                    BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProc.getInputStream()));
                    boolean exists = checkReader.lines().anyMatch(line -> line.trim().equals(tagName));
                    checkProc.waitFor();

                    if (exists) {
                        System.out.println("⚠️  Tag " + tagName + " already exists locally. Skipping creation.");
                    } else {
                        // ✅ Create the tag locally at the very end
                        System.out.println("🏷️  Creating local tag " + tagName);
                        ProcessBuilder tagPb = new ProcessBuilder("git", "tag", "-a", tagName, "-m", "Release " + tagName);
                        tagPb.directory(BASE_PATH.toFile());
                        tagPb.inheritIO();
                        Process tagProc = tagPb.start();
                        if (tagProc.waitFor() != 0) {
                            throw new RuntimeException("❌ Failed to create local tag " + tagName);
                        }
                        System.out.println("✅ Created local tag " + tagName);
                    }

                    // ✅ Push tag if requested
                    if (pushTag) {
                        System.out.println("📤 Pushing tag " + tagName);
                        ProcessBuilder pushPb = new ProcessBuilder("git", "push", "origin", tagName);
                        pushPb.directory(BASE_PATH.toFile());
                        pushPb.inheritIO();
                        Process pushProc = pushPb.start();
                        if (pushProc.waitFor() != 0) {
                            throw new RuntimeException("❌ Failed to push tag " + tagName);
                        }
                        System.out.println("✅ Successfully pushed tag " + tagName);
                    } else {
                        System.out.println("ℹ️ Tag push skipped (no --push-tag flag).");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Error while preparing next development iteration: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Rebuilds and installs paint-shared-utils into the local Maven repository.
     * Keeps downstream module resolution consistent during version alignment.
     */
    private void rebuildSharedUtils() throws IOException, InterruptedException {
        Path utilsDir = BASE_PATH.resolve("paint-shared-utils");
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

    /** Holds the computed release and next development versions. */
    private static class VersionInfo {
        final String releaseVersion, nextDevVersion;
        VersionInfo(String release, String next) {
            this.releaseVersion = release;
            this.nextDevVersion = next;
        }
    }

    /**
     * Computes the release version (drops -SNAPSHOT) and the next development version (+1 with -SNAPSHOT).
     * The current implementation increments the last numeric segment regardless of -bump; the bump flag is
     * reserved for future expansion (major/minor/patch targeting).
     * @param currentVersion version read from the parent POM (expected to end with -SNAPSHOT)
     * @param bumpFlag hint for future bump strategy (currently informational)
     * @return a container with release and next-dev versions
     */
    private VersionInfo computeVersions(String currentVersion, String bumpFlag) {
        String base = currentVersion.replace("-SNAPSHOT", "").trim();

        // Extract last numeric segment
        String[] parts = base.split("\\.");
        int lastNum = Integer.parseInt(parts[parts.length - 1]);

        int next = lastNum + 1;
        // Future: honor -bump for major/minor/patch selection; currently increments last segment

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
     * Builds a module with the given Maven profile and copies matching artifacts to the destination directory.
     * Uses the global Maven repo (~/.m2/repository) to avoid per-module cache duplication.
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
     * Builds the macOS .app bundle for a module and copies the bundle directory tree to the destination.
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

    /**
     * Copies files from a directory that match a glob into the destination directory, replacing on conflict.
     */
    private void copyMatchingFiles(Path fromDir, String glob, Path destDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(fromDir, glob)) {
            for (Path file : stream) {
                Files.copy(file, destDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("✅ Copied " + file.getFileName() + " → " + destDir.getFileName());
            }
        }
    }

    /**
     * Recursively copies a directory tree from source to target, creating directories as needed.
     */
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

    /**
     * Aligns all project/module POM versions to {@code newVersion} using the Maven Versions Plugin and
     * reinstalls parent POMs so children can resolve them during the same run.
     */
    private void alignAllPomVersions(String newVersion) throws IOException, InterruptedException {
        System.out.println("🔄 Aligning all POM versions to " + newVersion + " using Maven Versions Plugin...");

        Path projectRoot = BASE_PATH.toAbsolutePath();

        // 1️⃣ Set the new version recursively across all modules (even nested)
        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "versions:set",
                "-DnewVersion=" + newVersion,
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:set");

        // 2️⃣ Install parent POMs locally so submodules can resolve them
        // Install top-level parent (paint-parent)
        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "-N", "install", "-DskipTests"
        ), BASE_PATH, "install paint-parent");

        // Install installer aggregator (paint-installer)
        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "-N", "install:install-file",
                "-Dfile=paint-installer/pom.xml",
                "-DgroupId=com.github.jjabakker",
                "-DartifactId=paint-installer",
                "-Dversion=" + newVersion,
                "-Dpackaging=pom"
        ), BASE_PATH, "install paint-installer");

        // 3️⃣ Update child module <parent> references to the new version
        runMaven(Arrays.asList(
                "mvn", "-q", "-B", "versions:update-child-modules",
                "-DforceVersion=true",
                "-DgenerateBackupPoms=false",
                "-DprocessAllModules=true"
        ), projectRoot, "versions:update-child-modules");

        System.out.println("✅ All modules (including nested) now aligned to version " + newVersion);
    }

    /**
     * Runs a Maven command in the specified directory and fails fast on non-zero exit.
     */
    private void runMaven(List<String> cmd, Path dir, String label) throws IOException, InterruptedException {
        System.out.println("🔧 Running (" + label + "): " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.inheritIO();
        if (pb.start().waitFor() != 0) {
            throw new RuntimeException("❌ Maven command failed: " + label);
        }
    }

    /**
     * Parses the first <version> element from the given pom.xml.
     * @return the version text or null if not found or on error
     */
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

    /**
     * Commits updated pom.xml files across the repository. No-op if not a Git repository
     * or if there are no staged changes after adding pom.xml files.
     */
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

    /**
     * Starts a process, merges stderr into stdout, and filters noisy warnings from output.
     * Used for Maven invocations to keep logs readable.
     */
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

    /**
     * Installs the current parent POM (-SNAPSHOT or otherwise) to the local Maven repository.
     */
    private void installParentPom() throws IOException, InterruptedException {
        Path parentPom = BASE_PATH.resolve("pom.xml");
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
        pb.directory(BASE_PATH.toFile());
        Process process = startAndFilterOutput(pb, "paint-parent");
        int exit = process.waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Failed to install paint-parent. Exit code: " + exit);
        }

        System.out.println("✅ Parent POM installed locally.");
    }

    /**
     * Installs a temporary copy of the parent POM under the given release version so children can resolve
     * it during the release alignment process.
     */
    private void installParentPomAsRelease(String releaseVersion) throws IOException, InterruptedException {
        Path parentPom = BASE_PATH.resolve("pom.xml");
        if (!Files.exists(parentPom)) {
            System.out.println("⚠️  Parent POM not found — skipping release install.");
            return;
        }

        System.out.println("\n🧩 Installing parent POM as release " + releaseVersion + "...");
        // Create a throwaway POM with only <version> rewritten to install under the release coordinate
        Path tmpPom = Files.createTempFile("parent-release-", ".xml");
        Files.copy(parentPom, tmpPom, StandardCopyOption.REPLACE_EXISTING);

        // Replace only the <version> tag in the temporary copy
        String content = new String(Files.readAllBytes(tmpPom), "UTF-8")
                .replaceAll("<version>.*?</version>", "<version>" + releaseVersion + "</version>");
        Files.write(tmpPom, content.getBytes("UTF-8"));

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
     * Creates/updates the installer payload.zip by zipping the app directory and optionally appending the
     * plugin subtree under /plugin within the zip archive.
     */
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

    /**
     * Builds the specified module (and its dependencies) with the provided project version.
     */
    private void runMavenModule(String module, String version) throws Exception {
        List<String> cmd = Arrays.asList(
                "mvn", "-q", "-U", "clean", "package",
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


    /**
     * Rewrites pom.xml files to strip "-SNAPSHOT" occurrences before release builds.
     */
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

    /**
     * Runs a generic shell command in the given directory, failing fast on non-zero exit.
     */
    private void runCommand(List<String> cmd, Path dir) throws IOException, InterruptedException {
        System.out.println("🔧 Running: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.inheritIO();
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Command failed: " + String.join(" ", cmd));
        }
    }

}