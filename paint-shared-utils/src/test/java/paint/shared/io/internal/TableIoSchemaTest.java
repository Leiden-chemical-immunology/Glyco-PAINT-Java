package paint.shared.io.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import paint.shared.objects.ExperimentInfo;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Schema-integrity tests for the four internal TableIO classes.
 *
 * <p>Each {@code emptyTable()} must expose columns whose names and types match
 * that entity's {@code Column} schema enum, in order. This directly pins the
 * behaviour that the planned TableIO deduplication touches (the
 * {@code getColumnHeaders()} / {@code getColumnTypes()} helpers), independent of
 * any CSV formatting concerns, so the refactor can be shown to preserve every
 * schema exactly — including {@link Recording}'s date-time column.</p>
 */
@DisplayName("TableIO — emptyTable schema matches the Column enum")
class TableIoSchemaTest {

    private static void assertMatchesSchema(Table table, String[] headers, ColumnType[] types) {
        assertEquals(headers.length, table.columnCount(), "column count");
        for (int i = 0; i < headers.length; i++) {
            assertEquals(headers[i], table.columnNames().get(i), "column name at index " + i);
            assertEquals(types[i], table.column(headers[i]).type(), "column type for '" + headers[i] + "'");
        }
    }

    @Test
    @DisplayName("Squares schema")
    void squaresSchema() {
        Square.Column[] c = Square.Column.values();
        String[] h = new String[c.length];
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) { h[i] = c[i].header; t[i] = c[i].type; }
        assertMatchesSchema(new SquaresTableIO().emptyTable(), h, t);
    }

    @Test
    @DisplayName("Tracks schema")
    void tracksSchema() {
        Track.Column[] c = Track.Column.values();
        String[] h = new String[c.length];
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) { h[i] = c[i].header; t[i] = c[i].type; }
        assertMatchesSchema(new TracksTableIO().emptyTable(), h, t);
    }

    @Test
    @DisplayName("Recordings schema (includes the date-time column)")
    void recordingsSchema() {
        Recording.Column[] c = Recording.Column.values();
        String[] h = new String[c.length];
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) { h[i] = c[i].header; t[i] = c[i].type; }
        assertMatchesSchema(new RecordingsTableIO().emptyTable(), h, t);
    }

    @Test
    @DisplayName("ExperimentInfo schema")
    void experimentInfoSchema() {
        ExperimentInfo.Column[] c = ExperimentInfo.Column.values();
        String[] h = new String[c.length];
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) { h[i] = c[i].header; t[i] = c[i].type; }
        assertMatchesSchema(new ExperimentInfoTableIO().emptyTable(), h, t);
    }
}
