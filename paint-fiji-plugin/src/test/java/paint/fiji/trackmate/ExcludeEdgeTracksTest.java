/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Spot;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link RunTrackMateOnRecording#excludeEdgeTracks(Model, int, int)} on a small model
 * built by hand, with frames 0..9.
 */
class ExcludeEdgeTracksTest {

    private static final int FIRST_FRAME = 0;
    private static final int LAST_FRAME  = 9;

    /** Builds a track of linked spots, one per frame from {@code from} to {@code to}. */
    private static Spot addTrack(Model model, double x, int from, int to) {
        Spot first    = null;
        Spot previous = null;
        for (int frame = from; frame <= to; frame++) {
            Spot spot = new Spot(x, 0.0, 0.0, 0.5, 1.0);
            spot.putFeature(Spot.FRAME, (double) frame);
            model.addSpotTo(spot, frame);
            if (previous != null) {
                model.addEdge(previous, spot, 1.0);
            }
            if (first == null) {
                first = spot;
            }
            previous = spot;
        }
        return first;
    }

    private static Integer trackIdOf(Model model, Spot spot) {
        return model.getTrackModel().trackIDOf(spot);
    }

    @Test
    void hidesTracksTouchingFirstOrLastFrameOnly() {
        Model model = new Model();
        model.beginUpdate();
        Spot startsAtFirst = addTrack(model, 1.0, 0, 4);   // in frame 0
        Spot endsAtLast    = addTrack(model, 2.0, 5, 9);   // in frame 9
        Spot spansAll      = addTrack(model, 3.0, 0, 9);   // in both
        Spot inside        = addTrack(model, 4.0, 1, 8);   // neither
        Spot shortInside   = addTrack(model, 5.0, 3, 4);   // neither
        model.endUpdate();

        assertEquals(5, model.getTrackModel().nTracks(true));

        int hidden = RunTrackMateOnRecording.excludeEdgeTracks(model, FIRST_FRAME, LAST_FRAME);

        assertEquals(3, hidden);
        assertEquals(2, model.getTrackModel().nTracks(true));

        Set<Integer> visible = model.getTrackModel().trackIDs(true);
        assertFalse(visible.contains(trackIdOf(model, startsAtFirst)));
        assertFalse(visible.contains(trackIdOf(model, endsAtLast)));
        assertFalse(visible.contains(trackIdOf(model, spansAll)));
        assertTrue(visible.contains(trackIdOf(model, inside)));
        assertTrue(visible.contains(trackIdOf(model, shortInside)));
    }

    @Test
    void ignoresTracksAlreadyHidden() {
        Model model = new Model();
        model.beginUpdate();
        Spot edge = addTrack(model, 1.0, 0, 3);
        addTrack(model, 2.0, 2, 5);
        model.endUpdate();

        // Simulate a track that TrackMate's own filters already rejected.
        model.beginUpdate();
        model.setTrackVisibility(trackIdOf(model, edge), false);
        model.endUpdate();

        int hidden = RunTrackMateOnRecording.excludeEdgeTracks(model, FIRST_FRAME, LAST_FRAME);

        assertEquals(0, hidden);
        assertEquals(1, model.getTrackModel().nTracks(true));
    }

    @Test
    void nothingToHideWhenNoTrackTouchesTheEdges() {
        Model model = new Model();
        model.beginUpdate();
        addTrack(model, 1.0, 1, 8);
        addTrack(model, 2.0, 2, 3);
        model.endUpdate();

        assertEquals(0, RunTrackMateOnRecording.excludeEdgeTracks(model, FIRST_FRAME, LAST_FRAME));
        assertEquals(2, model.getTrackModel().nTracks(true));
    }
}
