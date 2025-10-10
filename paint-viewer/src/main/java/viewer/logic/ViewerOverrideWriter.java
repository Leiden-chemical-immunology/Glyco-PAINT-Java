package viewer.logic;

import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import viewer.utils.RecordingEntry;
import viewer.shared.SquareControlParams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ViewerOverrideWriter {
    private final File csvFile;

    public ViewerOverrideWriter(File csvFile) {
        this.csvFile = csvFile;
    }

    public void applyAndWrite(String scope, SquareControlParams params,
                              List<RecordingEntry> recordings, int currentIndex, Project project) {
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

    private void writeOverrideRecord(String recordingName, SquareControlParams params, String timestamp) {
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