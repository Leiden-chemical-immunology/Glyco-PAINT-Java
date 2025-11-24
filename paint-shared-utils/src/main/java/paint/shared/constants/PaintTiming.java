/*=============================================================================
 *  Class:        PaintTiming.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines timing-related constants for image acquisition and recordings.
 *============================================================================*/

package paint.shared.constants;

public final class PaintTiming {

    private PaintTiming() {
        // Prevent instantiation
    }

    // =====================
    // Timing
    // =====================

    /** Time between images (in seconds). */
    public static final double TIME_INTERVAL      = 0.05;

    /** Number of frames in a recording. */
    public static final double FRAMES             = 2000;

    /** Total duration of a recording (in seconds). */
    public static final double RECORDING_DURATION = FRAMES * TIME_INTERVAL;
    // TODO These should be JSON parameters
}