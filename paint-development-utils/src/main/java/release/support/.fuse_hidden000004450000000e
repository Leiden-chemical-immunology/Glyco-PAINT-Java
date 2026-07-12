/*=============================================================================
 *  Class:        GitUtils.java
 *  Package:      release
 *
 *  PURPOSE:
 *    Utility methods for Git-related operations during the release process.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-development-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package release.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public final class GitUtils {
    private GitUtils() {
    }

    public static void runCommand(List<String> cmd, Path dir) throws IOException, InterruptedException {
        System.out.println("🔧 Running: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        pb.inheritIO();
        int exit = pb.start().waitFor();
        if (exit != 0) {
            throw new RuntimeException("❌ Command failed: " + String.join(" ", cmd));
        }
    }

    public static boolean tagExists(String tagName, Path repoDir) throws IOException, InterruptedException {
        ProcessBuilder checkPb = new ProcessBuilder("git", "tag", "--list", tagName);
        checkPb.directory(repoDir.toFile());
        Process checkProc = checkPb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(checkProc.getInputStream()));
        boolean exists = false;
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().equals(tagName)) {
                exists = true;
                break;
            }
        }
        checkProc.waitFor();
        return exists;
    }

    public static void createLocalTag(String tagName, Path repoDir) throws IOException, InterruptedException {
        System.out.println("🏷️  Creating local tag " + tagName);
        ProcessBuilder tagPb = new ProcessBuilder("git", "tag", "-a", tagName, "-m", "Release " + tagName);
        tagPb.directory(repoDir.toFile());
        tagPb.inheritIO();
        Process tagProc = tagPb.start();
        if (tagProc.waitFor() != 0) {
            throw new RuntimeException("❌ Failed to create local tag " + tagName);
        }
        System.out.println("✅ Created local tag " + tagName);
    }

    public static void pushTag(String tagName, Path repoDir) throws IOException, InterruptedException {
        System.out.println("📤 Pushing tag " + tagName);
        ProcessBuilder pushPb = new ProcessBuilder("git", "push", "origin", tagName);
        pushPb.directory(repoDir.toFile());
        pushPb.inheritIO();
        Process pushProc = pushPb.start();
        if (pushProc.waitFor() != 0) {
            throw new RuntimeException("❌ Failed to push tag " + tagName);
        }
        System.out.println("✅ Successfully pushed tag " + tagName);
    }
}