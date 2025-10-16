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

    // @formatter:off
    private String uniqueKey;
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
    // @formatter:on

    /**
     * Default no-argument constructor. Initializes unset values.
     *
     */
    public Track() {
    }

    /**
     * Full constructor to initialize all fields of a {@code Track}.
     *
     * @param uniqueKey               unique identifier for the track
     * @param recordingName           name of the recording the track belongs to
     * @param trackId                 numerical track identifier
     * @param trackLabel              descriptive label for the track
     * @param numberOfSpots           number of spots in the track
     * @param numberOfGaps            number of gaps in the track
     * @param longestGap              longest gap between spots
     * @param trackDuration           total duration of the track
     * @param trackXLocation          average X location
     * @param trackYLocation          average Y location
     * @param trackDisplacement       net displacement from start to end
     * @param trackMaxSpeed           maximum speed recorded
     * @param trackMedianSpeed        median speed recorded
     * @param diffusionCoefficient    diffusion coefficient
     * @param diffusionCoefficientExt extended diffusion coefficient
     * @param totalDistance           total distance traveled
     * @param confinementRatio        ratio of displacement to total distance
     * @param squareNumber            square index containing this track
     * @param labelNumber             label index used for annotation
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

        // @formatter:off
        this.uniqueKey               = uniqueKey;
        this.recordingName           = recordingName;
        this.trackId                 = trackId;
        this.numberOfSpots           = numberOfSpots;
        this.numberOfGaps            = numberOfGaps;
        this.longestGap              = longestGap;
        this.trackDuration           = trackDuration;
        this.trackXLocation          = trackXLocation;
        this.trackYLocation          = trackYLocation;
        this.trackDisplacement       = trackDisplacement;
        this.trackMaxSpeed           = trackMaxSpeed;
        this.trackMedianSpeed        = trackMedianSpeed;
        this.diffusionCoefficient    = diffusionCoefficient;
        this.diffusionCoefficientExt = diffusionCoefficientExt;
        this.totalDistance           = totalDistance;
        this.confinementRatio        = confinementRatio;
        this.squareNumber            = squareNumber;
        this.labelNumber             = labelNumber;
        // @formatter:on
    }

    // --- Getters and Setters ---

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    public void setNumberOfSpots(int numberOfSpots) {
        this.numberOfSpots = numberOfSpots;
    }

    public int getNumberOfGaps() {
        return numberOfGaps;
    }

    public void setNumberOfGaps(int numberOfGaps) {
        this.numberOfGaps = numberOfGaps;
    }

    public int getLongestGap() {
        return longestGap;
    }

    public void setLongestGap(int longestGap) {
        this.longestGap = longestGap;
    }

    public double getTrackDuration() {
        return trackDuration;
    }

    public void setTrackDuration(double trackDuration) {
        this.trackDuration = trackDuration;
    }

    public double getTrackXLocation() {
        return trackXLocation;
    }

    public void setTrackXLocation(double trackXLocation) {
        this.trackXLocation = trackXLocation;
    }

    public double getTrackYLocation() {
        return trackYLocation;
    }

    public void setTrackYLocation(double trackYLocation) {
        this.trackYLocation = trackYLocation;
    }

    public double getTrackDisplacement() {
        return trackDisplacement;
    }

    public void setTrackDisplacement(double trackDisplacement) {
        this.trackDisplacement = trackDisplacement;
    }

    public double getTrackMaxSpeed() {
        return trackMaxSpeed;
    }

    public void setTrackMaxSpeed(double trackMaxSpeed) {
        this.trackMaxSpeed = trackMaxSpeed;
    }

    public double getTrackMedianSpeed() {
        return trackMedianSpeed;
    }

    public void setTrackMedianSpeed(double trackMedianSpeed) {
        this.trackMedianSpeed = trackMedianSpeed;
    }

    public double getDiffusionCoefficient() {
        return diffusionCoefficient;
    }

    public void setDiffusionCoefficient(double diffusionCoefficient) {
        this.diffusionCoefficient = diffusionCoefficient;
    }

    public double getDiffusionCoefficientExt() {
        return diffusionCoefficientExt;
    }

    public void setDiffusionCoefficientExt(double diffusionCoefficientExt) {
        this.diffusionCoefficientExt = diffusionCoefficientExt;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }

    public double getConfinementRatio() {
        return confinementRatio;
    }

    public void setConfinementRatio(double confinementRatio) {
        this.confinementRatio = confinementRatio;
    }

    public int getSquareNumber() {
        return squareNumber;
    }

    public void setSquareNumber(int squareNumber) {
        this.squareNumber = squareNumber;
    }

    public int getLabelNumber() {
        return labelNumber;
    }

    public void setLabelNumber(int labelNumber) {
        this.labelNumber = labelNumber;
    }

    /**
     * Returns a concise string representation of this track.
     *
     * @return a formatted string containing key track metrics
     */
    @Override
    public String toString() {
        return String.format(
                "Track[id=%d, recording=%s, spots=%d, duration=%.2f, displacement=%.2f, maxSpeed=%.2f, medianSpeed=%.2f]",
                trackId,
                recordingName,
                numberOfSpots,
                trackDuration,
                trackDisplacement,
                trackMaxSpeed,
                trackMedianSpeed
        );
    }
}