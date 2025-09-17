package paint.localUtilities;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;



public class Miscellaneous {

    /**
     * Reads a CSV file and loads it into a {@link Table}, forcing all columns
     * to be treated as strings regardless of their original type.
     *
     * @param csvPath the path to the CSV file to read
     * @return a {@code Table} containing all CSV data with string-typed columns
     * @throws Exception if the file cannot be read or parsed
     */
    public static Table readTableAsStrings(Path csvPath) throws Exception {
        String headerLine;
        try (BufferedReader br = Files.newBufferedReader(csvPath)) {
            headerLine = br.readLine();
        }
        if (headerLine == null) {
            return Table.create(csvPath.getFileName().toString());
        }

        // simple split on comma; switch to a CSV parser if headers may contain commas in quotes
        int columnCount = headerLine.split(",", -1).length;

        ColumnType[] types = new ColumnType[columnCount];
        Arrays.fill(types, ColumnType.STRING);

        CsvReadOptions options = CsvReadOptions.builder(csvPath.toFile())
                .header(true)
                .columnTypes(types)
                .build();

        return Table.read().usingOptions(options);
    }

    public static String friendlyMessage(Throwable t) {
        if (t == null)
            return "";
        String m = t.toString();
        int colon = m.lastIndexOf(':');
        return (colon != -1) ? m.substring(colon + 1).trim() : m;
    }

    // Same idea, but from the root cause
    public static String rootCauseFriendlyMessage(Throwable t) {
        if (t == null) return "";
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return friendlyMessage(cur);
    }

    public static void deleteAssociatedFiles(Path experimentInfoFile) {
        Path parentDir = experimentInfoFile.getParent();

        if (parentDir == null) {
            System.err.println("❌ experimentInfoFile has no parent directory.");
            return;
        }

        Path allTracks = parentDir.resolve("All Tracks.csv");
        Path allRecordings = parentDir.resolve("All Recordings.csv");

        //deleteIfExists(allTracks);
        //deleteIfExists(allRecordings);
    }

    private static void deleteIfExists(Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("🗑️ Deleted: " + path);
            } else {
                System.out.println("ℹ️ File not found (no deletion needed): " + path);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to delete " + path + ": " + e.getMessage());
        }
    }
}
