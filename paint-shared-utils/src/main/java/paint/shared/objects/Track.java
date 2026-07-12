/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.objects;

import tech.tablesaw.api.ColumnType;

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
     * @return the globally unique key for this track.
     */
    public String getUniqueKey() {
        return uniqueKey;
    }

    //=========================================================================
    // ACCESSORS & MUTATORS
    //=========================================================================

    /**
     * @param key the unique key to set.
     */
    public void setUniqueKey(String key) {
        this.uniqueKey = key;
    }

    /**
     * @return the name of the experiment.
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * @param name the experiment name to set.
     */
    public void setExperimentName(String name) {
        this.experimentName = name;
    }

    /**
     * @return the name of the recording.
     */
    public String getRecordingName() {
        return recordingName;
    }

    /**
     * @param name the recording name to set.
     */
    public void setRecordingName(String name) {
        this.recordingName = name;
    }

    /**
     * @return the TrackMate track ID.
     */
    public int getTrackId() {
        return trackId;
    }

    /**
     * @param id the track ID to set.
     */
    public void setTrackId(int id) {
        this.trackId = id;
    }

    /**
     * @return the total number of spots in this track.
     */
    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    /**
     * @param n the number of spots to set.
     */
    public void setNumberOfSpots(int n) {
        this.numberOfSpots = n;
    }

    /**
     * @return the total number of gaps in the track.
     */
    public int getNumberOfGaps() {
        return numberOfGaps;
    }

    /**
     * @param n the number of gaps to set.
     */
    public void setNumberOfGaps(int n) {
        this.numberOfGaps = n;
    }

    /**
     * @return the duration of the longest gap in frames.
     */
    public int getLongestGap() {
        return longestGap;
    }

    /**
     * @param n the longest gap to set.
     */
    public void setLongestGap(int n) {
        this.longestGap = n;
    }

    /**
     * @return the total duration of the track in seconds.
     */
    public double getTrackDuration() {
        return trackDuration;
    }

    /**
     * @param d the track duration to set.
     */
    public void setTrackDuration(double d) {
        this.trackDuration = d;
    }

    /**
     * @return the average X coordinate of the track.
     */
    public double getTrackXLocation() {
        return trackXLocation;
    }

    /**
     * @param x the X coordinate to set.
     */
    public void setTrackXLocation(double x) {
        this.trackXLocation = x;
    }

    /**
     * @return the average Y coordinate of the track.
     */
    public double getTrackYLocation() {
        return trackYLocation;
    }

    /**
     * @param y the Y coordinate to set.
     */
    public void setTrackYLocation(double y) {
        this.trackYLocation = y;
    }

    /**
     * @return the total displacement between start and end.
     */
    public double getTrackDisplacement() {
        return trackDisplacement;
    }

    /**
     * @param v the displacement to set.
     */
    public void setTrackDisplacement(double v) {
        this.trackDisplacement = v;
    }

    /**
     * @return the maximum instantaneous speed achieved.
     */
    public double getTrackMaxSpeed() {
        return trackMaxSpeed;
    }

    /**
     * @param v the max speed to set.
     */
    public void setTrackMaxSpeed(double v) {
        this.trackMaxSpeed = v;
    }

    /**
     * @return the median instantaneous speed.
     */
    public double getTrackMedianSpeed() {
        return trackMedianSpeed;
    }

    /**
     * @param v the median speed to set.
     */
    public void setTrackMedianSpeed(double v) {
        this.trackMedianSpeed = v;
    }

    /**
     * @return the computed short-term diffusion coefficient.
     */
    public double getDiffusionCoefficient() {
        return diffusionCoefficient;
    }

    /**
     * @param v the diffusion coefficient to set.
     */
    public void setDiffusionCoefficient(double v) {
        this.diffusionCoefficient = v;
    }

    /**
     * @return the extended diffusion coefficient.
     */
    public double getDiffusionCoefficientExt() {
        return diffusionCoefficientExt;
    }

    /**
     * @param v the extended diffusion coefficient to set.
     */
    public void setDiffusionCoefficientExt(double v) {
        this.diffusionCoefficientExt = v;
    }

    /**
     * @return the total distance traveled by the track.
     */
    public double getTotalDistance() {
        return totalDistance;
    }

    /**
     * @param v the total distance to set.
     */
    public void setTotalDistance(double v) {
        this.totalDistance = v;
    }

    /**
     * @return the confinement ratio (displacement / distance).
     */
    public double getConfinementRatio() {
        return confinementRatio;
    }

    /**
     * @param v the confinement ratio to set.
     */
    public void setConfinementRatio(double v) {
        this.confinementRatio = v;
    }

    /**
     * @return the index of the square containing this track.
     */
    public int getSquareNumber() {
        return squareNumber;
    }

    /**
     * @param n the square number to set.
     */
    public void setSquareNumber(int n) {
        this.squareNumber = n;
    }

    /**
     * @return the label index of the square containing this track.
     */
    public int getLabelNumber() {
        return labelNumber;
    }

    /**
     * @param n the label number to set.
     */
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
    }
}