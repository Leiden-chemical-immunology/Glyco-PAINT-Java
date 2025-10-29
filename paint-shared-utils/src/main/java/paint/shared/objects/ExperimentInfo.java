/******************************************************************************
 *  Class:        ExperimentInfo.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents metadata and associated data structures describing a single
 *    experiment recording within the PAINT analysis framework.
 *
 *  DESCRIPTION:
 *    The {@code ExperimentInfo} class encapsulates all metadata fields that
 *    describe an experiment, such as condition number, probe information,
 *    concentration, and threshold values. In addition, it maintains references
 *    to related entities, including {@link Square} and {@link Track} objects,
 *    and optionally a {@link tech.tablesaw.api.Table} containing track data.
 *
 *    This class provides constructors to create instances from explicit
 *    parameters or from key-value maps (as when reading experiment info
 *    from CSV or JSON sources). All fields are mutable through standard
 *    accessor and mutator methods.
 *
 *  KEY FEATURES:
 *    • Encapsulates all experimental metadata.
 *    • Links to associated {@link Square} and {@link Track} entities.
 *    • Provides map-based initialization for tabular imports.
 *    • Supports formatted string export for diagnostics and logging.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.objects;

import paint.shared.utils.Miscellaneous;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.Table;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

/**
 * Represents metadata and associated data objects for a single experiment.
 * <p>
 * This class encapsulates both the descriptive metadata and the related
 * data structures (squares, tracks, and tables) that define one recording
 * in the PAINT workflow.
 */
public class ExperimentInfo {

    // ───────────────────────────────────────────────────────────────────────────────
    // CORE FIELDS
    // ───────────────────────────────────────────────────────────────────────────────

    private String  experimentName;
    private String  recordingName;
    private int     conditionNumber;
    private int     replicateNumber;
    private String  probeName;
    private String  probeType;
    private String  cellType;
    private String  adjuvant;
    private double  concentration;
    private boolean processFlag;               // renamed from doProcess
    private double  threshold;
    
    // ───────────────────────────────────────────────────────────────────────────────
    // ASSOCIATED OBJECTS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * The collection of squares associated with this experiment.
     */
    private List<Square> squares = new ArrayList<>();
    private List<Track>  tracks   = new ArrayList<>();
    private Table        tracksTable;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Default constructor that creates an empty {@code ExperimentInfo} instance.
     */
    public ExperimentInfo() {
    }

    /**
     * Constructs a fully specified {@code ExperimentInfo} instance.
     *
     * @param experimentName  the experiment name
     * @param recordingName   the recording name
     * @param conditionNumber the condition number
     * @param replicateNumber the replicate number
     * @param probeName       the probe name
     * @param probeType       the probe type
     * @param cellType        the cell type
     * @param adjuvant        the adjuvant used
     * @param concentration   the concentration value
     * @param processFlag     whether this recording should be processed
     * @param threshold       the threshold for processing
     */
    public ExperimentInfo(String  experimentName,
                          String  recordingName,
                          int     conditionNumber,
                          int     replicateNumber,
                          String  probeName,
                          String  probeType,
                          String  cellType,
                          String  adjuvant,
                          double  concentration,
                          boolean processFlag,
                          double threshold) {
        this.experimentName  = experimentName;
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

    /**
     * Constructs an {@code ExperimentInfo} instance from a map of string key-value pairs.
     * <p>
     * Expected keys include:
     * <ul>
     *   <li>Experiment Name</li>
     *   <li>Recording Name</li>
     *   <li>Condition Number</li>
     *   <li>Replicate Number</li>
     *   <li>Probe Name</li>
     *   <li>Probe Type</li>
     *   <li>Cell Type</li>
     *   <li>Adjuvant</li>
     *   <li>Concentration</li>
     *   <li>Process Flag</li>
     *   <li>Threshold</li>
     * </ul>
     *
     * @param row the map of column names to values
     */
    public ExperimentInfo(Map<String, String> row) {
        try {
            this.experimentName  = row.get("Experiment Name");
            this.recordingName   = row.get("Recording Name");
            this.conditionNumber = parseInt(row.get("Condition Number"));
            this.replicateNumber = parseInt(row.get("Replicate Number"));
            this.probeName       = row.get("Probe Name");
            this.probeType       = row.get("Probe Type");
            this.cellType        = row.get("Cell Type");
            this.adjuvant        = row.get("Adjuvant");
            this.concentration   = parseDouble(row.get("Concentration"));
            this.processFlag     = Miscellaneous.isBooleanTrue(row.get("Process Flag"));
            this.threshold       = parseDouble(row.get("Threshold"));
        } catch (Exception e) {
            PaintLogger.errorf("Problem parsing Experiment Info");
            PaintLogger.errorf(row.toString());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS AND MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    public String getExperimentName() {
        return experimentName;
    }

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

    public List<Square> getSquares() {
        return squares;
    }

    public void setSquares(List<Square> squares) {
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

    // ───────────────────────────────────────────────────────────────────────────────
    // CONVENIENCE METHODS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a single {@link Square} to this experiment.
     *
     * @param square the square to add
     */
    public void addSquare(Square square) {
        this.squares.add(square);
    }

    /**
     * Adds a list of {@link Square} instances to this experiment.
     *
     * @param squares list of squares to add
     */
    public void addSquares(List<Square> squares) {
        this.squares.addAll(squares);
    }

    /**
     * Adds a single {@link Track} to this experiment.
     *
     * @param track the track to add
     */
    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // STRING REPRESENTATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a formatted string representation of this {@code ExperimentInfo}.
     *
     * @return formatted experiment information as string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\tExperiment Info");
        sb.append(String.format("\t              Experiment Name               : %s\n", experimentName));
        sb.append(String.format("\t              Recording Name                : %s%n", recordingName));
        sb.append(String.format("\t              Condition Nr                  : %d%n", conditionNumber));
        sb.append(String.format("\t              Replicate Nr                  : %d%n", replicateNumber));
        sb.append(String.format("\t              Probe Name                    : %s%n", probeName));
        sb.append(String.format("\t              Probe Type                    : %s%n", probeType));
        sb.append(String.format("\t              Cell Type                     : %s%n", cellType));
        sb.append(String.format("\t              Adjuvant                      : %s%n", adjuvant));
        sb.append(String.format("\t              Concentration                 : %.2f%n", concentration));
        sb.append(String.format("\t              Threshold                     : %.2f%n", threshold));
        if (tracks != null) {
            sb.append(String.format("\t              Number of tracks              : %d%n", tracks.size()));
        }
        if (squares != null) {
            sb.append(String.format("\t              Number of squares             : %d%n", squares.size()));
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