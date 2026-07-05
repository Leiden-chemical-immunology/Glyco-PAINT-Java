package paint.shared.io.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.Track;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization test for the Tracks CSV I/O path. Tracks have no date-time or
 * boolean columns, so every set field can be pinned through a full
 * write -> read round trip. Double test values are exact at 3 decimals to match
 * the writer's fixed precision.
 */
@DisplayName("TracksTableIO — CSV round trip")
class TracksTableIoRoundTripTest {

    private static String[] headers() {
        Track.Column[] c = Track.Column.values();
        String[] h = new String[c.length];
        for (int i = 0; i < c.length; i++) h[i] = c[i].header;
        return h;
    }

    private static ColumnType[] types() {
        Track.Column[] c = Track.Column.values();
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) t[i] = c[i].type;
        return t;
    }

    @Test
    @DisplayName("a track survives write -> read with its fields intact")
    void roundTripsOneTrack(@TempDir Path dir) throws Exception {
        TracksTableIO io = new TracksTableIO();

        Track original = new Track();
        original.setUniqueKey("exp§rec§11");
        original.setExperimentName("exp");
        original.setRecordingName("rec");
        original.setTrackId(11);
        original.setNumberOfSpots(20);
        original.setNumberOfGaps(1);
        original.setLongestGap(2);
        original.setTrackDuration(1.500);
        original.setTrackXLocation(10.250);
        original.setTrackYLocation(20.750);
        original.setTrackDisplacement(0.125);
        original.setTrackMaxSpeed(2.000);
        original.setTrackMedianSpeed(1.000);
        original.setDiffusionCoefficient(0.033);
        original.setDiffusionCoefficientExt(0.044);
        original.setTotalDistance(5.500);
        original.setConfinementRatio(0.900);
        original.setSquareNumber(7);
        original.setLabelNumber(3);

        Path csv = dir.resolve("tracks.csv");
        Table table = io.toTable(Collections.singletonList(original));
        io.writeCsv(table, csv);

        List<Track> back = io.toEntities(io.readCsvWithSchema(csv, headers(), types(), false));

        assertEquals(1, back.size());
        Track r = back.get(0);

        assertEquals("exp§rec§11", r.getUniqueKey());
        assertEquals("exp", r.getExperimentName());
        assertEquals("rec", r.getRecordingName());
        assertEquals(11, r.getTrackId());
        assertEquals(20, r.getNumberOfSpots());
        assertEquals(1, r.getNumberOfGaps());
        assertEquals(2, r.getLongestGap());
        assertEquals(7, r.getSquareNumber());
        assertEquals(3, r.getLabelNumber());
        assertEquals(1.500, r.getTrackDuration(), 1e-9);
        assertEquals(10.250, r.getTrackXLocation(), 1e-9);
        assertEquals(20.750, r.getTrackYLocation(), 1e-9);
        assertEquals(0.125, r.getTrackDisplacement(), 1e-9);
        assertEquals(0.033, r.getDiffusionCoefficient(), 1e-9);
        assertEquals(5.500, r.getTotalDistance(), 1e-9);
        assertEquals(0.900, r.getConfinementRatio(), 1e-9);
    }
}
