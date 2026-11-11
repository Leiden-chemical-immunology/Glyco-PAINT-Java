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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenSupportTest {

    @Test
    @DisplayName("the release installs the released modules together with every project module they depend on")
    void installsReleasedModulesWithEverythingTheyDependOn() {
        List<String> cmd = MavenSupport.installModulesAndDependenciesCommand(
                Arrays.asList("paint-viewer", "paint-fiji-plugin"));

        // The modules to build are named, and -am ("also make") lets Maven add every module they
        // depend on, so a new library module (as paint-ui-common once was) cannot be forgotten.
        int pl = cmd.indexOf("-pl");
        assertTrue(pl >= 0, "no -pl in " + cmd);
        assertEquals("paint-viewer,paint-fiji-plugin", cmd.get(pl + 1));
        assertTrue(cmd.contains("-am"), "no -am in " + cmd);
        assertTrue(cmd.contains("install"), "no install in " + cmd);
    }

    @Test
    @DisplayName("setting the version includes the modules that are only in a profile (packaging, old installers)")
    void versionCommandsIncludeProfileModules() {
        // Without the profiles, versions:set does not see packaging/pom.xml or the old installer
        // POMs, which then keep the old version and no longer build.
        for (boolean allModules : new boolean[]{true, false}) {
            List<String> cmd = MavenSupport.versionsSetCommand("1.2.3", allModules);
            assertTrue(cmd.contains("versions:set"), "no versions:set in " + cmd);
            assertTrue(cmd.contains("-Ppackaging,old-installers"), "profiles missing in " + cmd);
            assertTrue(cmd.contains("-DnewVersion=1.2.3"), "no version in " + cmd);
            assertTrue(cmd.contains("-DprocessAllModules=" + allModules), "processAllModules wrong in " + cmd);
        }
    }

    @Test
    @DisplayName("the installers are built from the packaging module, without the old installers")
    void installersCommandBuildsPackagingOnly() {
        List<String> cmd = MavenSupport.installersCommand();
        assertTrue(cmd.contains("-Ppackaging"), "no -Ppackaging in " + cmd);
        assertFalse(cmd.stream().anyMatch(a -> a.contains("old-installers")), "old installers in " + cmd);
        int pl = cmd.indexOf("-pl");
        assertTrue(pl >= 0, "no -pl in " + cmd);
        assertEquals("packaging", cmd.get(pl + 1));
        assertTrue(cmd.contains("-am"), "no -am in " + cmd);
    }

    @Test
    @DisplayName("the installers are asked for by the names packaging/pom.xml gives them")
    void installerNames() {
        assertEquals(List.of("Glyco-PAINT-0.0.140-macos.pkg", "Glyco-PAINT-0.0.140-windows-setup.exe"),
                     MavenSupport.installerNames("0.0.140"));
    }

    @Test
    @DisplayName("makensis is found on the PATH")
    void findsMakensisOnPath(@TempDir Path dir) throws Exception {
        Path makensis = Files.createFile(dir.resolve("makensis"),
                                         PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
        Path found = MavenSupport.findMakensis("/nonexistent" + java.io.File.pathSeparator + dir);
        // A Homebrew copy, if installed, comes first; otherwise the one on the PATH
        assertTrue(makensis.equals(found) || (found != null && found.toString().endsWith("/bin/makensis")),
                   "found: " + found);
    }
}
