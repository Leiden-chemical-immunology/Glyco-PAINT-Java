package paint.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link PaintLogger#error(String, Throwable)} overload. It logs a
 * message plus the throwable's stack trace, for the small number of sites that
 * report an actual exception. Ordinary messages ({@code infof}/{@code errorf}/…)
 * are unaffected and carry no stack trace.
 */
@DisplayName("PaintLogger.error(String, Throwable)")
class PaintLoggerThrowableTest {

    private static String readLog(Path projectDir, String base) throws IOException {
        Path logsDir = projectDir.resolve("Logs");
        try (Stream<Path> s = Files.list(logsDir)) {
            Path log = s.filter(p -> p.getFileName().toString().startsWith(base))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no log file created"));
            return new String(Files.readAllBytes(log), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("logs the message and the throwable's stack trace")
    void logsMessageAndStackTrace(@TempDir Path projectDir) throws IOException {
        PaintLogger.initialise(projectDir, "throwable-test");
        PaintLogger.error("boom while processing", new IllegalStateException("bad thing"));

        String content = readLog(projectDir, "throwable-test");
        assertTrue(content.contains("boom while processing"), "message should be logged");
        assertTrue(content.contains("IllegalStateException"), "exception class should appear");
        assertTrue(content.contains("bad thing"), "exception message should appear");
        assertTrue(content.contains("\tat "), "stack frames should appear");
    }

    @Test
    @DisplayName("a plain errorf logs no stack trace (overload is opt-in)")
    void plainErrorfHasNoStackTrace(@TempDir Path projectDir) throws IOException {
        PaintLogger.initialise(projectDir, "plain-test");
        PaintLogger.errorf("just a message");

        String content = readLog(projectDir, "plain-test");
        assertTrue(content.contains("just a message"));
        assertFalse(content.contains("\tat "), "a plain message must not include a stack trace");
    }
}
