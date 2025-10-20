/* IGNORE START
package validation;

import org.junit.jupiter.api.Test;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class RegressionTest {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir"));
    private static final Path OUTPUT_DIR = PROJECT_ROOT.resolve("target/test-output");
    private static final Path REFERENCE_DIR = PROJECT_ROOT.resolve("reference");

    private boolean compareCsv(Path generated, Path reference) throws Exception {
        if (!Files.exists(generated)) {
            PaintLogger.errorf("Missing generated file: %s", generated);
            return false;
        }
        if (!Files.exists(reference)) {
            PaintLogger.errorf("Missing reference file: %s", reference);
            return false;
        }

        Table gen = Table.read().csv(generated.toFile());
        Table ref = Table.read().csv(reference.toFile());

        // Sort both on all columns for deterministic comparison
        for (String col : gen.columnNames()) {
            gen = gen.sortOn(col);
            ref = ref.sortOn(col);
        }

        boolean identical = gen.structure().equals(ref.structure()) && gen.print().equals(ref.print());
        if (!identical) {
            PaintLogger.errorf("❌ Regression mismatch: %s", generated.getFileName());
        } else {
            PaintLogger.infof("✅ Regression passed: %s", generated.getFileName());
        }
        return identical;
    }

    @Test
    public void testAllRecordingsRegression() throws Exception {
        Path generated = OUTPUT_DIR.resolve("Recordings.csv");
        Path reference = REFERENCE_DIR.resolve("Recordings.csv");
        assertTrue("Recordings regression mismatch", compareCsv(generated, reference));
    }

    @Test
    public void testAllTracksRegression() throws Exception {
        Path generated = OUTPUT_DIR.resolve("Tracks.csv");
        Path reference = REFERENCE_DIR.resolve("Tracks.csv");
        assertTrue("All Tracks regression mismatch", compareCsv(generated, reference));
    }
}

IGNORE_END
*/