package generatesquares.calc;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalculateDensity {

    /**
     * Calculate the density of tracks in a square.
     *
     * @param nrTracks      number of tracks
     * @param area          area of the square (in µm²)
     * @param time          time in seconds (normally 100 sec = 2000 frames)
     * @param concentration concentration factor for normalization
     * @return density value
     */

    public static double calculateDensity(int nrTracks, double area, double time, double concentration) {
        if (area <= 0 || time <= 0 || concentration <= 0) {
            throw new IllegalArgumentException("Area, time, and concentration must be positive");
        }

        double density = nrTracks / area;
        density /= time;
        density /= concentration;

        return density;
    }

    public static double calculateAverageTrackCountOfBackground(Recording recording, int nrOfAverageCountSquares) {

        List<Integer> trackCounts = new ArrayList<>();
        List<Square> squares = recording.getSquaresOfRecording();

        for (Square sq : squares) {
            trackCounts.add(sq.getTracks().size());
        }

        // Sort descending
        trackCounts.sort(Collections.reverseOrder());

        int total = 0;
        int n = 0;

        // Find the first non-zero value
        int m;
        for (m = trackCounts.size() - 1; m >= 0; m--) {
            if (trackCounts.get(m) != 0) {
                break;
            }
        }

        // Iterate from the smallest to the largest (like Python's reverse loop)
        for (int i = m; i >= 0; i--) {
            int v = trackCounts.get(i);

            total += v;
            n++;
            if (n >= nrOfAverageCountSquares) {
                break;
            }

        }

        if (n == 0) {
            return 0.0;
        } else {
            return (double) total / n;
        }
    }
}