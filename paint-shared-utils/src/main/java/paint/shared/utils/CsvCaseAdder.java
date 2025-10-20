package paint.shared.utils;

import org.apache.commons.csv.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;

/**
 * Adds or updates a "Case" column with a fixed value in one or more CSV files.
 */
public class CsvCaseAdder {

    /**
     * Adds or updates a "Case" column in CSV files found in experiment subdirectories.
     *
     * @param root             root directory containing experiment folders
     * @param fileName         name of the CSV file to update
     * @param experimentNames  list of experiment directory names
     * @param caseName         value to insert in the "Case" column
     * @throws IOException if reading or writing fails
     */
    public static void addCase(Path root,
                               String fileName,
                               List<String> experimentNames,
                               String caseName) throws IOException {

        for (String exp : experimentNames) {
            Path csvPath = root.resolve(exp).resolve(fileName);
            if (!Files.exists(csvPath)) {
                System.err.println("File not found: " + csvPath);
                continue;
            }

            Path tempPath = csvPath.resolveSibling(fileName + ".tmp");

            try (
                    Reader reader = Files.newBufferedReader(csvPath);
                    CSVParser parser = CSVFormat.DEFAULT
                            .withFirstRecordAsHeader()
                            .parse(reader);
                    BufferedWriter writer = Files.newBufferedWriter(tempPath)
            ) {
                // Check if "Case" column already exists
                Map<String, Integer> headerMap = parser.getHeaderMap();
                boolean hasCaseColumn = headerMap.containsKey("Case");

                // Build final header
                List<String> headers = new ArrayList<>(headerMap.keySet());
                if (!hasCaseColumn) headers.add("Case");

                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0])));

                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();

                    // Copy existing columns
                    for (String h : headerMap.keySet()) {
                        if (h.equals("Case")) {
                            row.add(caseName); // overwrite if exists
                        } else {
                            row.add(record.get(h));
                        }
                    }

                    // Add new "Case" if it was missing
                    if (!hasCaseColumn) {
                        row.add(caseName);
                    }

                    printer.printRecord(row);
                }

                printer.flush();
                printer.close();
            }

            // Replace original file
            Files.move(tempPath, csvPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Updated 'Case' column in: " + csvPath);
        }
    }

    // Example usage
    public static void main(String[] args) throws IOException {
        Path root = Paths.get("/Users/hans/Paint Test Project/Sweep");
        List<String> exps = Arrays.asList("221012", "AnyName");
        addCase(root, RECORDINGS_CSV, exps, "Case 1");
    }
}