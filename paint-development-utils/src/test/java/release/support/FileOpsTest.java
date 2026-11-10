/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOpsTest {

    @TempDir
    Path builds;

    @Test
    @DisplayName("a release build removes the test build of the same version, and nothing else")
    void releaseBuildRemovesItsSnapshotBuild() throws IOException {
        Path release  = Files.createDirectories(builds.resolve("Glyco-PAINT-0.0.138"));
        Path snapshot = Files.createDirectories(builds.resolve("Glyco-PAINT-0.0.138-SNAPSHOT/macOS"))
                             .getParent();
        Files.write(snapshot.resolve("macOS/app.txt"), new byte[]{1});
        Path older    = Files.createDirectories(builds.resolve("Glyco-PAINT-0.0.137"));
        Path next     = Files.createDirectories(builds.resolve("Glyco-PAINT-0.0.139-SNAPSHOT"));

        assertTrue(FileOps.removeSupersededSnapshotBuild(release));

        assertFalse(Files.exists(snapshot));
        assertTrue(Files.isDirectory(release));
        assertTrue(Files.isDirectory(older));
        assertTrue(Files.isDirectory(next));
    }

    @Test
    @DisplayName("a test build (Rebuild only) removes nothing")
    void snapshotBuildRemovesNothing() throws IOException {
        Path snapshot = Files.createDirectories(builds.resolve("Glyco-PAINT-0.0.139-SNAPSHOT"));

        assertFalse(FileOps.removeSupersededSnapshotBuild(snapshot));

        assertTrue(Files.isDirectory(snapshot));
    }

    @Test
    @DisplayName("a release build without an earlier test build is fine")
    void releaseBuildWithoutSnapshotIsFine() throws IOException {
        Path release = Files.createDirectories(builds.resolve("Glyco-PAINT-0.0.140"));

        assertFalse(FileOps.removeSupersededSnapshotBuild(release));

        assertTrue(Files.isDirectory(release));
    }
}
