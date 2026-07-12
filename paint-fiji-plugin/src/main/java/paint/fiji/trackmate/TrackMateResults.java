/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;

import java.time.Duration;

/**
 * Immutable container for the results of a TrackMate analysis.
 * <p>
 * Encapsulates statistics such as the number of detected spots,
 * total and filtered tracks, number of analyzed frames, and
 * runtime duration.
 * </p>
 */
public class TrackMateResults {
    private final boolean  success;
    private final boolean  calculationPerformed;
    private final int      numberOfSpots;
    private final int      numberOfTracks;
    private final int      numberOfFilteredTracks;
    private final int      numberOfFrames;
    private final Duration duration;
    private final int      numberOfSpotsInAllTracks;


    /**
     * Constructs a {@code TrackMateResults} instance with only a success flag.
     * Assumes no calculations were performed and sets all numeric fields to zero.
     *
     * @param success whether the analysis succeeded
     */
    public TrackMateResults(boolean success) {
        this(success, false, 0, 0, 0, 0, null, 0);
    }

    /**
     * Constructs a fully detailed {@code TrackMateResults} instance.
     *
     * @param success                true if the analysis succeeded
     * @param calculationPerformed   true if processing was executed
     * @param numberOfSpots          number of detected spots
     * @param numberOfTracks         total number of identified tracks
     * @param numberOfFilteredTracks number of tracks after filtering
     * @param numberOfFrames         total number of frames analyzed
     * @param duration               runtime duration of analysis
     * @param numberOfSpotsInAllTracks total number of spots in all tracks
     */
    public TrackMateResults(boolean success,
                            boolean calculationPerformed,
                            int numberOfSpots,
                            int numberOfTracks,
                            int numberOfFilteredTracks,
                            int numberOfFrames,
                            Duration duration,
                            int numberOfSpotsInAllTracks) {
        this.success                  = success;
        this.calculationPerformed     = calculationPerformed;
        this.numberOfSpots            = numberOfSpots;
        this.numberOfTracks           = numberOfTracks;
        this.numberOfFilteredTracks   = numberOfFilteredTracks;
        this.numberOfFrames           = numberOfFrames;
        this.duration                 = duration;
        this.numberOfSpotsInAllTracks = numberOfSpotsInAllTracks;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * @return true if the analysis completed successfully
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return true if the calculation was performed
     */
    public boolean isCalculationPerformed() {
        return calculationPerformed;
    }

    /**
     * @return total number of detected spots
     */
    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    /**
     * @return number of tracks that passed filtering
     */
    public int getNumberOfFilteredTracks() {
        return numberOfFilteredTracks;
    }

    /**
     * @return number of frames analyzed
     */
    public int getNumberOfFrames() {
        return numberOfFrames;
    }

    /**
     * @return duration of the analysis, or {@code null} if unavailable
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * @return number of spots across all tracks
     */
    public int getNumberOfSpotsInAllTracks() {
        return numberOfSpotsInAllTracks;
    }

    // -------------------------------------------------------------------------
    // Representation
    // -------------------------------------------------------------------------

    /**
     * Returns a formatted summary string containing all key metrics.
     *
     * @return a string summarizing TrackMate run results
     */
    @Override
    public String toString() {
        return String.format(
                "Success: %b, Spots: %d, Tracks: %d, Filtered: %d, Frames: %d, Duration(ms): %d",
                success,
                numberOfSpots,
                numberOfTracks,
                numberOfFilteredTracks,
                numberOfFrames,
                duration != null ? duration.toMillis() : 0
        );
    }
}