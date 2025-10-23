package paint.shared.objects;

import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

/**
 * The ExperimentInfo class represents metadata and associated data objects
 * related to a specific scientific experiment. It contains core fields to
 * store metadata and collections to manage related objects such as
 * squares and tracks.
 *
 * This class provides constructors to initialize its fields, either through
 * explicit parameters or by parsing key-value pairs from a map. It includes
 * getter and setter methods for each field, as well as convenience methods
 * for adding or managing associated data objects.
 */
public class ExperimentInfo {

    // --- Core fields (columns in Recordings/Experiment Info) ---
    // @formatter:off
    private String  experimentName;
    private String  recordingName;
    private int     conditionNumber;
    private int     replicateNumber;
    private String  probeName;
    private String  probeType;
    private String  cellType;
    private String  adjuvant;
    private double  concentration;
    private boolean processFlag;               // renamed from doProcess
    private double  threshold;
    // @formatter:on

    // --- Associated objects ---
    private List<Square> squares = new ArrayList<>();
    private List<Track> tracks = new ArrayList<>();
    private Table tracksTable;

    // --- Constructors ---
    public ExperimentInfo() {
    }

    public ExperimentInfo(String experimentName,
                          String recordingName,
                          int conditionNumber,
                          int replicateNumber,
                          String probeName,
                          String probeType,
                          String cellType,
                          String adjuvant,
                          double concentration,
                          boolean processFlag,
                          double threshold) {

        // @formatter:off
        this.experimentName  = experimentName;
        this.recordingName   = recordingName;
        this.conditionNumber = conditionNumber;
        this.replicateNumber = replicateNumber;
        this.probeName       = probeName;
        this.probeType       = probeType;
        this.cellType        = cellType;
        this.adjuvant        = adjuvant;
        this.concentration   = concentration;
        this.processFlag     = processFlag;
        this.threshold       = threshold;
        // @formatter:on
    }

    /**
     * Constructs an ExperimentInfo from a row of string key-value pairs.
     * Expects keys like "Recording Name", "Condition Number", etc.
     *
     * @param row the map of column names to values (all as strings)
     */
    public ExperimentInfo(Map<String, String> row) {
        try {
            // @formatter:off
            this.experimentName  = row.get("Experiment Name");
            this.recordingName   = row.get("Recording Name");
            this.conditionNumber = parseInt(row.get("Condition Number"));
            this.replicateNumber = parseInt(row.get("Replicate Number"));
            this.probeName       = row.get("Probe Name");
            this.probeType       = row.get("Probe Type");
            this.cellType        = row.get("Cell Type");
            this.adjuvant        = row.get("Adjuvant");
            this.concentration   = parseDouble(row.get("Concentration"));
            this.processFlag     = parseBoolean(row.get("Process Flag"));
            this.threshold       = parseDouble(row.get("Threshold"));
            // @formatter:on
        } catch (Exception e) {
            PaintLogger.errorf("Problem in Experiment Info");
            PaintLogger.errorf(row.toString());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
        }
    }

    // --- Getters and Setters ---
    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public int getConditionNumber() {
        return conditionNumber;
    }

    public void setConditionNumber(int conditionNumber) {
        this.conditionNumber = conditionNumber;
    }

    public int getReplicateNumber() {
        return replicateNumber;
    }

    public void setReplicateNumber(int replicateNumber) {
        this.replicateNumber = replicateNumber;
    }

    public String getProbeName() {
        return probeName;
    }

    public void setProbeName(String probeName) {
        this.probeName = probeName;
    }

    public String getProbeType() {
        return probeType;
    }

    public void setProbeType(String probeType) {
        this.probeType = probeType;
    }

    public String getCellType() {
        return cellType;
    }

    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    public String getAdjuvant() {
        return adjuvant;
    }

    public void setAdjuvant(String adjuvant) {
        this.adjuvant = adjuvant;
    }

    public double getConcentration() {
        return concentration;
    }

    public void setConcentration(double concentration) {
        this.concentration = concentration;
    }

    public boolean isProcessFlag() {
        return processFlag;
    }

    public void setProcessFlag(boolean processFlag) {
        this.processFlag = processFlag;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public List<Square> getSquares() {
        return squares;
    }

    public void setSquares(List<Square> squares) {
        this.squares = squares;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public Table getTracksTable() {
        return tracksTable;
    }

    public void setTracksTable(Table tracksTable) {
        this.tracksTable = tracksTable;
    }

    // --- Convenience methods ---
    public void addSquare(Square square) {
        this.squares.add(square);
    }

    public void addSquares(List<Square> squares) {
        this.squares.addAll(squares);
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\tExperiment Info");
        sb.append(String.format("\t              Experiment Name               : %s\n", experimentName));
        sb.append(String.format("\t              Recording Name                : %s%n", recordingName));
        sb.append(String.format("\t              Condition Nr                  : %d%n", conditionNumber));
        sb.append(String.format("\t              Replicate Nr                  : %d%n", replicateNumber));
        sb.append(String.format("\t              Probe Name                    : %s%n", probeName));
        sb.append(String.format("\t              Probe Type                    : %s%n", probeType));
        sb.append(String.format("\t              Cell Type                     : %s%n", cellType));
        sb.append(String.format("\t              Adjuvant                      : %s%n", adjuvant));
        sb.append(String.format("\t              Concentration                 : %.2f%n", concentration));
        sb.append(String.format("\t              Threshold                     : %.2f%n", threshold));

        if (tracks != null) {
            sb.append(String.format("\t              Number of tracks              : %d%n", tracks.size()));
        }
        if (squares != null) {
            sb.append(String.format("\t              Number of squares             : %d%n", squares.size()));
        }

        int numberOfSquaresWithTracks = 0;
        for (Square square : squares) {
            if (square.getTracks() != null && !square.getTracks().isEmpty()) {
                numberOfSquaresWithTracks++;
            }
        }

        sb.append(String.format("\tNumber of squares with tracks : %d%n", numberOfSquaresWithTracks));
        return sb.toString();
    }
}