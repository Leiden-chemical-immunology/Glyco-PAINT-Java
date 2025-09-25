package paint.fiji.trackmate;

import java.time.Duration;

/**
 * Encapsulates the results of a TrackMate analysis run in Fiji.
 * <p>
 * This class holds summary statistics about the detection and tracking process,
 * including the number of detected spots, generated tracks, filtered tracks,
 * frames analyzed, runtime duration, and whether the analysis was successful.
 * </p>
 */
public class TrackMateResults {

    /**
     * Whether the TrackMate analysis finished successfully.
     */
    private final boolean success;

    /**
     * Whether the TrackMate analysis was performed
     */
    private final boolean calculationPerformed;

    /**
     * Number of detected spots in the analysis.
     */
    private final int numberOfSpots;

    /**
     * Number of tracks generated from detected spots.
     */
    private final int numberOfTracks;

    /**
     * Number of tracks remaining after filtering.
     */
    private final int numberOfFilteredTracks;

    /**
     * Number of frames analyzed.
     */
    private final int numberOFrames;

    /**
     * Total runtime of the analysis.
     */
    private final Duration duration;

    /**
     * Total number of spots contained in all tracks.
     */
    private final int numberOfSpotsInALlTracks;

    /**
     * Constructs a {@code TrackMateResults} instance representing a failed run.
     * <p>
     * All numeric values are set to zero and the duration is {@code null}.
     * </p>
     *
     * @param success whether the analysis succeeded (typically {@code false})
     */
    public TrackMateResults(boolean success, boolean calculationPerformed) {
        this.success = success;
        this.calculationPerformed = false;
        this.numberOfSpots = 0;
        this.numberOfTracks = 0;
        this.numberOfFilteredTracks = 0;
        this.numberOFrames = 0;
        this.duration = null;
        this.numberOfSpotsInALlTracks = 0;
    }

    public TrackMateResults(boolean success) {
        this.success = success;
        this.calculationPerformed = false;
        this.numberOfSpots = 0;
        this.numberOfTracks = 0;
        this.numberOfFilteredTracks = 0;
        this.numberOFrames = 0;
        this.duration = null;
        this.numberOfSpotsInALlTracks = 0;
    }

    /**
     * Constructs a {@code TrackMateResults} instance with full result details.
     *
     * @param success                  whether the analysis succeeded
     * @param numberOfSpots            number of detected spots
     * @param numberOfTracks           number of generated tracks
     * @param numberOfFilteredTracks   number of tracks after filtering
     * @param numberOfFrames           number of frames analyzed
     * @param duration                 runtime duration of the analysis
     * @param numberOfSpotsInALlTracks total number of spots in all tracks
     */
    public TrackMateResults(boolean success,
                            boolean calculationPerformed,
                            int numberOfSpots,
                            int numberOfTracks,
                            int numberOfFilteredTracks,
                            int numberOfFrames,
                            Duration duration,
                            int numberOfSpotsInALlTracks) {
        this.success = success;
        this.calculationPerformed = calculationPerformed;
        this.numberOfSpots = numberOfSpots;
        this.numberOfTracks = numberOfTracks;
        this.numberOfFilteredTracks = numberOfFilteredTracks;
        this.numberOFrames = numberOfFrames;
        this.duration = duration;
        this.numberOfSpotsInALlTracks = numberOfSpotsInALlTracks;
    }

    /**
     * @return the number of detected spots
     */
    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    /**
     * @return the number of generated tracks
     */
    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    /**
     * @return the number of tracks after filtering
     */
    public int getNumberOfFilteredTracks() {
        return numberOfFilteredTracks;
    }

    /**
     * @return the number of frames analyzed
     */
    public int getNumberOfFrames() {
        return numberOFrames;
    }

    /**
     * @return the total runtime duration
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * @return the number of spots contained in all tracks
     */
    public int getNumberOfSpotsInALlTracks() {
        return numberOfSpotsInALlTracks;
    }

    /**
     * @return {@code true} if the analysis succeeded, otherwise {@code false}
     */
    public boolean isSuccess() {
        return success;
    }

    public boolean isCalculationPerformed() {
        return calculationPerformed;
    }

    /**
     * Returns a formatted string summarizing the results.
     *
     * @return human-readable string with success flag, counts, and duration
     */
    @Override
    public String toString() {
        return String.format("Success: %b, Spots: %d, Tracks: %d, Filtered Tracks: %d, Frames: %d, Milliseconds: %d",
                success,
                numberOfSpots,
                numberOfTracks,
                numberOfFilteredTracks,
                numberOFrames,
                duration != null ? duration.toMillis() : 0);
    }
}
