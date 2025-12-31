/*=============================================================================
 *  Class:        PaintFileNames.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Centralizes the naming conventions for all primary data files used
 *    within the PAINT application.
 *
 *  DESCRIPTION:
 *    The {@code PaintFileNames} class defines constants for CSV files
 *    (Recordings, Squares, Tracks), configuration JSONs, and other
 *    standard outputs produced or consumed by the pipeline modules.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.constants;

/**
 * Centralizes the naming conventions for all primary data files used
 * within the PAINT application.
 */
public final class PaintFileNames {

    private PaintFileNames() {
        // Prevent instantiation
    }

    /** Standard name for the recordings metadata CSV file. */
    public static final String RECORDINGS_CSV                 = "Recordings.csv";
    /** Standard name for the tracks data CSV file. */
    public static final String TRACKS_CSV                     = "Tracks.csv";
    /** Standard name for the squares analysis results CSV file. */
    public static final String SQUARES_CSV                    = "Squares.csv";
    /** Standard name for the experiment-level info metadata CSV file. */
    public static final String EXPERIMENT_INFO_CSV            = "Experiment Info.csv";
    /** Default filename for the primary project configuration JSON. */
    public static final String PAINT_CONFIGURATION_JSON       = "Paint Configuration.json";
    /** Default filename for the parameter sweep configuration JSON. */
    public static final String PAINT_SWEEP_CONFIGURATION_JSON = "Paint Sweep Configuration.json";
}