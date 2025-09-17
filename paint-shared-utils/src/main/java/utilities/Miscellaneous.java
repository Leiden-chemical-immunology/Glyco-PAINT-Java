package utilities;

import java.time.Duration;

public class Miscellaneous {

    public static String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(" hour").append(hours > 1 ? "s " : " ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes > 1 ? "s " : " ");
        if (secs > 0 || sb.length() == 0) sb.append(secs).append(" second").append(secs != 1 ? "s" : "");

        return sb.toString().trim();
    }


}
