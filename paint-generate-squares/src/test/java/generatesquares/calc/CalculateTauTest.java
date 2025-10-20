package generatesquares.calc;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Test harness and visualization for {@link CalculateTau}.
 * <p>
 * Uses the real production fitter via {@link CalculateTau#debugFit(double[], double[])}.
 * Intended for visual and numeric validation against Python or other benchmarks.
 */
public class CalculateTauTest {

    public static void main(String[] args) {
        System.out.println("=== Test 1: Synthetic Exponential ===");
        double[] durations1 = {0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5};
        double[] freq1 = {2000, 1200, 750, 500, 300, 200, 150, 100, 70, 50};

        runAndPlotTest(durations1, freq1, "Synthetic Exponential", 997.0878843268896, 0.9995441821230724);

        System.out.println("\n=== Test 2: Irregular / Realistic Dataset ===");
        double[] durations2 = {0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.55, 0.65, 0.8, 1.1, 1.951, 2.251, 3.101, 5.702};
        double[] freq2 = {2.0, 2.0, 1.0, 4.0, 2.0, 1.0, 4.0, 1.0, 1.0, 2.0, 3.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

        runAndPlotTest(durations2, freq2, "Irregular Dataset", Double.NaN, Double.NaN);
    }

    private static void runAndPlotTest(double[] durations, double[] freq, String title,
                                       double expectedTau, double expectedR2) {

        double[] result = CalculateTau.debugFit(durations, freq);
        double tauMs = result[0];
        double rSquared = result[1];

        System.out.printf("τ = %.6f ms, R² = %.6f%n", tauMs, rSquared);

        if (!Double.isNaN(expectedTau)) {
            System.out.printf("Δτ vs. Python = %.6f%n", tauMs - expectedTau);
            System.out.printf("ΔR² vs. Python = %.6f%n", rSquared - expectedR2);
        }

        plotFitting(durations, freq, tauMs, rSquared, title);
    }

    /**
     * Generate the fitted curve and render both data and fit.
     */
    private static void plotFitting(double[] x, double[] y, double tauMs, double r2, String title) {
        double t = 1000.0 / tauMs;
        double m = Arrays.stream(y).max().orElse(1);
        double b = Arrays.stream(y).min().orElse(0);

        int n = 100;
        double[] fitX = new double[n];
        double[] fitY = new double[n];
        double maxX = Arrays.stream(x).max().orElse(1);

        for (int i = 0; i < n; i++) {
            fitX[i] = i * maxX / (n - 1);
            fitY[i] = m * Math.exp(-t * fitX[i]) + b;
        }

        System.out.printf("✅ Plot: %s | τ = %.3f ms, R² = %.6f%n", title, tauMs, r2);
        plotFit(x, y, fitX, fitY, title);
    }

    /**
     * Simple 2D plot using Swing.
     */
    private static void plotFit(double[] x, double[] y, double[] fitX, double[] fitY, String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int marginLeft = 70, marginRight = 40, marginTop = 40, marginBottom = 60;

                double minX = Arrays.stream(x).min().orElse(0);
                double maxX = Arrays.stream(x).max().orElse(1);
                double minY = Arrays.stream(y).min().orElse(0);
                double maxY = Arrays.stream(y).max().orElse(1);

                double xScale = (w - marginLeft - marginRight) / (maxX - minX);
                double yScale = (h - marginTop - marginBottom) / (maxY - minY);
                int y0 = h - marginBottom;

                g2.setColor(Color.GRAY);
                g2.drawLine(marginLeft, y0, w - marginRight, y0);
                g2.drawLine(marginLeft, y0, marginLeft, marginTop);

                g2.setColor(new Color(30, 100, 200));
                for (int i = 0; i < x.length; i++) {
                    int px = (int) (marginLeft + (x[i] - minX) * xScale);
                    int py = (int) (y0 - (y[i] - minY) * yScale);
                    g2.fillOval(px - 4, py - 4, 8, 8);
                }

                g2.setColor(Color.RED);
                for (int i = 1; i < fitX.length; i++) {
                    int x1 = (int) (marginLeft + (fitX[i - 1] - minX) * xScale);
                    int y1 = (int) (y0 - (fitY[i - 1] - minY) * yScale);
                    int x2 = (int) (marginLeft + (fitX[i] - minX) * xScale);
                    int y2 = (int) (y0 - (fitY[i] - minY) * yScale);
                    g2.drawLine(x1, y1, x2, y2);
                }

                g2.setColor(Color.BLACK);
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("Duration", w / 2 - 30, h - 20);
                g2.rotate(-Math.PI / 2);
                g2.drawString("Frequency", -h / 2 - 30, 25);
                g2.rotate(Math.PI / 2);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }
}