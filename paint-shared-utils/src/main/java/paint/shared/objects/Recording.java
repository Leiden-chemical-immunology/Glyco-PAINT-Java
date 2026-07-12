/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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

    /**
     * @return the name of the experiment this recording belongs to.
     */
    public String getExperimentName() {
        return experimentName;
    }

    // ============================================================================
    //  GETTERS / SETTERS  (GENERATED, UNCHANGED)
    // ============================================================================

    /**
     * @param experimentName the name of the experiment this recording belongs to.
     */
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    /**
     * @return the unique name of this recording.
     */
    public String getRecordingName() {
        return recordingName;
    }

    /**
     * @param recordingName the unique name of this recording.
     */
    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    /**
     * @return the condition number associated with this recording.
     */
    public int getConditionNumber() {
        return conditionNumber;
    }

    /**
     * @param conditionNumber the condition number associated with this recording.
     */
    public void setConditionNumber(int conditionNumber) {
        this.conditionNumber = conditionNumber;
    }

    /**
     * @return the replicate number of this recording.
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * @param replicateNumber the replicate number of this recording.
     */
    public void setReplicateNumber(int replicateNumber) {
        this.replicateNumber = replicateNumber;
    }

    /**
     * @return the name of the probe used in this recording.
     */
    public String getProbeName() {
        return probeName;
    }

    /**
     * @param probeName the name of the probe used in this recording.
     */
    public void setProbeName(String probeName) {
        this.probeName = probeName;
    }

    /**
     * @return the type of probe used.
     */
    public String getProbeType() {
        return probeType;
    }

    /**
     * @param probeType the type of probe used.
     */
    public void setProbeType(String probeType) {
        this.probeType = probeType;
    }

    /**
     * @return the cell type used in this experiment.
     */
    public String getCellType() {
        return cellType;
    }

    /**
     * @param cellType the cell type used in this experiment.
     */
    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    /**
     * @return the adjuvant used in this recording.
     */
    public String getAdjuvant() {
        return adjuvant;
    }

    /**
     * @param adjuvant the adjuvant used in this recording.
     */
    public void setAdjuvant(String adjuvant) {
        this.adjuvant = adjuvant;
    }

    /**
     * @return the probe concentration.
     */
    public double getConcentration() {
        return concentration;
    }

    /**
     * @param concentration the probe concentration.
     */
    public void setConcentration(double concentration) {
        this.concentration = concentration;
    }

    /**
     * @return true if the process flag is set for this recording.
     */
    public boolean isProcessFlagSet() {
        return processFlag;
    }

    /**
     * @param processFlag whether to set the process flag.
     */
    public void setProcessFlag(boolean processFlag) {
        this.processFlag = processFlag;
    }

    /**
     * @return the analysis threshold used.
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the analysis threshold to set.
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * @return the total number of spots detected.
     */
    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    /**
     * @param numberOfSpots the total number of spots detected.
     */
    public void setNumberOfSpots(int numberOfSpots) {
        this.numberOfSpots = numberOfSpots;
    }

    /**
     * @return the total number of tracks identified.
     */
    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    /**
     * @param numberOfTracks the total number of tracks identified.
     */
    public void setNumberOfTracks(int numberOfTracks) {
        this.numberOfTracks = numberOfTracks;
    }

    /**
     * @return the number of tracks identified in the background area.
     */
    public int getNumberOfTracksInBackground() {
        return numberOfTracksInBackground;
    }

    /**
     * @param numberOfTracksInBackground the number of tracks in the background.
     */
    public void setNumberOfTracksInBackground(int numberOfTracksInBackground) {
        this.numberOfTracksInBackground = numberOfTracksInBackground;
    }

    /**
     * @return the number of squares designated as background.
     */
    public int getNumberOfSquaresInBackground() {
        return numberOfSquaresInBackground;
    }

    /**
     * @param numberOfSquaresInBackground the number of squares in the background.
     */
    public void setNumberOfSquaresInBackground(int numberOfSquaresInBackground) {
        this.numberOfSquaresInBackground = numberOfSquaresInBackground;
    }

    /**
     * @return the average track count per background square.
     */
    public double getAverageTracksInBackGround() {
        return averageTracksInBackGround;
    }

    /**
     * @param averageTracksInBackGround the average background track count.
     */
    public void setAverageTracksInBackGround(double averageTracksInBackGround) {
        this.averageTracksInBackGround = averageTracksInBackGround;
    }

    /**
     * @return the total number of spots across all tracks.
     */
    public int getNumberOfSpotsInAllTracks() {
        return numberOfSpotsInAllTracks;
    }

    /**
     * @param numberOfSpotsInAllTracks the total number of spots in tracks.
     */
    public void setNumberOfSpotsInAllTracks(int numberOfSpotsInAllTracks) {
        this.numberOfSpotsInAllTracks = numberOfSpotsInAllTracks;
    }

    /**
     * @return the total number of frames in the movie.
     */
    public int getNumberOfFrames() {
        return numberOfFrames;
    }

    /**
     * @param numberOfFrames the total number of frames.
     */
    public void setNumberOfFrames(int numberOfFrames) {
        this.numberOfFrames = numberOfFrames;
    }

    /**
     * @return the total acquisition time in seconds.
     */
    public double getRunTime() {
        return runTime;
    }

    /**
     * @param runTime the total acquisition time.
     */
    public void setRunTime(double runTime) {
        this.runTime = runTime;
    }

    /**
     * @return the timestamp of the analysis.
     */
    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    /**
     * @param timeStamp the timestamp of the analysis.
     */
    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * @return true if this recording is excluded from final analysis.
     */
    public boolean isExcluded() {
        return exclude;
    }

    /**
     * @param exclude whether to exclude this recording.
     */
    public void setExcluded(boolean exclude) {
        this.exclude = exclude;
    }

    /**
     * @return the computed Tau value for the recording.
     */
    public double getTau() {
        return tau;
    }

    /**
     * @param tau the computed Tau value.
     */
    public void setTau(double tau) {
        this.tau = tau;
    }

    /**
     * @return the R-squared value of the binding kinetics fit.
     */
    public double getRSquared() {
        return rSquared;
    }

    /**
     * @param rSquared the R-squared value.
     */
    public void setRSquared(double rSquared) {
        this.rSquared = rSquared;
    }

    /**
     * @return the computed binding density.
     */
    public double getDensity() {
        return density;
    }

    /**
     * @param density the binding density.
     */
    public void setDensity(double density) {
        this.density = density;
    }

    /**
     * @return the list of {@link Square} objects associated with this recording.
     */
    public List<Square> getSquaresOfRecording() {
        return squares;
    }

    /**
     * @param squares the list of squares.
     */
    public void setSquaresOfRecording(List<Square> squares) {
        this.squares = squares;
    }

    /**
     * @param tracks the list of tracks.
     */
    public void setTracksList(List<Track> tracks) {
        this.tracks = tracks;
    }

    /**
     * @return the {@link Table} of track data.
     */
    public Table getTracksTable() {
        return tracksTable;
    }

    /**
     * @param tracksTable the track data table.
     */
    public void setTracksTable(Table tracksTable) {
        this.tracksTable = tracksTable;
    }

    /**
     * @return minimum required density ratio for filtering.
     */
    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    /**
     * @param minRequiredDensityRatio the minimum density ratio threshold.
     */
    public void setMinRequiredDensityRatio(double minRequiredDensityRatio) {
        this.minRequiredDensityRatio = minRequiredDensityRatio;
    }

    /**
     * @return minimum required R-squared for filtering.
     */
    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    /**
     * @param minRequiredRSquared the minimum R-squared threshold.
     */
    public void setMinRequiredRSquared(double minRequiredRSquared) {
        this.minRequiredRSquared = minRequiredRSquared;
    }

    /**
     * @return maximum allowable variability for filtering.
     */
    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    /**
     * @param maxAllowableVariability the maximum variability threshold.
     */
    public void setMaxAllowableVariability(double maxAllowableVariability) {
        this.maxAllowableVariability = maxAllowableVariability;
    }

    /**
     * @return the mode used for determining background neighbors.
     */
    public String getNeighbourMode() {
        return neighbourMode;
    }

    /**
     * @param neighbourMode the neighbor mode to set.
     */
    public void setNeighbourMode(String neighbourMode) {
        this.neighbourMode = neighbourMode;
    }

    /**
     * Appends a list of {@link Square} objects to this recording's internal square collection.
     *
     * @param squares the list of squares to add
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

        sb.append(String.format("\t                Min Required Density Ratio    : %.2f%n", minRequiredDensityRatio));
        sb.append(String.format("\t                Min Required R Squared        : %.2f%n", minRequiredRSquared));
        sb.append(String.format("\t                Max Allowable Variability     : %.2f%n", maxAllowableVariability));
        sb.append(String.format("\t                Neighbour Mode                : %s%n", neighbourMode));

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