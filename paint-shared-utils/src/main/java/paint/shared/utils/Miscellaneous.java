package paint.shared.utils;

import java.nio.file.Path;
import java.time.Duration;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;
import static paint.shared.constants.PaintConstants.TRACKS_CSV;


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

    public static String formatDuration(Duration duration) {
        int totalSeconds = (int) duration.getSeconds();
        return formatDuration(totalSeconds);
    }

    public static String friendlyMessage(Throwable t) {
        if (t == null) {
            return "";
        }
        String m = t.toString();
        int colon = m.lastIndexOf(':');
        return (colon != -1) ? m.substring(colon + 1).trim() : m;
    }

    public static double round(double value, int decimals) {
        if (decimals < 0) {
            return  value;
        }
        else {
            double scale = Math.pow(10, decimals);
            return Math.round(value * scale) / scale;
        }
    }

}