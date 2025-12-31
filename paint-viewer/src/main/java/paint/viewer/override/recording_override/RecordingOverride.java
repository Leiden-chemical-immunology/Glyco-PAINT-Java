/*==============================================================================
 *  Class:        RecordingOverride.java
 *  Package:      paint.viewer.override.recording_override
 *
 *  PURPOSE:
 *    Represents a single recording-level override entry. Each instance defines
 *    replacement threshold parameters for one recording, identified by the
 *    (experimentName, recordingName) pair.
 *
 *  DESCRIPTION:
 *    This class models one row from the "Recording Override.csv" file located in
 *    the Viewer directory of a PAINT project. It is a simple data container
 *    designed for direct CSV parsing and in-memory consumption by
 *    ImportRecordingOverride and related override/export utilities.
 *
 *    The values stored here replace the corresponding threshold parameters
 *    (density ratio, R², variability, neighbour mode) on the target recording.
 *
 *  KEY FEATURES:
 *    • Plain data container (POJO) with no behavior.
 *    • Field layout mirrors the Recording Override CSV columns.
 *    • Used exclusively for override import/export and in-memory mutation.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override.recording_override;

public class RecordingOverride {

    private String experimentName;
    private String recordingName;

    private double minRequiredDensityRatio;
    private double minRequiredRSquared;
    private double maxAllowableVariability;
    private String neighbourMode;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Default constructor required for CSV parsing and reflective instantiation.
     */
    public RecordingOverride() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS
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

    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    public void setMinRequiredDensityRatio(double minRequiredDensityRatio) {
        this.minRequiredDensityRatio = minRequiredDensityRatio;
    }

    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    public void setMinRequiredRSquared(double minRequiredRSquared) {
        this.minRequiredRSquared = minRequiredRSquared;
    }

    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    public void setMaxAllowableVariability(double maxAllowableVariability) {
        this.maxAllowableVariability = maxAllowableVariability;
    }

    public String getNeighbourMode() {
        return neighbourMode;
    }

    public void setNeighbourMode(String neighbourMode) {
        this.neighbourMode = neighbourMode;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // DEBUG
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable representation of this override entry,
     * primarily intended for logging and debugging.
     */
    @Override
    public String toString() {
        return "RecordingOverride{" +
                "experimentName='" + experimentName + '\'' +
                ", recordingName='" + recordingName + '\'' +
                ", minRequiredDensityRatio=" + minRequiredDensityRatio +
                ", minRequiredRSquared=" + minRequiredRSquared +
                ", maxAllowableVariability=" + maxAllowableVariability +
                ", neighbourMode='" + neighbourMode + '\'' +
                '}';
    }
}