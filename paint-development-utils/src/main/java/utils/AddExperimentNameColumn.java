package utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.*;

/**
 * Utility to traverse subdirectories, locate "All Tracks Java.csv" files,
 * and add an "Experiment Name" column directly after "Unique Key".
 * The value is derived from the recording name (e.g., "230417-Exp-1-A1-1" → "230417").
 */
public class AddExperimentNameColumn {

    /** Hardcoded root directory — adjust before running. */
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
            if (addExperimentNameColumn(csvFile)) {
                modifiedCount++;
                System.out.println("✔ Modified: " + csvFile);
            } else {
                System.out.println("ℹ No change: " + csvFile);
            }
        }

        System.out.println("\n✅ Done. Modified " + modifiedCount + " file(s).");
    }

    /**
     * Adds an "Experiment Name" column after "Unique Key" in the CSV if not already present.
     *
     * @param csvFile path to the CSV file
     * @return true if modified, false if skipped
     * @throws IOException if reading or writing fails
     */
    private static boolean addExperimentNameColumn(Path csvFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(csvFile);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            List<String> headers = new ArrayList<>(headerMap.keySet());

            // Skip if already contains Experiment Name
            if (headers.contains("Experiment Name")) {
                return false;
            }

            int uniqueKeyIndex = headers.indexOf("Unique Key");
            if (uniqueKeyIndex == -1) {
                System.out.println("⚠️ 'Unique Key' column not found in " + csvFile);
                return false;
            }

            // Insert new column name after "Unique Key"
            headers.add(uniqueKeyIndex + 1, "Experiment Name");

            // Determine experiment name from directory name (e.g., "230417-Exp-1-A1-1" → "230417")
            String dirName = csvFile.getParent().getFileName().toString();
            String experimentName = dirName.length() >= 6 ? dirName.substring(0, 6) : dirName;

            // Write a temporary file
            Path tempFile = csvFile.resolveSibling(csvFile.getFileName().toString() + ".tmp");

            try (BufferedWriter writer = Files.newBufferedWriter(tempFile);
                 CSVPrinter printer = new CSVPrinter(writer,
                                                     CSVFormat.DEFAULT.builder().setHeader(headers.toArray(new String[0])).build())) {

                for (CSVRecord record : parser) {
                    List<String> row = new ArrayList<>();

                    for (int i = 0; i < headers.size(); i++) {
                        String header = headers.get(i);

                        if (header.equals("Experiment Name")) {
                            row.add(experimentName);
                        } else {
                            // If the header existed in the old file, copy its value
                            row.add(record.isMapped(header) ? record.get(header) : "");
                        }
                    }

                    printer.printRecord(row);
                }
            }

            // Replace original file
            Files.move(tempFile, csvFile, StandardCopyOption.REPLACE_EXISTING);
            return true;
        }
    }
}