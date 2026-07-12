/*==============================================================================
 *  Class:        WriteRecordingExclude.java
 *  Package:      paint.viewer.override.recording_exclude
 *
 *  PURPOSE:
 *    Persists "recording excluded" state from the Viewer, by maintaining the
 *    project-level Viewer/Recording Exclude.csv list.
 *
 *  DESCRIPTION:
 *    The Viewer can mark recordings as excluded. Toggling the Exclude/Include
 *    button calls updateExcludeRecordingsCsv(...), which updates the project-level
 *    Viewer/Recording Exclude.csv file. That file holds a single column
 *    ("Recording Name") listing every excluded recording. When excluded=true the
 *    recording is added (if not already present); when excluded=false it is
 *    removed (if present).
 *
 *    Recording Exclude.csv — not the Exclude column of any Recordings.csv — is the
 *    authoritative record of what the user excluded. ImportRecordingExclude and
 *    ExportOverridesFromViewer both rebuild that column from this file, clearing it
 *    first. Nothing therefore writes the Exclude column directly; a
 *    patchRecordingExcluded(...) method that did so was never called by anything and
 *    has been removed.
 *
 *  KEY FEATURES:
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

import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;

public class WriteRecordingExclude {

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