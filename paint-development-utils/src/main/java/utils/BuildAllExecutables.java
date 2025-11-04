/* =================================================================================================
 *  PURPOSE    : Automate full version bumping and multi-platform builds for all Glyco-PAINT modules.
 *
 *  DESCRIPTION:
 *     This utility coordinates the end-to-end build pipeline for all Glyco-PAINT Java modules.
 *     It performs version management, dependency installation, Git commits, and packaging
 *     for both Windows and macOS executables in one run.
 *
 *  EXECUTION FLOW SUMMARY:
 *     1. Read current parent POM version (must end with -SNAPSHOT).
 *     2. Compute release and next development versions based on the bump flag.
 *     3. Build and install current SNAPSHOT versions of shared and parent modules.
 *     4. Install the parent POM also as a local release (for dependency resolution).
 *     5. Bump all POM versions across modules and commit to Git.
 *     6. Reinstall the bumped parent POM and rebuild shared-utils.
 *     7. Build each module for both platforms, fail-fast on errors.
 *     8. Copy built executables or app bundles into the organized build directory.
 *
 *  KEY FEATURES:
 *     • Full automated version bumping and tagging
 *     • Local parent POM release injection for offline builds
 *     • Fail-fast build execution with clear per-module reporting
 *     • Distinct build directories for macOS (.app bundles) and Windows (.exe)
 *     • Git commit integration for version control synchronization
 *
 *  COMMAND-LINE FLAGS:
 *     -bump <mode>     : Defines how to increment the version number.
 *                        Supported values:
 *                           0.0.x → increment patch (e.g., 0.0.26 → 0.0.27)
 *                           0.x.0 → increment minor (e.g., 0.2.9 → 0.3.0)
 *                           x.0.0 → increment major (e.g., 1.9.5 → 2.0.0)
 *
 *     --release        : Performs a full release sequence:
 *                           • Converts SNAPSHOT to release version
 *                           • Builds all modules for both platforms
 *                           • Creates and pushes Git tag (vX.Y.Z)
 *                           • Bumps back to next SNAPSHOT version
 *
 *     Example usage:
 *         java utils.BuildAllExecutables -bump 0.0.x --release
 *         java utils.BuildAllExecutables -bump 0.x.0
 *
 *  AUTHOR     : J.J. Bakker
 *  MODULE     : paint-development-utils
 *  UPDATED    : 2025-11-04
 *  COPYRIGHT  : (c) 2025 J.J. Bakker. All rights reserved.
 * =============================================================================================== */

package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 * Central build orchestrator for the entire Glyco-PAINT project.
 * <p>
 * Handles version bumping, Maven rebuilds, Git commits, and multi-platform packaging.
 * The build fails immediately upon any critical Maven error.
 */
public class BuildAllExecutables {

    /** List of modules to build for both platforms. */
    private static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment"
    );

    /** Base repository path containing all source modules. */
    private static final Path BASE_PATH = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");

    /** Target directory for completed builds. */
    private static final Path BUILDS_PATH = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Builds");

    /**
     * Main entry point.
     * Accepts an optional bump flag: "-bump 0.0.x" | "-bump 0.x.0" | "-bump x.0.0".
     */
    public static void main(String[] args) {
        try {
            String bumpFlag = "0.0.x"; // Default bump type (patch)
            boolean doRelease = false; // Default: no release

            // Parse command-line flags
            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-bump") && i + 1 < args.length) {
                    bumpFlag = args[i + 1];
                    i++; // skip version argument
                } else if (args[i].equalsIgnoreCase("--release")) {
                    doRelease = true;
                }
            }

            // Pass both arguments
            new BuildAllExecutables().run(bumpFlag, doRelease);

        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Main orchestration logic.
     * Coordinates version management, builds, and packaging for all modules.
     */
    private void run(String bumpFlag, boolean doRelease) throws Exception {
        System.out.println("=== Building Glyco-PAINT apps for macOS and Windows ===");

        Path parentPom = BASE_PATH.resolve("pom.xml");
        String currentVersion = getVersionFromPom(parentPom);
        if (currentVersion == null || !currentVersion.endsWith("-SNAPSHOT")) {
            throw new IllegalStateException("Expected SNAPSHOT version in parent pom.xml, found: " + currentVersion);
        }

        VersionInfo versionInfo = computeVersions(currentVersion, bumpFlag);
        System.out.println("🔢  Current:  " + currentVersion);
        System.out.println("🏷️  Release: " + versionInfo.releaseVersion);
        System.out.println("🚀 Next dev: " + versionInfo.nextDevVersion);

        // 1️⃣ Build old snapshot to ensure all dependencies are installed
        rebuildSharedUtils();
        installParentPom();

        // 2️⃣ If doing a release, switch all modules to release version first
        if (doRelease) {
            System.out.println("\n🎯 Preparing release " + versionInfo.releaseVersion);
            bumpAllPomVersions(currentVersion, versionInfo.releaseVersion);
            commitVersionBump(currentVersion, versionInfo.releaseVersion);
            installParentPom();
            rebuildSharedUtils();
        } else {
            installParentPomAsRelease(versionInfo.releaseVersion);
        }

        // 3️⃣ Prepare build directories
        Path buildRoot   = BUILDS_PATH.resolve("Glyco-PAINT-" + versionInfo.releaseVersion);
        Path windowsPath = buildRoot.resolve("Windows");
        Path macOSPath   = buildRoot.resolve("macOS");
        Files.createDirectories(windowsPath);
        Files.createDirectories(macOSPath);
        System.out.println("📦 Output base: " + buildRoot);

        // 4️⃣ Build all modules for both platforms (fail-fast)
        for (String module : MODULES) {
            Path moduleDir = BASE_PATH.resolve(module);
            System.out.println("\n---------------------------------------------");
            System.out.println("🏗️  Module: " + module);
            System.out.println("---------------------------------------------");

            buildAndCollect(moduleDir, "-Pwindows-exe", "*.exe", windowsPath);
            buildAndCollectMacApp(moduleDir, "-Pmacos-appbundle", macOSPath);
        }

        // 5️⃣ Tag and push if release requested
        if (doRelease) {
            createAndPushTag(versionInfo.releaseVersion);
        }

        // 6️⃣ Bump to next SNAPSHOT for continued development
        bumpAllPomVersions(versionInfo.releaseVersion, versionInfo.nextDevVersion);
        commitVersionBump(versionInfo.releaseVersion, versionInfo.nextDevVersion);
        installParentPom();
        rebuildSharedUtils();

        System.out.println("\n🎉 All builds complete!");
        System.out.println("✅ Output directory: " + buildRoot.toAbsolutePath());
    }
    // ======================================================================
    // 🔹 Shared Utility Builders
    // ======================================================================

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
    // 🔹 Version Computation and Data
    // ======================================================================

    /** Helper container for version state. */
    private static class VersionInfo {
        final String releaseVersion, nextDevVersion;
        VersionInfo(String release, String next) {
            this.releaseVersion = release;
            this.nextDevVersion = next;
        }
    }

    /**
     * Compute new release and next development versions.
     *
     * @param currentVersion current project version (e.g., 0.0.23-SNAPSHOT)
     * @param bumpFlag one of "0.0.x", "0.x.0", "x.0.0"
     */
    private VersionInfo computeVersions(String currentVersion, String bumpFlag) {
        String   base  = currentVersion.replace("-SNAPSHOT", "");
        String[] parts = base.split("\\.");
        int      major = Integer.parseInt(parts[0]);
        int      minor = Integer.parseInt(parts[1]);
        int      patch = Integer.parseInt(parts[2]);

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
     * Builds a module using a specified Maven profile, copying matching artifacts into the output directory.
     */
    private void buildAndCollect(Path moduleDir, String profile, String glob, Path destDir)
            throws IOException, InterruptedException {

        String localRepo = System.getProperty("user.home") + "/.m2/repository";
        List<String> offlineCmd = Arrays.asList(
                "mvn", "-o", "-U", "-q", "clean", "package",
                profile,
                "-Dmaven.repo.local=" + localRepo,
                "-Dmaven.artifact.threads=1"
        );

        System.out.println("🔧 Running: " + String.join(" ", offlineCmd) + " (in " + moduleDir.getFileName() + ")");
        ProcessBuilder pb = new ProcessBuilder(offlineCmd);
        pb.directory(moduleDir.toFile());
        Process process = startAndFilterOutput(pb, moduleDir.getFileName().toString());
        int exit = process.waitFor();

        // Retry online if offline build fails (common when parent SNAPSHOT not yet cached)
        if (exit != 0) {
            System.out.println("⚠️  Offline build failed, retrying online...");
            List<String> onlineCmd = new ArrayList<>(offlineCmd);
            onlineCmd.remove("-o");
            ProcessBuilder retry = new ProcessBuilder(onlineCmd);
            retry.directory(moduleDir.toFile());
            process = startAndFilterOutput(retry, moduleDir.getFileName().toString());
            exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("❌ Build failed for " + moduleDir.getFileName() + " (" + profile + ")");
            }
        }

        copyMatchingFiles(moduleDir.resolve("target"), glob, destDir);
    }

    /**
     * Builds the macOS `.app` bundle version of a module and copies results to destination.
     */
    private void buildAndCollectMacApp(Path moduleDir, String profile, Path destDir)
            throws IOException, InterruptedException {

        String localRepo = System.getProperty("user.home") + "/.m2/repository";
        List<String> offlineCmd = Arrays.asList(
                "mvn", "-o", "-U", "-q", "clean", "package",
                profile,
                "-Dmaven.repo.local=" + localRepo,
                "-Dmaven.artifact.threads=1"
        );
        System.out.println("🔧 Running: " + String.join(" ", offlineCmd) + " (in " + moduleDir.getFileName() + ")");

        ProcessBuilder pb = new ProcessBuilder(offlineCmd);
        pb.directory(moduleDir.toFile());
        Process process = startAndFilterOutput(pb, moduleDir.getFileName().toString());
        int     exit    = process.waitFor();

        if (exit != 0) {
            System.out.println("⚠️  Offline macOS build failed, retrying online...");
            List<String> onlineCmd = new ArrayList<>(offlineCmd);
            onlineCmd.remove("-o");
            ProcessBuilder retry = new ProcessBuilder(onlineCmd);
            retry.directory(moduleDir.toFile());
            process = startAndFilterOutput(retry, moduleDir.getFileName().toString());
            exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("❌ macOS build failed for " + moduleDir.getFileName());
            }
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(moduleDir.resolve("target"), "*.app")) {
            for (Path appBundle : stream) {
                Path dest = destDir.resolve(appBundle.getFileName());
                System.out.println("📦 Copying " + appBundle.getFileName() + " → " + destDir);
                copyDirectory(appBundle, dest);
                System.out.println("✅ Copied .app bundle");
            }
        }
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
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            t.transform(new DOMSource(doc), new StreamResult(Files.newOutputStream(pom)));
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
     * Installs the parent POM as a local release version (e.g., 0.0.24)
     * so that dependent modules can resolve it even when Maven runs offline.
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
        Process process = startAndFilterOutput(pb, "paint-parent-release");
        int exit = process.waitFor();
        Files.deleteIfExists(tmpPom);

        if (exit != 0)
            throw new RuntimeException("❌ Failed to install parent POM release version " + releaseVersion);

        System.out.println("✅ Installed paint-parent " + releaseVersion + " locally.");
    }

    /**
     * Creates and pushes a Git tag for the specified release version.
     * This automatically triggers the GitHub Actions workflow (which listens for "v*.*.*" tags).
     *
     * The tag is created as "vX.Y.Z" (e.g., "v0.0.20") and annotated with a standard message.
     * The working directory must be clean before tagging to ensure reproducible builds.
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
        Process        status = statusCheck.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(status.getInputStream()));
        boolean        dirty  = reader.lines().anyMatch(line -> !line.trim().isEmpty());
        status.waitFor();
        if (dirty) {
            throw new IllegalStateException("❌ Working tree not clean — please commit or stash changes before tagging.");
        }

        // --- Ensure tag doesn’t already exist
        ProcessBuilder checkTag = new ProcessBuilder("git", "tag", "--list", tagName);
        checkTag.directory(repoDir.toFile());
        Process        check     = checkTag.start();
        BufferedReader tagReader = new BufferedReader(new InputStreamReader(check.getInputStream()));
        boolean        exists    = tagReader.lines().anyMatch(line -> line.trim().equals(tagName));
        check.waitFor();
        if (exists) {
            throw new IllegalStateException("❌ Tag " + tagName + " already exists!");
        }

        // --- Create and push the tag
        List<String[]> commands = Arrays.asList(
                new String[]{"git", "tag", "-a", tagName, "-m", "Release " + tagName},
                new String[]{"git", "push", "origin", tagName}
        );

        for (String[] cmd : commands) {
            System.out.println("🔧 Running: " + String.join(" ", cmd));
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(repoDir.toFile());
            pb.inheritIO();
            Process process = pb.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("❌ Git command failed: " + String.join(" ", cmd));
            }
        }

        System.out.println("✅ Tagged and pushed " + tagName + " successfully!");
    }
}