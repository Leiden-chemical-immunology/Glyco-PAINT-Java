package paint.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link Miscellaneous}.
 *
 * <p>The duration cases below are the ones that used to live in a {@code main()} in
 * {@code Miscellaneous} itself: it printed them and left a human to eyeball the output. They
 * were well-chosen boundaries (the minute and hour rollovers, and the singular/plural edges),
 * so they are kept — but as assertions, where they can actually fail.
 */
class MiscellaneousTest {

    @ParameterizedTest(name = "{0}s -> \"{1}\"")
    @DisplayName("formatDuration renders seconds, minutes and hours with correct pluralisation")
    @CsvSource({
            // Seconds only. Zero must still say something.
            "0,    '0 seconds'",
            "1,    '1 second'",
            "5,    '5 seconds'",
            "59,   '59 seconds'",

            // Exactly on a rollover: the zero component is dropped, not printed as '0 seconds'.
            "60,   '1 minute'",
            "3600, '1 hour'",

            // Just past a rollover.
            "61,   '1 minute 1 second'",
            "65,   '1 minute 5 seconds'",
            "3605, '1 hour 5 seconds'",

            // Just before a rollover.
            "3599, '59 minutes 59 seconds'",

            // All three components, singular and plural.
            "3665, '1 hour 1 minute 5 seconds'",
            "7322, '2 hours 2 minutes 2 seconds'"
    })
    void formatsDurations(int seconds, String expected) {
        assertEquals(expected, Miscellaneous.formatDuration(seconds));
    }

    @Test
    @DisplayName("the Duration overload agrees with the seconds overload")
    void durationOverloadAgrees() {
        assertEquals(Miscellaneous.formatDuration(3665),
                     Miscellaneous.formatDuration(Duration.ofSeconds(3665)));
        assertEquals("1 hour 1 minute 5 seconds",
                     Miscellaneous.formatDuration(Duration.ofSeconds(3665)));
    }

    @Test
    @DisplayName("friendlyMessage strips the exception class prefix")
    void friendlyMessageStripsPrefix() {
        assertEquals("something broke",
                Miscellaneous.friendlyMessage(new IllegalStateException("something broke")));
    }

    @Test
    @DisplayName("friendlyMessage tolerates null rather than throwing")
    void friendlyMessageTolerantOfNull() {
        assertEquals("", Miscellaneous.friendlyMessage(null));
    }
}
