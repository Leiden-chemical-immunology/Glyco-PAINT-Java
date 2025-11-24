/*=============================================================================
 *  Class:        PaintColumnNames.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Centralizes all column name String constants used in PAINT CSV tables.
 *
 *  DESCRIPTION:
 *    Provides named String constants for all columns appearing in tracks,
 *    squares, recordings, and experiment info tables. This avoids hard-coded
 *    literals and keeps schema definitions consistent.
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-24
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.constants;

public final class PaintColumnNames {

    private PaintColumnNames() {
        // Prevent instantiation
    }

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
}