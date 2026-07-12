/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.square_override;

/**
 * Data container representing a single row from {@code Square Override.csv}.
 * <p>
 * Each instance identifies one square via:
 * <ul>
 *   <li>{@code experimentName}</li>
 *   <li>{@code recordingName}</li>
 *   <li>{@code squareNumber}</li>
 * </ul>
 * and provides the overridden {@code cellId} (plus an informational timestamp).
 * <p>
 * This class contains no business logic; it is intended for CSV parsing and
 * in-memory override application.
 */
public class SquareOverride {

    /** Experiment identifier used for matching. */
    private String experimentName;

    /** Recording identifier used for matching. */
    private String recordingName;

    /** Square number within the recording used for matching. */
    private int    squareNumber;

    /** Overridden cell ID to apply to the matched square. */
    private int    cellId;

    /** Timestamp captured when the override was written (informational). */
    private String timestamp;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /** Creates an empty SquareOverride (typically populated via CSV parsing). */
    public SquareOverride() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS
    // ───────────────────────────────────────────────────────────────────────────────

    /** @return experiment name used for matching */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Sets the experiment name used for matching.
     *
     * @param experimentName experiment identifier
     */
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    /** @return recording name used for matching */
    public String getRecordingName() {
        return recordingName;
    }

    /**
     * Sets the recording name used for matching.
     *
     * @param recordingName recording identifier
     */
    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    /** @return square number used for matching */
    public int getSquareNumber() {
        return squareNumber;
    }

    /**
     * Sets the square number used for matching.
     *
     * @param squareNumber square index/number within the recording
     */
    public void setSquareNumber(int squareNumber) {
        this.squareNumber = squareNumber;
    }

    /** @return overridden cell ID */
    public int getCellId() {
        return cellId;
    }

    /**
     * Sets the overridden cell ID to apply to the matched square.
     *
     * @param cellId new cell assignment
     */
    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    /**
     * Sets the informational timestamp associated with this override.
     *
     * @param timestamp timestamp string as written in the CSV
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // DEBUG
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Debug-friendly representation of this override row.
     *
     * @return string representation
     */
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