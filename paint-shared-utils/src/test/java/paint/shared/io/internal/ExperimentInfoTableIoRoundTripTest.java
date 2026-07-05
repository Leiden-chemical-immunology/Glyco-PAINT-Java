package paint.shared.io.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.ExperimentInfo;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization test for the ExperimentInfo CSV I/O path. This schema has
 * strings, integers, one boolean and doubles but no date-time column, so all set
 * fields can be pinned through a full write -> read round trip. Doubles are exact
 * at 3 decimals to match the writer's fixed precision.
 */
@DisplayName("ExperimentInfoTableIO — CSV round trip")
class ExperimentInfoTableIoRoundTripTest {

    private static String[] headers() {
        ExperimentInfo.Column[] c = ExperimentInfo.Column.values();
        String[] h = new String[c.length];
        for (int i = 0; i < c.length; i++) h[i] = c[i].header;
        return h;
    }

    private static ColumnType[] types() {
        ExperimentInfo.Column[] c = ExperimentInfo.Column.values();
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) t[i] = c[i].type;
        return t;
    }

    @Test
    @DisplayName("an experiment-info row survives write -> read with its fields intact")
    void roundTripsOneInfo(@TempDir Path dir) throws Exception {
        ExperimentInfoTableIO io = new ExperimentInfoTableIO();

        ExperimentInfo original = new ExperimentInfo();
        original.setExperimentName("exp");
        original.setRecordingName("rec");
        original.setConditionNumber(1);
        original.setReplicateNumber(2);
        original.setProbeName("probeA");
        original.setProbeType("typeB");
        original.setCellType("cellC");
        original.setAdjuvant("adjD");
        original.setConcentration(5.000);
        original.setProcessFlag(true);
        original.setThreshold(1.250);

        Path csv = dir.resolve("experiment_info.csv");
        Table table = io.toTable(Collections.singletonList(original));
        io.writeCsv(table, csv);

        List<ExperimentInfo> back = io.toEntities(io.readCsvWithSchema(csv, headers(), types(), false));

        assertEquals(1, back.size());
        ExperimentInfo r = back.get(0);

        assertEquals("exp", r.getExperimentName());
        assertEquals("rec", r.getRecordingName());
        assertEquals(1, r.getConditionNumber());
        assertEquals(2, r.getReplicateNumber());
        assertEquals("probeA", r.getProbeName());
        assertEquals("typeB", r.getProbeType());
        assertEquals("cellC", r.getCellType());
        assertEquals("adjD", r.getAdjuvant());
        assertEquals(5.000, r.getConcentration(), 1e-9);
        assertTrue(r.isProcessFlagSet());
        assertEquals(1.250, r.getThreshold(), 1e-9);
    }
}
