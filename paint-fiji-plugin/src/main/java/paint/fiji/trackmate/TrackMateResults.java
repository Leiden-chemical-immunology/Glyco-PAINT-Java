package paint.fiji.trackmate;

import java.time.Duration;

/**
 *
 */
public class TrackMateResults {
    private final boolean success;
    private final int numberOfSpots;
    private final int numberOfTracks;
    private final int numberOfFilteredTracks;
    private final int numberOFrames;
    private final Duration duration;
    private final int numberOfSpotsInALlTracks;

    public TrackMateResults(boolean success) {
        this.success = success;
        this.numberOfSpots = 0;
        this.numberOfTracks = 0;
        this.numberOfFilteredTracks = 0;
        this.numberOFrames = 0;
        this.duration = null;
        this.numberOfSpotsInALlTracks = 0;
    }

    public TrackMateResults(boolean success, int numberOfSpots, int numberOfTracks, int numberOfFilteredTracks, int numberOfFrames,  Duration duration, int numberOfSpotsInALlTracks) {
        this.success = success;
        this.numberOfSpots = numberOfSpots;
        this.numberOfTracks = numberOfTracks;
        this.numberOfFilteredTracks = numberOfFilteredTracks;
        this.numberOFrames = numberOfFrames;
        this.duration = duration;
        this.numberOfSpotsInALlTracks = numberOfSpotsInALlTracks;
    }

    public int getNumberOfSpots() { return numberOfSpots; }
    public int getNumberOfTracks() { return numberOfTracks; }
    public int getNumberOfFilteredTracks() { return numberOfFilteredTracks; }
    public int getNumberOfFrames() { return numberOFrames; }
    public Duration getDuration() { return duration; }
    public int getNumberOfSpotsInALlTracks() { return numberOfSpotsInALlTracks; }
    public boolean isSuccess() { return success; }

    public String toString() {
        return String.format("Success: %b, Spots: %d, Tracks: %d, Filtered Tracks: %d, Frames: %d, Milliseconds: %d",
                success, numberOfSpots, numberOfTracks, numberOfFilteredTracks, numberOFrames, duration.toMillis());
    }
}
