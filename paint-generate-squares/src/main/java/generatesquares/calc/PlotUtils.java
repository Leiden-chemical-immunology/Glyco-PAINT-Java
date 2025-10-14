package generatesquares.calc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Utility for rendering Tau fitting plots as BufferedImages.
 * No GUI window is shown — used by TauPlotCollector.
 */
public class PlotUtils {

    /**
     * Renders a Tau fitting plot showing data points and optional fitted curve.
     *
     * @param x          durations (domain)
     * @param y          frequencies (range)
     * @param result     CalculateTau result (may be null or invalid)
     * @param fitFailed  true if fit failed or invalid
     * @param width      image width
     * @param height     image height
     * @return BufferedImage ready to save
     */
    public static BufferedImage renderTauPlot(double[] x, double[] y,
                                              CalculateTau.CalculateTauResult result,
                                              boolean fitFailed,
                                              int width, int height) {

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        int marginLeft = 70, marginRight = 40, marginTop = 40, marginBottom = 60;
        int x0 = marginLeft;
        int y0 = height - marginBottom;

        double minX = Arrays.stream(x).min().orElse(0);
        double maxX = Arrays.stream(x).max().orElse(1);
        double minY = Arrays.stream(y).min().orElse(0);
        double maxY = Arrays.stream(y).max().orElse(1);

        // --- Add 5% padding on both axes so points stay fully inside ---
        double padX = (maxX - minX) * 0.05;
        double padY = (maxY - minY) * 0.05;

        if (padX == 0) padX = 1.0; // avoid zero division for flat x
        if (padY == 0) padY = 1.0; // avoid zero division for flat y

        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;

        double xScale = (width - marginLeft - marginRight) / (maxX - minX);
        double yScale = (height - marginTop - marginBottom) / (maxY - minY);

        // Axes
        g2.setColor(Color.GRAY);
        g2.drawLine(x0, y0, width - marginRight, y0);
        g2.drawLine(x0, y0, x0, marginTop);

        // Data points
        g2.setColor(new Color(30, 100, 200));
        for (int i = 0; i < x.length; i++) {
            int px = (int) (marginLeft + (x[i] - minX) * xScale);
            int py = (int) (y0 - (y[i] - minY) * yScale);
            g2.fillOval(px - 3, py - 3, 6, 6);
        }

        // Fitted curve (if available)
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
                double cx = minX + (maxX - minX) * i / steps;
                double cy = m * Math.exp(-t * cx) + b;
                int x1 = (int) (marginLeft + (prevX - minX) * xScale);
                int y1 = (int) (y0 - (prevY - minY) * yScale);
                int x2 = (int) (marginLeft + (cx - minX) * xScale);
                int y2 = (int) (y0 - (cy - minY) * yScale);
                g2.drawLine(x1, y1, x2, y2);
                prevX = cx;
                prevY = cy;
            }
        }

        // Labels
        g2.setColor(Color.BLACK);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
        g2.drawString("Duration", width / 2 - 30, height - 20);
        g2.rotate(-Math.PI / 2);
        g2.drawString("Frequency", -height / 2 - 30, 20);
        g2.rotate(Math.PI / 2);

        // Annotation
        g2.setColor(fitFailed ? Color.RED.darker() : new Color(0, 128, 0));
        String msg = fitFailed
                ? "Fit failed"
                : String.format("Tau = %.1f ms, R² = %.3f", result.getTau(), result.getRSquared());
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
        g2.drawString(msg, marginLeft + 10, marginTop + 20);

        g2.dispose();
        return img;
    }
}