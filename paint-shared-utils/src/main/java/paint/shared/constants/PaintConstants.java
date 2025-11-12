/*=============================================================================
 *  Class:        PaintConstants.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines a set of constants used within the application.
 *
 *  DESCRIPTION:
 *    This utility class includes constants related to file names, directories,
 *    geometry, timing, and schema definitions for tracks, squares, and recordings.
 *
 *  KEY FEATURES:
 *    - Centralized repository for all constant values across the application
 *    - Prevents hard-coding of values in multiple places
 *    - Ensures consistency by using public static final fields
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.shared.constants;

import tech.tablesaw.api.ColumnType;

public final class PaintConstants {

    private PaintConstants() {
        // Prevent instantiation
    }


    // =====================
    // Column Names
    // =====================

    public static final String ADJUVANT                                    = "Adjuvant";
    public static final String ALLOW_GAP_CLOSING                           = "ALLOW_GAP_CLOSING";
    public static final String ALLOW_TRACK_MERGING                         = "ALLOW_TRACK_MERGING";
    public static final String ALLOW_TRACK_SPLITTING                       = "ALLOW_TRACK_SPLITTING";
    public static final String ALTERNATIVE_LINKING_COST_FACTOR             = "ALTERNATIVE_LINKING_COST_FACTOR";
    public static final String AVERAGE_TRACKS_IN_BACKGROUND                = "Average Tracks in Background";
    public static final String BACKGROUND_PLOTS                            = "Background plots";
    public static final String CELL_ID                                     = "Cell ID";
    public static final String CELL_TYPE                                   = "Cell Type";
    public static final String COLUMN_NUMBER                               = "Column Number";
    public static final String CONCENTRATION                               = "Concentration";
    public static final String CONDITION_NUMBER                            = "Condition Number";
    public static final String CONFINEMENT_RATIO                           = "Confinement Ratio";
    public static final String DEBUG_RUNTRACKMATEONPROJECT                 = "Debug RunTrackMateOnProject";
    public static final String DEBUG_RUNTRACKMATEONRECORDING               = "Debug RunTrackMateOnRecording";
    public static final String DENSITY                                     = "Density";
    public static final String DENSITY_RATIO                               = "Density Ratio";
    public static final String DENSITY_RATIO_ORI                           = "Density Ratio Ori";
    public static final String DIFFUSION_COEFFICIENT                       = "Diffusion Coefficient";
    public static final String DIFFUSION_COEFFICIENT_EXT                   = "Diffusion Coefficient Ext";
    public static final String DO_MEDIAN_FILTERING                         = "DO_MEDIAN_FILTERING";
    public static final String DO_SUBPIXEL_LOCALIZATION                    = "DO_SUBPIXEL_LOCALIZATION";
    public static final String EXCLUDE                                     = "Exclude";
    public static final String EXCLUDE_ZERO_DC_TRACKS_FROM_TAU_CALCULATION = "Exclude zero DC tracks from Tau Calculation";
    public static final String EXPERIMENT_NAME                             = "Experiment Name";
    public static final String FRACTION_OF_SQUARES_TO_DETERMINE_BACKGROUND = "Fraction of Squares to Determine Background";
    public static final String GAP_CLOSING_MAX_DISTANCE                    = "GAP_CLOSING_MAX_DISTANCE";
    public static final String GENERATE_SQUARES                            = "Generate Squares";
    public static final String IMAGE_EXCLUDED                              = "Image Excluded";
    public static final String LABEL_NUMBER                                = "Label Number";
    public static final String LINKING_MAX_DISTANCE                        = "LINKING_MAX_DISTANCE";
    public static final String LONGEST_GAP                                 = "Longest Gap";
    public static final String MAX_ALLOWABLE_VARIABILITY                   = "Max Allowable Variability";
    public static final String MAX_DISPLACEMENT                            = "Max Displacement";
    public static final String MAX_FRAME_GAP                               = "MAX_FRAME_GAP";
    public static final String MAX_MAX_SPEED                               = "Max Max Speed";
    public static final String MAX_MEAN_SPEED                              = "Max Mean Speed";
    public static final String MAX_NR_SECONDS_PER_IMAGE                    = "MAX_NR_SECONDS_PER_IMAGE";
    public static final String MAX_NR_SPOTS_IN_IMAGE                       = "MAX_NR_SPOTS_IN_IMAGE";
    public static final String MAX_TRACK_DURATION                          = "Max Track Duration";
    public static final String MAX_VARIABILITY                             = "Max Variability";
    public static final String MEDIAN_DIFFUSION_COEFFICIENT                = "Median Diffusion Coefficient";
    public static final String MEDIAN_DIFFUSION_COEFFICIENT_EXT            = "Median Diffusion Coefficient Ext";
    public static final String MEDIAN_DISPLACEMENT                         = "Median Displacement";
    public static final String MEDIAN_MAX_SPEED                            = "Median Max Speed";
    public static final String MEDIAN_MEAN_SPEED                           = "Median Mean Speed";
    public static final String MEDIAN_TRACK_DURATION                       = "Median Track Duration";
    public static final String MERGING_MAX_DISTANCE                        = "MERGING_MAX_DISTANCE";
    public static final String MIN_DENSITY_RATIO                           = "Min Density Ratio";
    public static final String MIN_NR_SPOTS_IN_TRACK                       = "MIN_NR_SPOTS_IN_TRACK";
    public static final String MIN_REQUIRED_DENSITY_RATIO                  = "Min Required Density Ratio";
    public static final String MIN_REQUIRED_R_SQUARED                      = "Min Required R Squared";
    public static final String MIN_TRACK_DURATION                          = "Min Track Duration";
    public static final String MIN_TRACKS_TO_CALCULATE_TAU                 = "Min Tracks to Calculate Tau";
    public static final String NEIGHBOUR_MODE                              = "Neighbour Mode";
    public static final String NUMBER_OF_FRAMES                            = "Number of Frames";
    public static final String NUMBER_OF_GAPS                              = "Number of Gaps";
    public static final String NUMBER_OF_SPOTS                             = "Number of Spots";
    public static final String NUMBER_OF_SPOTS_IN_ALL_TRACKS               = "Number of Spots in All Tracks";
    public static final String NUMBER_OF_SQUARES_IN_BACKGROUND             = "Number of Squares in Background";
    public static final String NUMBER_OF_SQUARES_IN_COLUMN                 = "Number of Squares in Column";
    public static final String NUMBER_OF_SQUARES_IN_RECORDING              = "Number of Squares in Recording";
    public static final String NUMBER_OF_SQUARES_IN_ROW                    = "Number of Squares in Row";
    public static final String NUMBER_OF_TRACKS                            = "Number of Tracks";
    public static final String NUMBER_OF_TRACKS_IN_BACKGROUND              = "Number of Tracks in Background";
    public static final String PROBE_NAME                                  = "Probe Name";
    public static final String PROBE_TYPE                                  = "Probe Type";
    public static final String PROCESS_FLAG                                = "Process Flag";
    public static final String R_SQUARED                                   = "R Squared";
    public static final String RADIUS                                      = "RADIUS";
    public static final String RECORDING_NAME                              = "Recording Name";
    public static final String REPLICATE_NUMBER                            = "Replicate Number";
    public static final String ROW_NUMBER                                  = "Row Number";
    public static final String RUN_TIME                                    = "Run Time";
    public static final String VISIBLE                                     = "Visible";
    public static final String SPLITTING_MAX_DISTANCE                      = "SPLITTING_MAX_DISTANCE";
    public static final String SQUARE_MANUALLY_EXCLUDED                    = "Square Manually Excluded";
    public static final String SQUARE_NUMBER                               = "Square Number";
    public static final String TARGET_CHANNEL                              = "TARGET_CHANNEL";
    public static final String TAU                                         = "Tau";
    public static final String TAU_FITTING_PLOTS                           = "Tau Fitting plots";
    public static final String THRESHOLD                                   = "Threshold";
    public static final String TIME_STAMP                                  = "Time Stamp";
    public static final String TOTAL_DISPLACEMENT                          = "Total Displacement";
    public static final String TOTAL_DISTANCE                              = "Total Distance";
    public static final String TOTAL_TRACK_DURATION                        = "Total Track Duration";
    public static final String TRACK_COLOURING                             = "TRACK_COLOURING";
    public static final String TRACK_DISPLACEMENT                          = "Track Displacement";
    public static final String TRACK_DURATION                              = "Track Duration";
    public static final String TRACK_ID                                    = "Track Id";
    public static final String TRACK_MAX_SPEED                             = "Track Max Speed";
    public static final String TRACK_MEDIAN_SPEED                          = "Track Median Speed";
    public static final String TRACK_X_LOCATION                            = "Track X Location";
    public static final String TRACK_Y_LOCATION                            = "Track Y Location";
    public static final String UNIQUE_KEY                                  = "Unique Key";
    public static final String VARIABILITY                                 = "Variability";
    public static final String X0                                          = "X0";
    public static final String X1                                          = "X1";
    public static final String Y0                                          = "Y0";
    public static final String Y1                                          = "Y1";

    // Filenames
    // =====================

    public static final String RECORDINGS_CSV                              = "Recordings.csv";
    public static final String TRACKS_CSV                                  = "Tracks.csv";
    public static final String SQUARES_CSV                                 = "Squares.csv";
    public static final String EXPERIMENT_INFO_CSV                         = "Experiment Info.csv";
    public static final String PAINT_CONFIGURATION_JSON                    = "Paint Configuration.json";
    public static final String PAINT_SWEEP_CONFIGURATION_JSON              = "Paint Sweep Configuration.json";

    // =====================
    // Directories
    // =====================

    public static final String DIR_TRACKMATE_IMAGES                        = "TrackMate Images";
    public static final String DIR_BRIGHTFIELD_IMAGES                      = "Brightfield Images";

    // =====================
    // Geometry
    // =====================

    public static final double PIXEL_WIDTH                                 = 0.1603251;              // Specified by Nikon (in µm)
    public static final double PIXEL_HEIGHT                                = 0.1603251;              // Specified by Nikon (in µm)
    public static final int    NUMBER_PIXELS_WIDTH                         = 512;                    // Specified by Nikon
    public static final int    NUMBER_PIXELS_HEIGHT                        = 512;                    // Specified by Nikon
    public static final double IMAGE_WIDTH                                 = PIXEL_WIDTH * NUMBER_PIXELS_WIDTH;      // 82.08645 (in µm)
    public static final double IMAGE_HEIGHT                                = PIXEL_HEIGHT * NUMBER_PIXELS_HEIGHT;    // 82.08645 (in µm)

    // =====================
    // Timing
    // =====================

    public static final double TIME_INTERVAL                               = 0.05;                    // The time between images (in seconds)
    public static final double FRAMES                                      = 2000;                    // The number of images in a recording
    public static final double RECORDING_DURATION                          = FRAMES * TIME_INTERVAL;  // The timespan of a recording
    // TODO These should be JSON parameters

    // =====================
    // Track schema
    // =====================

    public static final String[] TRACKS_COLS = {
            UNIQUE_KEY,
            EXPERIMENT_NAME,
            RECORDING_NAME,
            TRACK_ID,
            NUMBER_OF_SPOTS,
            NUMBER_OF_GAPS,
            LONGEST_GAP,
            TRACK_DURATION,
            TRACK_X_LOCATION,
            TRACK_Y_LOCATION,
            TRACK_DISPLACEMENT,
            TRACK_MAX_SPEED,
            TRACK_MEDIAN_SPEED,
            DIFFUSION_COEFFICIENT,
            DIFFUSION_COEFFICIENT_EXT,
            TOTAL_DISTANCE,
            CONFINEMENT_RATIO,
            SQUARE_NUMBER,
            LABEL_NUMBER
    };

    public static final ColumnType[] TRACKS_TYPES = {
            ColumnType.STRING,  // UNIQUE_KEY
            ColumnType.STRING,  // EXPERIMENT_NAME
            ColumnType.STRING,  // RECORDING_NAME
            ColumnType.INTEGER, // TRACK_ID
            ColumnType.INTEGER, // NUMBER_OF_SPOTS
            ColumnType.INTEGER, // NUMBER_OF_GAPS
            ColumnType.INTEGER, // LONGEST_GAP
            ColumnType.DOUBLE,  // TRACK_DURATION
            ColumnType.DOUBLE,  // TRACK_X_LOCATION
            ColumnType.DOUBLE,  // TRACK_Y_LOCATION,
            ColumnType.DOUBLE,  // TRACK_DISPLACEMENT
            ColumnType.DOUBLE,  // TRACK_MAX_SPEED
            ColumnType.DOUBLE,  // TRACK_MEDIAN_SPEED
            ColumnType.DOUBLE,  // DIFFUSION_COEFFICIENT
            ColumnType.DOUBLE,  // DIFFUSION_COEFFICIENT_EXT
            ColumnType.DOUBLE,  // TOTAL_DISTANCE
            ColumnType.DOUBLE,  // CONFINEMENT_RATIO
            ColumnType.INTEGER, // SQUARE_NUMBER
            ColumnType.INTEGER  // LABEL_NUMBER
    };

    // =====================
    // Square schema
    // =====================

    public static final String[] SQUARES_COLS = {
            UNIQUE_KEY,
            EXPERIMENT_NAME,
            RECORDING_NAME,
            SQUARE_NUMBER,
            ROW_NUMBER,
            COLUMN_NUMBER,
            LABEL_NUMBER,
            CELL_ID,
            VISIBLE,
            SQUARE_MANUALLY_EXCLUDED,
            IMAGE_EXCLUDED,
            X0,
            Y0,
            X1,
            Y1,
            NUMBER_OF_TRACKS,
            VARIABILITY,
            DENSITY,
            DENSITY_RATIO,
            DENSITY_RATIO_ORI,
            TAU,
            R_SQUARED,
            MEDIAN_DIFFUSION_COEFFICIENT,
            MEDIAN_DIFFUSION_COEFFICIENT_EXT,
            MEDIAN_DISPLACEMENT,
            MAX_DISPLACEMENT,
            TOTAL_DISPLACEMENT,
            MEDIAN_MAX_SPEED,
            MAX_MAX_SPEED,
            MEDIAN_MAX_SPEED,
            MAX_MEAN_SPEED,
            MAX_TRACK_DURATION,
            TOTAL_TRACK_DURATION,
            MEDIAN_TRACK_DURATION
    };

    public static final ColumnType[] SQUARES_TYPES = {
            ColumnType.STRING,   // UNIQUE_KEY
            ColumnType.STRING,   // EXPERIMENT_NAME
            ColumnType.STRING,   // RECORDING_NAME
            ColumnType.INTEGER,  // SQUARE_NUMBER
            ColumnType.INTEGER,  // ROW_NUMBER
            ColumnType.INTEGER,  // COLUMN_NUMBER
            ColumnType.INTEGER,  // LABEL_NUMBER
            ColumnType.INTEGER,  // CELL_ID
            ColumnType.BOOLEAN,  // VISIBLE
            ColumnType.BOOLEAN,  // SQUARE_MANUALLY_EXCLUDED
            ColumnType.BOOLEAN,  // IMAGE_EXCLUDED
            ColumnType.DOUBLE,   // X0
            ColumnType.DOUBLE,   // Y0
            ColumnType.DOUBLE,   // X1
            ColumnType.DOUBLE,   // Y1
            ColumnType.INTEGER,  // NUMBER_OF_TRACKS
            ColumnType.DOUBLE,   // VARIABILITY
            ColumnType.DOUBLE,   // DENSITY
            ColumnType.DOUBLE,   // DENSITY_RATIO
            ColumnType.DOUBLE,   // DENSITY_RATIO_ORI
            ColumnType.DOUBLE,   // TAU
            ColumnType.DOUBLE,   // R_SQUARED
            ColumnType.DOUBLE,   // MEDIAN_DIFFUSION_COEFFICIENT
            ColumnType.DOUBLE,   // MEDIAN_DIFFUSION_COEFFICIENT_EXT
            ColumnType.DOUBLE,   // MEDIAN_DISPLACEMENT
            ColumnType.DOUBLE,   // MAX_DISPLACEMENT
            ColumnType.DOUBLE,   // TOTAL_DISPLACEMENT
            ColumnType.DOUBLE,   // MEDIAN_MAX_SPEED
            ColumnType.DOUBLE,   // MAX_MAX_SPEED
            ColumnType.DOUBLE,   // MEDIAN_MEAN_SPEED
            ColumnType.DOUBLE,   // MAX_MEAN_SPEED
            ColumnType.DOUBLE,   // MAX_TRACK_DURATION
            ColumnType.DOUBLE,   // TOTAL_TRACK_DURATION
            ColumnType.DOUBLE    // MEDIAN_TRACK_DURATION
    };

    // =====================
    // Recording schema
    // =====================

    public static final String[] RECORDINGS_COLS = {
            EXPERIMENT_NAME,
            RECORDING_NAME,
            CONDITION_NUMBER,
            REPLICATE_NUMBER,
            PROBE_NAME,
            PROBE_TYPE,
            CELL_TYPE,
            ADJUVANT,
            CONCENTRATION,
            PROCESS_FLAG,
            THRESHOLD,
            NUMBER_OF_SPOTS,
            NUMBER_OF_TRACKS,
            NUMBER_OF_TRACKS_IN_BACKGROUND,
            NUMBER_OF_SQUARES_IN_BACKGROUND,
            AVERAGE_TRACKS_IN_BACKGROUND,
            NUMBER_OF_SPOTS_IN_ALL_TRACKS,
            NUMBER_OF_FRAMES,
            RUN_TIME,
            TIME_STAMP,
            EXCLUDE,
            TAU,
            R_SQUARED,
            DENSITY,
            MIN_REQUIRED_DENSITY_RATIO,
            MIN_REQUIRED_R_SQUARED,
            MAX_ALLOWABLE_VARIABILITY,
            NEIGHBOUR_MODE
    };

    public static final ColumnType[] RECORDINGS_TYPES = {
            ColumnType.STRING,            // EXPERIMENT_NAME
            ColumnType.STRING,            // RECORDING_NAME
            ColumnType.INTEGER,           // CONDITION_NUMBER
            ColumnType.INTEGER,           // REPLICATE_NUMBER
            ColumnType.STRING,            // PROBE_NAME
            ColumnType.STRING,            // PROBE_TYPE
            ColumnType.STRING,            // CELL_TYPE
            ColumnType.STRING,            // ADJUVANT
            ColumnType.DOUBLE,            // CONCENTRATION
            ColumnType.BOOLEAN,           // PROCESS_FLAG
            ColumnType.DOUBLE,            // THRESHOLD
            ColumnType.INTEGER,           // NUMBER_OF_SPOTS
            ColumnType.INTEGER,           // NUMBER_OF_TRACKS
            ColumnType.INTEGER,           // NUMBER_OF_TRACKS_IN_BACKGROUND
            ColumnType.INTEGER,           // NUMBER_OF_SQUARES_IN_BACKGROUND
            ColumnType.DOUBLE,            // AVERAGE_TRACKS_IN_BACKGROUND
            ColumnType.INTEGER,           // NUMBER_OF_SPOTS_IN_ALL_TRACKS
            ColumnType.INTEGER,           // NUMBER_OF_FRAMES
            ColumnType.DOUBLE,            // RUN_TIME
            ColumnType.LOCAL_DATE_TIME,   // TIME_STAMP
            ColumnType.BOOLEAN,           // EXCLUDE
            ColumnType.DOUBLE,            // TAU
            ColumnType.DOUBLE,            // R_SQUARED
            ColumnType.DOUBLE,            // DENSITY
            ColumnType.DOUBLE,            // MIN_REQUIRED_DENSITY_RATIO
            ColumnType.DOUBLE,            // MIN_REQUIRED_R_SQUARED
            ColumnType.DOUBLE,            // MAX_ALLOWABLE_VARIABILITY
            ColumnType.STRING             // NEIGHBOUR_MODE
    };

    // =====================
    // Experiment info schema
    // =====================

    public static final String[] EXPERIMENT_INFO_COLS = {
            EXPERIMENT_NAME,
            RECORDING_NAME,
            CONDITION_NUMBER,
            REPLICATE_NUMBER,
            PROBE_NAME,
            PROBE_TYPE,
            CELL_TYPE,
            ADJUVANT,
            CONCENTRATION,
            PROCESS_FLAG,
            THRESHOLD
    };

    public static final ColumnType[] EXPERIMENT_INFO_TYPES = {
            ColumnType.STRING,   // EXPERIMENT_NAME
            ColumnType.STRING,   // RECORDING_NAME
            ColumnType.INTEGER,  // CONDITION_NUMBER
            ColumnType.INTEGER,  // REPLICATE_NUMBER
            ColumnType.STRING,   // PROBE_NAME
            ColumnType.STRING,   // PROBE_TYPE
            ColumnType.STRING,   // CELL_TYPE
            ColumnType.STRING,   // ADJUVANT
            ColumnType.DOUBLE,   // CONCENTRATION
            ColumnType.BOOLEAN,  // PROCESS_FLAG
            ColumnType.DOUBLE    // THRESHOLD
    };
}


