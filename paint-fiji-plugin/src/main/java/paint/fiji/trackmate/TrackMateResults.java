package paint.fiji.trackmate;

import java.time.Duration;

/**
 * Represents the results of a TrackMate analysis.
 *
 * This class is designed to encapsulate the outcomes of a TrackMate analysis, such as
 * whether the analysis was successful, the number of detected spots, tracks,
 * filtered tracks, and various other statistics related to the analysis process,
 * including duration and frame count.
 */
public class TrackMateResults {

    // @formatter: off
    private final boolean success;                  // Whether the TrackMate analysis finished successfully.
    private final boolean calculationPerformed;     // Whether the TrackMate analysis was performed
    private final int numberOfSpots;            // Number of detected spots in the analysis
    private final int numberOfTracks;           // Number of tracks generated from detected spots
    private final int numberOfFilteredTracks;   // Number of tracks remaining after filtering.
    private final int numberOFrames;            // Number of frames analyzed.
    private final Duration duration;                 // Total runtime of the analysis
    private final int numberOfSpotsInALlTracks; // Total number of spots contained in all tracks
    // @formatter: on

    /**
     * Constructs a {@code TrackMateResults} instance with basic analysis results.
     *
     * @param success               whether the analysis succeeded
     * @param calculationPerformed  whether the calculation was performed
     */
    public TrackMateResults(boolean success, boolean calculationPerformed) {

        // @formatter: off
        this.success = success;
        this.calculationPerformed = calculationPerformed;
        this.numberOfSpots = 0;
        this.numberOfTracks = 0;
        this.numberOfFilteredTracks = 0;
        this.numberOFrames = 0;
        this.duration = null;
        this.numberOfSpotsInALlTracks = 0;
        // @formatter: on
    }

    /**
     * Constructs a {@code TrackMateResults} instance with the specified success flag.
     *
     * @param success whether the analysis succeeded
     */
    public TrackMateResults(boolean success) {

        // @formatter: off
        this.success = success;
        this.calculationPerformed = false;
        this.numberOfSpots = 0;
        this.numberOfTracks = 0;
        this.numberOfFilteredTracks = 0;
        this.numberOFrames = 0;
        this.duration = null;
        this.numberOfSpotsInALlTracks = 0;
        // @formatter: on
    }

    /**
     * Constructs a {@code TrackMateResults} instance with detailed analysis results.
     *
     * @param success whether the analysis succeeded
     * @param calculationPerformed whether the calculation was performed
     * @param numberOfSpots the total number of detected spots
     * @param numberOfTracks the total number of tracks identified
     * @param numberOfFilteredTracks the number of tracks that passed filtering criteria
     * @param numberOfFrames the total number of frames analyzed
     * @param duration the time duration of the analysis
     * @param numberOfSpotsInALlTracks the number of spots present across all tracks
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

    public int getNumberOfSpots() {
        return numberOfSpots;
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public int getNumberOfFilteredTracks() {
        return numberOfFilteredTracks;
    }

    public int getNumberOfFrames() {
        return numberOFrames;
    }

    public Duration getDuration() {
        return duration;
    }

    public int getNumberOfSpotsInALlTracks() {
        return numberOfSpotsInALlTracks;
    }

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
