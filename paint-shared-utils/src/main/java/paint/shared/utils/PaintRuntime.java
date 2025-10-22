package paint.shared.utils;

import paint.shared.prefs.PaintPrefs;

public final class PaintRuntime {

    private static boolean verbose;
    private static String logLevel;

    private PaintRuntime() {}

    public static void initialiseFromPrefs() {
        verbose  = PaintPrefs.getBoolean("Verbose", false);
        logLevel = PaintPrefs.getString("Log Level", "INFO");
    }

    public static boolean isVerbose() {
        return verbose;
    }

    public static String getLogLevel() {
        return logLevel;
    }

    public static void setVerbose(boolean v) {
        verbose = v;
        PaintPrefs.putBoolean("Verbose", v);
    }

    public static void setLogLevel(String level) {
        logLevel = level;
        PaintPrefs.putString("Log Level", level);
    }
}