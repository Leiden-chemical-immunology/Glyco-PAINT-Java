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

public class writeRecordingExclude {

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
                    // If file exists but is malformed, rebuild minimal table.
                    table = Table.create("Exclude Recordings", StringColumn.create(colName));
                }
            } else {
                table = Table.create("Exclude Recordings", StringColumn.create(colName));
            }

            StringColumn col = table.stringColumn(colName);

            if (excluded) {
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
