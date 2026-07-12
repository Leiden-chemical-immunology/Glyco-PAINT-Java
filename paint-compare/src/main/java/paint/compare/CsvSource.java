package paint.compare;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a CSV file into a list of {@code column -> value} row maps for
 * {@link TableComparer}.
 *
 * <p>Uses Apache Commons CSV so quoted fields containing commas, embedded
 * quotes, and newlines are parsed correctly — unlike the hand-rolled
 * {@code line.split(",")} the old comparators used, which corrupts any such
 * field. Values are trimmed; a column absent from a row maps to {@code ""}.</p>
 */
public final class CsvSource {

    private CsvSource() {
    }

    public static List<Map<String, String>> read(Path csv) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .build();

        try (Reader reader = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            List<String> headers = parser.getHeaderNames();
            for (CSVRecord record : parser) {
                Map<String, String> row = new LinkedHashMap<>();
                for (String header : headers) {
                    row.put(header, record.isMapped(header) ? record.get(header) : "");
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
