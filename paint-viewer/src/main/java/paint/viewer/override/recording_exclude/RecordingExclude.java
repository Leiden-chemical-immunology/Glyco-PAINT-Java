package paint.viewer.override.recording_exclude;

public class RecordingExclude {

    private String recordingName;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    public RecordingExclude() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS
    // ───────────────────────────────────────────────────────────────────────────────

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }
    // ───────────────────────────────────────────────────────────────────────────────
    // DEBUG
    // ───────────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "RecordingOverride{" +
                "experimentName='" + recordingName + '}';
    }
}