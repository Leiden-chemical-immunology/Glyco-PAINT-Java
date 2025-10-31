/******************************************************************************
 *  Class:        TrackMateResults.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Represents the outcome of a TrackMate analysis run, encapsulating
 *    success flags, detection statistics, frame counts, and runtime duration.
 *
 *  DESCRIPTION:
 *    • Captures and stores per-recording results after TrackMate processing.
 *    • Tracks number of detected spots, total and filtered tracks, and
 *      the number of frames analyzed.
 *    • Provides duration metrics for performance reporting.
 *    • Used by {@link RunTrackMateOnRecording} and higher-level classes
 *      to summarize execution results.
 *
 *  RESPONSIBILITIES:
 *    • Store immutable TrackMate result data for one recording.
 *    • Provide structured accessors for logging and reporting.
 *    • Support consistent summary formatting via {@link #toString()}.
 *
 *  USAGE EXAMPLE:
 *    TrackMateResults result = new TrackMateResults(
 *        true, true, 452, 87, 73, 300, Duration.ofSeconds(28), 6124);
 *
 *  DEPENDENCIES:
 *    – java.time.Duration
 *    – paint.fiji.trackmate.RunTrackMateOnRecording
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

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
     * Default constructor.
     * <p>
     * Creates an immutable {@code TrackMateResults} instance with all
     * values initialized to default (false, zero, or null).
     * </p>
     */
    public TrackMateResults() {
        this(false, false, 0, 0, 0, 0, null, 0);
    }

    /**
     * Constructs a {@code TrackMateResults} instance with a success flag and
     * calculation state only, setting all numeric values to zero.
     *
     * @param success              whether the analysis succeeded
     * @param calculationPerformed whether the TrackMate pipeline was executed
     */
    public TrackMateResults(boolean success, boolean calculationPerformed) {
        this(success, calculationPerformed, 0, 0, 0, 0, null, 0);
    }

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
     * @return total number of generated tracks
     */
    public int getNumberOfTracks() {
        return numberOfTracks;
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