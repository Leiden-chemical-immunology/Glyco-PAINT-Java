package paint.shared.config.paintconfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import paint.shared.utils.PaintLogger;
import paint.shared.validate.JsonValidator;
import paint.shared.validate.JsonValidator.Result;

import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.io.Reader;
import java.io.Writer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
            // 1) Validate first
            Result v = JsonValidator.validate(path);
            if (!v.valid) {
                // 2) Log detailed diagnostic
                PaintLogger.errorf("Config JSON is invalid:%n%s", v.error);

                // 3) Safe backup the broken file beside it
                try {
                    Path backup = backupPath(path);
                    Files.createDirectories(backup.getParent());
                    Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
                    PaintLogger.warnf("Invalid config moved to: %s", backup);
                } catch (IOException io) {
                    PaintLogger.errorf("Failed to backup invalid config: %s", io.getMessage());
                }

                // 4) Start fresh with defaults
                root = new JsonObject();
                if (defaultsLoader != null) defaultsLoader.run();
                save();
                return;
            }

            // 5) If valid, read normally
            try (Reader r = Files.newBufferedReader(path)) {
                root = gson.fromJson(r, JsonObject.class);
                if (root == null) root = new JsonObject();
            } catch (IOException e) {
                PaintLogger.errorf("Failed to load config file: %s", e.getMessage());
                root = new JsonObject();
            }
        } else {
            root = new JsonObject();
            if (defaultsLoader != null) defaultsLoader.run();
            save();
        }
    }

    private static Path backupPath(Path original) {
        String base    = original.getFileName().toString();
        base           = base.substring(0, base.length() - 5);
        String bakName = base + ".invalid.json";
        return (original.getParent() == null) ? Paths.get(bakName) : original.getParent().resolve(bakName);
    }

    void save() {
        ensureLoaded(null);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                gson.toJson(root, writer);
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
        if (sec != null) {
            return sec;
        }
        JsonObject created = new JsonObject();
        root.add(section, created);
        return created;
    }

//    void removeSection(String section) {
//        ensureLoaded(null);
//        String toRemove = null;
//        for (String s : root.keySet()) {
//            if (s.equalsIgnoreCase(section)) {
//                toRemove = s;
//                break;
//            }
//        }
//        if (toRemove != null) {
//            root.remove(toRemove);
//        }
//    }

//    Set<String> sections() {
//        ensureLoaded(null);
//        return root.keySet();
//    }
}