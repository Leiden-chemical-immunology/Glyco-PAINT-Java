/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.recording_exclude;

/**
 * Lightweight value object representing a single excluded recording.
 * <p>
 * Instances of this class correspond one-to-one with rows in {@code Viewer/Recording
 * Exclude.csv}. The CSV contains a single column ("Recording Name") listing recordings that
 * should be excluded from downstream processing. This class intentionally contains only the
 * recording name. All exclusion semantics (how and where exclusions are applied) are handled
 * elsewhere by importer and applier utilities.
 * </p>
 * <ul>
 *   <li>Simple POJO with a single identifying field.</li>
 *   <li>Used for CSV import/export and in-memory exclusion processing.</li>
 *   <li>No business logic by design.</li>
 * </ul>
 */
public class RecordingExclude {

    /**
     * Name of the recording to be excluded.
     * Must exactly match the "Recording Name" used in Recordings.csv.
     */
    private String recordingName;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Default constructor required for CSV deserialization.
     */
    public RecordingExclude() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the recording name associated with this exclusion.
     *
     * @return recording name
     */
    public String getRecordingName() {
        return recordingName;
    }

    /**
     * Sets the recording name associated with this exclusion.
     *
     * @param recordingName recording name to exclude
     */
    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // DEBUG
    // ───────────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "RecordingExclude{" +
                "recordingName='" + recordingName + '\'' +
                '}';
    }
}