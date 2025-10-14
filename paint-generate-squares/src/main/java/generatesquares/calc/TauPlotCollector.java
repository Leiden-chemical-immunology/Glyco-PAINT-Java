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
 * Collects and saves Tau fitting plots into
 * separate folders for successful and failed fits.
 *
 * Each plot corresponds to one square of one recording.
 * Uses the CalculateTau result already computed — no refitting.
 */
public class TauPlotCollector {

    private static final File ROOT_DIR = new File("Tau Fitting Plots");
    private static final File SUCCESS_DIR = new File(ROOT_DIR, "Success");
    private static final File FAILED_DIR = new File(ROOT_DIR, "Failed");

    static {
        ensureDir(ROOT_DIR);
        ensureDir(SUCCESS_DIR);
        ensureDir(FAILED_DIR);
    }

    private static void ensureDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            System.err.println("⚠️ Failed to create directory: " + dir.getAbsolutePath());
        }
    }

    /**
     * Saves a Tau fitting plot into the proper subfolder (success/failed).
     *
     * @param tracks       list of tracks used in the fit
     * @param tauResult    the Tau fit result (already computed)
     * @param recordingName    recordingName name
     * @param squareIndex  square number
     */
    public static void saveFitPlot(List<Track> tracks,
                                   CalculateTau.CalculateTauResult tauResult,
                                   Path experimentPath,
                                   String recordingName,
                                   int squareIndex) {

        boolean fitFailed = (tauResult == null ||
                tauResult.getStatus() != CalculateTau.CalculateTauResult.Status.TAU_SUCCESS ||
                !Double.isFinite(tauResult.getTau()) ||
                !Double.isFinite(tauResult.getRSquared()));

        // Build frequency distribution (duration → count)
        Map<Double, Integer> freqMap = new TreeMap<>();
        for (Track t : tracks) {
            double d = t.getTrackDuration();
            freqMap.put(d, freqMap.getOrDefault(d, 0) + 1);
        }

        double[] x = new double[freqMap.size()];
        double[] y = new double[freqMap.size()];
        int k = 0;
        for (Map.Entry<Double, Integer> e : freqMap.entrySet()) {
            x[k] = e.getKey();
            y[k] = e.getValue();
            k++;
        }

        // Render image (no Swing)
        BufferedImage img = PlotUtils.renderTauPlot(x, y, tauResult, fitFailed, 900, 600);

        // Save in appropriate folder
        // Create output directories inside the experiment folder
        Path rootDir = experimentPath.resolve("Output").resolve("Tau Fitting Plots");
        Path targetDir = rootDir.resolve(fitFailed ? "Failed" : "Success");

        try {
            java.nio.file.Files.createDirectories(targetDir);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to create directory: " + targetDir);
            e.printStackTrace();
            return;
        }

        Path plotPath = targetDir.resolve(
                String.format("%s_square_%03d.png", recordingName, squareIndex)
        );


        try {
            ImageIO.write(img, "png", plotPath.toFile());
        } catch (IOException e) {
            System.err.println("⚠️ Failed to save plot for " + recordingName + " square " + squareIndex);
            e.printStackTrace();
        }
    }
}