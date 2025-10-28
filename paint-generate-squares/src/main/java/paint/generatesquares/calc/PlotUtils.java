/******************************************************************************
 *  Class:        PlotUtils.java
 *  Package:      paint.generatesquares.calc
 *
 *  PURPOSE:
 *    Provides lightweight rendering utilities for generating Tau-fitting plots,
 *    histograms and related graphical visualizations within the Paint experiment
 *    workflow.
 *
 *  DESCRIPTION:
 *    This utility class supports rendering of 2D visual representations such as:
 *      • Frequency–duration scatter plots with exponential curve fits
 *      • Histograms of track counts per square and background estimation
 *      • Saving Tau-fit plots (PNG) under “Success” or “Failed” directories
 *    Plots are rendered into BufferedImages, PDF documents or image files
 *    and can be saved or embedded in reports and GUIs.
 *
 *  RESPONSIBILITIES:
 *    • Draw frequency–duration scatter plots and fitted exponential curves
 *    • Display Tau and R² annotations on plots
 *    • Build histograms of track counts and background counts per square
 *    • Save Tau-fit images, handling directory structure and failure/success classification
 *    • Produce antialiased BufferedImage or PDF or PNG output for use in Paint analysis
 *
 *  USAGE EXAMPLE:
 *    BufferedImage img = PlotUtils.renderTauPlot(x, y, result, false, 800,600);
 *    PlotUtils.exportExperimentHistogramsToPdf(experiment, outputFile);
 *    PlotUtils.saveTauFitPlot(tracks, tauResult, experimentPath, recordingName, squareIndex);
 *
 *  DEPENDENCIES:
 *    – paint.shared.objects.{Experiment, Recording, Square, Track}
 *    – paint.shared.utils.PaintLogger
 *    – paint.generatesquares.calc.SquareUtils
 *    – org.apache.pdfbox and de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.generatesquares.calc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.List;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;
import paint.shared.utils.PaintLogger;

import static paint.generatesquares.calc.SquareUtils.calculateBackgroundDensity;

public class PlotUtils {

    private PlotUtils() {
        // Utility class — prevent instantiation
    }

    /**
     * Renders a Tau plot with given data points, fit curve, axes, labels, and annotations.
     *
     * @param x          the array of x-coordinate values for the data points to be plotted
     * @param y          the array of y-coordinate values for the data points to be plotted
     * @param result     the calculated result containing the Tau value and R² value for the exponential fit
     * @param fitFailed  a flag indicating whether the exponential fit failed
     * @param width      the width of the generated image in pixels
     * @param height     the height of the generated image in pixels
     * @return a BufferedImage representing the rendered Tau plot
     */
    public static BufferedImage renderTauPlot(double[] x, double[] y,
                                              CalculateTau.CalculateTauResult result,
                                              boolean fitFailed,
                                              int width, int height) {

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
        if (padX == 0) {
            padX = 1.0; // avoid zero-division for flat x data
        }
        if (padY == 0) {
            padY = 1.0; // avoid zero-division for flat y data
        }

        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;

        // --- Calculate scaling factors ---
        double xScale = (width - marginLeft - marginRight) / (maxX - minX);
        double yScale = (height - marginTop - marginBottom) / (maxY - minY);

        // --- Draw X and Y axes ---
        g2.setColor(Color.GRAY);
        g2.drawLine(x0, y0, width - marginRight, y0); // X-axis
        g2.drawLine(x0, y0, x0, marginTop);           // Y-axis

        // --- Draw data points ---
        g2.setColor(new Color(30, 100, 200)); // blue tone
        for (int i = 0; i < x.length; i++) {
            int px = (int)(marginLeft + (x[i] - minX) * xScale);
            int py = (int)(y0 - (y[i] - minY) * yScale);
            g2.fillOval(px - 3, py - 3, 6, 6);
        }

        // --- Draw fitted exponential curve (if available) ---
        if (!fitFailed && result != null && Double.isFinite(result.getTau())) {
            double tau = result.getTau();
            // Convert to rate constant (1/ms) — arbitrary scaling for display
            double t = (tau > 0) ? 1000.0 / tau : Double.NaN;

            double m = Arrays.stream(y).max().orElse(1); // amplitude
            double b = Arrays.stream(y).min().orElse(0); // baseline
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

        // --- Fit annotation (Tau/R² or "Fit failed") ---
        g2.setColor(fitFailed ? Color.RED.darker() : new Color(0, 128, 0));
        String msg = fitFailed
                ? "Fit failed"
                : String.format("Tau = %.1f ms, R² = %.3f",
                                result.getTau(), result.getRSquared());
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(msg, marginLeft + 10, marginTop + 20);

        // --- Cleanup and return image ---
        g2.dispose();
        return img;
    }

    /**
     * Exports histograms of the experimental data to a PDF file. Each recording of the
     * experiment is represented as a histogram in a separate page within the generated PDF.
     *
     * @param experiment The experiment containing recordings with track count data to be exported.
     * @param outputFile The output {@code Path} where the generated PDF file will be saved.
     * @throws IOException If an error occurs during the creation or saving of the PDF file.
     */
    public static void exportExperimentHistogramsToPdf(Experiment experiment, Path outputFile) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            for (Recording recording : experiment.getRecordings()) {
                List<Square> squares = recording.getSquaresOfRecording();
                if (squares == null || squares.isEmpty()) {
                    System.out.printf("Recording '%s': no squares%n", recording.getRecordingName());
                    continue;
                }

                SquareUtils.BackgroundEstimationResult backgroundResult = calculateBackgroundDensity(squares);
                Set<Square> backgroundSet = new HashSet<>(backgroundResult.getBackgroundSquares());
                double backgroundTracksPerSquare = backgroundResult.getBackgroundMean();

                int totalSquares = squares.size();
                int nBackground  = backgroundSet.size();
                int totalTracks  = squares.stream().mapToInt(Square::getNumberOfTracks).sum();
                int backgroundTracksTotal = backgroundSet.stream().mapToInt(Square::getNumberOfTracks).sum();

                int maxTracks = squares.stream()
                        .mapToInt(Square::getNumberOfTracks).max().orElse(0);
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

                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                float pageWidth  = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                int plotWidth  = 800;
                int plotHeight = 600;

                PdfBoxGraphics2D g2 = new PdfBoxGraphics2D(doc, plotWidth, plotHeight);

                drawHistogram(g2,
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
                              backgroundTracksPerSquare);

                g2.dispose();
                PDFormXObject formXObject = g2.getXFormObject();

                float margin  = 36f;
                float maxW    = pageWidth  - 2 * margin;
                float maxH    = pageHeight - 2 * margin;
                float scale   = Math.min(Math.min(maxW / plotWidth, maxH / plotHeight), 1.0f);
                float scaledW = plotWidth  * scale;
                float scaledH = plotHeight * scale;

                float offsetX = (pageWidth  - scaledW) / 2f;
                float offsetY = (pageHeight - scaledH) / 2f;

                try (PDPageContentStream contentStream =
                             new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.saveGraphicsState();
                    contentStream.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(offsetX, offsetY));
                    contentStream.transform(org.apache.pdfbox.util.Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(formXObject);
                    contentStream.restoreGraphicsState();
                }
                PaintLogger.debugf("Added histogram for recording '%s'%n", recording.getRecordingName());
            }

            PaintLogger.debugf("Added %d pages to %s%n", doc.getNumberOfPages(), outputFile);
            doc.save(outputFile.toFile());
        }
    }

    // ========== Tau-fit PNG output ==========

    /**
     * Saves a plot visualizing the Tau-fit result for a square as a PNG file under
     * “Success” or “Failed”, within the current experiment’s output directory.
     *
     * @param tracks         the list of tracks used to compute the frequency distribution
     *                       (their durations will form the x-axis)
     * @param tauResult      the result of the Tau calculation (may indicate failure)
     * @param experimentPath the base path of the experiment directory
     * @param recordingName  the name of the recording
     * @param squareIndex    the index of the square region (used to name the file)
     * @throws IOException   if an error occurs while creating directories or writing the file
     */
    public static void saveTauFitPlot(List<Track> tracks,
                                      CalculateTau.CalculateTauResult tauResult,
                                      Path experimentPath,
                                      String recordingName,
                                      int squareIndex) {
        boolean fitFailed = (tauResult == null ||
                tauResult.getStatus() != CalculateTau.CalculateTauResult.Status.TAU_SUCCESS ||
                !Double.isFinite(tauResult.getTau()) ||
                !Double.isFinite(tauResult.getRSquared()));

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

        BufferedImage img = renderTauPlot(x, y, tauResult, fitFailed, 900, 600);

        Path rootDir   = experimentPath.resolve("Output").resolve("Tau Fitting Plots");
        Path targetDir = rootDir.resolve(fitFailed ? "Failed" : "Success");

        try {
            Files.createDirectories(targetDir);
            Path plotPath = targetDir.resolve(String.format("%s_square_%03d.png", recordingName, squareIndex));
            ImageIO.write(img, "png", plotPath.toFile());
        } catch (Exception e) {
            // Log failure but do *not* throw
            PaintLogger.errorf("Failed to save Tau-fit plot for '%s' square %03d: %s",
                               recordingName, squareIndex, e.getMessage());
        }
    }

    // ========== Helper method: drawHistogram ==========

    private static void drawHistogram(Graphics2D g2,
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
                                      double avgTracksInBackground) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        int marginLeft   = 70;
        int marginBottom = 50;
        int marginTop    = 50;
        int marginRight  = 40;

        int binCount     = allBins.length;
        int maxBinCount = Arrays.stream(allBins).max().orElse(1);

        double xStep  = (double)(w - marginLeft - marginRight) / binCount;
        double yScale = (double)(h - marginTop - marginBottom) / maxBinCount;

        g2.setColor(Color.GRAY);
        int x0 = marginLeft;
        int y0 = h - marginBottom;
        g2.drawLine(x0, y0, w - marginRight, y0);
        g2.drawLine(x0, y0, x0, marginTop);

        g2.setColor(Color.BLACK);
        int nTicks = 5;
        for (int i = 0; i <= nTicks; i++) {
            int y = y0 - (i * (h - marginTop - marginBottom) / nTicks);
            int value = (int)Math.round(i * (double) maxBinCount / nTicks);
            g2.drawLine(x0 - 5, y, x0, y);
            g2.drawString(String.valueOf(value), x0 - 45, y + 5);
        }

        for (int i = 0; i < binCount; i++) {
            int total      = allBins[i];
            int background = bgBins[i];
            int x          = (int)(x0 + i * xStep);
            int barW       = (int)(xStep * 0.8);

            if (background > 0) {
                int bgHeight = (int)(background * yScale);
                int bgY      = y0 - bgHeight;
                g2.setColor(new Color(100,160,255,180));
                g2.fillRect(x, bgY, barW, bgHeight);
            }

            int fgHeight = (int)((total - background) * yScale);
            if (fgHeight > 0) {
                int fgY = y0 - fgHeight - (int)(background * yScale);
                g2.setColor(new Color(180,180,180));
                g2.fillRect(x, fgY, barW, fgHeight);
            }

            g2.setColor(Color.BLACK);
            g2.drawRect(x, y0 - (int)(total * yScale), barW, (int)(total * yScale));
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("Track count bins (bin size ≈ " + binSize + ")", w / 2 - 90, h - 15);
        g2.drawString("Number of squares", 10, marginTop - 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Track Count Histogram – " + title, w / 2 - 150, marginTop - 20);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(new Color(0,70,180));
        int textX = w - 370;
        int textY = marginTop + 20;

        g2.drawString(String.format("Number of squares in recording: %d", totalSquares),      textX, textY);
        g2.drawString(String.format("Number of tracks in recording: %d", totalTracks),     textX, textY + 20);
        g2.drawString(String.format("Number of background squares: %d", nBackground),      textX, textY + 40);
        g2.drawString(String.format("Number of tracks in background: %d", backgroundTracksTotal), textX, textY + 60);
        g2.drawString(String.format("Average number of tracks in the background: %.3f", avgTracksInBackground), textX, textY + 80);
    }
}