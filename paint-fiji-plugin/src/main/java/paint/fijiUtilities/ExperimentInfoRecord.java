package paint.fijiUtilities;

import java.util.Map;

public class ExperimentInfoRecord {
    public final String recordingName;
    public final String experimentDate;
    public final String experimentName;
    public final int conditionNr;
    public final int replicateNr;
    public final String probe;
    public final String probeType;
    public final String cellType;
    public final String adjuvant;
    public final double concentration;
    public final double threshold;
    public final boolean process;

    private ExperimentInfoRecord(
            String recordingName,
            String experimentDate,
            String experimentName,
            int conditionNr,
            int replicateNr,
            String probe,
            String probeType,
            String cellType,
            String adjuvant,
            double concentration,
            double threshold,
            boolean process) {
        this.recordingName = recordingName;
        this.experimentDate = experimentDate;
        this.experimentName = experimentName;
        this.conditionNr = conditionNr;
        this.replicateNr = replicateNr;
        this.probe = probe;
        this.probeType = probeType;
        this.cellType = cellType;
        this.adjuvant = adjuvant;
        this.concentration = concentration;
        this.threshold = threshold;
        this.process = process;
    }

    /**
     * Factory method to create an {@link ExperimentInfoRecord} from a CSV row map.
     * <p>
     * Each entry in the map represents a column header mapped to its string value,
     * e.g. {@code "Recording Name" -> "Sample1"}.
     * </p>
     *
     * @param row a map from column header to value (header → value), expected to contain
     *            keys such as:
     *            <ul>
     *              <li>{@code "Recording Name"}</li>
     *              <li>{@code "Experiment Date"}</li>
     *              <li>{@code "Experiment Name"}</li>
     *              <li>{@code "Condition Number"}</li>
     *              <li>{@code "Replicate Number"}</li>
     *              <li>{@code "Probe Name"}</li>
     *              <li>{@code "Probe Type"}</li>
     *              <li>{@code "Cell Type"}</li>
     *              <li>{@code "Adjuvant"}</li>
     *              <li>{@code "Concentration"}</li>
     *              <li>{@code "Threshold"}</li>
     *              <li>{@code "Process Flag"}</li>
     *            </ul>
     * @return a new {@code ExperimentInfoRecord} populated with values parsed from the row
     * @throws NumberFormatException if numeric fields such as
     *                               {@code "Condition Number"},
     *                               {@code "Replicate Number"},
     *                               {@code "Concentration"}, or
     *                               {@code "Threshold"} cannot be parsed
     */
    public static ExperimentInfoRecord fromRow(Map<String,String> row) {
        return new ExperimentInfoRecord(
                row.get("Recording Name"),
                row.get("Experiment Date"),
                row.get("Experiment Name"),
                parseInt(row.get("Condition Number")),
                parseInt(row.get("Replicate Number")),
                row.get("Probe Name"),
                row.get("Probe Type"),
                row.get("Cell Type"),
                row.get("Adjuvant"),
                parseDouble(row.get("Concentration")),
                parseDouble(row.get("Threshold")),
                parseBoolean(row.get("Process Flag"))
        );
    }

    // ---- Helpers ----

    private static int parseInt(String s) {
        if (s == null) {
            return -1;
        }
        try {
            return Integer.parseInt(s.trim());
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }

    private static double parseDouble(String s) {
        if (s == null) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(s.trim());
        }
        catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static boolean parseBoolean(String s) {
        if (s == null) {
            return false;
        }
        String v = s.trim().toLowerCase();
        return v.equals("yes")
                || v.equals("y")
                || v.equals("true")
                || v.equals("t")
                || v.equals("1"); // optional, if you also want to treat "1" as true
    }

    @Override
    public String toString() {
        return "Experiment Name:   " + experimentName + '\n' +
                "Recording Name:        " + recordingName + '\n' +
                "Experiment Date:       " + experimentDate + '\n' +
                "Condition Nr:          " + conditionNr + '\n' +
                "Replicate Nr:          " + replicateNr + '\n' +
                "Probe:                 " + probe + '\n' +
                "Probe Type:            " + probeType + '\n' +
                "Cell Type:             " + cellType + '\n' +
                "Adjuvant:              " + adjuvant + '\n' +
                "Concentration:         " + concentration + '\n' +
                "Threshold:             " + threshold + '\n' +
                "Process:               " + process + '\n';
    }
}
