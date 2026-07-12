/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.square_override;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.Recording;
import paint.viewer.model.RecordingEntry;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.viewer.override.ExportOverridesFromViewer.applySquareOverrides;
import static paint.viewer.override.square_override.ImportSquareOverride.loadSquareOverride;

/**
 * Pins down the contract of the square-override round trip.
 * <p>
 * The Viewer lets the user assign a cell ID to individual squares. Those assignments live in
 * {@code Viewer/Square Override.csv}, keyed by (experiment, recording, square number), and are
 * applied to the Squares table on export.
 * </p>
 * <p>
 * The write side is a <em>merge</em>, not a replace: squares the caller does not mention keep
 * whatever override they already had, and a cell ID of {@code 0} means "remove this square's
 * override" rather than "assign cell 0". {@link WriteSquareOverride#replaceSquareOverrides} is
 * the one operation that does discard a recording's existing rows. The distinction is easy to
 * get backwards and silent when it is, so both are fixed here.
 * </p>
 */
class SquareOverrideRoundTripTest {

    private static final String EXPERIMENT = "221012";
    private static final String REC_A      = "221012-Exp-1-A1-1";
    private static final String REC_B      = "221012-Exp-1-B2-3";

    /** A RecordingEntry with no images: loadImage(null) returns null, so this stays headless. */
    private static RecordingEntry entry(String recordingName) {
        Recording recording = new Recording();
        recording.setRecordingName(recordingName);
        return new RecordingEntry(recording, null, null, EXPERIMENT);
    }

    private static Map<Integer, Integer> assign(int square, int cellId) {
        Map<Integer, Integer> m = new LinkedHashMap<>();
        m.put(square, cellId);
        return m;
    }

    /** All overrides currently in the file, as "recording§square" → cellId. */
    private static Map<String, Integer> stored(Path project) {
        List<SquareOverride> all =
                loadSquareOverride(project.resolve("Viewer").resolve("Square Override.csv"));
        Map<String, Integer> out = new HashMap<>();
        for (SquareOverride o : all) {
            out.put(o.getRecordingName() + "§" + o.getSquareNumber(), o.getCellId());
        }
        return out;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Writing: merge semantics
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("an assignment is written and reads back")
    void writeThenReadRoundTrips(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));

        assertEquals(1, stored(project).size());
        assertEquals(3, stored(project).get(REC_A + "§7"));
    }

    @Test
    @DisplayName("writing merges: squares not mentioned keep the override they had")
    void writingMergesRatherThanReplaces(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));
        writer.writeSquareOverridesToFile(entry(REC_A), assign(8, 4));   // says nothing about 7

        assertEquals(3, stored(project).get(REC_A + "§7"), "square 7 must survive untouched");
        assertEquals(4, stored(project).get(REC_A + "§8"));
    }

    @Test
    @DisplayName("re-assigning the same square replaces it, and does not duplicate the row")
    void reassigningReplaces(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));
        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 9));

        assertEquals(1, stored(project).size(), "one row per (recording, square), not two");
        assertEquals(9, stored(project).get(REC_A + "§7"));
    }

    @Test
    @DisplayName("a cell ID of 0 removes the override rather than assigning cell 0")
    void cellIdZeroRemovesTheOverride(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));
        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 0));

        assertTrue(stored(project).isEmpty(), "cellId 0 means 'no override', not 'cell 0'");
    }

    @Test
    @DisplayName("writing one recording leaves another recording's overrides alone")
    void otherRecordingsArePreserved(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));
        writer.writeSquareOverridesToFile(entry(REC_B), assign(7, 5));

        assertEquals(3, stored(project).get(REC_A + "§7"));
        assertEquals(5, stored(project).get(REC_B + "§7"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Writing: replace semantics
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("replace discards this recording's other squares, but not another recording's")
    void replaceDiscardsOnlyThisRecording(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));
        writer.writeSquareOverridesToFile(entry(REC_A), assign(8, 4));
        writer.writeSquareOverridesToFile(entry(REC_B), assign(9, 5));

        writer.replaceSquareOverrides(entry(REC_A), assign(8, 6));

        assertFalse(stored(project).containsKey(REC_A + "§7"), "replace drops square 7");
        assertEquals(6, stored(project).get(REC_A + "§8"));
        assertEquals(5, stored(project).get(REC_B + "§9"), "another recording is untouched");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Querying
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("hasOverridesFor is true only for a recording that actually has some")
    void hasOverridesForIsSpecific(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);

        assertFalse(writer.hasOverridesFor(entry(REC_A)), "no file yet");

        writer.writeSquareOverridesToFile(entry(REC_A), assign(7, 3));

        assertTrue(writer.hasOverridesFor(entry(REC_A)));
        assertFalse(writer.hasOverridesFor(entry(REC_B)));
    }

    @Test
    @DisplayName("a recording whose name is a prefix of another is not confused with it")
    void prefixNamesAreNotConfused(@TempDir Path project) {
        // "…-A1-1" is a prefix of "…-A1-10". The row match is on "experiment,recording,", and
        // it is the trailing comma that keeps these apart. Two bugs of this exact shape were
        // found elsewhere in the Viewer, so hold the line here.
        String shortName = "221012-Exp-1-A1-1";
        String longName  = "221012-Exp-1-A1-10";

        WriteSquareOverride writer = new WriteSquareOverride(project);
        writer.writeSquareOverridesToFile(entry(longName), assign(7, 5));

        assertFalse(writer.hasOverridesFor(entry(shortName)),
                    "-A1-1 has no overrides; only -A1-10 does");

        writer.replaceSquareOverrides(entry(shortName), assign(1, 2));

        assertEquals(5, stored(project).get(longName + "§7"),
                     "replacing -A1-1 must not delete -A1-10's overrides");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Applying to the Squares table
    // ─────────────────────────────────────────────────────────────────────────────

    /** Minimal Squares table: applySquareOverrides only reads these four columns. */
    private static Table squares(String[] recordingNames, int[] squareNumbers) {
        StringColumn experiment = StringColumn.create("Experiment Name");
        StringColumn recording  = StringColumn.create("Recording Name");
        IntColumn    number     = IntColumn.create("Square Number");
        IntColumn    cellId     = IntColumn.create("Cell ID");
        for (int i = 0; i < recordingNames.length; i++) {
            experiment.append(EXPERIMENT);
            recording.append(recordingNames[i]);
            number.append(squareNumbers[i]);
            cellId.append(0);
        }
        return Table.create("Squares", experiment, recording, number, cellId);
    }

    private static int cellIdOf(Table t, int row) {
        return t.intColumn("Cell ID").get(row);
    }

    @Test
    @DisplayName("export sets the cell ID on exactly the overridden square")
    void applySetsOnlyTheMatchingSquare(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);
        writer.writeSquareOverridesToFile(entry(REC_A), assign(2, 6));

        Table t = squares(new String[]{REC_A, REC_A, REC_B},
                          new int[]   {1,     2,     2});

        applySquareOverrides(t, loadSquareOverride(
                project.resolve("Viewer").resolve("Square Override.csv")));

        assertEquals(0, cellIdOf(t, 0), "square 1 of A was not overridden");
        assertEquals(6, cellIdOf(t, 1), "square 2 of A was");
        assertEquals(0, cellIdOf(t, 2), "square 2 of B is a different recording");
    }

    @Test
    @DisplayName("export ignores an override for a square that is not in the table")
    void applyIgnoresUnknownSquares(@TempDir Path project) {
        WriteSquareOverride writer = new WriteSquareOverride(project);
        writer.writeSquareOverridesToFile(entry(REC_A), assign(999, 6));

        Table t = squares(new String[]{REC_A}, new int[]{1});

        applySquareOverrides(t, loadSquareOverride(
                project.resolve("Viewer").resolve("Square Override.csv")));

        assertEquals(0, cellIdOf(t, 0));
    }
}
