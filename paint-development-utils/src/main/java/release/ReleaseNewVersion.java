// =================================================================================================
//  PURPOSE    : Orchestrate full multi-module Glyco-PAINT builds across macOS and Windows.
//               (Behavior-preserving refactor: delegates to helper classes without changing logic.)
//  AUTHOR     : J.J. Bakker
//  MODULE     : paint-development-utils
//  UPDATED    : 2025-11-10
//  COPYRIGHT  : (c) 2025 J.J. Bakker. All rights reserved.
// =================================================================================================
package release;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class ReleaseNewVersion {

    public static void main(String[] args) {
        boolean bumpVersion = false;  // default: rebuild only
        boolean doRelease   = false;  // release implies bump + tag + push

        if (args.length == 0) {
            System.out.println("ℹ️  No command-line options specified.");

            // Determine current version
            final Path parentPom = PathsConfig.BASE_PATH.resolve("pom.xml");
            String currentVersion = PomUtils.getVersionFromPom(parentPom);
            if (currentVersion == null) {
                System.err.println("❌ Cannot determine current version from parent POM.");
                System.exit(1);
            }

            // Compute what bumping would do
            VersionInfo info = PomUtils.computeVersions(currentVersion, null);

            System.out.println();
            System.out.println("Current version: " + currentVersion);
            System.out.println();
            System.out.println("Choose an action:");
            System.out.println("  1) Rebuild only");
            System.out.println("     • Version stays         : " + currentVersion);
            System.out.println("     • No release/tag created");
            System.out.println();
            System.out.println("  2) Bump version only");
            System.out.println("     • Release version would : " + info.releaseVersion);
            System.out.println("     • Next dev version      : " + info.nextDevVersion);
            System.out.println("     • No release/tag created");
            System.out.println();
            System.out.println("  3) Full release (bump + tag + push)");
            System.out.println("     • Release version       : " + info.releaseVersion);
            System.out.println("     • Next dev version      : " + info.nextDevVersion);
            System.out.println();
            System.out.print("Enter 1, 2 or 3: ");

            try {
                byte[] buf = new byte[32];
                int read = System.in.read(buf);
                String choice = new String(buf, 0, read).trim();

                switch (choice) {
                    case "1":
                        bumpVersion = false;
                        doRelease   = false;
                        break;

                    case "2":
                        bumpVersion = true;
                        doRelease   = false;
                        break;

                    case "3":
                        bumpVersion = true;
                        doRelease   = true;
                        break;

                    default:
                        System.out.println("Invalid option. Defaulting to: rebuild only.");
                        bumpVersion = false;
                        doRelease   = false;
                }

            } catch (Exception e) {
                System.out.println("Input error. Defaulting to: rebuild only.");
                bumpVersion = false;
                doRelease   = false;
            }

            System.out.println();
        }

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            if ("--bump-version".equalsIgnoreCase(arg)) {
                bumpVersion = true;

            } else if ("--release".equalsIgnoreCase(arg)) {
                doRelease = true;

            } else {
                System.err.println("❌ Unknown option: " + arg);
                System.err.println();
                System.err.println("Usage:");
                System.err.println("  java -jar release-tool.jar [--bump-version] [--release]");
                System.err.println();
                System.err.println("Options:");
                System.err.println("  --bump-version   Bump to next development SNAPSHOT (no tag).");
                System.err.println("  --release        Full release: drop -SNAPSHOT, build, tag & push, then bump back to next SNAPSHOT.");
                System.err.println();
                System.err.println("Examples:");
                System.err.println("  (rebuild only)          :  java -jar release-tool.jar");
                System.err.println("  (bump to next snapshot) :  java -jar release-tool.jar --bump-version");
                System.err.println("  (full release)          :  java -jar release-tool.jar --release");
                System.exit(1);
            }
        }

        // Validate combination: --release implies --bump-version
        if (doRelease && !bumpVersion) {
            bumpVersion = true; // make it implicit so users can just pass --release
        }

        System.out.println("✅ Effective configuration:");
        System.out.println("   bump-version : " + (bumpVersion ? "yes" : "no"));
        System.out.println("   release      : " + (doRelease ? "yes" : "no"));
        System.out.println();

        try {
            new ReleaseNewVersion().run(bumpVersion, doRelease);
        } catch (Exception e) {
            System.err.println("❌ Build process failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean isValidBump(String s) {
        return s.matches("^(major|minor|patch|[0-9]+\\.[0-9]+\\.[0-9]+)$");
    }

    private static void abort(String message) {
        System.err.println("❌ " + message);
        System.err.println();
        System.err.println("Usage:");
        System.err.println("  java -jar release-tool.jar [options]");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  -bump <major|minor|patch|X.Y.Z>   Bump version or set explicitly");
        System.err.println("  --no-release                      Do not perform a release");
        System.err.println("  --push-tag                        Push tags after release");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  -bump patch");
        System.err.println("  -bump 1.4.0");
        System.err.println("  -bump minor --no-release");
        System.err.println();
        System.exit(1);
    }

    private void run(boolean bumpVersion, boolean doRelease) throws Exception {
        System.out.println("=== Building Glyco-PAINT apps for macOS and Windows ===");

        final Path parentPom = PathsConfig.BASE_PATH.resolve("pom.xml");
        String currentVersion = PomUtils.getVersionFromPom(parentPom);
        if (currentVersion == null) {
            throw new IllegalStateException("Could not determine version from parent pom.xml");
        }
        if (!currentVersion.endsWith("-SNAPSHOT")) {
            System.out.println("⚠️  Parent POM is not a SNAPSHOT (" + currentVersion + ") — converting to snapshot for continued development.");
            currentVersion = currentVersion + "-SNAPSHOT";
        }

        VersionInfo versionInfo = null;   // created only if bump or release requested

        // -------------------------------------------------------------
        // MODE 1 : Rebuild only
        // -------------------------------------------------------------
        if (!bumpVersion && !doRelease) {

            System.out.println("ℹ️  Rebuild-only mode. No version changes.");
            System.out.println("ℹ️  Using version: " + currentVersion);

            MavenSupport.installParentPom();
            MavenSupport.rebuildSharedUtils();

            // continue into build-artifacts block using currentVersion
            versionInfo = new VersionInfo(currentVersion, currentVersion + "-IGNORED");

        }
        // -------------------------------------------------------------
        // MODE 2 : Bump version only
        // -------------------------------------------------------------
        else if (bumpVersion && !doRelease) {

            versionInfo = PomUtils.computeVersions(currentVersion, null);

            System.out.println("🔧 Bumping version only (no release).");
            System.out.println("🔢  Current : " + currentVersion);
            System.out.println("🏷️  Release : " + versionInfo.releaseVersion);
            System.out.println("🚀 Next dev : " + versionInfo.nextDevVersion);

            MavenSupport.alignAllPomVersions(versionInfo.releaseVersion);
            MavenSupport.installParentPom();
            MavenSupport.rebuildSharedUtils();

        }
        // -------------------------------------------------------------
        // MODE 3 : Full release (bump + release)
        // -------------------------------------------------------------
        else {

            versionInfo = PomUtils.computeVersions(currentVersion, null);

            System.out.println("🚀 Full release mode.");
            System.out.println("🔢  Current : " + currentVersion);
            System.out.println("🏷️  Release : " + versionInfo.releaseVersion);
            System.out.println("🚀 Next dev : " + versionInfo.nextDevVersion);

            MavenSupport.installParentPomAsRelease(versionInfo.releaseVersion);
            MavenSupport.alignAllPomVersions(versionInfo.releaseVersion);
            PomUtils.removeSnapshotFromAllPoms(PathsConfig.BASE_PATH);

            MavenSupport.installParentPom();
            MavenSupport.rebuildSharedUtils();
        }

        // -------------------------------------------------------------
        // Build artifacts (shared across all modes)
        // -------------------------------------------------------------
        Path buildRoot     = PathsConfig.BUILDS_PATH.resolve("Glyco-PAINT-" + versionInfo.releaseVersion);
        Path windowsPath   = buildRoot.resolve("Windows");
        Path macOSPath     = buildRoot.resolve("macOS");
        Path pluginPath    = buildRoot.resolve("Plugins");
        Path installerPath = buildRoot.resolve("Installers");

        Files.createDirectories(buildRoot);
        Files.createDirectories(windowsPath);
        Files.createDirectories(macOSPath);
        Files.createDirectories(pluginPath);
        Files.createDirectories(installerPath);

        for (String module : PathsConfig.MODULES) {
            Path moduleDir = PathsConfig.BASE_PATH.resolve(module);
            System.out.println("\n---------------------------------------------");
            System.out.println("🏗️  Module: " + module);
            System.out.println("---------------------------------------------");

            MavenSupport.buildAndCollect(moduleDir, "-Pwindows-exe", "*.exe", windowsPath);
            MavenSupport.buildAndCollectMacApp(moduleDir, "-Pmacos-appbundle", macOSPath);

            List<String> installCmd = Arrays.asList(
                    "mvn", "-q", "install", "-DskipTests",
                    "-Dmaven.repo.local=" + System.getProperty("user.home") + "/.m2/repository"
            );
            ProcessBuilder pb = new ProcessBuilder(installCmd);
            pb.directory(moduleDir.toFile());
            Process p = ProcessRunner.startAndFilterOutput(pb, module);
            if (p.waitFor() != 0) {
                throw new RuntimeException("❌ Failed to install " + module);
            }
        }

        // Fiji plugin
        Path pluginDir = PathsConfig.BASE_PATH.resolve("paint-fiji-plugin");
        if (Files.exists(pluginDir.resolve("pom.xml"))) {
            MavenSupport.buildAndCollect(pluginDir, "", "*-jar-with-dependencies.jar", pluginPath);
        }

        // -------------------------------------------------------------
        // If bump or release: build installers
        // -------------------------------------------------------------
        if (bumpVersion || doRelease) {
            Path macInstallerRes = PathsConfig.BASE_PATH.resolve("paint-installer/paint-installer-macos/src/main/resources");
            Path winInstallerRes = PathsConfig.BASE_PATH.resolve("paint-installer/paint-installer-windows/src/main/resources");
            Files.createDirectories(macInstallerRes);
            Files.createDirectories(winInstallerRes);

            Path macPayload = macInstallerRes.resolve("payload.zip");
            Path winPayload = winInstallerRes.resolve("payload.zip");

            Files.deleteIfExists(macPayload);
            Files.deleteIfExists(winPayload);

            FileOps.zipPayload(macOSPath, pluginPath, macPayload);
            FileOps.zipPayload(windowsPath, pluginPath, winPayload);

            MavenSupport.runMavenModule("paint-installer/paint-installer-macos", versionInfo.releaseVersion);
            MavenSupport.runMavenModule("paint-installer/paint-installer-windows", versionInfo.releaseVersion);

            Path macTarget = PathsConfig.BASE_PATH.resolve("paint-installer/paint-installer-macos/target");
            Path winTarget = PathsConfig.BASE_PATH.resolve("paint-installer/paint-installer-windows/target");

            Path macBuilt = FileOps.latestMatching(macTarget, f -> f.contains("installer") && f.endsWith(".jar"));
            Path winBuilt = FileOps.latestMatching(winTarget, f -> f.matches(".*(exe|jar|shaded\\.jar)$"));

            Path macFinal = installerPath.resolve("Glyco-PAINT-Installer-macOS-" + versionInfo.releaseVersion + ".jar");
            Path winFinal = installerPath.resolve("Glyco-PAINT-Installer-Windows-" + versionInfo.releaseVersion + ".jar");

            Files.copy(macBuilt, macFinal, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(winBuilt, winFinal, StandardCopyOption.REPLACE_EXISTING);
        }

        // -------------------------------------------------------------
        // Post-release: tag + push + bump back to next SNAPSHOT
        // -------------------------------------------------------------
        if (doRelease) {
            System.out.println("\n🚀 Preparing next development iteration...");

            String nextSnapshot = versionInfo.nextDevVersion;

            MavenSupport.runMaven(Arrays.asList(
                    "mvn", "-q", "-B", "versions:set",
                    "-DnewVersion=" + nextSnapshot,
                    "-DgenerateBackupPoms=false",
                    "-DprocessAllModules=false"
            ), PathsConfig.BASE_PATH, "versions:set (parent only)");

            GitUtils.runCommand(Arrays.asList("bash", "-c",
                                              "shopt -s globstar; git add pom.xml **/pom.xml"), PathsConfig.BASE_PATH);

            GitUtils.runCommand(Arrays.asList("git", "commit", "-m",
                                              "Bump project to " + nextSnapshot + " for next development cycle"), PathsConfig.BASE_PATH);

            String tagName = "v" + versionInfo.releaseVersion;

            if (!GitUtils.tagExists(tagName, PathsConfig.BASE_PATH)) {
                GitUtils.createLocalTag(tagName, PathsConfig.BASE_PATH);
            }

            GitUtils.pushTag(tagName, PathsConfig.BASE_PATH);

            System.out.println("✅ Release complete. Tag pushed: " + tagName);
        }

        System.out.println("\n🎉 All builds complete for " + versionInfo.releaseVersion);
    }
}