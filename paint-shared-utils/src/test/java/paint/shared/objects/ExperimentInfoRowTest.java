package paint.shared.objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintStringConstants.ADJUVANT;
import static paint.shared.constants.PaintStringConstants.CELL_TYPE;
import static paint.shared.constants.PaintStringConstants.CONCENTRATION;
import static paint.shared.constants.PaintStringConstants.CONDITION_NUMBER;
import static paint.shared.constants.PaintStringConstants.EXPERIMENT_NAME;
import static paint.shared.constants.PaintStringConstants.PROBE_NAME;
import static paint.shared.constants.PaintStringConstants.PROBE_TYPE;
import static paint.shared.constants.PaintStringConstants.PROCESS_FLAG;
import static paint.shared.constants.PaintStringConstants.RECORDING_NAME;
import static paint.shared.constants.PaintStringConstants.REPLICATE_NUMBER;
import static paint.shared.constants.PaintStringConstants.THRESHOLD;

/**
 * Tests for the {@code ExperimentInfo(Map)} CSV-row constructor. Pins that a
 * valid row parses correctly and that a malformed numeric field fails fast with
 * a clear exception, rather than silently yielding a half-built object.
 */
@DisplayName("ExperimentInfo(Map) — CSV row parsing")
class ExperimentInfoRowTest {

    private static Map<String, String> validRow() {
        Map<String, String> r = new LinkedHashMap<>();
        r.put(EXPERIMENT_NAME, "exp");
        r.put(RECORDING_NAME, "rec");
        r.put(CONDITION_NUMBER, "1");
        r.put(REPLICATE_NUMBER, "2");
        r.put(PROBE_NAME, "probeA");
        r.put(PROBE_TYPE, "typeB");
        r.put(CELL_TYPE, "cellC");
        r.put(ADJUVANT, "adjD");
        r.put(CONCENTRATION, "5.0");
        r.put(PROCESS_FLAG, "yes");
        r.put(THRESHOLD, "1.25");
        return r;
    }

    @Test
    @DisplayName("a valid row populates all fields")
    void validRowParses() {
        ExperimentInfo info = new ExperimentInfo(validRow());
        assertEquals("exp", info.getExperimentName());
        assertEquals("rec", info.getRecordingName());
        assertEquals(1, info.getConditionNumber());
        assertEquals(2, info.getReplicateNumber());
        assertEquals("probeA", info.getProbeName());
        assertEquals(5.0, info.getConcentration(), 1e-9);
        assertTrue(info.isProcessFlagSet());
        assertEquals(1.25, info.getThreshold(), 1e-9);
    }

    @Test
    @DisplayName("a malformed numeric field throws instead of half-building")
    void malformedNumberThrows() {
        Map<String, String> bad = validRow();
        bad.put(CONCENTRATION, "not-a-number");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new ExperimentInfo(bad));
        assertTrue(ex.getMessage().contains("Experiment Info"),
                "exception message should identify the failure");
    }

    @Test
    @DisplayName("an empty numeric field also fails fast")
    void emptyNumberThrows() {
        Map<String, String> bad = validRow();
        bad.put(THRESHOLD, "");
        assertThrows(IllegalArgumentException.class, () -> new ExperimentInfo(bad));
    }
}
