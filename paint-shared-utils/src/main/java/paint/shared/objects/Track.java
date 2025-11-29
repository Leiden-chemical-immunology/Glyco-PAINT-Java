/*=============================================================================
 *  Class:        Track.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents a single molecular trajectory ("track") detected in a PAINT
 *    recording. A track consists of a series of spatial positions over time,
 *    from which a wide range of analytical metrics are derived:
 *
 *       • Displacement and total distance
 *       • Max/median speed
 *       • Diffusion coefficients
 *       • Gaps, duration, confinement ratios
 *
 *    The class also stores key metadata tying the track to its parent
 *    experiment, recording, and spatial region (square).
 *
 *  DESCRIPTION:
 *    This version embeds its own schema via the {@link Column} enum, replacing
 *    the external TrackSchema. Tablesaw-based IO now uses Track.Column.values()
 *    for both CSV header names and column types.
 *
 *  KEY FEATURES:
 *    • Embedded, fully self-describing schema enum
 *    • Complete set of motion and diffusion metrics
 *    • Grid/region association via square and label numbers
 *    • Clean, consistent toString() summary for diagnostics
 *    • Fully compatible with Tablesaw 0.43+ and Java 8
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-30
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.objects;

import tech.tablesaw.api.ColumnType;

import static paint.shared.utils.Miscellaneous.initialiseDoublesToNaN;

/**
 * Represents a single molecular trajectory extracted from a PAINT recording.
 * <p>
 * Tracks contain both raw geometric motion information and higher-level
 * analytics computed from those motions.
 */
public class Track {

    //=========================================================================
    // CORE TRACK ATTRIBUTES
    //=========================================================================

    private String uniqueKey;
    private String experimentName;
    private String recordingName;

    private int    trackId;
    private int    numberOfSpots;
    private int    numberOfGaps;
    private int    longestGap;

    private double trackDuration;
    private double trackXLocation;
    private double trackYLocation;
    private double trackDisplacement;
    private double trackMaxSpeed;
    private double trackMedianSpeed;

    private double diffusionCoefficient;
    private double diffusionCoefficientExt;
    private double totalDistance;
    private double confinementRatio;

    private int    squareNumber;
    private int    labelNumber;

    //=========================================================================
    //  CONSTRUCTORS
    //=========================================================================

    /** Creates an empty, uninitialized track. */
    public Track() { }

    /**
     * Fully initializes a {@code Track} with all metadata and motion parameters.
     */
    public Track(String uniqueKey,
            String experimentName,
            String recordingName,
            int trackId,
            int numberOfSpots,
            int numberOfGaps,
            int longestGap,
            double trackDuration,
            double trackXLocation,
            double trackYLocation,
            double trackDisplacement,
            double trackMaxSpeed,
            double trackMedianSpeed,
            double diffusionCoefficient,
            double diffusionCoefficientExt,
            double totalDistance,
            double confinementRatio,
            int squareNumber,
            int labelNumber) {

        initialiseDoublesToNaN(this);

        this.uniqueKey = uniqueKey;
        this.experimentName = experimentName;
        this.recordingName = recordingName;
        this.trackId = trackId;
        this.numberOfSpots = numberOfSpots;
        this.numberOfGaps = numberOfGaps;
        this.longestGap = longestGap;
        this.trackDuration = trackDuration;
        this.trackXLocation = trackXLocation;
        this.trackYLocation = trackYLocation;
        this.trackDisplacement = trackDisplacement;
        this.trackMaxSpeed = trackMaxSpeed;
        this.trackMedianSpeed = trackMedianSpeed;
        this.diffusionCoefficient = diffusionCoefficient;
        this.diffusionCoefficientExt = diffusionCoefficientExt;
        this.totalDistance = totalDistance;
        this.confinementRatio = confinementRatio;
        this.squareNumber = squareNumber;
        this.labelNumber = labelNumber;
    }

    public String getUniqueKey() {
        return uniqueKey;
    }

    //=========================================================================
    // ACCESSORS & MUTATORS
    //=========================================================================

    public void setUniqueKey(String key) {
        this.uniqueKey = key;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String name) {
        this.experimentName = name;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String name) {
        this.recordingName = name;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int id) {
        this.trackId = id;
    }

    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    public void setNumberOfSpots(int n) {
        this.numberOfSpots = n;
    }

    public int getNumberOfGaps() {
        return numberOfGaps;
    }

    public void setNumberOfGaps(int n) {
        this.numberOfGaps = n;
    }

    public int getLongestGap() {
        return longestGap;
    }

    public void setLongestGap(int n) {
        this.longestGap = n;
    }

    public double getTrackDuration() {
        return trackDuration;
    }

    public void setTrackDuration(double d) {
        this.trackDuration = d;
    }

    public double getTrackXLocation() {
        return trackXLocation;
    }

    public void setTrackXLocation(double x) {
        this.trackXLocation = x;
    }

    public double getTrackYLocation() {
        return trackYLocation;
    }

    public void setTrackYLocation(double y) {
        this.trackYLocation = y;
    }

    public double getTrackDisplacement() {
        return trackDisplacement;
    }

    public void setTrackDisplacement(double v) {
        this.trackDisplacement = v;
    }

    public double getTrackMaxSpeed() {
        return trackMaxSpeed;
    }

    public void setTrackMaxSpeed(double v) {
        this.trackMaxSpeed = v;
    }

    public double getTrackMedianSpeed() {
        return trackMedianSpeed;
    }

    public void setTrackMedianSpeed(double v) {
        this.trackMedianSpeed = v;
    }

    public double getDiffusionCoefficient() {
        return diffusionCoefficient;
    }

    public void setDiffusionCoefficient(double v) {
        this.diffusionCoefficient = v;
    }

    public double getDiffusionCoefficientExt() {
        return diffusionCoefficientExt;
    }

    public void setDiffusionCoefficientExt(double v) {
        this.diffusionCoefficientExt = v;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double v) {
        this.totalDistance = v;
    }

    public double getConfinementRatio() {
        return confinementRatio;
    }

    public void setConfinementRatio(double v) {
        this.confinementRatio = v;
    }

    public int getSquareNumber() {
        return squareNumber;
    }

    public void setSquareNumber(int n) {
        this.squareNumber = n;
    }

    public int getLabelNumber() {
        return labelNumber;
    }

    public void setLabelNumber(int n) {
        this.labelNumber = n;
    }

    /**
     * Returns a concise, human-readable summary of the track.
     */
    @Override
    public String toString() {
        return String.format(
                "Track[id=%d, experiment=%s, recording=%s, spots=%d, duration=%.2f, displacement=%.2f, maxSpeed=%.2f, medianSpeed=%.2f]",
                trackId,
                experimentName,
                recordingName,
                numberOfSpots,
                trackDuration,
                trackDisplacement,
                trackMaxSpeed,
                trackMedianSpeed
        );
    }

    //=========================================================================
    // EMBEDDED SCHEMA ENUM
    //=========================================================================

    public enum Column {

        UNIQUE_KEY(                "Unique Key",                ColumnType.STRING),
        EXPERIMENT_NAME(           "Experiment Name",           ColumnType.STRING),
        RECORDING_NAME(            "Recording Name",            ColumnType.STRING),
        TRACK_ID(                  "Track Id",                  ColumnType.INTEGER),
        NUMBER_OF_SPOTS(           "Number of Spots",           ColumnType.INTEGER),
        NUMBER_OF_GAPS(            "Number of Gaps",            ColumnType.INTEGER),
        LONGEST_GAP(               "Longest Gap",               ColumnType.INTEGER),
        TRACK_DURATION(            "Track Duration",            ColumnType.DOUBLE),
        TRACK_X_LOCATION(          "Track X Location",          ColumnType.DOUBLE),
        TRACK_Y_LOCATION(          "Track Y Location",          ColumnType.DOUBLE),
        TRACK_DISPLACEMENT(        "Track Displacement",        ColumnType.DOUBLE),
        TRACK_MAX_SPEED(           "Track Max Speed",           ColumnType.DOUBLE),
        TRACK_MEDIAN_SPEED(        "Track Median Speed",        ColumnType.DOUBLE),
        DIFFUSION_COEFFICIENT(     "Diffusion Coefficient",     ColumnType.DOUBLE),
        DIFFUSION_COEFFICIENT_EXT( "Diffusion Coefficient Ext", ColumnType.DOUBLE),
        TOTAL_DISTANCE(            "Total Distance",            ColumnType.DOUBLE),
        CONFINEMENT_RATIO(         "Confinement Ratio",         ColumnType.DOUBLE),
        SQUARE_NUMBER(             "Square Number",             ColumnType.INTEGER),
        LABEL_NUMBER(              "Label Number",              ColumnType.INTEGER);

        public final String     header;
        public final ColumnType type;

        Column(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }

        /**
         * Returns the zero-based column index.
         */
        public int index() {
            return ordinal();
        }
    }
}