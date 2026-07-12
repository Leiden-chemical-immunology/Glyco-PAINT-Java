/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.recording_override;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.Recording;
import paint.viewer.model.RecordingEntry;
import paint.viewer.model.SquareControlParams;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.viewer.override.recording_override.ImportRecordingOverride.loadRecordingOverride;

/**
 * Pins down the contract of the recording-override round trip.
 * <p>
 * A recording override is the set of square-selection thresholds (minimum required density
 * ratio, maximum allowable variability, minimum required R²) plus the neighbour mode, stored
 * per recording in {@code Viewer/Recording Override.csv} and keyed by (experiment, recording).
 * </p>
 * <p>
 * Writing is non-destructive and scope-driven: {@code "Recording"} touches only the current
 * recording, {@code "Experiment"} every recording in the same experiment, {@code "Project"}
 * every recording loaded. Rows for recordings outside the scope survive untouched. Each write
 * also updates the in-memory {@link Recording}, so the file and the model stay in step.
 * </p>
 */
class RecordingOverrideRoundTripTest {

    private static final String EXP_1 = "221012";
    private static final String EXP_2 = "221108";

    /** A RecordingEntry with no images: loadImage(null) returns null, so this stays headless. */
    private static RecordingEntry entry(String experimentName, String recordingName) {
        Recording recording = new Recording();
        recording.setRecordingName(recordingName);
        recording.setExperimentName(experimentName);
        return new RecordingEntry(recording, null, null, experimentName);
    }

    /**
     * Deliberately distinct values. If the writer and the reader ever disagree about which
     * column is which, these will land in the wrong fields and the assertions will catch it.
     */
    private static SquareControlParams params() {
        return new SquareControlParams(0.11, 0.22, 0.33, "Free");
    }

    /** The overrides currently in the file, keyed by recording name. */
    private static Map<String, RecordingOverride> stored(Path project) {
        List<RecordingOverride> all =
                loadRecordingOverride(project.resolve("Viewer").resolve("Recording Override.csv"));
        Map<String, RecordingOverride> out = new HashMap<>();
        for (RecordingOverride o : all) {
            out.put(o.getRecordingName(), o);
        }
        return out;
    }

    private static List<RecordingEntry> entries(RecordingEntry... es) {
        List<RecordingEntry> list = new ArrayList<>();
        for (RecordingEntry e : es) {
            list.add(e);
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Values survive the round trip
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("every threshold comes back in the field it was written to")
    void valuesRoundTripIntoTheRightFields(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        List<RecordingEntry> es = entries(entry(EXP_1, "221012-Exp-1-A1-1"));

        writer.writeRecordingOverridesToFile("Recording", params(), es, 0);

        RecordingOverride o = stored(project).get("221012-Exp-1-A1-1");
        assertEquals(EXP_1, o.getExperimentName());
        assertEquals(0.11,  o.getMinRequiredDensityRatio(), 1e-9);
        assertEquals(0.22,  o.getMaxAllowableVariability(), 1e-9);
        assertEquals(0.33,  o.getMinRequiredRSquared(),     1e-9);
        assertEquals("Free", o.getNeighbourMode());
    }

    @Test
    @DisplayName("the in-memory recording is updated as well as the file")
    void theModelIsUpdatedToo(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        RecordingEntry e = entry(EXP_1, "221012-Exp-1-A1-1");

        writer.writeRecordingOverridesToFile("Recording", params(), entries(e), 0);

        Recording r = e.getRecording();
        assertEquals(0.11,  r.getMinRequiredDensityRatio(), 1e-9);
        assertEquals(0.22,  r.getMaxAllowableVariability(), 1e-9);
        assertEquals(0.33,  r.getMinRequiredRSquared(),     1e-9);
        assertEquals("Free", r.getNeighbourMode());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Scope
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Recording scope writes only the current recording")
    void recordingScopeWritesOnlyTheCurrent(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        List<RecordingEntry> es = entries(
                entry(EXP_1, "221012-Exp-1-A1-1"),
                entry(EXP_1, "221012-Exp-1-B2-3"));

        writer.writeRecordingOverridesToFile("Recording", params(), es, 1);   // the second one

        assertEquals(1, stored(project).size());
        assertTrue(stored(project).containsKey("221012-Exp-1-B2-3"));
    }

    @Test
    @DisplayName("Experiment scope writes every recording of that experiment, and no other")
    void experimentScopeWritesThatExperimentOnly(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        List<RecordingEntry> es = entries(
                entry(EXP_1, "221012-Exp-1-A1-1"),
                entry(EXP_1, "221012-Exp-1-B2-3"),
                entry(EXP_2, "221108-Exp-2-C3-1"));

        writer.writeRecordingOverridesToFile("Experiment", params(), es, 0);

        Map<String, RecordingOverride> s = stored(project);
        assertEquals(2, s.size());
        assertTrue(s.containsKey("221012-Exp-1-A1-1"));
        assertTrue(s.containsKey("221012-Exp-1-B2-3"));
        assertFalse(s.containsKey("221108-Exp-2-C3-1"), "a different experiment is out of scope");
    }

    @Test
    @DisplayName("Project scope writes every recording loaded, across experiments")
    void projectScopeWritesEverything(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        List<RecordingEntry> es = entries(
                entry(EXP_1, "221012-Exp-1-A1-1"),
                entry(EXP_2, "221108-Exp-2-C3-1"));

        writer.writeRecordingOverridesToFile("Project", params(), es, 0);

        assertEquals(2, stored(project).size());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Non-destructiveness
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("writing one recording leaves an existing override for another alone")
    void writingIsNonDestructive(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        RecordingEntry a = entry(EXP_1, "221012-Exp-1-A1-1");
        RecordingEntry b = entry(EXP_1, "221012-Exp-1-B2-3");

        writer.writeRecordingOverridesToFile("Recording", params(), entries(a), 0);
        writer.writeRecordingOverridesToFile(
                "Recording", new SquareControlParams(0.9, 0.8, 0.7, "Strict"), entries(b), 0);

        Map<String, RecordingOverride> s = stored(project);
        assertEquals(2, s.size());
        assertEquals(0.11, s.get("221012-Exp-1-A1-1").getMinRequiredDensityRatio(), 1e-9);
        assertEquals(0.9,  s.get("221012-Exp-1-B2-3").getMinRequiredDensityRatio(), 1e-9);
    }

    @Test
    @DisplayName("re-writing the same recording replaces its row, and does not duplicate it")
    void rewritingReplaces(@TempDir Path project) {
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        List<RecordingEntry> es = entries(entry(EXP_1, "221012-Exp-1-A1-1"));

        writer.writeRecordingOverridesToFile("Recording", params(), es, 0);
        writer.writeRecordingOverridesToFile(
                "Recording", new SquareControlParams(0.9, 0.8, 0.7, "Strict"), es, 0);

        Map<String, RecordingOverride> s = stored(project);
        assertEquals(1, s.size(), "one row per (experiment, recording), not two");
        assertEquals(0.9,     s.get("221012-Exp-1-A1-1").getMinRequiredDensityRatio(), 1e-9);
        assertEquals("Strict", s.get("221012-Exp-1-A1-1").getNeighbourMode());
    }

    @Test
    @DisplayName("a recording whose name is a prefix of another gets its own row")
    void prefixNamesGetSeparateRows(@TempDir Path project) {
        // -A1-1 is a prefix of -A1-10. Two bugs of this shape were found in the Viewer, so
        // check that these keep separate override rows and do not overwrite one another.
        WriteRecordingOverride writer = new WriteRecordingOverride(project);
        RecordingEntry shortName = entry(EXP_1, "221012-Exp-1-A1-1");
        RecordingEntry longName  = entry(EXP_1, "221012-Exp-1-A1-10");

        writer.writeRecordingOverridesToFile("Recording", params(), entries(shortName), 0);
        writer.writeRecordingOverridesToFile(
                "Recording", new SquareControlParams(0.9, 0.8, 0.7, "Strict"), entries(longName), 0);

        Map<String, RecordingOverride> s = stored(project);
        assertEquals(2, s.size());
        assertEquals(0.11, s.get("221012-Exp-1-A1-1").getMinRequiredDensityRatio(), 1e-9);
        assertEquals(0.9,  s.get("221012-Exp-1-A1-10").getMinRequiredDensityRatio(), 1e-9);
    }
}
