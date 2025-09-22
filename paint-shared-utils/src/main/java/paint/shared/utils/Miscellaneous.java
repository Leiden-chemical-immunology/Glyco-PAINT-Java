package paint.shared.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public class Miscellaneous {

    public static void main(String[] args) {
        int[] testValues = {0, 5, 59, 60, 61, 65, 3599, 3600, 3605, 3665, 7322};

        for (int seconds : testValues) {
            System.out.printf("%5d seconds → %s%n", seconds, formatDuration(seconds));
        }
    }

    public static String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append(" hour").append(hours == 1 ? "" : "s").append(" ");
        }
        if (minutes > 0) {
            sb.append(minutes).append(" minute").append(minutes == 1 ? "" : "s").append(" ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append(" second").append(secs == 1 ? "" : "s");
        }

        return sb.toString().trim();
    }

    public static List<String[]> readTableAsStrings(Path csvPath) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(csvPath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            // Add header row
            List<String> header = new ArrayList<>(csvParser.getHeaderMap().keySet());
            rows.add(header.toArray(new String[0]));

            for (CSVRecord record : csvParser) {
                String[] row = new String[record.size()];
                for (int i = 0; i < record.size(); i++) {
                    row[i] = record.get(i);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public static String friendlyMessage(Throwable t) {
        if (t == null)
            return "";
        String m = t.toString();
        int colon = m.lastIndexOf(':');
        return (colon != -1) ? m.substring(colon + 1).trim() : m;
    }

    public static String rootCauseFriendlyMessage(Throwable t) {
        if (t == null) return "";
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return friendlyMessage(cur);
    }

    public static void deleteAssociatedFiles(Path experimentInfoFile) {
        Path parentDir = experimentInfoFile.getParent();

        if (parentDir == null) {
            System.err.println("❌ experimentInfoFile has no parent directory.");
            return;
        }

        Path allTracks = parentDir.resolve("All Tracks.csv");
        Path allRecordings = parentDir.resolve("All Recordings.csv");

        //deleteIfExists(allTracks);
        //deleteIfExists(allRecordings);
    }

    private static void deleteIfExists(Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("🗑️ Deleted: " + path);
            } else {
                System.out.println("ℹ️ File not found (no deletion needed): " + path);
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to delete " + path + ": " + e.getMessage());
        }
    }
}