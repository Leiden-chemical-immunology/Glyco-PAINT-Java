package paint.viewer.override.recording_exclude;

import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


import static paint.shared.constants.PaintStringConstants.*;
import static paint.shared.io.MainIOInterface.writeSpecificRecordingsFile;

public class RecordingExcludeApplier {

    /**
     * Loads and applies all recording overrides to the given list of
     * RecordingEntry objects. If the override CSV does not exist,
     * the method exits without modifying anything.
     *
     * @param recordingsTable list of in-memory RecordingEntry objects
     * @param projectPath      project root folder containing /Viewer
     */
    public static void applyRecordingExclude(Table recordingsTable,
            Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Exclude.csv");

        if (!Files.exists(csvPath)) {
            PaintLogger.infof("No Recording Override.csv present - no overrides to apply.");
            return;
        }

        List<RecordingExclude> excludes = loadRecordingExclude(csvPath);
        applyInternal(projectPath, recordingsTable, excludes);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────

    /**
     * Applies overrides already loaded from CSV. This method performs:
     * <ol>
     *     <li>Composite-key indexing</li>
     *     <li>In-place mutation of RecordingEntry thresholds</li>
     * </ol>
     *
     * @param recordingsTable   recording entries to update
     * @param excludes   list of excludes parsed from CSV
     */
    private static void applyInternal(Path projectPath, Table recordingsTable,
            List<RecordingExclude> excludes) {

        // Reset all the Exclude flags to false
        // Table recordingsTable  = readRecordingsTable(projectPath);

        BooleanColumn excludeCol = recordingsTable.booleanColumn("Exclude");

        for (int i = 0; i < excludeCol.size(); i++) {
            excludeCol.set(i, false);
        }

        for (RecordingExclude exclude : excludes) {
            StringColumn  nameCol      = recordingsTable.stringColumn("Recording Name");
            String        recordingName = exclude.getRecordingName();

            for (int row = 0; row < recordingsTable.rowCount(); row++) {
                if (recordingName.equals(nameCol.get(row))) {
                    excludeCol.set(row, true);
                    PaintLogger.infof("Recording excluded: " + recordingName);
                    break;
                }
            }
        }
        Path filePath = projectPath.resolve("Recordings-override.csv");
        writeSpecificRecordingsFile(filePath, recordingsTable);
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────

    /**
     * Loads all rows from the `Recording Override.csv` file into a list of
     * RecordingOverride objects.
     *
     * @param csvFile path to Recording Override.csv
     * @return list of parsed RecordingOverride objects
     */
    public static List<RecordingExclude> loadRecordingExclude(Path csvFile) {

        List<RecordingExclude> recordingExcludeList = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {
                RecordingExclude recordingExclude = new RecordingExclude();
                recordingExclude.setRecordingName(table.column(RECORDING_NAME).get(i).toString());
                recordingExcludeList.add(recordingExclude);
            }
        } catch (Exception ex) {
            PaintLogger.errorf("Error reading Recording Exclude.csv → " + ex.getMessage());
        }

        return recordingExcludeList;
    }

}
