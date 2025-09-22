package trackMateTest;


import paint.shared.utils.AppLogger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TrackMateRunnerSingle {

    static {
        net.imagej.patcher.LegacyInjector.preinit();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java TrackMateRunnerSingle <imagePath>");
            System.exit(1);
        }
        try {
            TrackMateLauncher.runTrackMateOnPath(args[0]);
        } catch (Exception e) {
            System.err.println("Failed to run TrackMate: " + e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw.toString());
            System.exit(1);
        }
    }
}

