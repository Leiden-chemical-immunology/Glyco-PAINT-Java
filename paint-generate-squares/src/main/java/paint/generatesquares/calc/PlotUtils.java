/******************************************************************************
 *  Class:        PlotUtils.java
 *  Package:      generatesquares.calc
 *
 *  PURPOSE:
 *    Provides lightweight rendering utilities for generating Tau fitting plots
 *    and related graphical visualizations.
 *
 *  DESCRIPTION:
 *    This utility class renders 2D visual representations of Tau fits and their
 *    corresponding frequency data. It supports rendering of data points, fitted
 *    exponential decay curves, labeled axes, and annotations summarizing Tau
 *    and R² statistics. The plots are rendered directly into a BufferedImage,
 *    suitable for saving or embedding in reports and GUIs.
 *
 *  RESPONSIBILITIES:
 *    • Draw frequency–duration scatter plots
 *    • Render fitted exponential decay curves
 *    • Display Tau and R² annotations
 *    • Produce anti-aliased BufferedImage output for use in Paint analysis
 *
 *  USAGE EXAMPLE:
 *    BufferedImage img = PlotUtils.renderTauPlot(x, y, result, false, 800, 600);
 *    ImageIO.write(img, "png", outputFile);
 *
 *  DEPENDENCIES:
 *    - java.awt.*
 *    - java.awt.image.BufferedImage
 *    - generatesquares.calc.CalculateTau
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-23
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.generatesquares.calc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;


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
            int px = (int) (marginLeft + (x[i] - minX) * xScale);
            int py = (int) (y0 - (y[i] - minY) * yScale);
            g2.fillOval(px - 3, py - 3, 6, 6); // 6px diameter point
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
}