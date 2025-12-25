/*==============================================================================
 *  Class:        RecordingExclude.java
 *  Package:      paint.viewer.override.recording_exclude
 *
 *  PURPOSE:
 *    Lightweight value object representing a single excluded recording.
 *
 *  DESCRIPTION:
 *    Instances of this class correspond one-to-one with rows in
 *    {@code Viewer/Recording Exclude.csv}. The CSV contains a single column
 *    ("Recording Name") listing recordings that should be excluded from
 *    downstream processing.
 *
 *    This class intentionally contains only the recording name. All exclusion
 *    semantics (how and where exclusions are applied) are handled elsewhere
 *    by importer and applier utilities.
 *
 *  KEY FEATURES:
 *    • Simple POJO with a single identifying field.
 *    • Used for CSV import/export and in-memory exclusion processing.
 *    • No business logic by design.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-25
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.override.recording_exclude;

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