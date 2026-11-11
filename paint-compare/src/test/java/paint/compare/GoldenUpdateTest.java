/*
 * Copyright (c) 2026 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.compare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("GoldenUpdate — which regression gates may re-bless their reference")
class GoldenUpdateTest {

    @Test
    @DisplayName("no property: no gate re-blesses")
    void none() {
        assertEquals(Set.of(), GoldenUpdate.requested(null));
        assertEquals(Set.of(), GoldenUpdate.requested(" "));
    }

    @Test
    @DisplayName("one gate named: only that gate re-blesses")
    void oneGate() {
        assertEquals(Set.of("squares"), GoldenUpdate.requested("squares"));
        assertEquals(Set.of("trackmate"), GoldenUpdate.requested(" TrackMate "));
    }

    @Test
    @DisplayName("both gates named")
    void bothGates() {
        assertEquals(Set.of("squares", "trackmate"), GoldenUpdate.requested("squares, trackmate"));
    }

    @Test
    @DisplayName("the old 'true', or an unknown name, is refused")
    void trueRefused() {
        assertThrows(IllegalArgumentException.class, () -> GoldenUpdate.requested("true"));
        assertThrows(IllegalArgumentException.class, () -> GoldenUpdate.requested("squares,tracks"));
    }
}
