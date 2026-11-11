/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods for Git-related operations during the release process.
 */
@SuppressWarnings("SameParameterValue")
public final class GitUtils {
    private GitUtils() {
    }

    public static void runCommand(List<String> cmd, Path dir) throws IOException, InterruptedException {
        System.out.println("🔧 Running: " + String.join(" ", cmd));
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(dir.toFile());
        int exit = runEchoingOutput(pb);
        if (exit != 0) {
            throw new RuntimeException("❌ Command failed: " + String.join(" ", cmd));
        }
    }

    /**
     * Returns the uncommitted changes ({@code git status --porcelain}), one entry per file;
     * empty when the working tree is clean.
     */
    public static List<String> uncommittedChanges(Path repoDir) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain");
        pb.directory(repoDir.toFile());
        Process proc = pb.start();
        List<String> changes = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    changes.add(line);
                }
            }
        }
        if (proc.waitFor() != 0) {
            throw new RuntimeException("❌ git status failed in " + repoDir);
        }
        return changes;
    }

    /**
     * Commits the changes to pom.xml files that are already under version control, and nothing
     * else: a new pom.xml (a module that is not committed yet) and other changed files stay out.
     */
    public static void commitTrackedPoms(String message, Path repoDir) throws IOException, InterruptedException {
        runCommand(Arrays.asList("git", "add", "-u", "--", ":(glob)**/pom.xml"), repoDir);
        runCommand(Arrays.asList("git", "commit", "-m", message), repoDir);
    }

    /**
     * Commits the POMs with the release version and tags that commit {@code v<releaseVersion>},
     * so checking out the tag gives exactly what was released.
     */
    public static void commitReleaseAndTag(String releaseVersion, Path repoDir) throws IOException, InterruptedException {
        commitTrackedPoms("Release " + releaseVersion, repoDir);
        createLocalTag("v" + releaseVersion, repoDir);
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
        if (runEchoingOutput(tagPb) != 0) {
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

    /**
     * Runs the process and prints its output (stdout and stderr together) through
     * {@code System.out}, line by line. Unlike {@code inheritIO()}, the output then passes through
     * Java, so a test run by Maven Surefire does not see git writing directly to the stream it
     * uses to talk to the test process ("Corrupted channel" warning). Input stays inherited.
     *
     * @return the exit code
     */
    private static int runEchoingOutput(ProcessBuilder pb) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        Process proc = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
        return proc.waitFor();
    }
}
