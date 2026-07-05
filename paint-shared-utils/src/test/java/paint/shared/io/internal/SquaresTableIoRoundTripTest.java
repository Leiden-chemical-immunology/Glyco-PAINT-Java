package paint.shared.io.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.Square;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization test for the Squares CSV I/O path
 * ({@link SquaresTableIO} + {@link BaseTableIO}). It writes a square to CSV
 * through the production writer and reads it back through the production
 * schema-validated reader, asserting the values that survive the round trip.
 *
 * <p>This pins the current entity &harr; table &harr; CSV behaviour so the planned
 * TableIO deduplication can be shown to preserve it exactly.</p>
 *
 * <p>Note two intentional, pre-existing traits of the format that this test
 * documents rather than challenges:</p>
 * <ul>
 *   <li>Doubles are written with fixed 3-decimal precision, so test values are
 *       chosen to be exact at 3 decimals.</li>
 *   <li>Unset (NaN) doubles are written as empty and read back as NaN/missing.</li>
 * </ul>
 */
@DisplayName("SquaresTableIO — CSV round trip")
class SquaresTableIoRoundTripTest {

    private static String[] headers() {
        Square.Column[] cols = Square.Column.values();
        String[] h = new String[cols.length];
        for (int i = 0; i < cols.length; i++) h[i] = cols[i].header;
        return h;
    }

    private static ColumnType[] types() {
        Square.Column[] cols = Square.Column.values();
        ColumnType[] t = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) t[i] = cols[i].type;
        return t;
    }

    @Test
    @DisplayName("a square survives write -> read with all value-bearing fields intact")
    void roundTripsOneSquare(@TempDir Path dir) throws Exception {
        SquaresTableIO io = new SquaresTableIO();

        Square original = new Square(
                "exp§rec§5", "exp", "rec",
                /* squareNumber */ 5,
                /* rowNumber    */ 1,
                /* colNumber    */ 2,
                /* x0 */ 1.24, /* y0 */ 2.00, /* x1 */ 3.01, /* y1 */ 4.99);
        original.setLabelNumber(3);
        original.setCellId(9);
        original.setVisible(true);
        original.setSquareManuallyExcluded(false);
        original.setImageExcluded(false);
        original.setNumberOfTracks(42);
        original.setTau(12.500);       // exact at 3 decimals
        original.setRSquared(0.995);
        original.setDensity(3.142);

        Path csv = dir.resolve("squares.csv");
        Table table = io.toTable(Collections.singletonList(original));
        io.writeCsv(table, csv);

        Table read = io.readCsvWithSchema(csv, headers(), types(), false);
        List<Square> back = io.toEntities(read);

        assertEquals(1, back.size(), "expected exactly one square back");
        Square r = back.get(0);

        // Strings
        assertEquals("exp§rec§5", r.getUniqueKey());
        assertEquals("exp", r.getExperimentName());
        assertEquals("rec", r.getRecordingName());
        // Integers
        assertEquals(5, r.getSquareNumber());
        assertEquals(1, r.getRowNumber());
        assertEquals(2, r.getColNumber());
        assertEquals(3, r.getLabelNumber());
        assertEquals(9, r.getCellId());
        assertEquals(42, r.getNumberOfTracks());
        // Booleans
        assertTrue(r.isVisible());
        assertEquals(false, r.isSquareManuallyExcluded());
        assertEquals(false, r.isImageExcluded());
        // Doubles (exact at 3 decimals)
        assertEquals(1.24, r.getX0(), 1e-9);
        assertEquals(2.00, r.getY0(), 1e-9);
        assertEquals(3.01, r.getX1(), 1e-9);
        assertEquals(4.99, r.getY1(), 1e-9);
        assertEquals(12.500, r.getTau(), 1e-9);
        assertEquals(0.995, r.getRSquared(), 1e-9);
        assertEquals(3.142, r.getDensity(), 1e-9);

        // Unset double is written empty and comes back NaN/missing.
        assertTrue(Double.isNaN(r.getVariability()),
                "unset double should round-trip as NaN");
    }

    @Test
    @DisplayName("reading a CSV whose header is wrong fails loudly rather than returning garbage")
    void wrongHeaderThrows(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("bad.csv");
        java.nio.file.Files.write(csv,
                "Not The Right,Header\n1,2\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        SquaresTableIO io = new SquaresTableIO();
        // A structurally wrong CSV must throw (which layer throws is unimportant);
        // the point is that it does not silently succeed.
        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> io.readCsvWithSchema(csv, headers(), types(), false));
    }
}
