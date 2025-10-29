/******************************************************************************
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
 ******************************************************************************/

package paint.viewer.logic;

import paint.shared.utils.PaintLogger;
import paint.viewer.utils.RecordingEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles per-square cell assignment persistence by writing overrides to
 * a CSV file named {@code Square Override.csv} located in the Viewer folder
 * of the project. Each record links a specific square to a user-assigned
 * cell ID, allowing persistent tracking of manual cell assignments.
 *
 * Columns:
 * <ul>
 *   <li>experimentName</li>
 *   <li>recordingName</li>
 *   <li>squareId</li>
 *   <li>cellId</li>
 *   <li>timestamp</li>
 * </ul>
 *
 * The writer ensures the Viewer directory exists, headers are created if
 * missing, and each update replaces any previous record for the same square.
 * This mirrors {@link paint.viewer.logic.RecordingOverrideWriter} in structure
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
        if (Files.notExists(viewerPath)) {
            try {
                Files.createDirectories(viewerPath);   // ensures Viewer folder exists
            } catch (IOException e) {
                PaintLogger.warnf("Failed to create Viewer directory: %s", e.getMessage());
            }
        }
        this.csvFilePath = viewerPath.resolve("Square Override.csv");
    }

    /**
     * Writes or updates cell assignment overrides for all selected squares
     * within the specified recording entry. Each square’s assignment is
     * recorded with an associated timestamp. If an entry for a square already
     * exists, it is replaced with the new cell ID.
     *
     * @param recordingEntry  the recording containing the relevant squares
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

            // Initialize CSV with headers if new or malformed
            if (lines.isEmpty() || !lines.get(0).startsWith("experimentName,")) {
                lines.clear();
                lines.add("experimentName,recordingName,squareId,cellId,timestamp");
            }

            // Update or append records for each (squareNumber, cellId) pair
            for (Map.Entry<Integer, Integer> entry : squareAssignments.entrySet()) {
                int squareNumber = entry.getKey();
                int cellId       = entry.getValue();

                String prefix = experimentName + "," + recordingName + "," + squareNumber + ",";

                boolean found = false;
                for (int i = 1; i < lines.size(); i++) {
                    if (lines.get(i).startsWith(prefix)) {
                        found = true;
                        if (cellId == 0) {
                            // Remove entry if it exists and cellId == 0
                            lines.remove(i);
                        } else {
                            // Replace existing record
                            String newLine = experimentName + "," + recordingName + "," + squareNumber + "," + cellId + "," + timestamp;
                            lines.set(i, newLine);
                        }
                        break;
                    }
                }

                // Add new entry only if cellId > 0 and not found
                if (cellId != 0 && !found) {
                    String newLine = experimentName + "," + recordingName + "," + squareNumber + "," + cellId + "," + timestamp;
                    lines.add(newLine);
                }
            }

            // Atomic write via temporary file to prevent data loss
            Path tmpFilePath = csvFilePath.resolveSibling(csvFilePath.getFileName().toString() + ".tmp");
            Files.write(tmpFilePath, lines);
            Files.move(tmpFilePath, csvFilePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException ex) {
            PaintLogger.errorf("Error writing square overrides: %s", ex.getMessage());
            ex.printStackTrace();
        }
    }
}