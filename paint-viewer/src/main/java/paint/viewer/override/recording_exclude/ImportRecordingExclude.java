/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.recording_exclude;

import paint.shared.utils.PaintLogger;
import paint.viewer.model.RecordingEntry;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintStringConstants.*;

public class ImportRecordingExclude {

    /**
     * Loads and applies recording excludes to the provided list of {@link RecordingEntry}
     * objects. If "Recording Exclude.csv" does not exist, this method returns without
     * modifying anything.
     *
     * @param recordingEntries recordings to update in memory
     * @param projectPath      project root folder containing /Viewer/Recording Exclude.csv
     */
    public static void importRecordingExcludes(List<RecordingEntry> recordingEntries, Path projectPath) {
        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Exclude.csv");

        if (!Files.exists(csvPath)) {
            PaintLogger.infof("No Recording Exclude.csv present - no overrides to apply.");
            return;
        }

        List<RecordingExclude> excludes = loadRecordingExclude(csvPath);
        applyInternal(projectPath, recordingEntries, excludes);
    }

    /**
     * Loads and applies recording excludes to the provided in-memory Recordings table.
     * If "Recording Exclude.csv" does not exist, this method returns without modifying
     * anything.
     *
     * @param recordingsTable Recordings table to update in memory
     * @param projectPath     project root folder containing /Viewer/Recording Exclude.csv
     */
    public static void importRecordingExcludes(Table recordingsTable, Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Exclude.csv");

        if (!Files.exists(csvPath)) {
            PaintLogger.infof("No Recording Exclude.csv present - no overrides to apply.");
            return;
        }

        List<RecordingExclude> excludes = loadRecordingExclude(csvPath);
        applyInternal(projectPath, recordingsTable, excludes);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────

    /**
     * Applies excludes already loaded from CSV to the provided Recordings table.
     * This method always resets the full Exclude column to {@code false} first,
     * and then sets {@code true} only for Recording Names listed in the exclude file.
     *
     * @param projectPath      project root folder (currently only used for optional debug output)
     * @param recordingsTable  target Recordings table to update
     * @param excludes         list of excludes parsed from CSV
     */
    private static void applyInternal(Path projectPath, Table recordingsTable,
            List<RecordingExclude> excludes) {

        BooleanColumn excludeCol = recordingsTable.booleanColumn("Exclude");

        // Reset all to false first (deterministic)
        for (int i = 0; i < excludeCol.size(); i++) {
            excludeCol.set(i, false);
        }

        // Apply excludes by Recording Name
        boolean first = true;
        for (RecordingExclude exclude : excludes) {
            StringColumn  nameCol       = recordingsTable.stringColumn("Recording Name");
            String        recordingName = exclude.getRecordingName();

            for (int row = 0; row < recordingsTable.rowCount(); row++) {
                if (recordingName.equals(nameCol.get(row))) {
                    excludeCol.set(row, true);
                    if (first) {
                        PaintLogger.blankline();
                        first = false;
                    }
                    PaintLogger.infof("Recording excluded: " + recordingName);
                    break;
                }
            }
        }
    }

    /**
     * Applies excludes already loaded from CSV to the provided list of {@link RecordingEntry}
     * objects. This method always resets all entries to {@code excluded=false} first,
     * and then sets {@code true} only for Recording Names listed in the exclude file.
     *
     * @param projectPath      project root folder (currently only used for optional debug output)
     * @param recordingEntries target entries to update in memory
     * @param excludes         list of excludes parsed from CSV
     */
    private static void applyInternal(Path projectPath, List<RecordingEntry> recordingEntries,
            List<RecordingExclude> excludes) {

        // Reset all to false first (deterministic)
        for (RecordingEntry entry : recordingEntries) {
            entry.getRecording().setExcluded(false);
        }

        // Apply excludes by Recording Name
        for (RecordingExclude exclude : excludes) {
            String recordingName = exclude.getRecordingName();

            for (RecordingEntry entry : recordingEntries) {
                if (recordingName.equals(entry.getRecordingName())) {
                    entry.getRecording().setExcluded(true);
                    PaintLogger.infof("Recording excluded: " + recordingName);
                    break;
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────

    /**
     * Loads all rows from "Recording Exclude.csv" into a list of {@link RecordingExclude}
     * objects. The file is expected to contain a column named {@code Recording Name}.
     *
     * @param csvFile path to Recording Exclude.csv
     * @return list of parsed {@link RecordingExclude} objects (possibly empty)
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