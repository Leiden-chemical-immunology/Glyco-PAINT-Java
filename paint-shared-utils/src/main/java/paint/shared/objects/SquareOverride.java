/*=============================================================================
 *  Class:        SquareOverride.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents an override entry for a Square. Overrides are applied to the
 *    main Squares table to correct or replace the cellId for a specific square
 *    identified by (experimentName, recordingName, squareId).
 *
 *  DESCRIPTION:
 *    This class models one row from the Squares Override CSV file. It contains
 *    only simple fields with getters and setters so it can be parsed directly
 *    from CSV and used in the override-application process.
 *
 *  KEY FEATURES:
 *    • Plain data container (POJO).
 *    • Matches the CSV columns exactly.
 *    • Used by SquareOverrideApplier to update Square objects.
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

package paint.shared.objects;

public class SquareOverride {

    private String experimentName;
    private String recordingName;
    private int    squareNumber;
    private int    cellId;
    private String timestamp;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    public SquareOverride() {
    }

    public SquareOverride(String experimentName,
            String recordingName,
            int    squareNumber,
            int    cellId,
            String timestamp) {
        this.experimentName = experimentName;
        this.recordingName  = recordingName;
        this.squareNumber   = squareNumber;
        this.cellId         = cellId;
        this.timestamp      = timestamp;
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

    public int getSquareNumber() {
        return squareNumber;
    }

    public void setSquareNumber(int squareNumber) {
        this.squareNumber = squareNumber;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // DEBUG
    // ───────────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "SquareOverride{" +
                "experimentName='" + experimentName + '\'' +
                ", recordingName='" + recordingName + '\'' +
                ", squareNumber=" + squareNumber +
                ", cellId=" + cellId +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}