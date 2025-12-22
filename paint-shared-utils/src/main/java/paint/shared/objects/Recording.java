/*=============================================================================
 *  Class:        Recording.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents all metadata, analysis results, filter thresholds, and
 *    associated objects for a single PAINT recording. Each Recording
 *    encapsulates experiment-level metadata (probe, adjuvant, replicate, etc.)
 *    as well as all analysis metrics computed during the Generate Squares
 *    workflow.
 *
 *  DESCRIPTION:
 *    • Defines the unified CSV schema through the embedded Column enum
 *      (replacing RecordingSchema.* entirely).
 *    • Stores both experiment metadata and analysis-derived metrics.
 *    • Holds child objects (Squares, Tracks) and their Tablesaw tables.
 *    • Used throughout Generate Squares, TrackMate post-processing, filtering,
 *      and validation.
 *
 *    This class is serialised/deserialised by:
 *        – RecordingsTableIO
 *        – MainIOInterface  (public I/O façade)
 *
 *  RESPONSIBILITIES:
 *    • Provide a schema definition for recording-level CSV files.
 *    • Store experiment metadata and derived analytical metrics.
 *    • Store filter parameters used for determining visible squares.
 *    • Provide getters/setters used by the Generate Squares pipeline.
 *    • Serve as the container for associated Square and Track objects.
 *
 *  USAGE EXAMPLE:
 *      Recording rec = new Recording("ExpA", "R01", 1, 1,
 *                                    "AF647", "Dye", "T-cell", "None",
 *                                    1.0, true, 25.0);
 *      rec.setDensity(0.34);
 *      rec.getSquaresOfRecording().add(square);
 *
 *  DEPENDENCIES:
 *    – tech.tablesaw.api.Table
 *    – paint.shared.objects.{Square, Track}
 *    – paint.shared.io.internal.RecordingsTableIO
 *    – paint.shared.io.MainIOInterface
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.objects;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents all metadata and analytical metrics associated with a single
 * recording in a PAINT experiment.
 *
 * <p>This class embeds its own schema via {@link Recording.Column}, removing
 * the dependency on the old RecordingSchema class. The schema is used by the
 * I/O layer to read/write CSV tables in a strict and stable manner.</p>
 */
public class Recording {

    // ============================================================================
    //  CORE METADATA FIELDS
    // ============================================================================

    private String        experimentName;
    private String        recordingName;
    private int           conditionNumber;
    private int           replicateNumber;
    private String        probeName;
    private String        probeType;
    private String        cellType;
    private String        adjuvant;
    private double        concentration;
    private boolean       processFlag;
    private double        threshold;

    // ============================================================================
    //  ANALYSIS METRICS
    // ============================================================================

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

    // ============================================================================
    //  FILTER PARAMETERS
    // ============================================================================
    private double         minRequiredDensityRatio;
    private double         minRequiredRSquared;
    private double         maxAllowableVariability;
    private String         neighbourMode;

    // ============================================================================
    //  ASSOCIATED OBJECTS
    // ============================================================================
    private List<Square> squares = new ArrayList<>();
    private List<Track>  tracks  = new ArrayList<>();
    private Table        tracksTable;
    /**
     * Creates an empty Recording.
     */
    public Recording() {
    }

    // ============================================================================
    //  CONSTRUCTORS
    // ============================================================================

    public String getExperimentName() {
        return experimentName;
    }

    // ============================================================================
    //  GETTERS / SETTERS  (GENERATED, UNCHANGED)
    // ============================================================================

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

    public boolean isProcessFlagSet() {
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

    public boolean isExcluded() {
        return exclude;
    }

    public void setExcluded(boolean exclude) {
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

    public void setTracksList(List<Track> tracks) {
        this.tracks = tracks;
    }

    public Table getTracksTable() {
        return tracksTable;
    }

    public void setTracksTable(Table tracksTable) {
        this.tracksTable = tracksTable;
    }

    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    public void setMinRequiredDensityRatio(double minRequiredDensityRatio) {
        this.minRequiredDensityRatio = minRequiredDensityRatio;
    }

    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    public void setMinRequiredRSquared(double minRequiredRSquared) {
        this.minRequiredRSquared = minRequiredRSquared;
    }

    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    public void setMaxAllowableVariability(double maxAllowableVariability) {
        this.maxAllowableVariability = maxAllowableVariability;
    }

    public String getNeighbourMode() {
        return neighbourMode;
    }

    public void setNeighbourMode(String neighbourMode) {
        this.neighbourMode = neighbourMode;
    }

    /**
     * Append multiple squares into this recording.
     */
    public void addSquares(List<Square> squares) {
        this.squares.addAll(squares);
    }

    // ============================================================================
    //  CONVENIENCE
    // ============================================================================

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
            if (numberOfSquaresWithTracks > 0) {
                sb.append(String.format("\t                Number of squares with tracks  : %d%n", numberOfSquaresWithTracks));
            }
        }

        sb.append(String.format("\t                Min Required Density Ratio     : %.4f%n", minRequiredDensityRatio));
        sb.append(String.format("\t                Min Required R Squared         : %.4f%n", minRequiredRSquared));
        sb.append(String.format("\t                Max Allowable Variability      : %.4f%n", maxAllowableVariability));
        sb.append(String.format("\t                Neighbour Mode                 : %s%n", neighbourMode));

        return sb.toString();
    }

    /**
     * Defines the unified CSV schema for a Recording.
     * Each enum constant corresponds to one column header and its type.
     */
    public enum Column {

        // --- Experiment metadata ---
        EXPERIMENT_NAME(                 "Experiment Name",                 ColumnType.STRING),
        RECORDING_NAME(                  "Recording Name",                  ColumnType.STRING),
        CONDITION_NUMBER(                "Condition Number",                ColumnType.INTEGER),
        REPLICATE_NUMBER(                "Replicate Number",                ColumnType.INTEGER),
        PROBE_NAME(                      "Probe Name",                      ColumnType.STRING),
        PROBE_TYPE(                      "Probe Type",                      ColumnType.STRING),
        CELL_TYPE(                       "Cell Type",                       ColumnType.STRING),
        ADJUVANT(                        "Adjuvant",                        ColumnType.STRING),
        CONCENTRATION(                   "Concentration",                   ColumnType.DOUBLE),
        PROCESS_FLAG(                    "Process Flag",                    ColumnType.BOOLEAN),
        THRESHOLD(                       "Threshold",                       ColumnType.DOUBLE),

        // --- Derived analysis metrics ---
        NUMBER_OF_SPOTS(                 "Number of Spots",                 ColumnType.INTEGER),
        NUMBER_OF_TRACKS(                "Number of Tracks",                ColumnType.INTEGER),
        NUMBER_OF_TRACKS_IN_BACKGROUND(  "Number of Tracks in Background",  ColumnType.INTEGER),
        NUMBER_OF_SQUARES_IN_BACKGROUND( "Number of Squares in Background", ColumnType.INTEGER),
        AVERAGE_TRACKS_IN_BACKGROUND(    "Average Tracks in Background",    ColumnType.DOUBLE),
        NUMBER_OF_SPOTS_IN_ALL_TRACKS(   "Number of Spots in All Tracks",   ColumnType.INTEGER),
        NUMBER_OF_FRAMES(                "Number of Frames",                ColumnType.INTEGER),
        RUN_TIME(                        "Run Time",                        ColumnType.DOUBLE),
        TIME_STAMP(                      "Time Stamp",                      ColumnType.LOCAL_DATE_TIME),
        EXCLUDE(                         "Exclude",                         ColumnType.BOOLEAN),
        TAU(                             "Tau",                             ColumnType.DOUBLE),
        R_SQUARED(                       "R Squared",                       ColumnType.DOUBLE),
        DENSITY(                         "Density",                         ColumnType.DOUBLE),

        // --- Filter / configuration values ---
        MIN_REQUIRED_DENSITY_RATIO(      "Min Required Density Ratio",      ColumnType.DOUBLE),
        MIN_REQUIRED_R_SQUARED(          "Min Required R Squared",          ColumnType.DOUBLE),
        MAX_ALLOWABLE_VARIABILITY(       "Max Allowable Variability",       ColumnType.DOUBLE),
        NEIGHBOUR_MODE(                  "Neighbour Mode",                  ColumnType.STRING);

        public final String     header;
        public final ColumnType type;

        Column(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }
}