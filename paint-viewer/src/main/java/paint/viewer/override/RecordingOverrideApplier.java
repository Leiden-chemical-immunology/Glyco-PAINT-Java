package paint.viewer.override;

import static paint.shared.constants.PaintConstants.*;

import paint.viewer.model.RecordingEntry;

import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RecordingOverrideApplier {

    // ────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ────────────────────────────────────────────────────────────
    public static void applyRecordingOverrides(List<RecordingEntry> recordingEntries,
            Path projectPath) {

        Path csvPath = projectPath.resolve("Viewer").resolve("Recording Override.csv");

        if (!Files.exists(csvPath)) {
            System.out.println("Recording Override.csv not found → no overrides applied.");
            return;
        }

        List<RecordingOverride> overrides = load(csvPath);
        applyInternal(recordingEntries, overrides);
    }

    // ────────────────────────────────────────────────────────────
    // INTERNAL APPLY
    // ────────────────────────────────────────────────────────────
    private static void applyInternal(List<RecordingEntry> entries,
            List<RecordingOverride> overrides) {

        Map<String, RecordingOverride> map = new HashMap<>();

        for (RecordingOverride o : overrides) {
            map.put(key(o.experimentName, o.recordingName), o);
        }

        int applied = 0;

        for (RecordingEntry entry : entries) {

            String key = key(entry.getExperimentName(), entry.getRecordingName());
            RecordingOverride override = map.get(key);

            if (override != null) {

                entry.getRecording().setMinRequiredDensityRatio(override.minRequiredDensityRatio);
                entry.getRecording().setMinRequiredRSquared(override.minRequiredRSquared);
                entry.getRecording().setMaxAllowableVariability(override.maxAllowableVariability);
                entry.getRecording().setNeighbourMode(override.neighbourMode);

                applied++;
            }
        }

        System.out.println("Recording overrides applied: " + applied);
    }

    // ────────────────────────────────────────────────────────────
    // CSV LOADING
    // ────────────────────────────────────────────────────────────
    private static List<RecordingOverride> load(Path csvFile) {

        List<RecordingOverride> list = new ArrayList<>();

        try {
            Table table = Table.read().csv(csvFile.toString());

            for (int i = 0; i < table.rowCount(); i++) {
                RecordingOverride recordingOverride = new RecordingOverride();

                recordingOverride.experimentName          = table.column(EXPERIMENT_NAME).get(i).toString();
                recordingOverride.recordingName           = table.column(RECORDING_NAME).get(i).toString();
                recordingOverride.minRequiredDensityRatio = Double.parseDouble(table.column(MIN_REQUIRED_DENSITY_RATIO).get(i).toString());
                recordingOverride.minRequiredRSquared     = Double.parseDouble(table.column(MIN_REQUIRED_R_SQUARED).get(i).toString());
                recordingOverride.maxAllowableVariability = Double.parseDouble(table.column(MAX_ALLOWABLE_VARIABILITY).get(i).toString());
                recordingOverride.neighbourMode           = table.column(NEIGHBOUR_MODE).get(i).toString();

                list.add(recordingOverride);
            }
        } catch (Exception ex) {
            System.err.println("Error reading Recording Override.csv → " + ex.getMessage());
        }

        return list;
    }

    // ────────────────────────────────────────────────────────────
    // MODEL + KEY
    // ────────────────────────────────────────────────────────────
    private static final class RecordingOverride {
        String experimentName;
        String recordingName;
        double minRequiredDensityRatio;
        double minRequiredRSquared;
        double maxAllowableVariability;
        String neighbourMode;
    }

    private static String key(String experimentName, String recordingName) {
        return experimentName + "§" + recordingName;
    }

    private RecordingOverrideApplier() {}
}