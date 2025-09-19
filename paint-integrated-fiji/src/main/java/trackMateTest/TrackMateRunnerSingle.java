package trackMateTest;


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
            e.printStackTrace();
            System.exit(1);
        }
    }
}

