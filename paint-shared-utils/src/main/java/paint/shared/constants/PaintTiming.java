/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.constants;

/**
 * Defines timing-related constants and playback settings for the PAINT application.
 * <p>
 * The {@code PaintTiming} class centralizes constants related to frame intervals, acquisition
 * rates, and UI animation/playback timings.
 * </p>
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