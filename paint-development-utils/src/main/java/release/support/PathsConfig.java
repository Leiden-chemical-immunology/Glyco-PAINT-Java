/*=============================================================================
 *  Class:        PathsConfig.java
 *  Package:      release
 *
 *  PURPOSE:
 *    Defines path constants and directory structures for the release pipeline.
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

import java.nio.file.*;
import java.util.*;

public final class PathsConfig {
    private PathsConfig() {}

    public static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    public static final Path BASE_PATH    = PROJECT_ROOT.resolve("Glyco-PAINT-Java");
    public static final Path BUILDS_PATH  = PROJECT_ROOT.resolve("Glyco-PAINT-Builds");

    public static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment",
            "paint-fiji-plugin"
    );
}