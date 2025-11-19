package paint.generatesquares.calc;
import static paint.shared.constants.PaintConstants.*;

import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import paint.shared.utils.CalculateTau;
import paint.shared.utils.PaintLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static paint.generatesquares.calc.SquareUtils.calculateBackgroundDensity;

public class PlotUtils {

    private PlotUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Renders a Tau plot with the given data points, the fitted curve (if available),
     * axes, labels, and annotation text.
     *
     * @param x         the x-coordinates of the data points
     * @param y         the y-coordinates of the data points
     * @param result    the Tau-fit result, including Tau and R²
     * @param fitFailed true if the exponential fit failed
     * @param width     image width in pixels
     * @param height    image height in pixels
     * @return          a BufferedImage containing the rendered plot
     */
    public static BufferedImage renderTauPlot(double[] x,
            double[]                        y,
            CalculateTau.CalculateTauResult result,
            boolean                         fitFailed,
            int                             width,
            int                             height) {

        // --- Initialize blank image and 2D context ---
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // --- Draw background ---
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // --- Define margins and plotting area ---
        // @format:off
        int marginLeft   = 70;
        int marginRight  = 40;
        int marginTop    = 40;
        int marginBottom = 60;
        int x0           = marginLeft;
        int y0           = height - marginBottom;
        // @format:on

        // --- Determine axis ranges from data ---
        double minX = Arrays.stream(x).min().orElse(0);
        double maxX = Arrays.stream(x).max().orElse(1);
        double minY = Arrays.stream(y).min().orElse(0);
        double maxY = Arrays.stream(y).max().orElse(1);

        // Add 5% padding on both axes so points stay within the frame
        double padX = (maxX - minX) * 0.05;
        double padY = (maxY - minY) * 0.05;
        if (padX == 0) padX = 1.0;
        if (padY == 0) padY = 1.0;

        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;

        // --- Scaling factors ---
        double xScale = (width - marginLeft - marginRight) / (maxX - minX);
        double yScale = (height - marginTop - marginBottom) / (maxY - minY);

        // --- Axes ---
        g2.setColor(Color.GRAY);
        g2.drawLine(x0, y0, width - marginRight, y0);
        g2.drawLine(x0, y0, x0, marginTop);

        // --- Data points ---
        g2.setColor(new Color(30, 100, 200));
        for (int i = 0; i < x.length; i++) {
            int px = (int)(marginLeft + (x[i] - minX) * xScale);
            int py = (int)(y0 - (y[i] - minY) * yScale);
            g2.fillOval(px - 3, py - 3, 6, 6);
        }

        // --- Fitted exponential curve ---
        if (!fitFailed && result != null && Double.isFinite(result.getTau())) {
            double tau = result.getTau();
            double t = (tau > 0) ? 1000.0 / tau : Double.NaN;

            double m = Arrays.stream(y).max().orElse(1);
            double b = Arrays.stream(y).min().orElse(0);
            g2.setColor(Color.RED);

            int steps = 200;
            double prevX = minX;
            double prevY = m * Math.exp(-t * prevX) + b;
            for (int i = 1; i <= steps; i++) {

                // @format:off
                double cx = minX + (maxX - minX) * i / steps;
                double cy = m * Math.exp(-t * cx) + b;
                int x1    = (int) (marginLeft + (prevX - minX) * xScale);
                int y1    = (int) (y0 - (prevY - minY) * yScale);
                int x2    = (int) (marginLeft + (cx - minX) * xScale);
                int y2    = (int) (y0 - (cy - minY) * yScale);
                // @format:on

                g2.drawLine(x1, y1, x2, y2);
                prevX = cx;
                prevY = cy;
            }
        }

        // --- Axis labels ---
        g2.setColor(Color.BLACK);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Duration", width / 2 - 30, height - 20);

        g2.rotate(-Math.PI / 2);
        g2.drawString("Frequency", -height / 2 - 30, 20);
        g2.rotate(Math.PI / 2);

        // --- Fit annotation ---
        g2.setColor(fitFailed ? Color.RED.darker() : new Color(0, 128, 0));
        String msg = fitFailed
                ? "Fit failed"
                : String.format("Tau = %.1f ms, R² = %.3f", result.getTau(), result.getRSquared());
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(msg, marginLeft + 10, marginTop + 20);

        g2.dispose();
        return img;
    }

    /**
     * Exports a background-track histogram for each recording as a PNG file:
     * <p>
     *     Output/Background Plots/<recording>.png
     * <p>
     * @param experiment      the experiment containing the recordings
     * @param experimentPath  the experiment root path
     * @throws IOException    if image output fails
     */
    public static void exportBackgroundHistogramsToPngs(Experiment experiment, Path experimentPath) throws IOException {        Path outputDir = experimentPath.resolve("Output").resolve(BACKGROUND_PLOTS);
        Files.createDirectories(outputDir);

        for (Recording recording : experiment.getRecordings()) {
            List<Square> squares = recording.getSquaresOfRecording();
            if (squares == null || squares.isEmpty()) {
                PaintLogger.debugf("Recording '%s': no squares%n", recording.getRecordingName());
                continue;
            }

            SquareUtils.BackgroundEstimationResult backgroundResult          = calculateBackgroundDensity(squares);
            Set<Square>                            backgroundSet             = new HashSet<>(backgroundResult.getBackgroundSquares());
            double                                 backgroundTracksPerSquare = backgroundResult.getBackgroundMean();

            int totalSquares          = squares.size();
            int nBackground           = backgroundSet.size();
            int totalTracks           = squares.stream().mapToInt(Square::getNumberOfTracks).sum();
            int backgroundTracksTotal = backgroundSet.stream().mapToInt(Square::getNumberOfTracks).sum();

            int maxTracks = squares.stream().mapToInt(Square::getNumberOfTracks).max().orElse(0);
            int binSize   = Math.max(1, maxTracks / 20);
            int binCount  = (maxTracks / binSize) + 1;

            int[] allBins = new int[binCount];
            int[] bgBins  = new int[binCount];

            for (Square sq : squares) {
                int n   = sq.getNumberOfTracks();
                int bin = Math.min(n / binSize, binCount - 1);
                allBins[bin]++;
                if (backgroundSet.contains(sq)) {
                    bgBins[bin]++;
                }
            }

            int plotWidth  = 900;
            int plotHeight = 600;
            BufferedImage img = new BufferedImage(plotWidth, plotHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();

            // Draw histogram into the graphics context
            drawHistogram(
                    g2,
                    plotWidth,
                    plotHeight,
                    allBins,
                    bgBins,
                    binSize,
                    recording.getRecordingName(),
                    totalSquares,
                    totalTracks,
                    nBackground,
                    backgroundTracksTotal,
                    backgroundTracksPerSquare
            );
            g2.dispose();

            Path outFile = outputDir.resolve(recording.getRecordingName() + ".png");
            ImageIO.write(img, "png", outFile.toFile());
            PaintLogger.debugf("Wrote background histogram to %s%n", outFile);
        }
    }

    // ========================================================================
    // Tau-fit PNG output
    // ========================================================================

    /**
     * Saves a Tau-fit plot (successful or failed) for the given square.
     * Output is stored as:
     *
     *     Output/Tau-Fitting Plots/Success/<...>.png
     *     Output/Tau-Fitting Plots/Failed/<...>.png
     * <p>
     * The output directory tree is created automatically.
     * <p>
     * @param tracks         the tracks used in Tau fitting
     * @param tauResult      the Tau fitting result (may indicate failure)
     * @param experimentPath the root path of the experiment
     * @param recordingName  the recording name
     * @param squareIndex    the square index (used in file naming)
     */
    public static void saveTauFitPlot(
            List<Track> tracks,
            CalculateTau.CalculateTauResult tauResult,
            Path experimentPath,
            String recordingName,
            int squareIndex
    ) {
        boolean fitFailed =
                (tauResult == null) ||
                        (tauResult.getStatus() != CalculateTau.CalculateTauResult.Status.TAU_SUCCESS) ||
                        !Double.isFinite(tauResult.getTau()) ||
                        !Double.isFinite(tauResult.getRSquared());

        // Build frequency distribution: duration → count
        Map<Double, Integer> freqMap = new TreeMap<>();
        for (Track track : tracks) {
            double d = track.getTrackDuration();
            freqMap.put(d, freqMap.getOrDefault(d, 0) + 1);
        }

        double[] x = new double[freqMap.size()];
        double[] y = new double[freqMap.size()];
        int idx = 0;
        for (Map.Entry<Double, Integer> e : freqMap.entrySet()) {
            x[idx] = e.getKey();
            y[idx] = e.getValue();
            idx++;
        }

        // Render plot
        BufferedImage img = renderTauPlot(x, y, tauResult, fitFailed, 900, 600);

        // Determine output directory
        Path rootDir   = experimentPath.resolve("Output").resolve(TAU_FITTING_PLOTS);
        Path targetDir = rootDir.resolve(fitFailed ? "Failed" : "Success");

        try {
            Files.createDirectories(targetDir);
            Path plotPath = targetDir.resolve(
                    String.format("%s_square_%03d.png", recordingName, squareIndex)
            );
            ImageIO.write(img, "png", plotPath.toFile());
        } catch (Exception e) {
            // Log failure without throwing
            PaintLogger.errorf(
                    "Failed to save Tau-fit plot for '%s' square %03d: %s",
                    recordingName,
                    squareIndex,
                    e.getMessage()
            );
        }
    }

    // ========================================================================
    // Histogram drawing helper
    // ========================================================================

    /**
     * Draws a histogram into the given Graphics2D context. The histogram displays:
     * <p>
     *   • Track-count distribution across all squares
     *   • Track-count distribution among "background" squares
     *   • Summary statistics rendered as text in the upper-right corner
     * <p>
     * All layout calculations are pixel-accurate to ensure consistent rendering
     * regardless of image size.
     */
    private static void drawHistogram(
            Graphics2D g2,
            int w,
            int h,
            int[] allBins,
            int[] bgBins,
            int binSize,
            String title,
            int totalSquares,
            int totalTracks,
            int nBackground,
            int backgroundTracksTotal,
            double avgTracksInBackground
    ) {
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        int marginLeft   = 70;
        int marginBottom = 50;
        int marginTop    = 50;
        int marginRight  = 40;

        int binCount    = allBins.length;
        int maxBinCount = Arrays.stream(allBins).max().orElse(1);

        double xStep  = (double)(w - marginLeft - marginRight) / binCount;
        double yScale = (double)(h - marginTop - marginBottom) / maxBinCount;

        // Axes
        g2.setColor(Color.GRAY);
        int x0 = marginLeft;
        int y0 = h - marginBottom;
        g2.drawLine(x0, y0, w - marginRight, y0);
        g2.drawLine(x0, y0, x0, marginTop);

        // Tick marks
        g2.setColor(Color.BLACK);
        int nTicks = 5;
        for (int i = 0; i <= nTicks; i++) {
            int y = y0 - (i * (h - marginTop - marginBottom) / nTicks);
            int value = (int)Math.round(i * (double)maxBinCount / nTicks);
            g2.drawLine(x0 - 5, y, x0, y);
            g2.drawString(String.valueOf(value), x0 - 45, y + 5);
        }

        // Bars
        for (int i = 0; i < binCount; i++) {
            int total      = allBins[i];
            int background = bgBins[i];
            int x          = (int)(x0 + i * xStep);
            int barW       = (int)(xStep * 0.8);

            if (background > 0) {
                int bgHeight = (int)(background * yScale);
                int bgY      = y0 - bgHeight;
                g2.setColor(new Color(100, 160, 255, 180));
                g2.fillRect(x, bgY, barW, bgHeight);
            }

            int fgHeight = (int)((total - background) * yScale);
            if (fgHeight > 0) {
                int fgY = y0 - fgHeight - (int)(background * yScale);
                g2.setColor(new Color(180, 180, 180));
                g2.fillRect(x, fgY, barW, fgHeight);
            }

            g2.setColor(Color.BLACK);
            g2.drawRect(x, y0 - (int)(total * yScale), barW, (int)(total * yScale));
        }

        // Labels
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString(
                "Track count bins (bin size ≈ " + binSize + ")",
                w / 2 - 90,
                h - 15
        );
        g2.drawString("Number of squares", 10, marginTop - 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString(
                "Track Count Histogram – " + title,
                w / 2 - 150,
                marginTop - 20
        );

        // Right-side summary panel
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(new Color(0, 70, 180));

        int textX = w - 400;
        int textY = marginTop + 20;
        int lineSpacing = 20;

        String[] labels = new String[]{
                "Number of squares in recording",
                "Number of tracks in recording",
                "Number of background squares",
                "Number of tracks in background",
                "Average number of tracks in the background"
        };
        String[] values = new String[]{
                String.valueOf(totalSquares),
                String.valueOf(totalTracks),
                String.valueOf(nBackground),
                String.valueOf(backgroundTracksTotal),
                String.format("%.3f", avgTracksInBackground)
        };

        FontMetrics fm = g2.getFontMetrics();
        int maxLabelW = 0;
        for (String label : labels) {
            maxLabelW = Math.max(maxLabelW, fm.stringWidth(label));
        }

        // Column positions
        int colonPad = 10;                  // space between label end and colon
        int valuePad = 10;                  // space between colon and value
        int colonX = textX + maxLabelW + colonPad;
        int valueX = colonX + fm.stringWidth(":") + valuePad;

        for (int i = 0; i < labels.length; i++) {
            int y = textY + i * lineSpacing;
            g2.drawString(labels[i], textX, y);
            g2.drawString(":", colonX, y);
            g2.drawString(values[i], valueX, y);
        }
    }
}