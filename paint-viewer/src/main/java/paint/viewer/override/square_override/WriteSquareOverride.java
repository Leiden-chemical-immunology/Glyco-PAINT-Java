/*==============================================================================
 *  Class:        WriteSquareOverride.java
 *  Package:      paint.viewer.override
 *
 *  PURPOSE:
 *    Manages persistence of per-square cell assignment overrides in the
 *    PAINT Viewer. Allows user-selected squares to be assigned to specific
 *    cell IDs, and ensures these assignments are stored on disk and restored
 *    across viewer sessions.
 *
 *  DESCRIPTION:
 *    This class writes and updates the CSV file:
 *
 *        Viewer/Square Override.csv
 *
 *    Each row records a mapping:
 *        experimentName, recordingName, squareNumber, cellId, timestamp
 *
 *    Assignments are stored per square, per recording, and are maintained
 *    non-destructively unless explicitly replaced. The class supports:
 *
 *      • Merging new assignments into existing ones.
 *      • Replacing all assignments for a recording.
 *      • Checking whether any assignments exist for a recording.
 *      • Atomically writing changes using temporary file replacement.
 *
 *  KEY FEATURES:
 *    • Per-square persistent cell assignments for the PAINT Viewer.
 *    • Non-destructive merging of overrides.
 *    • Complete replacement mode for a recording’s overrides.
 *    • Fully atomic file writes.
 *    • Automatically rebuilds malformed CSV headers.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-30
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override.square_override;

import paint.shared.utils.PaintLogger;
import paint.viewer.model.RecordingEntry;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import static paint.shared.constants.PaintStringConstants.SQUARE_NUMBER;
import static paint.shared.constants.PaintStringConstants.CELL_ID;
import static paint.shared.constants.PaintStringConstants.TIME_STAMP;
import static paint.shared.constants.PaintStringConstants.EXPERIMENT_NAME;
import static paint.shared.constants.PaintStringConstants.RECORDING_NAME;

//import static paint.shared.constants.PaintColumnNames.*;

/**
 * Handles persistent per-square cell assignments by maintaining the CSV file
 * {@code Square Override.csv} inside the project's {@code Viewer} directory.
 * <p>
 * Each override entry specifies:
 * <ul>
 *     <li>Experiment name</li>
 *     <li>Recording name</li>
 *     <li>Square number</li>
 *     <li>Cell ID</li>
 *     <li>Timestamp</li>
 * </ul>
 *
 * <p>The class supports:
 * <ul>
 *     <li>Merging new assignments into existing ones</li>
 *     <li>Replacing all assignments for a recording</li>
 *     <li>Checking whether overrides exist for a recording</li>
 *     <li>Atomic writes using a temporary file</li>
 * </ul>
 */
public class WriteSquareOverride {

    /** Path to: Viewer/Square Override.csv */
    private final Path csvFilePath;

    /** CSV header columns */
    private static final String[] HEADER = {
            EXPERIMENT_NAME,
            RECORDING_NAME,
            SQUARE_NUMBER,
            CELL_ID,
            TIME_STAMP
    };

    /**
     * Builds a composite key for a (experiment, recording, square) combination.
     */
    private static String key(String exp, String rec, int square) {
        return exp + "§" + rec + "§" + square;
    }

    /**
     * Constructs a new writer for the given project.
     * Ensures that the Viewer/ directory exists.
     *
     * @param projectPath root path of the project
     */
    public WriteSquareOverride(Path projectPath) {
        Path viewerPath = projectPath.resolve("Viewer");
        try {
            Files.createDirectories(viewerPath);
        } catch (IOException ignored) {}

        this.csvFilePath = viewerPath.resolve("Square Override.csv");
    }

    /**
     * Writes or updates the cell assignment overrides for selected squares.
     * This merges new assignments with any existing ones.
     *
     * @param recordingEntry          the recording entry to update
     * @param assignments map of squareNumber → cellId
     */
    public void writeSquareOverridesToFile(RecordingEntry recordingEntry, Map<Integer, Integer> assignments) {
        String experiment = recordingEntry.getExperimentName();
        String recording  = recordingEntry.getRecordingName();
        String timestamp  = LocalDateTime.now().toString();

        // Load existing overrides into a map
        Map<String, String> map = loadExistingRows();

        // Update or remove entries
        for (Map.Entry<Integer, Integer> e : assignments.entrySet()) {
            int square = e.getKey();
            int cellId = e.getValue();

            String k = key(experiment, recording, square);

            if (cellId == 0) {
                // Remove override for this square
                map.remove(k);
            } else {
                // Update override
                String line = experiment + "," +
                        recording  + "," +
                        square     + "," +
                        cellId     + "," +
                        timestamp;

                map.put(k, line);
            }
        }

        writeAll(map);
    }

    // ========================================================================
    //   LOAD EXISTING ROWS FROM CSV
    // ========================================================================

    /**
     * Reads the CSV file into a map keyed by (experiment, recording, square).
     * Automatically handles missing files or malformed headers.
     *
     * @return a map of compositeKey → CSV row
     */
    private Map<String, String> loadExistingRows() {
        Map<String, String> map = new LinkedHashMap<>();

        if (!Files.exists(csvFilePath)) {
            return map;
        }

        try {
            List<String> lines = Files.readAllLines(csvFilePath);
            if (lines.isEmpty()) {
                return map;
            }

            String expectedHeader = String.join(",", HEADER);

            // Validate header
            if (!lines.get(0).equalsIgnoreCase(expectedHeader)) {
                PaintLogger.warnf("Invalid Square Override.csv header. Rebuilding.");
                return map;  // Start fresh
            }

            // Parse each row
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;

                String[] p = line.split(",", 5);
                if (p.length < 4) continue;

                String exp = p[0];
                String rec = p[1];
                int square = Integer.parseInt(p[2]);

                map.put(key(exp, rec, square), line);
            }

        } catch (Exception e) {
            PaintLogger.errorf("Failed reading Square Override.csv: %s", e.getMessage());
        }

        return map;
    }

    // ========================================================================
    //   SAVE ALL ROWS BACK TO DISK
    // ========================================================================

    /**
     * Writes all overrides back to the CSV file using an atomic file replace.
     *
     * @param map compositeKey → CSV row
     */
    private void writeAll(Map<String, String> map) {

        String header = String.join(",", HEADER);

        List<String> out = new ArrayList<>();
        out.add(header);
        out.addAll(map.values());

        Path tmp = csvFilePath.resolveSibling(csvFilePath.getFileName() + ".tmp");

        try {
            Files.write(tmp, out);
            Files.move(tmp, csvFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            PaintLogger.errorf("Failed writing Square Override.csv: %s", e.getMessage());
        }
    }

    // ========================================================================
    //   QUERY FOR EXISTING OVERRIDES
    // ========================================================================

    /**
     * Checks whether this recording already has override entries.
     *
     * @param re recording entry
     * @return true if at least one override exists for this recording
     */
    public boolean hasOverridesFor(RecordingEntry re) {
        String prefix = re.getExperimentName() + "," + re.getRecordingName() + ",";
        try {
            if (!Files.exists(csvFilePath)) {
                return false;
            }
            return Files.readAllLines(csvFilePath)
                        .stream()
                        .anyMatch(line -> line.startsWith(prefix));
        } catch (IOException e) {
            PaintLogger.warnf("Error checking square overrides: %s", e.getMessage());
            return false;
        }
    }

    // ========================================================================
    //   MERGE / REPLACE LOGIC
    // ========================================================================

    /**
     * Merges new assignments into existing ones without removing others.
     */
    public void mergeSquareOverrides(RecordingEntry re, Map<Integer, Integer> newAssignments) {
        writeSquareOverridesToFile(re, newAssignments);
    }

    /**
     * Replaces all existing assignments for the given recording with only the
     * provided assignments.
     *
     * @param re             the recording entry whose overrides should be replaced
     * @param newAssignments map of squareNumber → cellId
     */
    public void replaceSquareOverrides(
            RecordingEntry re,
            Map<Integer, Integer> newAssignments
    ) {
        String prefix = re.getExperimentName() + "," + re.getRecordingName() + ",";

        try {
            List<String> lines = Files.exists(csvFilePath)
                    ? Files.readAllLines(csvFilePath)
                    : new ArrayList<>();

            String header = String.join(",", HEADER);

            // Validate header
            if (lines.isEmpty() || !lines.get(0).equals(header)) {
                lines.clear();
                lines.add(header);
            }

            // Remove all old rows for this recording
            lines.removeIf(line -> line.startsWith(prefix) && !line.equals(header));

            // Add new entries
            String experimentName = re.getExperimentName();
            String recordingName  = re.getRecordingName();
            String timestamp      = LocalDateTime.now().toString();

            for (Map.Entry<Integer,Integer> entry : newAssignments.entrySet()) {
                int cellId = entry.getValue();
                if (cellId == 0) continue;

                int squareNumber = entry.getKey();

                String newLine =
                        experimentName + "," +
                                recordingName  + "," +
                                squareNumber   + "," +
                                cellId         + "," +
                                timestamp;

                lines.add(newLine);
            }

            // Atomic write
            Path tmp = csvFilePath.resolveSibling("Square Override.tmp");
            Files.write(tmp, lines);
            Files.move(tmp, csvFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            PaintLogger.errorf("Error writing square overrides: %s", ex.getMessage());
        }
    }
}