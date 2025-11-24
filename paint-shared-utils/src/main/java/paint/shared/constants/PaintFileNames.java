/*=============================================================================
 *  Class:        PaintFilenames.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines standard filenames used by the PAINT application.
 *
 *  DESCRIPTION:
 *    Central location for CSV and JSON filenames to avoid hard-coded strings
 *    throughout the codebase.
 *============================================================================*/

package paint.shared.constants;

public final class PaintFileNames {

    private PaintFileNames() {
        // Prevent instantiation
    }

    // =====================
    // Filenames
    // =====================

    public static final String RECORDINGS_CSV                 = "Recordings.csv";
    public static final String TRACKS_CSV                     = "Tracks.csv";
    public static final String SQUARES_CSV                    = "Squares.csv";
    public static final String EXPERIMENT_INFO_CSV            = "Experiment Info.csv";
    public static final String PAINT_CONFIGURATION_JSON       = "Paint Configuration.json";
    public static final String PAINT_SWEEP_CONFIGURATION_JSON = "Paint Sweep Configuration.json";
}