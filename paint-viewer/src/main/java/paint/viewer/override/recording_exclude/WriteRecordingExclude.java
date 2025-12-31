/*==============================================================================
 *  Class:        WriteRecordingExclude.java
 *  Package:      paint.viewer.override.recording_exclude
 *
 *  PURPOSE:
 *    Utility methods for persisting "recording excluded" state from the Viewer.
 *    Supports:
 *      (1) patching the per-experiment Recordings.csv Exclude flag, and
 *      (2) maintaining the project-level Viewer/Recording Exclude.csv list.
 *
 *  DESCRIPTION:
 *    The Viewer can mark recordings as excluded. This class provides two small
 *    persistence helpers:
 *
 *      • patchRecordingExcluded(...)
 *          Reads an experiment's Recordings.csv, updates the "Exclude" boolean
 *          for one recording, and writes the file back to disk.
 *
 *      • updateExcludeRecordingsCsv(...)
 *          Updates the project-level Viewer/Recording Exclude.csv file, which
 *          contains a single column ("Recording Name") listing all excluded
 *          recordings. When excluded=true the recording is added (if not
 *          already present). When excluded=false it is removed (if present).
 *
 *    These methods are intended to be called directly from Viewer UI actions
 *    (e.g., toggling the Exclude/Include button).
 *
 *  KEY FEATURES:
 *    • Updates the per-experiment Recordings.csv Exclude column for one recording.
 *    • Maintains the Viewer/Recording Exclude.csv list (create/read/update).
 *    • Avoids duplicates when adding; removes all matching rows when removing.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override.recording_exclude;

import paint.shared.io.MainIOInterface;
import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;

import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;

public class WriteRecordingExclude {

    /**
     * Updates the per-experiment Recordings.csv by setting the {@code Exclude}
     * flag for a single recording.
     * <p>
     * This method reads the experiment's recordings table, finds the row whose
     * recording name matches {@code recordingName}, updates the boolean exclude
     * value, and writes the table back to disk.
     *
     * @param experimentPath path to the experiment folder that contains Recordings.csv
     * @param recordingName  the recording name to update
     * @param excluded       the new exclude state to store
     */
    public static void patchRecordingExcluded(
            Path experimentPath,
            String recordingName,
            boolean excluded
    ) {
        Table table = MainIOInterface.readRecordingsTable(experimentPath);
        if (table == null) {
            return;
        }

        StringColumn nameCol     = table.stringColumn(Recording.Column.RECORDING_NAME.header);
        BooleanColumn excludedCol = table.booleanColumn(Recording.Column.EXCLUDE.header);

        for (int row = 0; row < table.rowCount(); row++) {
            if (nameCol.get(row).equals(recordingName)) {
                excludedCol.set(row, excluded);
                break;
            }
        }

        MainIOInterface.writeSpecificRecordingsFile(
                experimentPath.resolve(RECORDINGS_CSV),
                table
        );
    }

    /**
     * Updates the project-level {@code Viewer/Recording Exclude.csv} file.
     * <p>
     * The file is expected to contain exactly one column named {@code "Recording Name"}
     * listing recordings that should be treated as excluded. If the Viewer folder
     * or CSV does not exist, they are created as needed.
     * <ul>
     *   <li>If {@code excluded == true}: add {@code recordingName} if not present.</li>
     *   <li>If {@code excluded == false}: remove all matching rows for {@code recordingName}.</li>
     * </ul>
     *
     * @param projectPath    project root folder containing the {@code Viewer} directory
     * @param recordingName  recording name to add/remove
     * @param excluded       whether the recording should be present in the exclude list
     */
    public static void updateExcludeRecordingsCsv(Path projectPath, String recordingName, boolean excluded) {
        try {
            Path viewerDir = projectPath.resolve("Viewer");
            if (Files.notExists(viewerDir)) {
                Files.createDirectories(viewerDir);
            }

            Path file = viewerDir.resolve("Recording Exclude.csv");

            final String colName = "Recording Name";
            Table table;

            if (Files.exists(file)) {
                table = Table.read().csv(file.toString());
                if (!table.columnNames().contains(colName)) {
                    // If the file exists but is malformed (missing expected column), rebuild a minimal table.
                    table = Table.create("Exclude Recordings", StringColumn.create(colName));
                }
            } else {
                // Create a new minimal exclude table with the expected single column.
                table = Table.create("Exclude Recordings", StringColumn.create(colName));
            }

            StringColumn col = table.stringColumn(colName);

            if (excluded) {
                // Append only if it is not already present.
                boolean already = false;
                for (int i = 0; i < table.rowCount(); i++) {
                    if (recordingName.equals(col.get(i))) {
                        already = true;
                        break;
                    }
                }
                if (!already) {
                    col.append(recordingName);
                }
            } else {
                // Remove all occurrences (iterate backwards to keep indices valid).
                for (int i = table.rowCount() - 1; i >= 0; i--) {
                    if (recordingName.equals(col.get(i))) {
                        table = table.dropRows(i);
                    }
                }
            }

            table.write().csv(file.toString());

        } catch (Exception e) {
            PaintLogger.errorf("Failed to update Exclude Recordings.csv: %s", e.getMessage());
        }
    }
}