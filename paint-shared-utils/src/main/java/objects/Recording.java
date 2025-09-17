package objects;

import tech.tablesaw.api.Table;

import java.util.*;


public class Recording {

    // The first set exists in both Experiment Info and All Recordings Experiment Info
    private String recordingName;              // 0
    private int conditionNumber;               // 1
    private int replicateNumber;               // 2
    private String probeName;                  // 3
    private String probeType;                  // 4
    private String cellType;                   // 5
    private String adjuvant;                   // 6
    private double concentration;              // 7
    private boolean doProcess;                 // 8
    private double threshold;                  // 9
    private int numberOfSpots;                 // 10
    private int numberOfTracks;                // 11
    private int numberOfSpotsInAllTracks;      // 12
    private int numberOfFrames;                // 13
    private double runTime;                    // 14
    private String timeStamp;                  // 15
    private boolean exclude;                   // 16
    private double tau;                        // 17
    private double rSquared;                   // 18
    private double density;                    // 19

    private List<Square> squares = new ArrayList<>();
    private List<Track> tracks = new ArrayList<>();

    private Table tracksTable;

    // Constructors

    public Recording() {
        this.squares = new ArrayList<>();
    }

    public Recording(String recordingName,
                     int conditionNumber,
                     int replicateNumber,
                     String probeName,
                     String probeType,
                     String cellType,
                     String adjuvant,
                     double concentration,
                     boolean doProcess,
                     double threshold) {
        this.recordingName = recordingName;
        this.conditionNumber = conditionNumber;
        this.replicateNumber = replicateNumber;
        this.probeName = probeName;
        this.probeType = probeType;
        this.cellType = cellType;
        this.adjuvant = adjuvant;
        this.concentration = concentration;
        this.doProcess = doProcess;
        this.threshold = threshold;
        this.squares = new ArrayList<>();
        this.tracks = new ArrayList<>();
    }

    // Getters and setters

    public String getRecordingName() { return recordingName; }
    public void setRecordingName(String recordingName) { this.recordingName = recordingName; }

    public int getConditionNumber() { return conditionNumber; }
    public void setConditionNumber(int conditionNumber) { this.conditionNumber = conditionNumber; }

    public int getReplicateNumber() { return replicateNumber; }
    public void setReplicateNumber(int replicateNumber) { this.replicateNumber = replicateNumber; }

    public String getProbeName() { return probeName; }
    public void setProbeName(String probeName) { this.probeName = probeName; }

    public String getProbeType() { return probeType; }
    public void setProbeType(String probeType) { this.probeType = probeType; }

    public String getCellType() { return cellType; }
    public void setCellType(String cellType) { this.cellType = cellType; }

    public String getAdjuvant() { return adjuvant; }
    public void setAdjuvant(String adjuvant) { this.adjuvant = adjuvant; }

    public double getConcentration() { return concentration; }
    public void setConcentration(double concentration) { this.concentration = concentration; }

    public boolean isDoProcess() { return doProcess; }
    public void setDoProcess(boolean doProcess) { this.doProcess = doProcess; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public int getNumberOfSpots() { return numberOfSpots; }
    public void setNumberOfSpots(int numberOfSpots) { this.numberOfSpots = numberOfSpots; }

    public int getNumberOfTracks() { return numberOfTracks; }
    public void setNumberOfTracks(int numberOfTracks) { this.numberOfTracks = numberOfTracks; }

    public double getRunTime() { return runTime; }
    public void setRunTime(double runTime) { this.runTime = runTime; }

    public int getNumberOfFrames() { return numberOfFrames; }
    public void setNumberOfFrames(int recordingSize) { this.numberOfFrames = recordingSize; }

    public String getTimeStamp() { return timeStamp; }
    public void setTimeStamp(String timeStamp) { this.timeStamp = timeStamp; }

    public int getNumberOfSpotsInAllTracks() { return numberOfSpotsInAllTracks; }
    public void setNumberOfSpotsInAllTracks(int numberOfSpotsInAllTracks) { this.numberOfSpotsInAllTracks = numberOfSpotsInAllTracks; }

    public boolean isExclude() { return exclude; }
    public void setExclude(boolean exclude) { this.exclude = exclude; }

    public double getTau() { return tau; }
    public void setTau(double tau) { this.tau = tau; }

    public double getRSquared() { return rSquared; }
    public void setRSquared(double rSquared) { this.rSquared = rSquared; }

    public double getDensity() { return density; }
    public void setDensity(double density) { this.density = density; }


    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public List<Square> getSquares() {
        return squares;
    }

    public void setSquares(List<Square> squares) {
        this.squares = squares;
    }

    public Table getTracksTable() {
        return tracksTable;
    }
    public void setTracksTable(Table tracksTable) {
        this.tracksTable = tracksTable;
    }

    public void addSquares(List <Square> squares) {
        this.squares.addAll(squares);
    }

    public void addSquare(Square square) {
        this.squares.add(square);
    }

    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    private static Boolean checkBooleanValue(String string) {
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t"));
        return yesValues.contains(string.trim().toLowerCase());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("\tRecording Name                : %s%n",   recordingName));
        sb.append(String.format("\tCondition Nr                  : %d%n", conditionNumber));
        sb.append(String.format("\tReplicate Nr                  : %d%n", replicateNumber));
        sb.append(String.format("\tProbe Name                    : %s%n",   probeName));
        sb.append(String.format("\tProbe Type                    : %s%n",   probeType));
        sb.append(String.format("\tCell Type                     : %s%n",   cellType));
        sb.append(String.format("\tAdjuvant                      : %s%n",   adjuvant));
        sb.append(String.format("\tConcentration                 : %.2f%n", concentration));
        sb.append(String.format("\tThreshold                     : %.2f%n", threshold));
        sb.append(String.format("\tExclude                       : %b%n",   exclude));
        sb.append(String.format("\tTime Stamp                    : %s%n",   timeStamp));
        sb.append(String.format("\tNumber of Spots               : %d%n", numberOfSpots));
        sb.append(String.format("\tNumber of Tracks              : %d%n", numberOfTracks));
        sb.append(String.format("\tNumber of Spots in All Tracks : %d%n",   numberOfSpotsInAllTracks));
        sb.append(String.format("\tRun Time                      : %.2f%n", runTime));
        sb.append(String.format("\tNumber of Frames              : %d%n",   numberOfFrames));
        sb.append(String.format("\tTau                           : %.2f%n", tau));
        sb.append(String.format("\tR Squared                     : %.2f%n", rSquared));
        sb.append(String.format("\tDensity                       : %.2f%n", density));

        if (tracks != null) {
            sb.append(String.format("\tNumber of tracks              : %d%n", tracks.size()));
        }
        if (squares != null) {
            sb.append(String.format("\tNumber of square              : %d%n", squares.size()));
        }

        int numberOfSquaresWithTracks = 0;
        for (Square square : squares) {
            if (square.getTracks() != null && !square.getTracks().isEmpty()) {
                numberOfSquaresWithTracks += 1;
            }
        }
        sb.append(String.format("\tNumber of squares with tracks : %d%n", numberOfSquaresWithTracks));

        return sb.toString();
    }
}
