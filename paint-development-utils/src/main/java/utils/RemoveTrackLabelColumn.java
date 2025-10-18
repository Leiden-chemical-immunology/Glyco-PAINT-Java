package utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple utility to traverse subdirectories, locate "All Tracks Java.csv" files,
 * remove the column "Track Label" if present, and overwrite each file.
 */
public class RemoveTrackLabelColumn {

    /** Hardcoded root directory — adjust this path before running. */
    private static final Path ROOT = Paths.get("/Users/hans/Paint Test Project");

    public static void main(String[] args) throws IOException {
        System.out.println("🔍 Scanning for CSV files under: " + ROOT);
        List<Path> csvFiles = new ArrayList<>();

        // Recursively collect all files named "All Tracks Java.csv"
        Files.walk(ROOT)
                .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().equals("All Tracks Java.csv"))
                .forEach(csvFiles::add);

        if (csvFiles.isEmpty()) {
            System.out.println("No 'All Tracks Java.csv' files found.");
            return;
        }

        System.out.println("Found " + csvFiles.size() + " file(s). Processing...");
        int modifiedCount = 0;

        for (Path csvFile : csvFiles) {
            if (removeTrackLabelColumn(csvFile)) {
                modifiedCount++;
                System.out.println("✔ Modified: " + csvFile);
            } else {
                System.out.println("ℹ No change (column not found): " + csvFile);
            }
        }

        System.out.println("\nDone. Modified " + modifiedCount + " file(s).");
    }

    /**
     * Removes the "Track Label" column from the given CSV file if present.
     * The file is overwritten in place.
     *
     * @param csvFile path to the CSV file
     * @return true if the column was found and removed, false otherwise
     * @throws IOException if reading or writing fails
     */
    private static boolean removeTrackLabelColumn(Path csvFile) throws IOException {
        // Read all records first
        try (Reader reader = Files.newBufferedReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            if (!headerMap.containsKey("Track Label")) {
                return false; // nothing to do
            }

            // Create new header list without "Track Label"
            List<String> newHeaders = new ArrayList<>();
            for (String h : headerMap.keySet()) {
                if (!h.equals("Track Label")) {
                    newHeaders.add(h);
                }
            }

            // Write a temporary file
            Path tempFile = csvFile.resolveSibling(csvFile.getFileName().toString() + ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile);
                 CSVPrinter printer = new CSVPrinter(writer,
                                                     CSVFormat.DEFAULT.builder().setHeader(newHeaders.toArray(new String[0])).build())) {

                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();
                    for (String h : newHeaders) {
                        row.add(record.get(h));
                    }
                    printer.printRecord(row);
                }
            }

            // Replace original file with the temp one
            Files.move(tempFile, csvFile, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }
}