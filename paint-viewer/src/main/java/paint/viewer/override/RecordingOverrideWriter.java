package paint.viewer.override;

import paint.shared.utils.PaintLogger;
import paint.viewer.model.SquareControlParams;
import paint.viewer.model.RecordingEntry;
import paint.viewer.override.RecordingOverride;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import static paint.shared.constants.PaintConstants.*;

public class RecordingOverrideWriter {

    private final Path csvFilePath;

    private static final String[] HEADER = {
            EXPERIMENT_NAME,
            RECORDING_NAME,
            TIME_STAMP,
            MIN_REQUIRED_DENSITY_RATIO,
            MAX_ALLOWABLE_VARIABILITY,
            MIN_REQUIRED_R_SQUARED,
            NEIGHBOUR_MODE
    };

    public RecordingOverrideWriter(Path projectPath) {
        Path viewerPath = projectPath.resolve("Viewer");
        try {
            Files.createDirectories(viewerPath);
        } catch (IOException e) {
            PaintLogger.warnf("Failed to create Viewer directory: %s", e.getMessage());
        }
        this.csvFilePath = viewerPath.resolve("Recording Override.csv");
    }

    public void applyAndWrite(String scope,
            SquareControlParams  params,
            List<RecordingEntry> recordings,
            int currentIndex) {

        String timestamp = LocalDateTime.now().toString();

        if ("Recording".equals(scope)) {
            RecordingEntry recordingEntry = recordings.get(currentIndex);
            writeOrUpdate(recordingEntry.getExperimentName(), recordingEntry.getRecordingName(), params, timestamp);
            updateMemory(recordingEntry, params);
            return;
        }

        if ("Experiment".equals(scope)) {
            String exp = recordings.get(currentIndex).getExperimentName();
            for (RecordingEntry recordingEntry : recordings) {
                if (recordingEntry.getExperimentName().equals(exp)) {
                    writeOrUpdate(recordingEntry.getExperimentName(), recordingEntry.getRecordingName(), params, timestamp);
                    updateMemory(recordingEntry, params);
                }
            }
            return;
        }

        if ("Project".equals(scope)) {
            for (RecordingEntry recordingEntry : recordings) {
                writeOrUpdate(recordingEntry.getExperimentName(), recordingEntry.getRecordingName(), params, timestamp);
                updateMemory(recordingEntry, params);
            }
        }
    }

    // ====================================================================================
    // WRITE / UPDATE LOGIC (NON-DESTRUCTIVE)
    // ====================================================================================
    private void writeOrUpdate(
            String              experimentName,
            String              recordingName,
            SquareControlParams params,
            String              timestamp) {

        Map<String, String> map = loadExistingRows();

        String line = experimentName  + "," +
                      recordingName + "," +
                      timestamp + "," +
                      params.minRequiredDensityRatio + "," +
                      params.maxAllowableVariability + "," +
                      params.minRequiredRSquared + "," +
                      params.neighbourMode;

        map.put(recordingName, line);

        writeAll(map);
    }

    // ====================================================================================
    // READ CSV INTO A MAP (recordingName → row)
    // ====================================================================================
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

            // Validate header
            String expectedHeader = String.join(",", HEADER);
            if (!lines.get(0).equalsIgnoreCase(expectedHeader)) {
                PaintLogger.warnf("Invalid Recording Override.csv header. Rebuilding.");
                return map; // ignore malformed file
            }

            // Parse rows
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }

                String key = line.split(",", 2)[0];
                map.put(key, line);
            }

        } catch (IOException ex) {
            PaintLogger.errorf("Failed reading Recording Override.csv: %s", ex.getMessage());
        }

        return map;
    }

    // ====================================================================================
    // WRITE OUT HEADER + ALL ROWS
    // ====================================================================================
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
            PaintLogger.errorf("Failed writing Recording Override.csv: %s", e.getMessage());
        }
    }

    // ====================================================================================
    // UPDATE IN-MEMORY RECORDING ENTRY
    // ====================================================================================
    private void updateMemory(RecordingEntry re, SquareControlParams params) {
        re.getRecording().setMinRequiredDensityRatio(params.minRequiredDensityRatio);
        re.getRecording().setMaxAllowableVariability(params.maxAllowableVariability);
        re.getRecording().setMinRequiredRSquared(params.minRequiredRSquared);
        re.getRecording().setNeighbourMode(params.neighbourMode);
    }
}