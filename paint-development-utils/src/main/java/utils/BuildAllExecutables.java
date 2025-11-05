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

package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    private static final Path BASE_PATH = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");
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

            new BuildAllExecutables().run(bumpFlag, doRelease);

        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run(String bumpFlag, boolean doRelease) throws Exception {
        System.out.println("=== Building Glyco-PAINT apps for macOS and Windows ===");

        Path parentPom = BASE_PATH.resolve("pom.xml");
        String currentVersion = getVersionFromPom(parentPom);
        if (currentVersion == null)
            throw new IllegalStateException("Could not determine version from parent pom.xml");

        if (!currentVersion.endsWith("-SNAPSHOT")) {
            System.out.println("⚠️  Parent POM is not a SNAPSHOT (" + currentVersion + ") — converting to snapshot for continued development.");
            currentVersion = currentVersion + "-SNAPSHOT";
        }

        VersionInfo versionInfo = computeVersions(currentVersion, bumpFlag);
        System.out.println("🔢  Current:  " + currentVersion);
        System.out.println("🏷️  Release: " + versionInfo.releaseVersion);
        System.out.println("🚀 Next dev: " + versionInfo.nextDevVersion);

        // --- Prepare environment ---
        installParentPom();
        rebuildSharedUtils();

        // --- Release preparation ---
        if (doRelease) {
            System.out.println("\n🎯 Preparing release " + versionInfo.releaseVersion);
            bumpAllPomVersions(currentVersion, versionInfo.releaseVersion);
            commitVersionBump(currentVersion, versionInfo.releaseVersion);
            installParentPomAsRelease(versionInfo.releaseVersion);
            installParentPom();
            rebuildSharedUtils();
        } else {
            installParentPomAsRelease(versionInfo.releaseVersion);
        }

        // --- Prepare output directories ---
        Path buildRoot   = BUILDS_PATH.resolve("Glyco-PAINT-" + versionInfo.releaseVersion);
        Path windowsPath = buildRoot.resolve("Windows");
        Path macOSPath   = buildRoot.resolve("macOS");
        Path pluginPath  = buildRoot.resolve("Plugins");
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

            // --- Install built module into local repo for plugin dependencies ---
            System.out.println("📦 Installing " + module + " into local Maven repo...");
            List<String> installCmd = Arrays.asList(
                    "mvn", "-q", "install", "-DskipTests",
                    "-Dmaven.repo.local=" + System.getProperty("user.home") + "/.m2/repository"
            );
            ProcessBuilder installPb = new ProcessBuilder(installCmd);
            installPb.directory(moduleDir.toFile());
            Process installProc = startAndFilterOutput(installPb, module);
            if (installProc.waitFor() != 0)
                throw new RuntimeException("❌ Failed to install " + module + " into local repo.");
            System.out.println("✅ Installed " + module + " locally.");
        }

        // --- Build the Fiji plugin ---
        Path pluginDir = BASE_PATH.resolve("paint-fiji-plugin");
        if (Files.exists(pluginDir.resolve("pom.xml"))) {
            System.out.println("\n---------------------------------------------");
            System.out.println("🔬 Module: paint-fiji-plugin");
            System.out.println("---------------------------------------------");

            buildAndCollect(pluginDir, "", "*.jar", pluginPath);
        } else {
            System.out.println("⚠️  paint-fiji-plugin not found — skipping plugin build.");
        }

        // --- Tag release if needed ---
        if (doRelease) {
            createAndPushTag(versionInfo.releaseVersion);
        }

        // --- Bump back to next development version ---
        System.out.println("\n🔄 Restoring development version (" + versionInfo.nextDevVersion + ")...");
        bumpAllPomVersions(versionInfo.releaseVersion, versionInfo.nextDevVersion);

        // --- Explicit final commit for new SNAPSHOT version ---
        System.out.println("📝 Committing final version bump (" + versionInfo.nextDevVersion + ")...");
        ProcessBuilder addFinal = new ProcessBuilder("bash", "-c", "git add pom.xml **/pom.xml");
        addFinal.directory(BASE_PATH.toFile());
        addFinal.inheritIO();
        addFinal.start().waitFor();

        // Check if anything was staged
        ProcessBuilder diffFinal = new ProcessBuilder("git", "diff", "--cached", "--quiet");
        diffFinal.directory(BASE_PATH.toFile());
        Process diffProc = diffFinal.start();
        int diffExit = diffProc.waitFor();

        if (diffExit != 0) {
            String msg = "Bump version: " + versionInfo.releaseVersion + " → " + versionInfo.nextDevVersion;
            ProcessBuilder commitFinal = new ProcessBuilder("git", "commit", "-m", msg);
            commitFinal.directory(BASE_PATH.toFile());
            commitFinal.inheritIO();
            if (commitFinal.start().waitFor() == 0)
                System.out.println("✅ Committed final POM version bump to " + versionInfo.nextDevVersion);
            else
                System.err.println("⚠️  Could not commit final version bump automatically.");
        } else {
            System.out.println("ℹ️  No remaining POM changes to commit.");
        }

        // --- Push new SNAPSHOT commit ---
        System.out.println("\n📦 Pushing new development version (" + versionInfo.nextDevVersion + ") to main...");
        ProcessBuilder pushMain = new ProcessBuilder("git", "push", "origin", "main");
        pushMain.directory(BASE_PATH.toFile());
        pushMain.inheritIO();
        Process pushProc = pushMain.start();
        pushProc.waitFor();
        if (pushProc.exitValue() == 0) {
            System.out.println("✅ Pushed new development version to main.");
        } else {
            System.err.println("⚠️  Failed to push new development version — please push manually.");
        }

        // --- Ensure updated parent is reinstalled ---
        installParentPom();
        rebuildSharedUtils();


        // ======================================================================
        // 🔹 Build Installer Payloads (macOS + Windows)
        // ======================================================================

        // --- macOS payload ---
        System.out.println("\n---------------------------------------------");
        System.out.println("📦 Building macOS Installer Payload");
        System.out.println("---------------------------------------------");

        Path macInstallerResources = BASE_PATH.resolve("paint-installer-mac/src/main/resources");
        Files.createDirectories(macInstallerResources);
        Path macPayloadZip = macInstallerResources.resolve("payload.zip");
        Files.deleteIfExists(macPayloadZip);

        // Create mac payload.zip from .app bundles + plugin folder
        zipPayload(macOSPath, pluginPath, macPayloadZip);
        System.out.println("✅ macOS payload ready: " + macPayloadZip);

        // --- Windows payload ---
        System.out.println("\n---------------------------------------------");
        System.out.println("📦 Building Windows Installer Payload");
        System.out.println("---------------------------------------------");

        Path winInstallerResources = BASE_PATH.resolve("paint-installer-windows/src/main/resources");
        Files.createDirectories(winInstallerResources);
        Path winPayloadZip = winInstallerResources.resolve("payload.zip");
        Files.deleteIfExists(winPayloadZip);

        // Create Windows payload.zip from Windows builds + plugin folder
        zipPayload(windowsPath, pluginPath, winPayloadZip);
        System.out.println("✅ Windows payload ready: " + winPayloadZip);

        // ======================================================================
        // 🔹 Build Installer JAR and EXE
        // ======================================================================

        // macOS JAR installer
        System.out.println("\n---------------------------------------------");
        System.out.println("🛠️  Building Glyco-PAINT macOS Installer JAR");
        System.out.println("---------------------------------------------");
        runMavenModule("paint-installer-macos", versionInfo.releaseVersion);

        // Windows EXE installer
        System.out.println("\n---------------------------------------------");
        System.out.println("🛠️  Building Glyco-PAINT Windows Installer EXE");
        System.out.println("---------------------------------------------");
        runMavenModule("paint-installer-windows", versionInfo.releaseVersion);

        System.out.println("\n✅ Both installers built successfully for version " + versionInfo.releaseVersion);        System.out.println("✅ Installer built with version " + versionInfo.releaseVersion);

        System.out.println("\n🎉 All builds complete!");
        System.out.println("✅ Output directory: " + buildRoot.toAbsolutePath());
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
        String base  = currentVersion.replace("-SNAPSHOT", "");
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

        if (exit != 0)
            throw new RuntimeException("❌ macOS build failed for " + moduleDir.getFileName());

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
                }
                else {
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
     * Forces every pom.xml in the tree to have the same version as the parent.
     * The parent version is the single source of truth.
     */
    private void bumpAllPomVersions(String oldVersion, String newVersion) throws Exception {
        System.out.println("🔄 Enforcing unified version across all modules: " + newVersion);

        Path parentPom = BASE_PATH.resolve("pom.xml");
        updatePomVersionFull(parentPom, oldVersion, newVersion, true);

        Files.walk(BASE_PATH)
                .filter(p -> p.getFileName().toString().equals("pom.xml"))
                .filter(p -> !p.equals(parentPom))
                .forEach(p -> {
                    try {
                        updatePomVersionFull(p, oldVersion, newVersion, false);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed updating " + p + ": " + e.getMessage(), e);
                    }
                });

        System.out.println("✅ All modules now use version " + newVersion);
    }


    /**
     * Updates or inserts version tags so that both <project><version> and <parent><version>
     * match the parent version. Also updates inter-module dependencies.
     */
    private void updatePomVersionFull(Path pom, String oldVersion, String newVersion, boolean isParent) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(Files.newInputStream(pom));

        Element project = (Element) doc.getElementsByTagName("project").item(0);
        boolean modified = false;

        // --- 1️⃣ Set or enforce <project><version>
        NodeList versionNodes = project.getElementsByTagName("version");
        Node projectVersion = null;
        for (int i = 0; i < versionNodes.getLength(); i++) {
            Node v = versionNodes.item(i);
            if (v.getParentNode().equals(project)) {
                projectVersion = v;
                break;
            }
        }

        if (projectVersion != null) {
            if (!projectVersion.getTextContent().trim().equals(newVersion)) {
                projectVersion.setTextContent(newVersion);
                modified = true;
            }
        } else {
            // Insert <version> directly after <artifactId> if possible, otherwise append at end
            NodeList artifactNodes = project.getElementsByTagName("artifactId");
            Element v = doc.createElement("version");
            v.setTextContent(newVersion);

            if (artifactNodes.getLength() > 0) {
                Node artifact = artifactNodes.item(0);
                Node next = artifact.getNextSibling();

                // Skip over whitespace text nodes if present
                while (next != null && next.getNodeType() == Node.TEXT_NODE) {
                    next = next.getNextSibling();
                }

                if (next != null && next.getParentNode() == project) {
                    project.insertBefore(v, next);
                } else {
                    project.appendChild(v);
                }
            } else {
                project.appendChild(v);
            }
            modified = true;
        }

        // --- 2️⃣ Update <parent><version>
        NodeList parentNodes = project.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parent = (Element) parentNodes.item(0);
            NodeList parentVersionList = parent.getElementsByTagName("version");
            if (parentVersionList.getLength() > 0) {
                Node v = parentVersionList.item(0);
                if (!v.getTextContent().trim().equals(newVersion)) {
                    v.setTextContent(newVersion);
                    modified = true;
                }
            }
        }

        // --- 3️⃣ Update all inter-module dependencies with same groupId
        NodeList deps = project.getElementsByTagName("dependency");
        for (int i = 0; i < deps.getLength(); i++) {
            Element dep = (Element) deps.item(i);
            NodeList gidList = dep.getElementsByTagName("groupId");
            if (gidList.getLength() > 0) {
                String gid = gidList.item(0).getTextContent().trim();
                if (gid.equals("com.github.jjabakker")) {
                    NodeList vList = dep.getElementsByTagName("version");
                    if (vList.getLength() > 0) {
                        Node v = vList.item(0);
                        if (!v.getTextContent().trim().equals(newVersion)) {
                            v.setTextContent(newVersion);
                            modified = true;
                        }
                    } else {
                        Element v = doc.createElement("version");
                        v.setTextContent(newVersion);
                        dep.appendChild(v);
                        modified = true;
                    }
                }
            }
        }

        if (modified) {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();

            // Keep output exactly as-is: no pretty-print, no newlines
            t.setOutputProperty(OutputKeys.INDENT, "no");
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            t.setOutputProperty(OutputKeys.METHOD, "xml");
            t.setOutputProperty(OutputKeys.STANDALONE, "no");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");

            try (OutputStream out = Files.newOutputStream(pom)) {
                t.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
            }

            System.out.println("📝 Enforced version " + newVersion + " in " + pom.getFileName());
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
        String addCommand = "git add **/pom.xml";
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

        if (commitExit == 0)
            System.out.println("✅ Committed pom.xml version bump: " + message);
        else
            System.err.println("⚠️  git commit failed (nothing staged or error).");
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
            Process verifyProc = verifyClean.start();
            BufferedReader verifyReader = new BufferedReader(new InputStreamReader(verifyProc.getInputStream()));
            boolean stillDirty = verifyReader.lines().anyMatch(line -> !line.trim().isEmpty());
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
            throw new IllegalStateException("❌ Tag " + tagName + " already exists!");
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
        if (pb.start().waitFor() != 0)
            throw new RuntimeException("❌ Failed to zip payload at " + appDir);

        if (Files.exists(pluginDir) && Files.list(pluginDir).findAny().isPresent()) {
            String cmdStr = String.format(
                    "cd \"%s\" && mkdir -p ../_plugin_tmp && cp -R . ../_plugin_tmp/plugin && " +
                            "cd ../_plugin_tmp && zip -qry \"%s\" plugin && cd .. && rm -rf _plugin_tmp",
                    pluginDir.toAbsolutePath(), outputZip.toAbsolutePath()
            );
            ProcessBuilder addPb = new ProcessBuilder("bash", "-c", cmdStr);
            addPb.inheritIO();
            if (addPb.start().waitFor() != 0)
                throw new RuntimeException("❌ Failed to append plugin to " + outputZip);
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
        if (pb.start().waitFor() != 0)
            throw new RuntimeException("❌ Maven build failed for module: " + module);
    }
}