package paint.shared.config.paintconfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import paint.shared.utils.PaintLogger;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/** Owns the root JsonObject and handles load/save. */
class ConfigStore {

    private final Path path;
    private final Gson gson;

    private JsonObject root;  // lazily loaded

    ConfigStore(Path path, Gson gson) {
        this.path = path;
        this.gson = gson;
    }

    JsonObject root() {
        ensureLoaded(null);
        return root;
    }

    void ensureLoaded(Runnable defaultsLoader) {
        if (root != null) return;

        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                root = gson.fromJson(r, JsonObject.class);
                if (root == null) root = new JsonObject();
            } catch (IOException e) {
                PaintLogger.errorf("Failed to load config file: %s", e.getMessage());
                root = new JsonObject();
            }
        } else {
            root = new JsonObject();
            if (defaultsLoader != null) {
                defaultsLoader.run();
            }
            save();
        }
    }

    void save() {
        ensureLoaded(null);
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                gson.toJson(root, w);
            }
        } catch (IOException e) {
            PaintLogger.errorf("Failed to save config file: %s", e.getMessage());
        }
    }

    JsonObject getSection(String section) {
        ensureLoaded(null);
        for (String s : root.keySet()) {
            if (s.equalsIgnoreCase(section)) {
                return root.getAsJsonObject(s);
            }
        }
        return null;
    }

    JsonObject getOrCreateSection(String section) {
        ensureLoaded(null);
        JsonObject sec = getSection(section);
        if (sec != null) return sec;
        JsonObject created = new JsonObject();
        root.add(section, created);
        return created;
    }

    void removeSection(String section) {
        ensureLoaded(null);
        String toRemove = null;
        for (String s : root.keySet()) {
            if (s.equalsIgnoreCase(section)) {
                toRemove = s;
                break;
            }
        }
        if (toRemove != null) root.remove(toRemove);
    }

    Set<String> sections() {
        ensureLoaded(null);
        return root.keySet();
    }
}