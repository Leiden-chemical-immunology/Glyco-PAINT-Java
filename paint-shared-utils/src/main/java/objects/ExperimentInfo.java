package objects;

import tech.tablesaw.api.Table;

import java.util.*;

/**
 * Represents a Recording with metadata, squares, and tracks.
 */
public class ExperimentInfo {

    // --- Core fields (columns in All Recordings/Experiment Info) ---
    private String recordingName;
    private int conditionNumber;
    private int replicateNumber;
    private String probeName;
    private String probeType;
    private String cellType;
    private String adjuvant;
    private double concentration;
    private boolean processFlag;               // renamed from doProcess
    private double threshold;


    // --- Associated objects ---
    private List<Square> squares = new ArrayList<>();
    private List<Track> tracks = new ArrayList<>();
    private Table tracksTable;

    // --- Constructors ---
    public ExperimentInfo() {}

    public ExperimentInfo(String recordingName,
                          int conditionNumber,
                          int replicateNumber,
                          String probeName,
                          String probeType,
                          String cellType,
                          String adjuvant,
                          double concentration,
                          boolean processFlag,
                          double threshold) {
        this.recordingName = recordingName;
        this.conditionNumber = conditionNumber;
        this.replicateNumber = replicateNumber;
        this.probeName = probeName;
        this.probeType = probeType;
        this.cellType = cellType;
        this.adjuvant = adjuvant;
        this.concentration = concentration;
        this.processFlag = processFlag;
        this.threshold = threshold;
    }

    // --- Getters and Setters ---
    public String getRecordingName() { return recordingName; }
    public void setRecordingName(String recordingName) { this.recordingName = recordingName; }

    public int getConditionNumber() { return conditionNumber; }
    public void setConditionNumber(int conditionNumber) { this.conditionNumber = conditionNumber; }

    public int getReplicateNumber() { return replicateNumber; }
    public void setReplicateNumber(int replicateNumber) { this.replicateNumber = replicateNumber; }

    public String getProbeName() { return probeName; }
    public void setProbeName(String probeName) { this.probeName = probeName; }

    public String getProbeType() { return probeType; }
    public void setProbeType(String probeType) { this.probeType = probeType; }

    public String getCellType() { return cellType; }
    public void setCellType(String cellType) { this.cellType = cellType; }

    public String getAdjuvant() { return adjuvant; }
    public void setAdjuvant(String adjuvant) { this.adjuvant = adjuvant; }

    public double getConcentration() { return concentration; }
    public void setConcentration(double concentration) { this.concentration = concentration; }

    public boolean isProcessFlag() { return processFlag; }
    public void setProcessFlag(boolean processFlag) { this.processFlag = processFlag; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }


    // --- Associated objects ---
    public List<Square> getSquares() { return squares; }
    public void setSquares(List<Square> squares) { this.squares = squares; }

    public List<Track> getTracks() { return tracks; }
    public void setTracks(List<Track> tracks) { this.tracks = tracks; }

    public Table getTracksTable() { return tracksTable; }
    public void setTracksTable(Table tracksTable) { this.tracksTable = tracksTable; }

    // --- Convenience methods ---
    public void addSquare(Square square) { this.squares.add(square); }
    public void addSquares(List<Square> squares) { this.squares.addAll(squares); }
    public void addTrack(Track track) { this.tracks.add(track); }

    // --- Debug helpers ---
    private static Boolean checkBooleanValue(String string) {
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t"));
        return yesValues.contains(string.trim().toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("\tRecording Name                : %s%n", recordingName));
        sb.append(String.format("\tCondition Nr                  : %d%n", conditionNumber));
        sb.append(String.format("\tReplicate Nr                  : %d%n", replicateNumber));
        sb.append(String.format("\tProbe Name                    : %s%n", probeName));
        sb.append(String.format("\tProbe Type                    : %s%n", probeType));
        sb.append(String.format("\tCell Type                     : %s%n", cellType));
        sb.append(String.format("\tAdjuvant                      : %s%n", adjuvant));
        sb.append(String.format("\tConcentration                 : %.2f%n", concentration));
        sb.append(String.format("\tThreshold                     : %.2f%n", threshold));

        if (tracks != null) {
            sb.append(String.format("\tNumber of tracks              : %d%n", tracks.size()));
        }
        if (squares != null) {
            sb.append(String.format("\tNumber of squares             : %d%n", squares.size()));
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