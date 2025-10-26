package paint.shared.utils;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The Miscellaneous class contains utility methods for various purposes,
 * including formatting durations, extracting user-friendly messages
 * from exceptions, and rounding numeric values.
 */
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

    public static boolean isBooleanValue(String value) {
        if (value.equals("true") || value.equals("1") || value.equals("yes") || value.equals("y") || value.equals("t")) {
            return (true);
        } else if (value.equals("false") || value.equals("0") || value.equals("no") || value.equals("n") || value.equals("f")) {
            return (false);
        } else {
            PaintLogger.errorf("Invalid boolean value: %s", value);
            return (false);
        }
    }

    public static Boolean checkBooleanValue(String value) {
        value = value.trim().toLowerCase();

        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t", "1"));
        Set<String> noValues = new HashSet<>(Arrays.asList("n", "no", "false", "f", "0"));

        return yesValues.contains(value) || noValues.contains(value);
    }

    public static Boolean getBooleanValue(String value) {
        value = value.trim().toLowerCase();

        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t", "1"));
        Set<String> noValues = new HashSet<>(Arrays.asList("n", "no", "false", "f", "0"));

        if (yesValues.contains(value)) {
            return true;
        }
        else if (noValues.contains(value)) {
            return false;
        }
        else {
            PaintLogger.errorf("Invalid boolean value: %s, default to 'false'", value);
            return false;
        }
    }

    public static Boolean isBooleanTrue(String value) {
        value = value.trim().toLowerCase();

        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "ye", "yes", "ok", "true", "t", "1"));

        return yesValues.contains(value);

    }

}