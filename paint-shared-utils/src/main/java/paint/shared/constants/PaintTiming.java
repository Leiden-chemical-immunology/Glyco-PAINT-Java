/*=============================================================================
 *  Class:        PaintTiming.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines timing-related constants and playback settings for the PAINT
 *    application.
 *
 *  DESCRIPTION:
 *    The {@code PaintTiming} class centralizes constants related to
 *    frame intervals, acquisition rates, and UI animation/playback
 *    timings.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.constants;

/**
 * Defines timing-related constants and playback settings for the PAINT
 * application.
 */
public final class PaintTiming {

    private PaintTiming() {
        // Prevent instantiation
    }

    /** Default time interval between frames in seconds. */
    public static final double TIME_INTERVAL      = 0.05;
    /** Default number of frames per recording. */
    public static final double FRAMES             = 2000;
    /** Default total duration of a recording in seconds. */
    public static final double RECORDING_DURATION = FRAMES * TIME_INTERVAL;
    // TODO These should be JSON parameters
}