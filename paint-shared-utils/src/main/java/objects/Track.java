package objects;

public class Track {
    private String uniqueKey;                 // 0
    private String recordingName;             // 1
    private int    trackId;                   // 2
    private String trackLabel;                // 3
    private int    numberOfSpots;             // 4
    private int    numberOfGaps;              // 5
    private int    longestGap;                // 6
    private double trackDuration;             // 7
    private double trackXLocation;            // 8
    private double trackYLocation;            // 9
    private double trackDisplacement;         // 10
    private double trackMaxSpeed;             // 11
    private double trackMedianSpeed;          // 12
    private double diffusionCoefficient;      // 13
    private double diffusionCoefficientExt;   // 14
    private double totalDistance;             // 15
    private double confinementRatio;          // 16`
    private int    squareNumber;              // 17
    private int    labelNumber;               // 18

    public Track() { }

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
                 double trackMaxSpeedCalc,
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

    // Getters and setters

    public String getUniqueKey() { return uniqueKey; }
    public void setUniqueKey(String uniqueKey) { this.uniqueKey = uniqueKey;}

    public String getRecordingName() { return recordingName; }
    public void setRecordingName(String recordingName) { this.recordingName = recordingName;}

    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }

    public String getTrackLabel() { return trackLabel; }
    public void setTrackLabel(String trackLabel) { this.trackLabel = trackLabel; }

    public int getNumberOfSpots() { return numberOfSpots; }
    public void setNumberOfSpots(int nrSpots) { this.numberOfSpots = nrSpots; }

    public int getNumberOfGaps() { return numberOfGaps; }
    public void setNumberOfGaps(int nrGaps) { this.numberOfGaps = nrGaps; }

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
}
