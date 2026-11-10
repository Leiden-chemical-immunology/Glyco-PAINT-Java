/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
