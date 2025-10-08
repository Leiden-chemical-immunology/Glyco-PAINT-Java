package paint.shared.objects;

import tech.tablesaw.api.Table;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a Recording with metadata, squares, and tracks.
 */
public class Recording
    {
    
    // --- Core fields (columns in All Recordings/Experiment Info) ---
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
    private int           numberOfSpots;
    private int           numberOfTracks;
    private int           numberOfSpotsInAllTracks;
    private int           numberOfFrames;
    private double        runTime;
    private LocalDateTime timeStamp;
    private boolean       exclude;
    private double        tau;
    private double        rSquared;
    private double        density;
    
    // --- Associated objects ---
    private List<Square> squares = new ArrayList<>();
    private List<Track>  tracks  = new ArrayList<>();
    private Table        tracksTable;
    
    // --- Constructors ---
    public Recording() {
    }
    
    public Recording(
            String recordingName,
            int conditionNumber,
            int replicateNumber,
            String probeName,
            String probeType,
            String cellType,
            String adjuvant,
            double concentration,
            boolean processFlag,
            double threshold
    ) {
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
    
    // --- Getters and Setters ---
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
    
    // --- Associated objects ---
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
    
    // --- Convenience methods ---
    public void addSquare(Square square) {
        this.squares.add(square);
    }
    
    public void addSquares(List<Square> squares) {
        this.squares.addAll(squares);
    }
    
    public void addTrack(Track track) {
        this.tracks.add(track);
    }
    
    // --- Debug helpers ---
    private static Boolean checkBooleanValue(String string) {
        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t"));
        return yesValues.contains(string.trim().toLowerCase());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("\tRecording Name                : %s%n", recordingName));
        sb.append(String.format("\tCondition Nr                  : %d%n", conditionNumber));
        sb.append(String.format("\tReplicate Nr                  : %d%n", replicateNumber));
        sb.append(String.format("\tProbe Name                    : %s%n", probeName));
        sb.append(String.format("\tProbe Type                    : %s%n", probeType));
        sb.append(String.format("\tCell Type                     : %s%n", cellType));
        sb.append(String.format("\tAdjuvant                      : %s%n", adjuvant));
        sb.append(String.format("\tConcentration                 : %.2f%n", concentration));
        sb.append(String.format("\tThreshold                     : %.2f%n", threshold));
        sb.append(String.format("\tExclude                       : %b%n", exclude));
        sb.append(String.format("\tTime Stamp                    : %s%n", timeStamp));
        sb.append(String.format("\tNumber of Spots               : %d%n", numberOfSpots));
        sb.append(String.format("\tNumber of Tracks              : %d%n", numberOfTracks));
        sb.append(String.format("\tNumber of Spots in All Tracks : %d%n", numberOfSpotsInAllTracks));
        sb.append(String.format("\tRun Time                      : %.2f%n", runTime));
        sb.append(String.format("\tNumber of Frames              : %d%n", numberOfFrames));
        sb.append(String.format("\tTau                           : %.2f%n", tau));
        sb.append(String.format("\tR Squared                     : %.2f%n", rSquared));
        sb.append(String.format("\tDensity                       : %.2f%n", density));
        
        if (tracks != null) {
            sb.append(String.format("\tNumber of tracks              : %d%n", tracks.size()));
        }
        if (squares != null) {
            sb.append(String.format("\tNumber of squares             : %d%n", squares.size()));
        }
        
        int numberOfSquaresWithTracks = 0;
        for (Square square : squares) {
            if (square.getTracks() != null && !square.getTracks().isEmpty()) {
                numberOfSquaresWithTracks++;
            }
        }
        sb.append(String.format("\tNumber of squares with tracks : %d%n", numberOfSquaresWithTracks));
        
        return sb.toString();
    }
    }