package paint.shared.objects;

/**
 * Represents a single tracked trajectory (track) within a recording.
 * <p>
 * A {@code Track} aggregates various properties that describe its identity,
 * statistics about detected spots, dynamics such as displacement, speed,
 * and diffusion coefficients, as well as contextual information such as
 * square and label numbers.
 * </p>
 */
public class Track {

    /** Unique identifier for this track (often combines recording and track ID). */
    private String uniqueKey;

    /** Name of the recording this track belongs to. */
    private String recordingName;

    /** Numerical identifier of the track within the recording. */
    private int trackId;

    /** Human-readable label assigned to the track. */
    private String trackLabel;

    /** Total number of spots detected in this track. */
    private int numberOfSpots;

    /** Number of gaps detected between spots in the track. */
    private int numberOfGaps;

    /** Longest continuous gap length in the track. */
    private int longestGap;

    /** Total duration of the track, in time units (e.g., seconds or frames). */
    private double trackDuration;

    /** X-coordinate of the track’s central or average position. */
    private double trackXLocation;

    /** Y-coordinate of the track’s central or average position. */
    private double trackYLocation;

    /** Net displacement of the track from start to end. */
    private double trackDisplacement;

    /** Maximum speed observed in this track. */
    private double trackMaxSpeed;

    /** Median speed observed across the track. */
    private double trackMedianSpeed;

    /** Estimated diffusion coefficient of the track. */
    private double diffusionCoefficient;

    /** Extended diffusion coefficient (alternative or refined calculation). */
    private double diffusionCoefficientExt;

    /** Total distance traveled along the track. */
    private double totalDistance;

    /** Confinement ratio, describing constrained vs. free motion. */
    private double confinementRatio;

    /** Index of the square this track belongs to (for spatial partitioning). */
    private int squareNumber;

    /** Label number used for annotation or classification. */
    private int labelNumber;

    /**
     * Default no-argument constructor.
     * Initializes a {@code Track} with unset or zero values.
     */
    public Track() {}

    /**
     * Full constructor to initialize all fields of a {@code Track}.
     *
     * @param uniqueKey unique identifier for the track
     * @param recordingName name of the recording the track belongs to
     * @param trackId numerical track identifier
     * @param trackLabel descriptive label for the track
     * @param numberOfSpots number of spots in the track
     * @param numberOfGaps number of gaps in the track
     * @param longestGap longest gap between spots
     * @param trackDuration total duration of the track
     * @param trackXLocation average X location
     * @param trackYLocation average Y location
     * @param trackDisplacement net displacement from start to end
     * @param trackMaxSpeed maximum speed recorded
     * @param trackMedianSpeed median speed recorded
     * @param diffusionCoefficient diffusion coefficient
     * @param diffusionCoefficientExt extended diffusion coefficient
     * @param totalDistance total distance traveled
     * @param confinementRatio ratio of displacement to total distance
     * @param squareNumber square index containing this track
     * @param labelNumber label index used for annotation
     */
    public Track(String uniqueKey,
                 String recordingName,
                 int trackId,
                 String trackLabel,
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
        this.uniqueKey = uniqueKey;
        this.recordingName = recordingName;
        this.trackId = trackId;
        this.trackLabel = trackLabel;
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

    // --- Getters and Setters ---
    // (JavaDoc for each omitted here for brevity, but you could add
    //  short descriptions like "Gets the track label" or "Sets the max speed".)

    public String getUniqueKey() { return uniqueKey; }
    public void setUniqueKey(String uniqueKey) { this.uniqueKey = uniqueKey; }

    public String getRecordingName() { return recordingName; }
    public void setRecordingName(String recordingName) { this.recordingName = recordingName; }

    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }

    public String getTrackLabel() { return trackLabel; }
    public void setTrackLabel(String trackLabel) { this.trackLabel = trackLabel; }

    public int getNumberOfSpots() { return numberOfSpots; }
    public void setNumberOfSpots(int numberOfSpots) { this.numberOfSpots = numberOfSpots; }

    public int getNumberOfGaps() { return numberOfGaps; }
    public void setNumberOfGaps(int numberOfGaps) { this.numberOfGaps = numberOfGaps; }

    public int getLongestGap() { return longestGap; }
    public void setLongestGap(int longestGap) { this.longestGap = longestGap; }

    public double getTrackDuration() { return trackDuration; }
    public void setTrackDuration(double trackDuration) { this.trackDuration = trackDuration; }

    public double getTrackXLocation() { return trackXLocation; }
    public void setTrackXLocation(double trackXLocation) { this.trackXLocation = trackXLocation; }

    public double getTrackYLocation() { return trackYLocation; }
    public void setTrackYLocation(double trackYLocation) { this.trackYLocation = trackYLocation; }

    public double getTrackDisplacement() { return trackDisplacement; }
    public void setTrackDisplacement(double trackDisplacement) { this.trackDisplacement = trackDisplacement; }

    public double getTrackMaxSpeed() { return trackMaxSpeed; }
    public void setTrackMaxSpeed(double trackMaxSpeed) { this.trackMaxSpeed = trackMaxSpeed; }

    public double getTrackMedianSpeed() { return trackMedianSpeed; }
    public void setTrackMedianSpeed(double trackMedianSpeed) { this.trackMedianSpeed = trackMedianSpeed; }

    public double getDiffusionCoefficient() { return diffusionCoefficient; }
    public void setDiffusionCoefficient(double diffusionCoefficient) { this.diffusionCoefficient = diffusionCoefficient; }

    public double getDiffusionCoefficientExt() { return diffusionCoefficientExt; }
    public void setDiffusionCoefficientExt(double diffusionCoefficientExt) { this.diffusionCoefficientExt = diffusionCoefficientExt; }

    public double getTotalDistance() { return totalDistance; }
    public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }

    public double getConfinementRatio() { return confinementRatio; }
    public void setConfinementRatio(double confinementRatio) { this.confinementRatio = confinementRatio; }

    public int getSquareNumber() { return squareNumber; }
    public void setSquareNumber(int squareNumber) { this.squareNumber = squareNumber; }

    public int getLabelNumber() { return labelNumber; }
    public void setLabelNumber(int labelNumber) { this.labelNumber = labelNumber; }

    /**
     * Returns a concise string representation of this track.
     *
     * @return a summary including ID, label, recording name,
     *         number of spots, duration, displacement, and speed metrics.
     */
    @Override
    public String toString() {
        return String.format(
                "Track[id=%d, label=%s, recording=%s, spots=%d, duration=%.2f, displacement=%.2f, maxSpeed=%.2f, medianSpeed=%.2f]",
                trackId, trackLabel, recordingName, numberOfSpots, trackDuration, trackDisplacement, trackMaxSpeed, trackMedianSpeed
        );
    }
}