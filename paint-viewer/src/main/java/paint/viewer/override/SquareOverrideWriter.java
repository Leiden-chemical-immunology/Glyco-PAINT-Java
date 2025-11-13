/*==============================================================================
 *  Class:        SquareOverrideWriter.java
 *  Package:      paint.viewer.logic
 *
 *  PURPOSE:
 *    Manages persistence of per-square cell assignment overrides in the
 *    PAINT viewer, enabling user-defined cell associations for individual
 *    squares to be stored, updated, and restored across sessions.
 *
 *  DESCRIPTION:
 *    The {@code SquareOverrideWriter} writes and maintains a structured CSV
 *    file named {@code Square Override.csv} containing cell assignment data
 *    for each square in a recording. Each record specifies the experiment,
 *    recording, square ID, assigned cell ID, and timestamp of the override.
 *
 *    The writer ensures the Viewer directory exists, validates and writes
 *    headers if necessary, replaces entries for squares that have been
 *    reassigned, and performs all writes atomically using a temporary file.
 *
 *  KEY FEATURES:
 *    • Stores persistent square-to-cell mapping overrides.
 *    • Automatically creates and maintains CSV structure and headers.
 *    • Replaces existing entries for previously assigned squares.
 *    • Performs atomic write operations using temporary files.
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
==============================================================================*/
package paint.viewer.override;

import paint.shared.utils.PaintLogger;
import paint.viewer.model.RecordingEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static paint.shared.constants.PaintConstants.*;

/**
 * Handles per-square cell assignment persistence by writing overrides to
 * a CSV file named {@code Square Override.csv} located in the Viewer folder
 * of the project. Each record links a specific square to a user-assigned
 * cell ID, allowing persistent tracking of manual cell assignments.
 * <p>
 * Columns:
 * <ul>
 *   <li>experimentName</li>
 *   <li>recordingName</li>
 *   <li>squareId</li>
 *   <li>cellId</li>
 *   <li>timestamp</li>
 * </ul>
 * <p>
 * The writer ensures the Viewer directory exists, headers are created if
 * missing, and each update replaces any previous record for the same square.
 * This mirrors {@link paint.viewer.override.RecordingOverrideWriter} in structure
 * and reliability, but applies to square-level (cell) granularity.
 */
public class SquareOverrideWriter {

    private final Path csvFilePath;

    /**
     * Constructs a new {@code SquareOverrideWriter} for a given project.
     * Ensures that the Viewer directory exists before assigning the file path.
     *
     * @param projectPath the root path of the project containing the Viewer directory
     */
    public SquareOverrideWriter(Path projectPath) {
        Path viewerPath = projectPath.resolve("Viewer");
        try {
            Files.createDirectories(viewerPath);
        } catch (IOException ignored) {}
        this.csvFilePath = viewerPath.resolve("Square Override.csv");
    }

    /**
     * Writes or updates cell assignment overrides for all selected squares
     * within the specified recording entry. Each square’s assignment is
     * recorded with an associated timestamp. If an entry for a square already
     * exists, it is replaced with the new cell ID.
     *
     * @param recordingEntry    the recording containing the relevant squares
     * @param squareAssignments the list of squares that have been assigned to a cell
     */
    public void writeSquareOverrides(RecordingEntry recordingEntry,
            Map<Integer, Integer> squareAssignments) {

        String timestamp      = LocalDateTime.now().toString();
        String experimentName = recordingEntry.getExperimentName();
        String recordingName  = recordingEntry.getRecordingName();

        try {
            List<String> lines = new ArrayList<>();
            if (Files.exists(csvFilePath)) {
                lines = Files.readAllLines(csvFilePath);
            }

            // Use constants — no hard-coded strings
            String header = EXPERIMENT_NAME + "," +
                            RECORDING_NAME  + "," +
                            SQUARE_NUMBER   + "," +
                            CELL_ID         + "," +
                            TIME_STAMP;

            if (lines.isEmpty() || !lines.get(0).equals(header)) {
                lines.clear();
                lines.add(header);
            }

            for (Map.Entry<Integer, Integer> entry : squareAssignments.entrySet()) {
                int squareNumber = entry.getKey();
                int cellId       = entry.getValue();

                String prefix = experimentName + "," + recordingName + "," + squareNumber + ",";

                boolean found = false;
                for (int i = 1; i < lines.size(); i++) {
                    if (lines.get(i).startsWith(prefix)) {
                        found = true;

                        if (cellId == 0) {
                            lines.remove(i);
                        } else {
                            String newLine = experimentName + "," +
                                             recordingName  + "," +
                                             squareNumber   + "," +
                                             cellId         + "," +
                                             timestamp;
                            lines.set(i, newLine);
                        }
                        break;
                    }
                }

                if (!found && cellId != 0) {
                    String newLine = experimentName + "," +
                                     recordingName  + "," +
                                     squareNumber   + "," +
                                     cellId         + "," +
                                     timestamp;
                    lines.add(newLine);
                }
            }

            Path tmp = csvFilePath.resolveSibling("Square Override.tmp");
            Files.write(tmp, lines);
            Files.move(tmp, csvFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException ex) {
            PaintLogger.errorf("Error writing square overrides: %s", ex.getMessage());
        }
    }
}