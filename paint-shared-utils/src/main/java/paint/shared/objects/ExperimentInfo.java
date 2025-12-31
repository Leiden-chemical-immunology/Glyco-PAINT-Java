/*=============================================================================
 *  Class:        ExperimentInfo.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents all metadata associated with a single experiment recording.
 *    This includes probe information, condition parameters, process flags,
 *    and thresholding values, as well as links to Square and Track entities.
 *
 *  DESCRIPTION:
 *    This class now embeds its own schema definition through the
 *    {@link Column} enum, replacing ExperimentInfoSchema. Table I/O classes
 *    extract headers and Tablesaw types directly from this enum, ensuring
 *    consistency and eliminating redundant schema classes.
 *
 *    ExperimentInfo may be constructed manually, or from a key-value map
 *    (such as parsed CSV rows). All fields are mutable through getters and
 *    setters, and the class provides a formatted toString() summary.
 *
 *  KEY FEATURES:
 *    • Fully embedded schema via Column enum (headers + column types)
 *    • Provides core metadata for each experiment recording
 *    • Holds associated Square and Track objects
 *    • Supports initialization from CSV/JSON-like key/value maps
 *    • Java 8 compatible; used by ExperimentInfoTableIO and validators
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.objects;

import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.ColumnType;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static paint.shared.constants.PaintStringConstants.*;
import static paint.shared.utils.BooleanUtils.isBooleanTrue;

/**
 * Represents all metadata and associated data objects for a single experiment
 * recording in the PAINT analysis framework.
 *
 * <p>
 * This class contains experiment-level descriptive metadata as well as
 * references to the square and track objects produced during analysis.
 * </p>
 *
 * <p>
 * The schema for ExperimentInfo is embedded directly in {@link Column}, which
 * binds human-readable CSV headers to their corresponding Tablesaw types.
 * Table I/O classes use this enum for all schema operations.
 * </p>
 */
public class ExperimentInfo {

    //=========================================================================
    //  CORE FIELDS
    //=========================================================================

    private String  experimentName;
    private String  recordingName;
    private int     conditionNumber;
    private int     replicateNumber;
    private String  probeName;
    private String  probeType;
    private String  cellType;
    private String  adjuvant;
    private double  concentration;
    private boolean processFlag;
    private double  threshold;

    /** Associated objects for downstream analysis. */
    private final List<Square> squares = new ArrayList<>();
    private final List<Track>  tracks  = new ArrayList<>();

    /**
     * Default empty constructor.
     */
    public ExperimentInfo() {
    }

    //=========================================================================
    //  CONSTRUCTORS
    //=========================================================================

    /**
     * Constructs an ExperimentInfo instance from a row of key-value pairs
     * (typically parsed from experiment_info.csv).
     *
     * @param row the map of header → text value
     */
    public ExperimentInfo(Map<String, String> row) {
        try {
            this.experimentName  = row.get(EXPERIMENT_NAME);
            this.recordingName   = row.get(RECORDING_NAME);
            this.conditionNumber = parseInt(row.get(CONDITION_NUMBER));
            this.replicateNumber = parseInt(row.get(REPLICATE_NUMBER));
            this.probeName       = row.get(PROBE_NAME);
            this.probeType       = row.get(PROBE_TYPE);
            this.cellType        = row.get(CELL_TYPE);
            this.adjuvant        = row.get(ADJUVANT);
            this.concentration   = parseDouble(row.get(CONCENTRATION));
            this.processFlag     = isBooleanTrue(row.get(PROCESS_FLAG));
            this.threshold       = parseDouble(row.get(THRESHOLD));
        }
        catch (Exception e) {
            PaintLogger.errorf("Problem parsing Experiment Info");
            PaintLogger.errorf(row.toString());

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("Exception details:\n" + sw);
        }
    }

    /**
     * @return the name of the experiment.
     */
    public String getExperimentName() {
        return experimentName;
    }

    //=========================================================================
    //  ACCESSORS & MUTATORS
    //=========================================================================

    /**
     * @param experimentName the experiment name to set.
     */
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    /**
     * @return the unique name of the recording.
     */
    public String getRecordingName() {
        return recordingName;
    }

    /**
     * @param recordingName the recording name to set.
     */
    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    /**
     * @return the condition number.
     */
    public int getConditionNumber() {
        return conditionNumber;
    }

    /**
     * @param conditionNumber the condition number to set.
     */
    public void setConditionNumber(int conditionNumber) {
        this.conditionNumber = conditionNumber;
    }

    /**
     * @return the replicate number.
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * @param replicateNumber the replicate number to set.
     */
    public void setReplicateNumber(int replicateNumber) {
        this.replicateNumber = replicateNumber;
    }

    /**
     * @return the name of the probe.
     */
    public String getProbeName() {
        return probeName;
    }

    /**
     * @param probeName the probe name to set.
     */
    public void setProbeName(String probeName) {
        this.probeName = probeName;
    }

    /**
     * @return the type of probe.
     */
    public String getProbeType() {
        return probeType;
    }

    /**
     * @param probeType the probe type to set.
     */
    public void setProbeType(String probeType) {
        this.probeType = probeType;
    }

    /**
     * @return the cell type used.
     */
    public String getCellType() {
        return cellType;
    }

    /**
     * @param cellType the cell type to set.
     */
    public void setCellType(String cellType) {
        this.cellType = cellType;
    }

    /**
     * @return the adjuvant used.
     */
    public String getAdjuvant() {
        return adjuvant;
    }

    /**
     * @param adjuvant the adjuvant to set.
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
     * @param concentration the concentration to set.
     */
    public void setConcentration(double concentration) {
        this.concentration = concentration;
    }

    /**
     * @return true if the process flag is set.
     */
    public boolean isProcessFlagSet() {
        return processFlag;
    }

    /**
     * @param processFlag the process flag to set.
     */
    public void setProcessFlag(boolean processFlag) {
        this.processFlag = processFlag;
    }

    /**
     * @return the analysis threshold.
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @param threshold the threshold to set.
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }


    /**
     * Returns a human-readable summary of this ExperimentInfo, useful
     * for diagnostic logs and console output.
     */
    @Override
    public String toString() {

        return "\tExperiment Info\n" +
                String.format("\t  Experiment Name    : %s%n", experimentName) +
                String.format("\t  Recording Name     : %s%n", recordingName) +
                String.format("\t  Condition Nr       : %d%n", conditionNumber) +
                String.format("\t  Replicate Nr       : %d%n", replicateNumber) +
                String.format("\t  Probe Name         : %s%n", probeName) +
                String.format("\t  Probe Type         : %s%n", probeType) +
                String.format("\t  Cell Type          : %s%n", cellType) +
                String.format("\t  Adjuvant           : %s%n", adjuvant) +
                String.format("\t  Concentration      : %.2f%n", concentration) +
                String.format("\t  Threshold          : %.2f%n", threshold) +
                String.format("\t  Track Count        : %d%n", tracks.size()) +
                String.format("\t  Square Count       : %d%n", squares.size());
    }

    //=========================================================================
    // ENUM
    //=========================================================================

    public enum Column {

        EXPERIMENT_NAME(  "Experiment Name",  ColumnType.STRING),
        RECORDING_NAME(   "Recording Name",   ColumnType.STRING),
        CONDITION_NUMBER( "Condition Number", ColumnType.INTEGER),
        REPLICATE_NUMBER( "Replicate Number", ColumnType.INTEGER),
        PROBE_NAME(       "Probe Name",       ColumnType.STRING),
        PROBE_TYPE(       "Probe Type",       ColumnType.STRING),
        CELL_TYPE(        "Cell Type",        ColumnType.STRING),
        ADJUVANT(         "Adjuvant",         ColumnType.STRING),
        CONCENTRATION(    "Concentration",    ColumnType.DOUBLE),
        PROCESS_FLAG(     "Process Flag",     ColumnType.BOOLEAN),
        THRESHOLD(        "Threshold",        ColumnType.DOUBLE);

        /**
         * The CSV header for this column.
         */
        public final String header;

        /**
         * The Tablesaw type associated with this column.
         */
        public final ColumnType type;

        Column(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }
}