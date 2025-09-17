import java.util.*;


public class TestFrequency {
    public static void main(String[] args) {
        double[] trackDurations = {2.5, 3.0, 2.5, 4.0, 5.0, 3.0, 2.5, 4.0, 5.0, 6.5, 5.0};

        // Use a TreeMap to store frequency counts automatically sorted by keys (Track Duration)
        Map<Double, Integer> frequencyMap = new TreeMap<>();

        // Count frequencies
        for (double duration : trackDurations) {
            frequencyMap.put(duration, frequencyMap.getOrDefault(duration, 0) + 1);
        }

        // Print frequency distribution
        System.out.println("Track Duration | Frequency");
        for (Map.Entry<Double, Integer> entry : frequencyMap.entrySet()) {
            System.out.printf("%14.2f | %9d%n", entry.getKey(), entry.getValue());
        }
    }
}

