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
 * The TauPlotCollector class is responsible for generating and saving Tau fitting plot images
 * to designated directories based on the result of Tau fitting operations. It organizes the
 * plots into subdirectories labeled "Success" or "Failed" according to the fitting outcome.
 * This class ensures proper directory structure and handles rendering and file output without
 * requiring a graphical user interface (GUI).
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
     * Ensures that a given directory exists. If the directory does not exist,
     * it attempts to create it. Logs an error message if the directory creation fails.
     *
     * @param dir the directory to be checked or created
     */
    private static void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("⚠️ Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Saves a plot visualizing the distribution of track durations and the tau fit result.
     * The generated plot is written as a PNG file to an appropriate directory based on the
     * success or failure of the fit.
     *
     * @param tracks the list of tracks used to compute the frequency distribution of track durations
     * @param tauResult the result of the tau calculation, containing tau values, R-squared, and the fit status
     * @param experimentPath the base path of the experiment, used to determine the output directory
     * @param recordingName the name of the recording associated with the current plot
     * @param squareIndex the index of the square region associated with the current recording
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