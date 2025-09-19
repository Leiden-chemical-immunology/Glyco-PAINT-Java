package paint.shared.constants;

import tech.tablesaw.api.ColumnType;

/**
 * Centralized constants for the Paint project.
 * <p>
 * This class defines file names, directory names, schema column definitions,
 * and associated {@link ColumnType} arrays used for parsing and validating
 * CSV files in the Paint application.
 * </p>
 * <p>
 * The constants are grouped by domain:
 * <ul>
 *     <li>Filenames and directory names</li>
 *     <li>General configuration values</li>
 *     <li>Schema definitions for Tracks, Squares, Recordings, and Experiments</li>
 * </ul>
 * </p>
 * <p><b>Note:</b> This class depends on Tablesaw's {@link ColumnType} for schema typing.</p>
 */
public final class PaintConstants {

    private PaintConstants() {
        // Prevent instantiation
    }

    // =====================
    // Filenames
    // =====================

    /** CSV file name containing recording-level statistics. */
    public static final String RECORDINGS_CSV = "All Recordings Java.csv";

    /** CSV file name containing track-level data. */
    public static final String TRACKS_CSV = "All Tracks Java.csv";

    /** CSV file name containing square-level data. */
    public static final String SQUARES_CSV = "All Squares Java.csv";

    /** CSV file name containing experiment-level metadata. */
    public static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";

    /** JSON file name containing Paint project configuration. */
    public static final String PAINT_JSON = "Paint Configuration.json";

    /** JSON configuration file used throughout Paint. */
    public static final String PAINT_CONFIGURATION_JSON = "Paint Configuration.json";

    // =====================
    // Directories
    // =====================

    /** Directory name for TrackMate-processed images. */
    public static final String DIR_TRACKMATE_IMAGES = "TrackMate Images";

    /** Directory name for brightfield images. */
    public static final String DIR_BRIGHTFIELD_IMAGES = "Brightfield Images";

    // =====================
    // Column names (common)
    // =====================

    /** Shared column name for recording identifiers. */
    public static final String COL_RECORDING_NAME = "Recording Name";

    // =====================
    // Geometry
    // =====================

    /** Default image width in microns. */
    public static final double IMAGE_WIDTH = 82.0864;

    /** Default image height in microns. */
    public static final double IMAGE_HEIGHT = 82.0864;

    // =====================
    // Timing
    // =====================

    /** Default time interval between frames (seconds). */
    public static final double TIME_INTERVAL = 0.05;

    // =====================
    // Track schema
    // =====================

    /** Column names for the Track-level CSV schema. */
    public static final String[] TRACK_COLS = {
            "Unique Key", "Recording Name", "Track Id", "Track Label",
            "Number of Spots", "Number of Gaps", "Longest Gap",
            "Track Duration", "Track X Location", "Track Y Location",
            "Track Displacement", "Track Max Speed", "Track Median Speed",
            "Diffusion Coefficient", "Diffusion Coefficient Ext",
            "Total Distance", "Confinement Ratio", "Square Number", "Label Number"
    };

    /** Column types for the Track-level CSV schema. */
    public static final ColumnType[] TRACK_TYPES = {
            ColumnType.STRING,  // Unique Key
            ColumnType.STRING,  // Recording Name
            ColumnType.INTEGER, // Track Id
            ColumnType.STRING,  // Track Label
            ColumnType.INTEGER, // Number of Spots
            ColumnType.INTEGER, // Number of Gaps
            ColumnType.INTEGER, // Longest Gap
            ColumnType.DOUBLE,  // Track Duration
            ColumnType.DOUBLE,  // Track X Location
            ColumnType.DOUBLE,  // Track Y Location
            ColumnType.DOUBLE,  // Track Displacement
            ColumnType.DOUBLE,  // Track Max Speed
            ColumnType.DOUBLE,  // Track Median Speed
            ColumnType.DOUBLE,  // Diffusion Coefficient
            ColumnType.DOUBLE,  // Diffusion Coefficient Ext
            ColumnType.DOUBLE,  // Total Distance
            ColumnType.DOUBLE,  // Confinement Ratio
            ColumnType.INTEGER, // Square Number
            ColumnType.INTEGER  // Label Number
    };

    // =====================
    // Square schema
    // =====================

    /** Column names for the Square-level CSV schema. */
    public static final String[] SQUARE_COLS = {
            "Unique Key",
            "Recording Name",
            "Square Number",
            "Row Number",
            "Column Number",
            "Label Number",
            "Cell ID",
            "Selected",
            "Square Manually Excluded",
            "Image Excluded",
            "X0",
            "Y0",
            "X1",
            "Y1",
            "Number of Tracks",
            "Variability",
            "Density",
            "Density Ratio",
            "Tau",
            "R Squared",
            "Median Diffusion Coefficient",
            "Median Diffusion Coefficient Ext",
            "Median Long Track Duration",
            "Median Short Track Duration",
            "Median Displacement",
            "Max Displacement",
            "Total Displacement",
            "Median Max Speed",
            "Max Max Speed",
            "Median Mean Speed",
            "Max Mean Speed",
            "Max Track Duration",
            "Total Track Duration",
            "Median Track Duration"
    };

    /** Column types for the Square-level CSV schema. */
    public static final ColumnType[] SQUARE_TYPES = {
            ColumnType.STRING,   // Unique Key
            ColumnType.STRING,   // Recording Name
            ColumnType.INTEGER,  // Square Number
            ColumnType.INTEGER,  // Row Number
            ColumnType.INTEGER,  // Column Number
            ColumnType.INTEGER,  // Label Number
            ColumnType.INTEGER,  // Cell ID
            ColumnType.BOOLEAN,  // Selected
            ColumnType.BOOLEAN,  // Square Manually Excluded
            ColumnType.BOOLEAN,  // Image Excluded
            ColumnType.DOUBLE,   // X0
            ColumnType.DOUBLE,   // Y0
            ColumnType.DOUBLE,   // X1
            ColumnType.DOUBLE,   // Y1
            ColumnType.INTEGER,  // Number of Tracks
            ColumnType.DOUBLE,   // Variability
            ColumnType.DOUBLE,   // Density
            ColumnType.DOUBLE,   // Density Ratio
            ColumnType.DOUBLE,   // Tau
            ColumnType.DOUBLE,   // R Squared
            ColumnType.DOUBLE,   // Median Diffusion Coefficient
            ColumnType.DOUBLE,   // Median Diffusion Coefficient Ext
            ColumnType.DOUBLE,   // Median Long Track Duration
            ColumnType.DOUBLE,   // Median Short Track Duration
            ColumnType.DOUBLE,   // Median Displacement
            ColumnType.DOUBLE,   // Max Displacement
            ColumnType.DOUBLE,   // Total Displacement
            ColumnType.DOUBLE,   // Median Max Speed
            ColumnType.DOUBLE,   // Max Max Speed
            ColumnType.DOUBLE,   // Median Mean Speed
            ColumnType.DOUBLE,   // Max Mean Speed
            ColumnType.DOUBLE,   // Max Track Duration
            ColumnType.DOUBLE,   // Total Track Duration
            ColumnType.DOUBLE    // Median Track Duration
    };

    // =====================
    // Recording schema
    // =====================

    /** Column names for the Recording-level CSV schema. */
    public static final String[] RECORDING_COLS = {
            "Recording Name",
            "Condition Number",
            "Replicate Number",
            "Probe Name",
            "Probe Type",
            "Cell Type",
            "Adjuvant",
            "Concentration",
            "Process Flag",
            "Threshold",
            "Number of Spots",
            "Number of Tracks",
            "Number of Spots in All Tracks",
            "Number of Frames",
            "Run Time",
            "Time Stamp",
            "Exclude",
            "Tau",
            "R Squared",
            "Density"
    };

    /** Column types for the Recording-level CSV schema. */
    public static final ColumnType[] RECORDING_TYPES = {
            ColumnType.STRING,            // Recording Name
            ColumnType.INTEGER,           // Condition Number
            ColumnType.INTEGER,           // Replicate Number
            ColumnType.STRING,            // Probe Name
            ColumnType.STRING,            // Probe Type
            ColumnType.STRING,            // Cell Type
            ColumnType.STRING,            // Adjuvant
            ColumnType.DOUBLE,            // Concentration
            ColumnType.BOOLEAN,           // Process Flag
            ColumnType.DOUBLE,            // Threshold
            ColumnType.INTEGER,           // Number of Spots
            ColumnType.INTEGER,           // Number of Tracks
            ColumnType.INTEGER,           // Number of Spots in All Tracks
            ColumnType.INTEGER,           // Number of Frames
            ColumnType.DOUBLE,            // Run Time
            ColumnType.LOCAL_DATE_TIME,   // Time Stamp
            ColumnType.BOOLEAN,           // Exclude
            ColumnType.DOUBLE,            // Tau
            ColumnType.DOUBLE,            // R Squared
            ColumnType.DOUBLE             // Density
    };

    // =====================
    // Experiment info schema
    // =====================

    /** Column names for the Experiment info CSV schema. */
    public static final String[] EXPERIMENT_INFO_COLS = {
            "Recording Name",
            "Condition Number",
            "Replicate Number",
            "Probe Name",
            "Probe Type",
            "Cell Type",
            "Adjuvant",
            "Concentration",
            "Process Flag",
            "Threshold"
    };

    /** Column types for the Experiment info CSV schema. */
    public static final ColumnType[] EXPERIMENT_INFO_TYPES = {
            ColumnType.STRING,   // Recording Name
            ColumnType.INTEGER,  // Condition Number
            ColumnType.INTEGER,  // Replicate Number
            ColumnType.STRING,   // Probe Name
            ColumnType.STRING,   // Probe Type
            ColumnType.STRING,   // Cell Type
            ColumnType.STRING,   // Adjuvant
            ColumnType.DOUBLE,   // Concentration
            ColumnType.BOOLEAN,  // Process Flag
            ColumnType.DOUBLE    // Threshold
    };

    /** Minimal experiment list schema (single column). */
    public static final String[] EXPERIMENT_COLS = {
            "experimentName"
    };
}