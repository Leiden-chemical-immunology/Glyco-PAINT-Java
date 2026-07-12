/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import paint.shared.objects.Square;
import paint.viewer.override.recording_override.RecordingOverride;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.io.MainIOInterface.squareListToTable;
import static paint.shared.io.MainIOInterface.squareTableToList;
import static paint.viewer.override.ExportOverridesFromViewer.applyRecordingOverrides;

/**
 * Covers {@link ExportOverridesFromViewer#applyRecordingOverrides}, which decides — via the
 * per-recording selection thresholds — which squares end up visible in the exported Squares
 * file. It is therefore the point at which an override actually changes the analysis.
 * <p>
 * The essential property is <em>containment</em>: applying an override to one recording must
 * recompute the visibility of that recording's squares and leave every other recording's
 * squares exactly as they were. The method converts the whole squares table to objects and
 * back, so it would be easy for it to disturb recordings it was never asked about.
 * </p>
 */
class ApplyRecordingOverridesTest {

    private static final String EXPERIMENT = "221012";
    private static final String REC_A      = "221012-Exp-1-A1-1";
    private static final String REC_B      = "221012-Exp-1-B2-3";

    /** Neighbour mode "Free" skips the neighbour pass, so visibility is purely the thresholds. */
    private static final String FREE = "Free";

    /**
     * A square with the three values the filter tests. Visible is seeded {@code true} so that a
     * square being switched *off* is observable, not just a square being switched on.
     */
    private static Square square(String recording, int number,
                                 double densityRatio, double variability, double rSquared) {
        Square s = new Square();
        s.setExperimentName(EXPERIMENT);
        s.setRecordingName(recording);
        s.setSquareNumber(number);
        s.setRowNumber(number / 10);
        s.setColNumber(number % 10);
        s.setDensityRatio(densityRatio);
        s.setVariability(variability);
        s.setRSquared(rSquared);
        s.setVisible(true);
        return s;
    }

    private static Table recordingsTable(String... recordingNames) {
        StringColumn experiment  = StringColumn.create("Experiment Name");
        StringColumn recording   = StringColumn.create("Recording Name");
        DoubleColumn density     = DoubleColumn.create("Min Required Density Ratio");
        DoubleColumn rSquared    = DoubleColumn.create("Min Required R Squared");
        DoubleColumn variability = DoubleColumn.create("Max Allowable Variability");
        StringColumn mode        = StringColumn.create("Neighbour Mode");

        for (String r : recordingNames) {
            experiment.append(EXPERIMENT);
            recording.append(r);
            density.append(0.0);
            rSquared.append(0.0);
            variability.append(99.0);
            mode.append(FREE);
        }
        return Table.create("Recordings", experiment, recording, density, rSquared, variability, mode);
    }

    private static RecordingOverride override(String recordingName,
                                              double minDensity, double maxVariability, double minRSquared) {
        RecordingOverride o = new RecordingOverride();
        o.setExperimentName(EXPERIMENT);
        o.setRecordingName(recordingName);
        o.setMinRequiredDensityRatio(minDensity);
        o.setMaxAllowableVariability(maxVariability);
        o.setMinRequiredRSquared(minRSquared);
        o.setNeighbourMode(FREE);
        return o;
    }

    /** Reads the visibility of one square back out of the (mutated) squares table. */
    private static boolean visible(Table squares, String recording, int squareNumber) {
        for (Square s : squareTableToList(squares)) {
            if (recording.equals(s.getRecordingName()) && s.getSquareNumber() == squareNumber) {
                return s.isVisible();
            }
        }
        throw new IllegalArgumentException("No such square: " + recording + " #" + squareNumber);
    }

    @Test
    @DisplayName("an override recomputes visibility for its own recording only")
    void overrideAffectsOnlyItsOwnRecording() {
        List<Square> squares = new ArrayList<>();
        // Recording A: one square that will pass a strict threshold, one that will not.
        squares.add(square(REC_A, 1, /*density*/ 5.0, /*variability*/ 1.0, /*r2*/ 0.9));
        squares.add(square(REC_A, 2, /*density*/ 0.5, /*variability*/ 1.0, /*r2*/ 0.9));
        // Recording B: identical values, but B is not overridden. It must not be touched.
        squares.add(square(REC_B, 1, /*density*/ 0.5, /*variability*/ 1.0, /*r2*/ 0.9));

        Table squaresTable    = squareListToTable(squares);
        Table recordings      = recordingsTable(REC_A, REC_B);

        applyRecordingOverrides(recordings, squaresTable,
                                Collections.singletonList(override(REC_A, 2.0, 5.0, 0.5)));

        assertTrue(visible(squaresTable, REC_A, 1),  "A#1 has density 5.0 >= 2.0, so it stays visible");
        assertFalse(visible(squaresTable, REC_A, 2), "A#2 has density 0.5 < 2.0, so it is filtered out");
        assertTrue(visible(squaresTable, REC_B, 1),
                   "B was not overridden: its squares must be left exactly as they were, "
                           + "even though B#1 would fail A's thresholds");
    }

    @Test
    @DisplayName("the override's thresholds are written back into the recordings table")
    void thresholdsLandInTheRecordingsTable() {
        Table squaresTable = squareListToTable(
                Collections.singletonList(square(REC_A, 1, 5.0, 1.0, 0.9)));
        Table recordings   = recordingsTable(REC_A, REC_B);

        applyRecordingOverrides(recordings, squaresTable,
                                Collections.singletonList(override(REC_A, 2.0, 5.0, 0.5)));

        assertEquals(2.0, recordings.doubleColumn("Min Required Density Ratio").get(0), 1e-9);
        assertEquals(5.0, recordings.doubleColumn("Max Allowable Variability").get(0), 1e-9);
        assertEquals(0.5, recordings.doubleColumn("Min Required R Squared").get(0), 1e-9);

        assertEquals(0.0,  recordings.doubleColumn("Min Required Density Ratio").get(1), 1e-9,
                     "the recording that was not overridden keeps its own thresholds");
        assertEquals(99.0, recordings.doubleColumn("Max Allowable Variability").get(1), 1e-9);
    }

    @Test
    @DisplayName("two overrides both apply, and neither undoes the other")
    void twoOverridesBothApply() {
        // This is what the loop-hoisting has to get right: the squares are converted to objects
        // once and both recordings are filtered against that one list. If the second override
        // rebuilt its list from a stale table, it would discard the first one's work.
        List<Square> squares = new ArrayList<>();
        squares.add(square(REC_A, 1, 0.5, 1.0, 0.9));   // will fail A's density threshold
        squares.add(square(REC_B, 1, 0.5, 1.0, 0.9));   // will fail B's density threshold
        squares.add(square(REC_A, 2, 5.0, 1.0, 0.9));   // passes
        squares.add(square(REC_B, 2, 5.0, 1.0, 0.9));   // passes

        Table squaresTable = squareListToTable(squares);
        Table recordings   = recordingsTable(REC_A, REC_B);

        List<RecordingOverride> overrides = new ArrayList<>();
        overrides.add(override(REC_A, 2.0, 5.0, 0.5));
        overrides.add(override(REC_B, 2.0, 5.0, 0.5));

        applyRecordingOverrides(recordings, squaresTable, overrides);

        assertFalse(visible(squaresTable, REC_A, 1), "A#1 filtered out by A's override");
        assertFalse(visible(squaresTable, REC_B, 1), "B#1 filtered out by B's override");
        assertTrue(visible(squaresTable, REC_A, 2),  "A#2 passes");
        assertTrue(visible(squaresTable, REC_B, 2),  "B#2 passes");
    }

    @Test
    @DisplayName("with no overrides, the squares table is left completely alone")
    void noOverridesChangesNothing() {
        List<Square> squares = new ArrayList<>();
        squares.add(square(REC_A, 1, 0.0, 99.0, Double.NaN));   // would fail any real filter
        Table squaresTable = squareListToTable(squares);
        Table recordings   = recordingsTable(REC_A);

        applyRecordingOverrides(recordings, squaresTable, Collections.<RecordingOverride>emptyList());

        assertTrue(visible(squaresTable, REC_A, 1),
                   "no override means no filtering: the square keeps the visibility it had");
    }
}
