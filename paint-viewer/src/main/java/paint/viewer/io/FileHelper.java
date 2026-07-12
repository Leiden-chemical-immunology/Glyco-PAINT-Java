/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.io;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static paint.shared.constants.PaintStringConstants.RECORDING_NAME;

/**
 * Static utility class providing file operations for the PAINT Viewer, including:
 * <ul>
 *     <li>Exporting Swing components as high-resolution PNG images</li>
 *     <li>Filtering a project's {@code squares.csv} to a temporary CSV</li>
 *     <li>Automatically opening filtered results via Desktop integration</li>
 * </ul>
 *
 * <p>Instantiation is prevented via a private constructor.</p>
 */
public final class FileHelper {

    /** Prevent instantiation. */
    private FileHelper() {
    }


    // =========================================================================
    // CSV FILTERING
    // =========================================================================

    /**
     * Extracts only the rows from an experiment's {@code squares.csv} matching the
     * given recording name, writes them to a temporary CSV file, marks the file
     * read-only, and opens it using the desktop's default CSV viewer.
     *
     * <p>If no rows match, an empty CSV (with header) is still produced.</p>
     *
     * @param projectRoot   the root directory of the PAINT project
     * @param experimentName the experiment folder containing {@code squares.csv}
     * @param recordingName  the recording name to filter for
     * @throws IOException if reading or writing fails, or Desktop integration is unavailable
     */
    public static void filterAndOpenSquaresCsv(Path projectRoot,
            String experimentName,
            String recordingName)
            throws IOException {

        Path origCsv = projectRoot
                .resolve(experimentName)
                .resolve("squares.csv");

        if (!Files.exists(origCsv)) {
            throw new IOException("Squares.csv not found: " + origCsv);
        }

        // Create temporary filtered file
        Path tempFile = Files.createTempFile("Squares " + recordingName, ".csv");

        try (BufferedReader r = Files.newBufferedReader(origCsv);
             BufferedWriter w = Files.newBufferedWriter(tempFile)) {

            String header = r.readLine();
            if (header != null) {
                w.write(header);
                w.newLine();

                // Match the "Recording Name" field exactly.
                //
                // This used to be line.contains(recordingName), which is a substring test over the
                // whole row: recording "…-A4-1" then also matched every row of "…-A4-10", and the
                // user was shown a mixture of two recordings' squares. Fall back to the old
                // behaviour only if the column is missing, so an unexpected header shows too much
                // rather than nothing.
                int nameCol = indexOfColumn(header, RECORDING_NAME);

                String line;
                while ((line = r.readLine()) != null) {
                    boolean match = (nameCol < 0)
                            ? line.contains(recordingName)
                            : recordingName.equals(fieldAt(line, nameCol));
                    if (match) {
                        w.write(line);
                        w.newLine();
                    }
                }
            }
        }

        tempFile.toFile().setReadOnly();

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(tempFile.toFile());
        } else {
            throw new IOException("Desktop integration not supported.");
        }
    }

    /**
     * Returns the position of a named column in a CSV header line.
     *
     * @param headerLine the CSV header line
     * @param columnName the column to look for
     * @return the zero-based column index, or {@code -1} if the column is absent
     */
    private static int indexOfColumn(String headerLine, String columnName) {
        String[] columns = headerLine.split(",", -1);
        for (int i = 0; i < columns.length; i++) {
            if (columnName.equals(columns[i].trim())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns one field of a CSV line. The files written by PAINT contain no quoted or
     * embedded commas, so a plain split is sufficient here.
     *
     * @param line  the CSV line
     * @param index the zero-based field index
     * @return the field, trimmed, or {@code null} if the line has too few fields
     */
    private static String fieldAt(String line, int index) {
        String[] fields = line.split(",", -1);
        return (index < fields.length) ? fields[index].trim() : null;
    }
}