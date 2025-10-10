package paint.shared.utils;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import paint.shared.objects.Experiment;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistogramPdfExporter {

    public static void exportExperimentHistogramsToPdf(Experiment experiment, Path outputFile) throws IOException {

        try (PDDocument doc = new PDDocument()) {

            for (Recording recording : experiment.getRecordings()) {
                List<Square> squares = recording.getSquaresOfRecording();
                if (squares == null || squares.isEmpty()) {
                    System.out.printf("Recording '%s': no squares%n", recording.getRecordingName());
                    continue;
                }

                // --- Compute background estimation ---
                SquareUtils.BackgroundEstimationResult backgroundResult = SquareUtils.estimateBackgroundDensity(squares);

                Set<Square> backgroundSet = new HashSet<>(backgroundResult.getBackgroundSquares());
                double backgroundTracksPerSquare = backgroundResult.getBackgroundMean();

                int totalSquares = squares.size();
                int nBackground = backgroundSet.size();
                int totalTracks = squares.stream().mapToInt(Square::getNumberOfTracks).sum();
                int backgroundTracksTotal = backgroundSet.stream().mapToInt(Square::getNumberOfTracks).sum();

                // --- Build histogram bins ---
                int maxTracks = squares.stream().mapToInt(Square::getNumberOfTracks).max().orElse(0);
                int binSize = Math.max(1, maxTracks / 20);
                int binCount = (maxTracks / binSize) + 1;

                int[] allBins = new int[binCount];
                int[] bgBins = new int[binCount];

                for (Square sq : squares) {
                    int n = sq.getNumberOfTracks();
                    int bin = Math.min(n / binSize, binCount - 1);
                    allBins[bin]++;
                    if (backgroundSet.contains(sq)) {
                        bgBins[bin]++;
                    }
                }

                // --- Create a new PDF page per recording ---
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                // Draw at your preferred Swing-like size
                int plotWidth = 800;
                int plotHeight = 600;

                PdfBoxGraphics2D g2 = new PdfBoxGraphics2D(doc, plotWidth, plotHeight);

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
                PDFormXObject formXObject = g2.getXFormObject();

                // ✅ Scale-to-fit into the page with margins and center it
                // @formatter:off
                float margin  = 36f; // 0.5 inch
                float maxW    = pageWidth - 2 * margin;
                float maxH    = pageHeight - 2 * margin;
                float scale   = Math.min(Math.min(maxW / plotWidth, maxH / plotHeight), 1.0f); // don't upscale
                float scaledW = plotWidth * scale;
                float scaledH = plotHeight * scale;

                float offsetX = (pageWidth - scaledW) / 2f;
                float offsetY = (pageHeight - scaledH) / 2f;
                // @formatter:on

                try (PDPageContentStream contentStream =
                             new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.saveGraphicsState();
                    // order matters: translate first, then scale (so geometry applies scale then translate)
                    contentStream.transform(org.apache.pdfbox.util.Matrix.getTranslateInstance(offsetX, offsetY));
                    contentStream.transform(org.apache.pdfbox.util.Matrix.getScaleInstance(scale, scale));
                    contentStream.drawForm(formXObject);
                    contentStream.restoreGraphicsState();
                }
                PaintLogger.debugf("Added histogram for recording '%s'%n", recording.getRecordingName());
            }

            // --- Save the combined document ---
            PaintLogger.debugf("Added %d pages to %s%n", doc.getNumberOfPages(), outputFile);
            doc.save(outputFile.toFile());
        }

        PaintLogger.debugf("✅ Saved combined experiment PDF → %s%n", outputFile);
    }

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

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, w, h);

        int marginLeft   = 70;
        int marginBottom = 50;
        int marginTop    = 50;
        int marginRight  = 40;

        int binCount = allBins.length;
        int maxBinCount = Arrays.stream(allBins).max().orElse(1);

        double xStep = (double) (w - marginLeft - marginRight) / binCount;
        double yScale = (double) (h - marginTop - marginBottom) / maxBinCount;

        g2.setColor(Color.GRAY);
        int x0 = marginLeft;
        int y0 = h - marginBottom;
        g2.drawLine(x0, y0, w - marginRight, y0);
        g2.drawLine(x0, y0, x0, marginTop);

        g2.setColor(Color.BLACK);
        int nTicks = 5;
        for (int i = 0; i <= nTicks; i++) {
            int y = y0 - (i * (h - marginTop - marginBottom) / nTicks);
            int value = (int) Math.round(i * (double) maxBinCount / nTicks);
            g2.drawLine(x0 - 5, y, x0, y);
            g2.drawString(String.valueOf(value), x0 - 45, y + 5);
        }

        for (int i = 0; i < binCount; i++) {
            int total = allBins[i];
            int background = bgBins[i];
            int x = (int) (x0 + i * xStep);
            int barW = (int) (xStep * 0.8);

            if (background > 0) {
                int bgHeight = (int) (background * yScale);
                int bgY = y0 - bgHeight;
                g2.setColor(new Color(100, 160, 255, 180));
                g2.fillRect(x, bgY, barW, bgHeight);
            }

            int fgHeight = (int) ((total - background) * yScale);
            if (fgHeight > 0) {
                int fgY = y0 - fgHeight - (int) (background * yScale);
                g2.setColor(new Color(180, 180, 180));
                g2.fillRect(x, fgY, barW, fgHeight);
            }

            g2.setColor(Color.BLACK);
            g2.drawRect(x, y0 - (int) (total * yScale), barW, (int) (total * yScale));
        }

        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g2.drawString("Track count bins (bin size ≈ " + binSize + ")", w / 2 - 90, h - 15);
        g2.drawString("Number of squares", 10, marginTop - 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString("Track Count Histogram – " + title, w / 2 - 150, marginTop - 20);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        g2.setColor(new Color(0, 70, 180));
        int textX = w - 370;
        int textY = marginTop + 20;

        g2.drawString(String.format("Number of squares in recording: %d", totalSquares), textX, textY);
        g2.drawString(String.format("Number of tracks in recording: %d", totalTracks), textX, textY + 20);
        g2.drawString(String.format("Number of background squares: %d", nBackground), textX, textY + 40);
        g2.drawString(String.format("Number of tracks in background: %d", backgroundTracksTotal), textX, textY + 60);
        g2.drawString(String.format("Average number of tracks in the background: %.3f", avgTracksInBackground), textX, textY + 80);
    }
}