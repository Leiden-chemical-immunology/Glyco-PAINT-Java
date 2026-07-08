package paint.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link SweepConfig} parses defensively: a malformed (non-object)
 * file surfaces as a clear {@link IOException}, and a non-boolean sweep flag
 * disables that one attribute instead of throwing.
 */
@DisplayName("SweepConfig — robust JSON parsing")
class SweepConfigTest {

    private static Path write(Path dir, String name, String json) throws IOException {
        Path p = dir.resolve(name);
        Files.write(p, json.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    @Test
    @DisplayName("a non-object root fails as IOException, not a raw coercion error")
    void nonObjectRootThrowsIOException(@TempDir Path dir) throws IOException {
        Path f = write(dir, "arr.json", "[1, 2, 3]");
        assertThrows(IOException.class, () -> new SweepConfig(f.toString()));
    }

    @Test
    @DisplayName("a non-boolean sweep flag is treated as disabled, not an error")
    void nonBooleanFlagIsIgnored(@TempDir Path dir) throws IOException {
        String json = "{"
                + "\"Sweep\": {\"Radius\": \"maybe\", \"Threshold\": true},"
                + "\"Threshold\": {\"a\": 1, \"b\": 2}"
                + "}";
        SweepConfig cfg = new SweepConfig(write(dir, "sweep.json", json).toString());

        Map<String, List<Number>> active = cfg.getActiveSweepValues("Sweep");

        // "Radius" flag is a string -> ignored; "Threshold" is true -> included.
        assertFalse(active.containsKey("Radius"), "non-boolean flag should be ignored");
        assertTrue(active.containsKey("Threshold"));
        assertEquals(Arrays.asList(1, 2), active.get("Threshold"));
    }

    @Test
    @DisplayName("a valid sweep config returns the enabled numeric values")
    void validConfigParses(@TempDir Path dir) throws IOException {
        String json = "{"
                + "\"Sweep\": {\"Radius\": true},"
                + "\"Radius\": {\"v0\": 0.5, \"v1\": 1.5}"
                + "}";
        SweepConfig cfg = new SweepConfig(write(dir, "ok.json", json).toString());

        Map<String, List<Number>> active = cfg.getActiveSweepValues("Sweep");
        assertEquals(Arrays.asList(0.5, 1.5), active.get("Radius"));
    }
}
