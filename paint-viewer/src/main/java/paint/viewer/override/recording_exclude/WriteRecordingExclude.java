/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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