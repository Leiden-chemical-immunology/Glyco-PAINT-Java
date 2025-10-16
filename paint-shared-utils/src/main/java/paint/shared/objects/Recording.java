package paint.shared.objects;

import tech.tablesaw.api.Table;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a single imaging or experimental recording within a Paint project.
 * <p>
 * A {@code Recording} stores metadata about experimental conditions,
 * quantitative analysis results (e.g., number of spots, tracks, tau, R², etc.),
 * and associations to related objects such as {@link Square} and {@link Track}.
 * </p>
 * <p>
 * Each recording corresponds to one row in the <b>All Recordings</b>  CSV files, and may
 * include references to derived data such as Tablesaw {@link Table} instances for track analysis.
 * </p>
 *
 * <h3>Core Responsibilities</h3>
 * <ul>
 *   <li>Maintain metadata about a single recording (probe, condition, threshold, etc.).</li>
 *   <li>Hold quantitative measurements produced by analysis.</li>
 *   <li>Reference associated {@link Square} and {@link Track} objects.</li>
 *   <li>Provide a human-readable summary via {@link #toString()}.</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> This class is not thread-safe. External synchronization
 * is required if instances are accessed from multiple threads.</p>
 */
public class Recording {

    // --- Core fields (columns in All Recordings/Experiment Info) ---
    // @formatter:off
    private String        recordingName;
    private int           conditionNumber;
    private int           replicateNumber;
    private String        probeName;
    private String        probeType;
    private String        cellType;
    private String        adjuvant;
    private double        concentration;
    private boolean       processFlag;               // renamed from doProcess
    private double        threshold;
    private int           numberOfSpots;
    private int           numberOfTracks;
    private int           numberOfTracksInBackground;
    private int           numberOfSquaresInBackground;
    private double        averageTracksInBackGround;
    private int           numberOfSpotsInAllTracks;
    private int           numberOfFrames;
    private double        runTime;
    private LocalDateTime timeStamp;
    private boolean       exclude;
    private double        tau;
    private double        rSquared;
    private double        density;

    private List<Square> squares = new ArrayList<>();
    private List<Track>  tracks  = new ArrayList<>();
    private Table        tracksTable;
    // @formatter:on

    /**
     * Creates an empty {@code Recording} with default values.
     */
    public Recording() {
    }

    /**
     * Creates a {@code Recording} initialized with its basic metadata.
     *
     * @param recordingName   the name or identifier of the recording
     * @param conditionNumber the associated experimental condition number
     * @param replicateNumber the replicate number within the experiment
     * @param probeName       the name of the probe used
     * @param probeType       the probe type (e.g., dye, antibody, etc.)
     * @param cellType        the cell type involved in the experiment
     * @param adjuvant        the adjuvant (if any) used in the experiment
     * @param concentration   the concentration of the probe or treatment
     * @param processFlag     whether this recording should be processed
     * @param threshold       the detection or filtering threshold
     */
    public Recording(String recordingName,
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

    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    public void setNumberOfSpots(int numberOfSpots) {
        this.numberOfSpots = numberOfSpots;
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public void setNumberOfTracks(int numberOfTracks) {
        this.numberOfTracks = numberOfTracks;
    }

    public int getNumberOfSpotsInAllTracks() {
        return numberOfSpotsInAllTracks;
    }

    public void setNumberOfSpotsInAllTracks(int numberOfSpotsInAllTracks) {
        this.numberOfSpotsInAllTracks = numberOfSpotsInAllTracks;
    }

    public int getNumberOfTracksInBackground() {
        return numberOfTracksInBackground;
    }

    public void setNumberOfTracksInBackground(int numberOfTracksInBackground) {
        this.numberOfTracksInBackground = numberOfTracksInBackground;
    }

    public int getNumberOfSquaresInBackground() {
        return numberOfSquaresInBackground;
    }

    public void setNumberOfSquaresInBackground(int numberOfSquaresInBackground) {
        this.numberOfSquaresInBackground = numberOfSquaresInBackground;
    }

    public double getAverageTracksInBackGround() {
        return averageTracksInBackGround;
    }

    public void setAverageTracksInBackGround(double averageTracksInBackGround) {
        this.averageTracksInBackGround = averageTracksInBackGround;
    }

    public int getNumberOfFrames() {
        return numberOfFrames;
    }

    public void setNumberOfFrames(int numberOfFrames) {
        this.numberOfFrames = numberOfFrames;
    }

    public double getRunTime() {
        return runTime;
    }

    public void setRunTime(double runTime) {
        this.runTime = runTime;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public double getTau() {
        return tau;
    }

    public void setTau(double tau) {
        this.tau = tau;
    }

    public double getRSquared() {
        return rSquared;
    }

    public void setRSquared(double rSquared) {
        this.rSquared = rSquared;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public List<Square> getSquaresOfRecording() {
        return squares;
    }

    public void setSquaresOfRecording(List<Square> squares) {
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

    public void addSquare(Square square) {
        this.squares.add(square);
    }

    public void addSquares(List<Square> squares) {
        this.squares.addAll(squares);
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    private static Boolean checkBooleanValue(String string) {
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t"));
        return yesValues.contains(string.trim().toLowerCase());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("\tRecording Information"));
        sb.append(String.format("\t                Recording Name                : %s%n", recordingName));
        sb.append(String.format("\t                Condition Nr                  : %d%n", conditionNumber));
        sb.append(String.format("\t                Replicate Nr                  : %d%n", replicateNumber));
        sb.append(String.format("\t                Probe Name                    : %s%n", probeName));
        sb.append(String.format("\t                Probe Type                    : %s%n", probeType));
        sb.append(String.format("\t                Cell Type                     : %s%n", cellType));
        sb.append(String.format("\t                Adjuvant                      : %s%n", adjuvant));
        sb.append(String.format("\t                Concentration                 : %.2f%n", concentration));
        sb.append(String.format("\t                Threshold                     : %.2f%n", threshold));
        sb.append(String.format("\t                Exclude                       : %b%n", exclude));
        sb.append(String.format("\t                Time Stamp                    : %s%n", timeStamp));
        sb.append(String.format("\t                Number of Spots               : %d%n", numberOfSpots));
        sb.append(String.format("\t                Number of Tracks              : %d%n", numberOfTracks));
        sb.append(String.format("\t                Number of Spots in All Tracks : %d%n", numberOfSpotsInAllTracks));
        sb.append(String.format("\t                Run Time                      : %.2f%n", runTime));
        sb.append(String.format("\t                Number of Frames              : %d%n", numberOfFrames));
        sb.append(String.format("\t                Tau                           : %.2f%n", tau));
        sb.append(String.format("\t                R Squared                     : %.2f%n", rSquared));
        sb.append(String.format("\t                Density                       : %.2f%n", density));

        if (tracks != null) {
            sb.append(String.format("\t                Number of tracks              : %d%n", tracks.size()));
        }
        if (squares != null) {
            sb.append(String.format("\t                Number of squares             : %d%n", squares.size()));
        }

        int numberOfSquaresWithTracks = 0;
        if (squares != null) {
            for (Square square : squares) {
                if (square.getTracks() != null && !square.getTracks().isEmpty()) {
                    numberOfSquaresWithTracks++;
                }
            }
        }
        sb.append(String.format("\tNumber of squares with tracks : %d%n", numberOfSquaresWithTracks));

        return sb.toString();
    }
}