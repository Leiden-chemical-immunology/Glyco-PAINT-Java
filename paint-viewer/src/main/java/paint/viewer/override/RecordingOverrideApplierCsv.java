package paint.viewer.override;

import static paint.shared.constants.PaintConstants.*;

import paint.shared.io.RecordingsTableIO;
import paint.viewer.model.RecordingEntry;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RecordingOverrideApplierCsv {

    // ──────────────────────────────────────────────
    // Load the base input file: Recordings.csv
    // ──────────────────────────────────────────────
    public static List<RecordingEntry> loadRecordingEntries(Path projectPath) throws Exception {

        Path             csvPath = projectPath.resolve("Recordings.csv");
        RecordingsTableIO io      = new RecordingsTableIO();
        Table table              = io.readCsv(csvPath);



        if (!Files.exists(csvPath)) {
            throw new Exception("Recordings.csv not found in project");
        }

//        Table                table = Table.read().csv(csv.toString());
        List<RecordingEntry> list  = new ArrayList<>();

//        for (int i = 0; i < table.rowCount(); i++) {
//            RecordingEntry recordingEntry = new RecordingEntry();
//            recordingEntry.setExperimentName(table.column(EXPERIMENT_NAME).get(i).toString());
//            recordingEntry.setRecordingName(table.column(RECORDING_NAME).get(i).toString());
//            recordingEntry.loadRecording(projectPath);       // your existing loader
//            list.add(recordingEntry);
//        }

        return list;
    }

    // ──────────────────────────────────────────────
    // Apply overrides FROM Recordings.csv ITSELF
    // ──────────────────────────────────────────────
    public static void applyRecordingCsvOverrides(List<RecordingEntry> entries,
            Path projectPath) throws Exception {

        Path csv = projectPath.resolve("Recordings.csv");
        Table t = Table.read().csv(csv.toString());

        for (RecordingEntry entry : entries) {
            String exp = entry.getExperimentName();
            String rec = entry.getRecordingName();

            for (int row = 0; row < t.rowCount(); row++) {

                String e2 = t.column(EXPERIMENT_NAME).get(row).toString();
                String r2 = t.column(RECORDING_NAME).get(row).toString();

                if (!e2.equals(exp) || !r2.equals(rec)) continue;

                entry.getRecording().setMinRequiredDensityRatio(
                        Double.parseDouble(t.column(MIN_REQUIRED_DENSITY_RATIO).get(row).toString()));
                entry.getRecording().setMinRequiredRSquared(
                        Double.parseDouble(t.column(MIN_REQUIRED_R_SQUARED).get(row).toString()));
                entry.getRecording().setMaxAllowableVariability(
                        Double.parseDouble(t.column(MAX_ALLOWABLE_VARIABILITY).get(row).toString()));
                entry.getRecording().setNeighbourMode(
                        t.column(NEIGHBOUR_MODE).get(row).toString());
            }
        }
    }

    // ──────────────────────────────────────────────
    // Write OUT: Recordings-<suffix>.csv
    // ──────────────────────────────────────────────
    public static void writeRecordingEntries(Path projectPath,
            String suffix,
            List<RecordingEntry> entries) throws Exception {

        Path out = projectPath.resolve("Recordings-" + suffix + ".csv");

        Table t = Table.create("Recordings");

        int n = entries.size();
        String[] exp = new String[n];
        String[] rec = new String[n];
        double[] minDR = new double[n];
        double[] minR2 = new double[n];
        double[] maxVar = new double[n];
        String[] neigh = new String[n];

        for (int i = 0; i < n; i++) {
            RecordingEntry e = entries.get(i);

            exp[i] = e.getExperimentName();
            rec[i] = e.getRecordingName();
            minDR[i] = e.getRecording().getMinRequiredDensityRatio();
            minR2[i] = e.getRecording().getMinRequiredRSquared();
            maxVar[i] = e.getRecording().getMaxAllowableVariability();
            neigh[i] = e.getRecording().getNeighbourMode();
        }

        t.addColumns(
                tech.tablesaw.api.StringColumn.create(EXPERIMENT_NAME, exp),
                tech.tablesaw.api.StringColumn.create(RECORDING_NAME, rec),
                tech.tablesaw.api.DoubleColumn.create(MIN_REQUIRED_DENSITY_RATIO, minDR),
                tech.tablesaw.api.DoubleColumn.create(MIN_REQUIRED_R_SQUARED, minR2),
                tech.tablesaw.api.DoubleColumn.create(MAX_ALLOWABLE_VARIABILITY, maxVar),
                tech.tablesaw.api.StringColumn.create(NEIGHBOUR_MODE, neigh)
        );

        t.write().csv(out.toString());
        System.out.println("Wrote: " + out.getFileName());
    }

    private RecordingOverrideApplierCsv() {}
}