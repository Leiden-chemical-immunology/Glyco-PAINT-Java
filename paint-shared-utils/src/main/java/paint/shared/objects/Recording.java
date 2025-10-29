/******************************************************************************
 *  Class:        Recording.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents metadata, measurements, and analysis results for a single
 *    recording within an experiment in the PAINT framework.
 *
 *  DESCRIPTION:
 *    The {@code Recording} class encapsulates all relevant information about
 *    a single experimental recording. This includes identifiers (experiment,
 *    condition, probe), numeric metrics (spot/track counts, thresholds, runtime),
 *    and references to associated {@link Square} and {@link Track} objects.
 *    It also optionally holds a {@link tech.tablesaw.api.Table} containing track data.
 *
 *  KEY FEATURES:
 *    • Holds metadata, numerical results, and object collections for a recording.
 *    • Supports experiment structure hierarchy via {@link Square} and {@link Track}.
 *    • Provides detailed and formatted summary output through {@link #toString()}.
 *    • Java 8–compliant design for compatibility with PAINT utilities.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.objects;

import tech.tablesaw.api.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents metadata and associated analysis data for a specific recording
 * within an experiment. This class includes measurement metrics, analysis
 * parameters, and relationships to {@link Square} and {@link Track} objects.
 */
public class Recording {

    // --- Core fields (columns in Recordings/Experiment Info) ---
    
    private String        experimentName;
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

    // ───────────────────────────────────────────────────────────────────────────────
    // ANALYSIS METRICS
    // ───────────────────────────────────────────────────────────────────────────────

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

    // ───────────────────────────────────────────────────────────────────────────────
    // ASSOCIATED OBJECTS
    // ───────────────────────────────────────────────────────────────────────────────

    private List<Square> squares = new ArrayList<>();
    private List<Track>  tracks  = new ArrayList<>();
    private Table        tracksTable;
    


    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    //
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates an empty {@code Recording} with default values.
     */
    public Recording() {
    }

    /**
     * Creates a {@code Recording} initialized with its basic metadata.
     *
     * @param experimentName  the experiment name
     * @param recordingName   the recording identifier
     * @param conditionNumber the condition number
     * @param replicateNumber the replicate number
     * @param probeName       the probe name
     * @param probeType       the probe type (e.g., dye, antibody)
     * @param cellType        the cell type
     * @param adjuvant        the adjuvant, if any
     * @param concentration   the probe/treatment concentration
     * @param processFlag     whether this recording should be processed
     * @param threshold       threshold value for analysis
     */
    public Recording(String  experimentName,
                     String  recordingName,
                     int     conditionNumber,
                     int     replicateNumber,
                     String  probeName,
                     String  probeType,
                     String  cellType,
                     String  adjuvant,
                     double  concentration,
                     boolean processFlag,
                     double  threshold) {
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
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS AND MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

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

    public int getNumberOfSpotsInAllTracks() {
        return numberOfSpotsInAllTracks;
    }

    public void setNumberOfSpotsInAllTracks(int numberOfSpotsInAllTracks) {
        this.numberOfSpotsInAllTracks = numberOfSpotsInAllTracks;
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

    // ───────────────────────────────────────────────────────────────────────────────
    // CONVENIENCE METHODS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a {@link Square} to this recording.
     */
    public void addSquare(Square square) {
        this.squares.add(square);
    }

    /**
     * Adds a list of {@link Square} objects to this recording.
     */
    public void addSquares(List<Square> squares) {
        this.squares.addAll(squares);
    }

    /**
     * Adds a {@link Track} to this recording.
     */
    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // STRING REPRESENTATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a formatted string representation of this recording and its key data.
     *
     * @return formatted string containing recording details
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\tRecording Information");
        sb.append(String.format("\t                Experiment Name               : %s%n", experimentName));
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