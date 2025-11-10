package release;

import java.nio.file.*;
import java.util.*;

final class PathsConfig {
    private PathsConfig() {}

    static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).getParent();
    static final Path BASE_PATH    = PROJECT_ROOT.resolve("Glyco-PAINT-Java");
    static final Path BUILDS_PATH  = PROJECT_ROOT.resolve("Glyco-PAINT-Builds");

    static final List<String> MODULES = Arrays.asList(
            "paint-viewer",
            "paint-generate-squares",
            "paint-get-omero",
            "paint-create-experiment",
            "paint-fiji-plugin"
    );
}