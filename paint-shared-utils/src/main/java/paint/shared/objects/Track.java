package paint.shared.objects;

public class Track {

    private String uniqueKey;
    private String recordingName;
    private int trackId;
    private String trackLabel;
    private int numberOfSpots;
    private int numberOfGaps;
    private int longestGap;
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
    private int squareNumber;
    private int labelNumber;

    public Track() {}

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

    // --- Debug string ---
    @Override
    public String toString() {
        return String.format(
                "Track[id=%d, label=%s, recording=%s, spots=%d, duration=%.2f, displacement=%.2f, maxSpeed=%.2f, medianSpeed=%.2f]",
                trackId, trackLabel, recordingName, numberOfSpots, trackDuration, trackDisplacement, trackMaxSpeed, trackMedianSpeed
        );
    }
}