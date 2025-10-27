package paint.viewer.logic;

import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import paint.viewer.utils.RecordingEntry;
import paint.viewer.shared.SquareControlParams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class for managing and recording viewer override configurations in a CSV file.
 * The ViewerOverrideWriter is designed to log and persist parameter overrides at different
 * scoping levels: Recording, Experiment, and Project.
 *
 * The class ensures that recording-specific override configurations are saved in a managed
 * structured way within the specified CSV file, and it handles both appending and replacing
 * existing entries based on the recording names.
 */
public class ViewerOverrideWriter {
    private final File csvFile;

    public ViewerOverrideWriter(File csvFile) {
        this.csvFile = csvFile;
    }

    /**
     * Applies the given parameters and writes override records based on the specified scope.
     * The scope determines the granularity at which the override is applied:
     * "Recording", "Experiment", or "Project".
     *
     * @param scope the scope of the operation, either "Recording", "Experiment", or "Project"
     * @param params the parameters to be written as override values
     * @param recordings the list of recording entries from which the data will be determined
     * @param currentIndex the index of the current recording entry being processed in the recordings list
     * @param project the project within which the operation is being performed
     */
    public void applyAndWrite(String scope,
                              SquareControlParams params,
                              List<RecordingEntry> recordings,
                              int currentIndex,
                              Project project) {
        String timestamp = LocalDateTime.now().toString();

        if ("Recording".equals(scope)) {
            RecordingEntry current = recordings.get(currentIndex);
            writeOverrideRecord(current.getRecordingName(), params, timestamp);

        } else if ("Experiment".equals(scope)) {
            RecordingEntry cur = recordings.get(currentIndex);
            for (RecordingEntry r : recordings) {
                if (r.getExperimentName().equals(cur.getExperimentName())) {
                    writeOverrideRecord(r.getRecordingName(), params, timestamp);
                }
            }

        } else if ("Project".equals(scope)) {
            for (RecordingEntry r : recordings) {
                writeOverrideRecord(r.getRecordingName(), params, timestamp);
            }
        }
    }

    /**
     * Writes an override record into the CSV file based on the provided recording name,
     * control parameters, and timestamp. If the recording already exists in the file,
     * its entry is updated; otherwise, a new record is added. If the file does not
     * exist or does not have valid headers, the necessary headers are added.
     *
     * @param recordingName the name of the recording for which the override is written
     * @param params the control parameters containing density ratio, variability,
     *               R-squared value, and neighbour mode
     * @param timestamp the timestamp associated with the override record
     */
    private void writeOverrideRecord(String recordingName,
                                     SquareControlParams params,
                                     String timestamp) {
        PaintLogger.infof(
                "Override for '%s': MinDensityRatio=%.0f, MaxVariability=%.0f, MinRSquared=%.2f, NeighbourMode=%s",
                recordingName, params.densityRatio, params.variability, params.rSquared, params.neighbourMode
        );

        try {
            List<String> lines = new ArrayList<String>();
            if (csvFile.exists()) {
                lines = Files.readAllLines(csvFile.toPath());
            }

            if (lines.isEmpty() || !lines.get(0).startsWith("recordingName,")) {
                lines.clear();
                lines.add("recordingName,timestamp,densityRatio,variability,rSquared,neighbourMode");
            }

            String prefix = recordingName + ",";
            String newLine = recordingName + "," + timestamp + "," +
                    params.densityRatio + "," + params.variability + "," +
                    params.rSquared + "," + params.neighbourMode;

            boolean replaced = false;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).startsWith(prefix)) {
                    lines.set(i, newLine);
                    replaced = true;
                    break;
                }
            }

            if (!replaced) {
                lines.add(newLine);
            }

            File tmp = new File(csvFile.getParentFile(), csvFile.getName() + ".tmp");
            Files.write(tmp.toPath(), lines);
            Files.move(tmp.toPath(), csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}