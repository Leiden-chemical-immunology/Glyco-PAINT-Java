/*=============================================================================
 *  Class:        RecordingOverride.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents an override entry for a Recording. Overrides are applied to the
 *    main Recordings table to correct or replace threshold parameters for a 
 *    specific recording identified by (experimentName, recordingName).
 *
 *  DESCRIPTION:
 *    This class models one row from the Recording Override CSV file. It contains
 *    plain fields with getters and setters so it can be parsed directly from CSV
 *    and consumed by ImportRecordingOverride.
 *
 *  KEY FEATURES:
 *    • Plain data container (POJO).
 *    • Matches the CSV columns exactly.
 *    • Used by ImportRecordingOverride to update Recording rows.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-11
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

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