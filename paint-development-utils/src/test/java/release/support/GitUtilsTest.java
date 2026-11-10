/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the release tool's git steps against a throwaway repository in a temporary folder.
 */
class GitUtilsTest {

    @TempDir
    Path repo;

    @BeforeEach
    void createRepository() throws Exception {
        assumeTrue(git("--version").startsWith("git version"), "git is not available");
        git("init", "-q");
        git("config", "user.email", "test@example.invalid");
        git("config", "user.name", "Release Tool Test");
        git("config", "commit.gpgsign", "false");
        git("config", "tag.gpgsign", "false");

        write("pom.xml", pom("0.0.138-SNAPSHOT"));
        write("paint-viewer/pom.xml", pom("0.0.138-SNAPSHOT"));
        write("paint-viewer/src/Viewer.java", "class Viewer {}");
        git("add", "-A");
        git("commit", "-q", "-m", "Initial");
    }

    @Test
    @DisplayName("only tracked pom.xml files are committed; a new pom.xml and other changes stay out")
    void commitsOnlyTrackedPoms() throws Exception {
        write("pom.xml", pom("0.0.138"));
        write("paint-viewer/pom.xml", pom("0.0.138"));
        write("paint-bundle/pom.xml", pom("0.0.138"));             // new module, not yet committed
        write("paint-viewer/src/Viewer.java", "class Viewer { }"); // unrelated work in progress

        GitUtils.commitTrackedPoms("Release 0.0.138", repo);

        List<String> committed = Arrays.asList(git("show", "--name-only", "--format=", "HEAD").split("\n"));
        assertEquals(Arrays.asList("paint-viewer/pom.xml", "pom.xml"), committed);
        assertTrue(GitUtils.uncommittedChanges(repo).contains("?? paint-bundle/"));
        assertTrue(GitUtils.uncommittedChanges(repo).contains(" M paint-viewer/src/Viewer.java"));
    }

    @Test
    @DisplayName("the release tag points at the commit that has the release version")
    void tagPointsAtReleaseVersion() throws Exception {
        write("pom.xml", pom("0.0.138"));
        write("paint-viewer/pom.xml", pom("0.0.138"));

        GitUtils.commitReleaseAndTag("0.0.138", repo);

        // What the release tool does next: bump to the next development version
        write("pom.xml", pom("0.0.139-SNAPSHOT"));
        write("paint-viewer/pom.xml", pom("0.0.139-SNAPSHOT"));
        GitUtils.commitTrackedPoms("Bump project to 0.0.139-SNAPSHOT", repo);

        assertEquals(pom("0.0.138"), git("show", "v0.0.138:pom.xml"));
        assertEquals(pom("0.0.138"), git("show", "v0.0.138:paint-viewer/pom.xml"));
        assertEquals(pom("0.0.139-SNAPSHOT"), git("show", "HEAD:pom.xml"));
    }

    @Test
    @DisplayName("uncommitted changes are reported, and a clean tree reports none")
    void reportsUncommittedChanges() throws Exception {
        assertTrue(GitUtils.uncommittedChanges(repo).isEmpty());

        write("paint-viewer/src/Viewer.java", "class Viewer { }");
        assertFalse(GitUtils.uncommittedChanges(repo).isEmpty());
    }

    // ------------------------------------------------------------------------------------------

    private static String pom(String version) {
        return "<project><version>" + version + "</version></project>";
    }

    private void write(String relative, String content) throws IOException {
        Path file = repo.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private String git(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "git";
        System.arraycopy(args, 0, cmd, 1, args.length);
        Process p = new ProcessBuilder(cmd).directory(repo.toFile()).redirectErrorStream(true).start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = p.getInputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        if (p.waitFor() != 0) {
            throw new IllegalStateException("git " + String.join(" ", args) + " failed: " + out);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
    }
}
