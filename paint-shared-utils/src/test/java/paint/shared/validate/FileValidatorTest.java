package paint.shared.validate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.ExperimentInfo;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the CSV file validators against their own declared schemas.
 *
 * <p>Rather than hard-coding 27 column names per file type — which would just duplicate the
 * {@code Column} enums and rot the moment a column is added — each test builds a file
 * <em>from</em> the schema, then mutates it. So these tests keep working as the schema evolves,
 * and they fail only when the validator genuinely stops enforcing something.
 */
class FileValidatorTest {

    // ───────────────────────────────────────────────────────────────────────────────
    // FIXTURE BUILDING
    // ───────────────────────────────────────────────────────────────────────────────

    /** A value that is valid for the given column type. */
    private static String validValueFor(ColumnType type) {
        if (type == ColumnType.STRING)          return "abc";
        if (type == ColumnType.INTEGER)         return "1";
        if (type == ColumnType.DOUBLE)          return "1.5";
        if (type == ColumnType.BOOLEAN)         return "true";
        // The validator's format requires seconds; real data looks like 2025-11-17T18:30:27.637.
        if (type == ColumnType.LOCAL_DATE_TIME) return "2025-11-17T18:30:27.637";
        throw new IllegalArgumentException("No sample value defined for column type " + type);
    }

    /** Writes a CSV with the given header and rows, and returns it. */
    private static File writeCsv(Path dir, String name, List<String> header, List<List<String>> rows)
            throws IOException {
        StringBuilder sb = new StringBuilder(String.join(",", header)).append('\n');
        for (List<String> row : rows) {
            sb.append(String.join(",", row)).append('\n');
        }
        Path file = dir.resolve(name);
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        return file.toFile();
    }

    /** One row of type-appropriate values for the given types. */
    private static List<String> validRow(ColumnType[] types) {
        List<String> row = new ArrayList<>();
        for (ColumnType type : types) {
            row.add(validValueFor(type));
        }
        return row;
    }

    /**
     * Runs the standard battery against one validator and its schema: a well-formed file passes,
     * and each way of breaking it is detected.
     */
    private static void assertEnforcesSchema(AbstractFileValidator validator,
                                             List<String> header,
                                             ColumnType[] types,
                                             String fileName,
                                             Path dir) throws IOException {

        // 1. A well-formed file is accepted.
        File good = writeCsv(dir, fileName, header, Arrays.asList(validRow(types)));
        ValidationResult ok = validator.validate(good);
        assertTrue(ok.isValid(),
                fileName + ": a well-formed file should validate, but got: " + ok.getErrors());

        // 2. A missing column is rejected.
        List<String> shortHeader = new ArrayList<>(header.subList(0, header.size() - 1));
        List<String> shortRow    = new ArrayList<>(validRow(types).subList(0, types.length - 1));
        File missingColumn = writeCsv(dir, "missing-column-" + fileName, shortHeader,
                Arrays.asList(shortRow));
        assertFalse(validator.validate(missingColumn).isValid(),
                fileName + ": a file with a missing column must be rejected");

        // 3. A renamed column is rejected.
        List<String> renamedHeader = new ArrayList<>(header);
        renamedHeader.set(0, "Not A Real Column");
        File renamedColumn = writeCsv(dir, "renamed-column-" + fileName, renamedHeader,
                Arrays.asList(validRow(types)));
        assertFalse(validator.validate(renamedColumn).isValid(),
                fileName + ": a file with a renamed column must be rejected");

        // 4. A value of the wrong type is rejected — but only where a wrong type is possible.
        //    (Every string is a valid STRING, so a schema of all strings cannot fail this way.)
        int typedColumn = firstNonStringColumn(types);
        if (typedColumn >= 0) {
            List<String> badRow = validRow(types);
            badRow.set(typedColumn, "not-a-number");
            File badValue = writeCsv(dir, "bad-value-" + fileName, header, Arrays.asList(badRow));
            assertFalse(validator.validate(badValue).isValid(),
                    fileName + ": '" + header.get(typedColumn) + "' is "
                            + types[typedColumn] + "; a non-" + types[typedColumn]
                            + " value must be rejected");
        }

        // 5. A file that does not exist is reported, not thrown.
        ValidationResult absent = validator.validate(dir.resolve("no-such-file.csv").toFile());
        assertFalse(absent.isValid(), fileName + ": a missing file must be reported as invalid");
    }

    /** Index of the first column whose type is not STRING, or -1 if there is none. */
    private static int firstNonStringColumn(ColumnType[] types) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] != ColumnType.STRING && types[i] != ColumnType.LOCAL_DATE_TIME) {
                return i;
            }
        }
        return -1;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // THE FOUR FILE TYPES
    // ───────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SquaresValidator")
    class Squares {
        @Test
        @DisplayName("enforces the Square.Column schema")
        void enforcesSchema(@TempDir Path dir) throws IOException {
            List<String>  header = new ArrayList<>();
            ColumnType[]  types  = new ColumnType[Square.Column.values().length];
            int i = 0;
            for (Square.Column col : Square.Column.values()) {
                header.add(col.header);
                types[i++] = col.type;
            }
            assertEnforcesSchema(new SquaresValidator(), header, types, "Squares.csv", dir);
        }
    }

    @Nested
    @DisplayName("TracksValidator")
    class Tracks {
        @Test
        @DisplayName("enforces the Track.Column schema")
        void enforcesSchema(@TempDir Path dir) throws IOException {
            List<String>  header = new ArrayList<>();
            ColumnType[]  types  = new ColumnType[Track.Column.values().length];
            int i = 0;
            for (Track.Column col : Track.Column.values()) {
                header.add(col.header);
                types[i++] = col.type;
            }
            assertEnforcesSchema(new TracksValidator(), header, types, "Tracks.csv", dir);
        }
    }

    @Nested
    @DisplayName("RecordingsValidator")
    class Recordings {
        @Test
        @DisplayName("enforces the Recording.Column schema")
        void enforcesSchema(@TempDir Path dir) throws IOException {
            List<String>  header = new ArrayList<>();
            ColumnType[]  types  = new ColumnType[Recording.Column.values().length];
            int i = 0;
            for (Recording.Column col : Recording.Column.values()) {
                header.add(col.header);
                types[i++] = col.type;
            }
            assertEnforcesSchema(new RecordingsValidator(), header, types, "Recordings.csv", dir);
        }
    }

    @Nested
    @DisplayName("ExperimentInfoValidator")
    class ExperimentInfoFile {
        @Test
        @DisplayName("enforces the ExperimentInfo.Column schema")
        void enforcesSchema(@TempDir Path dir) throws IOException {
            List<String>  header = new ArrayList<>();
            ColumnType[]  types  = new ColumnType[ExperimentInfo.Column.values().length];
            int i = 0;
            for (ExperimentInfo.Column col : ExperimentInfo.Column.values()) {
                header.add(col.header);
                types[i++] = col.type;
            }
            assertEnforcesSchema(new ExperimentInfoValidator(), header, types,
                    "Experiment Info.csv", dir);
        }
    }
}
