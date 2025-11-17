/*==============================================================================
 *  Class:        FileHelper.java
 *  Package:      paint.viewer.io
 *
 *  PURPOSE:
 *    Provides static utility methods for exporting viewer panels as high-resolution
 *    images and for generating temporary, filtered CSV files from project data.
 *
 *  DESCRIPTION:
 *    This class contains general-purpose file operations used by the PAINT Viewer:
 *
 *      • High-resolution PNG export of any Swing component (e.g. the square grid).
 *      • Extraction of a temporary CSV containing only rows for a given recording.
 *      • Automatic directory creation and safe file handling.
 *
 *    {@code FileHelper} is strictly a static utility class and cannot be
 *    instantiated.
 *
 *  KEY FEATURES:
 *    • Scaled PNG export with high-quality rendering hints.
 *    • CSV filtering by recording name with automatic temp-file creation.
 *    • Desktop integration to immediately open generated CSV files.
 *    • Zero state — all methods are pure utilities.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *==============================================================================*/

package paint.viewer.io;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains(recordingName)) {
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
}