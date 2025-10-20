package generatesquares.calc;

import paint.shared.objects.Track;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Utility class responsible for collecting and saving Tau fitting plots.
 * <p>
 * Each plot corresponds to one square of one recording. It separates results into
 * <b>Success</b> and <b>Failed</b> subfolders depending on the fit status.
 * <p>
 * This class does not perform Tau fitting — it assumes that a {@link CalculateTau.CalculateTauResult}
 * is already available and uses {@link PlotUtils} to render the corresponding frequency plot image.
 */
public class TauPlotCollector {


    private static final File ROOT_DIR = new File("Tau Fitting Plots"); // Root output directory for Tau fitting plots.
    private static final File SUCCESS_DIR = new File(ROOT_DIR, "Success");  // Subdirectory for successful fits.
    private static final File FAILED_DIR = new File(ROOT_DIR, "Failed");    // Subdirectory for failed fits.

    // Initialize directory structure at class load time.
    static {
        ensureDir(ROOT_DIR);
        ensureDir(SUCCESS_DIR);
        ensureDir(FAILED_DIR);
    }

    /**
     * Ensures that a directory exists, creating it if necessary.
     *
     * @param dir the directory to verify or create
     */
    private static void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("⚠️ Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Saves a Tau fitting plot image into the appropriate subfolder
     * (either <b>Success</b> or <b>Failed</b>).
     * <p>
     * The output file is stored under:
     * <pre>
     * {experimentPath}/Output/Tau Fitting Plots/{Success|Failed}/{recordingName}_square_XXX.png
     * </pre>
     *
     * @param tracks         list of tracks used in the fit (used to derive frequency data)
     * @param tauResult      the Tau fit result (already computed, may be {@code null})
     * @param experimentPath path to the experiment root directory
     * @param recordingName  name of the recording (used for output filename)
     * @param squareIndex    index (ID) of the square within the recording
     */
    public static void saveFitPlot(List<Track> tracks,
                                   CalculateTau.CalculateTauResult tauResult,
                                   Path experimentPath,
                                   String recordingName,
                                   int squareIndex) {

        // Determine if the fit should be treated as failed
        boolean fitFailed = (tauResult == null ||
                tauResult.getStatus() != CalculateTau.CalculateTauResult.Status.TAU_SUCCESS ||
                !Double.isFinite(tauResult.getTau()) ||
                !Double.isFinite(tauResult.getRSquared()));

        // --- Build frequency distribution of track durations ---
        // Each track duration becomes a key; value = count of tracks with that duration.
        Map<Double, Integer> freqMap = new TreeMap<>();
        for (Track track : tracks) {
            double d = track.getTrackDuration();
            freqMap.put(d, freqMap.getOrDefault(d, 0) + 1);
        }

        // Convert frequency map to x/y arrays for plotting
        double[] x = new double[freqMap.size()];
        double[] y = new double[freqMap.size()];
        int k = 0;
        for (Map.Entry<Double, Integer> e : freqMap.entrySet()) {
            x[k] = e.getKey();
            y[k] = e.getValue();
            k++;
        }

        // --- Render plot image (no GUI required) ---
        BufferedImage img = PlotUtils.renderTauPlot(x, y, tauResult, fitFailed, 900, 600);

        // --- Determine output directory ---
        // Output directory structure:
        // {experimentPath}/Output/Tau Fitting Plots/Success or Failed
        Path rootDir = experimentPath.resolve("Output").resolve("Tau Fitting Plots");
        Path targetDir = rootDir.resolve(fitFailed ? "Failed" : "Success");

        try {
            java.nio.file.Files.createDirectories(targetDir);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to create directory: " + targetDir);
            e.printStackTrace();
            return;
        }

        // Define output file name, e.g. "Recording_01_square_003.png"
        Path plotPath = targetDir.resolve(String.format("%s_square_%03d.png", recordingName, squareIndex));

        // --- Write the image file ---
        try {
            ImageIO.write(img, "png", plotPath.toFile());
        } catch (IOException e) {
            System.err.println("⚠️ Failed to save plot for " + recordingName + " square " + squareIndex);
            e.printStackTrace();
        }
    }
}