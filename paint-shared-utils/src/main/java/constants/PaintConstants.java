package constants;

public final class PaintConstants {

    private PaintConstants() {
        // Prevent instantiation
    }

    // Filenames
    public static final String RECORDINGS_CSV = "All Recordings Java.csv";
    public static final String TRACKS_CSV = "All Tracks Java.csv";
    public static final String SQUARES_CSV = "All Squares Java.csv";
    public static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";
    public static final String PAINT_JSON = "Paint Configuration.json";

    // Directories
    public static final String DIR_TRACKMATE_IMAGES = "TrackMate Images";
    public static final String DIR_BRIGHTFIELD_IMAGES = "Brightfield Images";

    // Configuration
    public static final String PAINT_CONFIGURATION_JSON = "Paint Configuration.json";

    // Column names
    // public static final String COL_EXT_RECORDING_NAME = "Ext Recording Name";
    public static final String COL_RECORDING_NAME = "Recording Name";

    // Squares
    public static final double IMAGE_WIDTH = 82.0864;
    public static final double IMAGE_HEIGHT = 82.0864;

    // Time
    public static final double TIME_INTERVAL = 0.05;


    public static final String[] TRACK_COLS = {
            "Unique Key",                  // 0    String
            "Recording Name",              // 1    String
            "Track Id",                    // 2    int
            "Track Label",                 // 3    String
            "Number of Spots",             // 4    int
            "Number of Gaps",              // 5    int
            "Longest Gap",                 // 6    int
            "Track Duration",              // 7    double
            "Track X Location",            // 8    double
            "Track Y Location",            // 9    double
            "Track Displacement",          // 10   double
            "Track Max Speed",             // 11   double
            "Track Median Speed",          // 12   double
            "Diffusion Coefficient",       // 13   double
            "Diffusion Coefficient Ext",   // 14   double
            "Total Distance",              // 15   double
            "Confinement Ratio",           // 16   double
            "Square Number",               // 17   int
            "Label Number"                 // 18   int
    };

    public static final String[] SQUARE_COLS = {
            "Unique Key",                       // 0      String
            "Recording Name",                   // 1      String
            "Square Number",                    // 2      int
            "Row Number",                       // 3      int
            "Column Number",                    // 4      int
            "Label Number",                     // 5      int
            "Cell ID",                          // 6      int
            "Selected",                         // 7      boolean
            "Square Manually Excluded",         // 8      boolean
            "Image Excluded",                   // 9      boolean
            "X0",                               // 10     double
            "Y0",                               // 11     double
            "X1",                               // 12     double
            "Y1",                               // 13     double
            "Number of Tracks",                 // 14     int
            "Variability",                      // 15     double
            "Density",                          // 16     double
            "Density Ratio",                    // 17     double
            "Tau",                              // 18     double
            "R Squared",                        // 19     double
            "Median Diffusion Coefficient",     // 20     double
            "Median Diffusion Coefficient Ext", // 21     double
            "Median Long Track Duration",       // 22     double
            "Median Short Track Duration",      // 23     double
            "Median Displacement",              // 24     double
            "Max Displacement",                 // 25     double
            "Total Displacement",               // 26     double
            "Median Max Speed",                 // 27     double
            "Max Max Speed",                    // 28     double
            "Median Mean Speed",                // 29     double
            "Max Mean Speed",                   // 30     double
            "Max Track Duration",               // 31     double
            "Total Track Duration",             // 32     double
            "Median Track Duration"             // 33     double
    };


    public static final String[] RECORDING_COLS = {
            "Recording Name",                // 0     String
            "Condition Number",              // 1     int
            "Replicate Number",              // 2     int
            "Probe Name",                    // 3     String
            "Probe Type",                    // 4     String
            "Cell Type",                     // 5     String
            "Adjuvant",                      // 6     String
            "Concentration",                 // 7     double
            "Process Flag",                  // 8     boolean
            "Threshold",                     // 9     double
            "Number of Spots",               // 10    int
            "Number of Tracks",              // 11    int
            "Number of Spots in All Tracks", // 12    int
            "Number of Frames",              // 13    int
            "Run Time",                      // 14    double
            "Time Stamp",                    // 15    String
            "Exclude",                       // 16    boolean
            "Tau",                           // 17    double
            "R Squared",                     // 18    double
            "Density"                        // 19    double
    };

    public static final String[] EXPERIMENT_INFO_COLS = {
            "Recording Name",                // 0     String
            "Condition Number",              // 1     int
            "Replicate Number",              // 2     int
            "Probe Name",                    // 3     String
            "Probe Type",                    // 4     String
            "Cell Type",                     // 5     String
            "Adjuvant",                      // 6     String
            "Concentration",                 // 7     double
            "Process Flag",                  // 8     boolean
            "Threshold",                     // 9     double
    };

    public static final String[] EXPERIMENT_COLS = {
            "experimentName"
    };

}