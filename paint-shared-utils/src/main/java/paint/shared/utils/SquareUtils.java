package paint.shared.utils;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.objects.Track;

import java.util.*;

public class SquareUtils {

    private SquareUtils() {}

    /**
     * Apply visibility/selection rules to a list of squares.
     * Marks each square as selected if all filter criteria are met and numeric values are valid.
     */
    public static void applyVisibilityFilter(Recording recording,
                                             double minDensityRatio,
                                             double maxVariability,
                                             double minRSquared,
                                             String neighbourMode) {

        List<Square> squares = recording.getSquaresOfRecording();
        if (squares == null || squares.isEmpty()) {
            return;
        }

        int total = squares.size();
        int visibleBasic = 0;

        // --- First pass: basic visibility based on numerical criteria ---
        for (Square square : squares) {
            boolean passes = square.getDensityRatio() >= minDensityRatio
                    && square.getVariability() <= maxVariability
                    && square.getRSquared() >= minRSquared
                    && !Double.isNaN(square.getRSquared());

            square.setSelected(passes);
            if (passes) {
                visibleBasic++;
            }
        }

         //PaintLogger.debugf("VisibilityFilter [%s] basic pass: %d / %d squares visible", neighbourMode, visibleBasic, total);

        // --- Second pass: neighbour filtering ---
        if ("Free".equalsIgnoreCase(neighbourMode)) {
            // PaintLogger.debugf("NeighbourMode = Free → skipping neighbour constraints");
            return;
        }

        Set<Square> keep = new HashSet<>();
        int keptCount = 0;

        for (Square square : squares) {
            if (!square.isSelected()) {
                continue;
            }

            boolean hasNeighbour = false;
            int r = square.getRowNumber();
            int c = square.getColNumber();

            for (Square other : squares) {
                if (other == square || !other.isSelected()) {
                    continue;
                }
                int dr = Math.abs(other.getRowNumber() - r);
                int dc = Math.abs(other.getColNumber() - c);

                if ("Relaxed".equalsIgnoreCase(neighbourMode)) {
                    // Touches at corner or edge
                    if (dr <= 1 && dc <= 1 && (dr + dc) > 0) {
                        hasNeighbour = true;
                        break;
                    }
                } else if ("Strict".equalsIgnoreCase(neighbourMode)) {
                    // Must share an edge (no corner-only contact)
                    if ((dr == 1 && dc == 0) || (dr == 0 && dc == 1)) {
                        hasNeighbour = true;
                        break;
                    }
                }
            }

            if (hasNeighbour) {
                keep.add(square);
                keptCount++;
            }
        }

        // --- Apply neighbour filtering result ---
        for (Square sq : squares) {
            if (!keep.contains(sq)) {
                sq.setSelected(false);
            }
        }

        PaintLogger.debugf("NeighbourMode [%s] neighbour-filtered: %d / %d retained",
                           neighbourMode, keptCount, visibleBasic);
    }


    /**
     * Iterative mean/std-based background estimation.
     * Returns both the mean and the final background squares.
     */
    public static BackgroundEstimationResult estimateBackgroundDensity(List<Square> squares) {
        if (squares == null || squares.isEmpty())
            return new BackgroundEstimationResult(Double.NaN, Collections.emptyList());

        double mean = squares.stream()
                .mapToDouble(Square::getNumberOfTracks)
                .average().orElse(Double.NaN);

        if (Double.isNaN(mean) || mean == 0)
            return new BackgroundEstimationResult(mean, Collections.emptyList());

        final double EPSILON = 0.01;
        final int MAX_ITER = 10;
        double prevMean;

        List<Square> current = new ArrayList<>(squares);

        for (int iter = 0; iter < MAX_ITER; iter++) {
            prevMean = mean;
            final double meanForLambda = mean;

            double std = Math.sqrt(current.stream()
                                           .mapToDouble(sq -> Math.pow(sq.getNumberOfTracks() - meanForLambda, 2))
                                           .average().orElse(0));

            final double threshold = meanForLambda + 2 * std;

            List<Square> filtered = new ArrayList<>();
            for (Square sq : current) {
                if (sq.getNumberOfTracks() <= threshold) {
                    filtered.add(sq);
                }
            }

            if (filtered.isEmpty())
                break;

            mean = filtered.stream()
                    .mapToDouble(Square::getNumberOfTracks)
                    .average().orElse(mean);

            current = filtered;

            if (Math.abs(mean - prevMean) / prevMean < EPSILON)
                break;
        }

        return new BackgroundEstimationResult(mean, current);
    }


    public static List<Track> getTracksFromSelectedSquares(Recording recording) {
        List<Track> selectedTracks = new ArrayList<>();

        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected() && square.getTracks() != null) {
                selectedTracks.addAll(square.getTracks());
            }
        }

        return selectedTracks;
    }

    public static int getNumberOfSelectedSquares(Recording recording) {
        int count = 0;

        for (Square square : recording.getSquaresOfRecording()) {
            if (square.isSelected()) {
                count++;
            }
        }

        return count;
    }

    private static double getMedian(List<Double> values) {
        if (values.isEmpty()) return Double.NaN;
        int n = values.size();
        if (n % 2 == 1) return values.get(n / 2);
        return 0.5 * (values.get(n / 2 - 1) + values.get(n / 2));
    }


    /* IGNORE START
    public static BackgroundEstimationResult estimateBackgroundDensityRobust(List<Square> squares) {
        if (squares == null || squares.isEmpty()) {
            return new BackgroundEstimationResult(Double.NaN, Collections.emptyList());
        }

        List<Square> current = new ArrayList<>(squares);
        final double EPSILON = 0.01;
        final int MAX_ITER = 10;
        double prevMedian = 0;

        for (int iter = 0; iter < MAX_ITER; iter++) {
            List<Double> values = current.stream()
                    .map(s -> (double) s.getNumberOfTracks())
                    .filter(v -> v >= 0 && !Double.isNaN(v))
                    .sorted()
                    .collect(Collectors.toList());

            double median = getMedian(values);

            // Median absolute deviation
            List<Double> deviations = new ArrayList<>();
            for (double v : values) deviations.add(Math.abs(v - median));
            double mad = getMedian(deviations);
            double robustSigma = 1.4826 * mad;

            // ---- safeguard: if median or MAD ~ 0, fall back to mean/std
            if (mad < 1e-6 || median < 1e-6) {
                double mean = current.stream()
                        .mapToDouble(Square::getNumberOfTracks)
                        .average().orElse(Double.NaN);
                double std = Math.sqrt(current.stream()
                                               .mapToDouble(s -> Math.pow(s.getNumberOfTracks() - mean, 2))
                                               .average().orElse(0));
                final double thresholdFallback = mean + 1.5 * std;

                List<Square> filtered = current.stream()
                        .filter(sq -> sq.getNumberOfTracks() <= thresholdFallback)
                        .collect(Collectors.toList());

                return new BackgroundEstimationResult(mean, filtered);
            }

            // --- Adaptive threshold selection ---
            double thresholdBase = median + 1.0 * robustSigma;
            final double t1 = thresholdBase;
            List<Square> filtered = current.stream()
                    .filter(sq -> sq.getNumberOfTracks() <= t1)
                    .collect(Collectors.toList());

            double fraction = (double) filtered.size() / squares.size();

            if (fraction < 0.3) {
                final double t2 = median + 1.5 * robustSigma;
                filtered = current.stream()
                        .filter(sq -> sq.getNumberOfTracks() <= t2)
                        .collect(Collectors.toList());
            } else if (fraction > 0.8) {
                final double t3 = median + 0.8 * robustSigma;
                filtered = current.stream()
                        .filter(sq -> sq.getNumberOfTracks() <= t3)
                        .collect(Collectors.toList());
            }

            if (filtered.isEmpty())
                break;

            // Check convergence
            if (Math.abs(median - prevMedian) / (prevMedian == 0 ? 1 : prevMedian) < EPSILON)
                return new BackgroundEstimationResult(median, filtered);

            prevMedian = median;
            current = filtered;
        }

        double finalMean = current.stream()
                .mapToDouble(Square::getNumberOfTracks)
                .average().orElse(Double.NaN);

        return new BackgroundEstimationResult(finalMean, current);
    }
     IGNORE END */

    /**
     * Common container for background estimation results.
     */
    public static class BackgroundEstimationResult {
        private final double backgroundMean;
        private final List<Square> backgroundSquares;

        public BackgroundEstimationResult(double backgroundMean, List<Square> backgroundSquares) {
            this.backgroundMean = backgroundMean;
            this.backgroundSquares = backgroundSquares;
        }

        public double getBackgroundMean() {
            return backgroundMean;
        }

        public List<Square> getBackgroundSquares() {
            return backgroundSquares;
        }
    }


    /* IGNORE START
    public static void showTrackCountDistribution(Recording recording) {
        List<Square> squares = recording.getSquaresOfRecording();
        if (squares == null || squares.isEmpty()) {
            System.out.printf("Recording '%s': no squares%n", recording.getRecordingName());
            return;
        }

        // Collect counts
        List<Integer> counts = squares.stream()
                .map(Square::getNumberOfTracks)
                .sorted()
                .collect(Collectors.toList());  // ✅ instead of .toList()

        IntSummaryStatistics stats = counts.stream()
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        System.out.printf("%nRecording '%s': Track count distribution%n", recording.getRecordingName());
        System.out.printf("Squares: %d  Min: %d  Max: %d  Mean: %.2f  Median: %.2f%n",
                          counts.size(), stats.getMin(), stats.getMax(),
                          stats.getAverage(), getMedianInt(counts));

        // Build histogram (bin size = auto-scaled)
        int maxCount = stats.getMax();
        int binSize = Math.max(1, maxCount / 20); // 20 bins max
        int[] bins = new int[(maxCount / binSize) + 1];

        for (int c : counts) {
            bins[c / binSize]++;
        }

        // Print as text histogram
        for (int i = 0; i < bins.length; i++) {
            int lower = i * binSize;
            int upper = lower + binSize - 1;
            System.out.printf("%3d–%3d | %-40s %d%n", lower, upper,
                              repeat('*', Math.min(40, bins[i] * 40 / counts.size())),
                              bins[i]);
        }
    }
    IGNORE END */

    private static double getMedianInt(List<Integer> values) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        int n = values.size();
        if (n % 2 == 1) {
            return values.get(n / 2);
        }
        return 0.5 * (values.get(n / 2 - 1) + values.get(n / 2));
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /* IGNORE START
    public static void plotHybridTrackDistribution(Recording recording) {
        List<Square> squares = recording.getSquaresOfRecording();
        if (squares == null || squares.isEmpty()) {
            System.out.printf("Recording '%s': no squares%n", recording.getRecordingName());
            return;
        }

        // --- Collect counts and bin them ---
        Map<String, Integer> bins = new LinkedHashMap<>();

        // Individual bins for 1–10
        for (int i = 1; i <= 10; i++) {
            final int val = i;
            long count = squares.stream().filter(sq -> sq.getNumberOfTracks() == val).count();
            bins.put(String.valueOf(i), (int) count);
        }

        // Group >10 into bins of 10 (11–20, 21–30, …)
        int maxTracks = squares.stream().mapToInt(Square::getNumberOfTracks).max().orElse(0);
        for (int start = 11; start <= maxTracks; start += 10) {
            int end = Math.min(start + 9, maxTracks);
            final int s = start;
            final int e = end;
            long count = squares.stream()
                    .filter(sq -> {
                        int n = sq.getNumberOfTracks();
                        return n >= s && n <= e;
                    })
                    .count();
            bins.put(start + "–" + end, (int) count);
        }

        // --- Create chart window ---
        JFrame frame = new JFrame("Hybrid Track Count Distribution – " + recording.getRecordingName());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(900, 450);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int margin = 50;

                int maxCount = bins.values().stream().mapToInt(Integer::intValue).max().orElse(1);
                double xStep = (double) (w - 2 * margin) / bins.size();
                double yScale = (double) (h - 2 * margin) / maxCount;

                // Axes
                g2.setColor(Color.GRAY);
                g2.drawLine(margin, h - margin, w - margin, h - margin); // x-axis
                g2.drawLine(margin, h - margin, margin, margin);         // y-axis

                // Draw bars
                int i = 0;
                for (Map.Entry<String, Integer> e : bins.entrySet()) {
                    int x = (int) (margin + i * xStep);
                    int barHeight = (int) (e.getValue() * yScale);
                    int y = h - margin - barHeight;

                    g2.setColor(i < 10 ? Color.BLUE : new Color(180, 60, 60)); // 1–10 vs grouped
                    g2.fillRect(x, y, (int) (xStep * 0.8), barHeight);

                    // Labels
                    g2.setColor(Color.BLACK);
                    g2.setFont(g2.getFont().deriveFont(10f));
                    g2.drawString(e.getKey(), x + 3, h - margin + 12);

                    i++;
                }

                // Axis labels
                g2.setColor(Color.BLACK);
                g2.drawString("Track count (1–10 individual, rest grouped by 10)", w / 2 - 140, h - 10);
                g2.drawString("Number of squares", 10, margin - 10);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }
    IGNORE END  */

    /* IGNORE START
    public static void plotTrackCountHistogramWithBackground(
            Recording recording,
            SquareUtils.BackgroundEstimationResult backgroundResult) {

        List<Square> squares = recording.getSquaresOfRecording();
        if (squares == null || squares.isEmpty()) {
            System.out.printf("Recording '%s': no squares%n", recording.getRecordingName());
            return;
        }

        Set<Square> backgroundSet = new HashSet<>(backgroundResult.getBackgroundSquares());
        double backgroundMean = backgroundResult.getBackgroundMean();

        int totalSquares = squares.size();
        int nBackground = backgroundSet.size();
        int totalTracks = squares.stream().mapToInt(Square::getNumberOfTracks).sum();
        int backgroundTracksTotal = backgroundSet.stream().mapToInt(Square::getNumberOfTracks).sum();
        double averageTracksInBackground =
                (nBackground > 0) ? (backgroundTracksTotal / (double) nBackground) : Double.NaN;

        // --- Build histogram bins ---
        int maxTracks = squares.stream().mapToInt(Square::getNumberOfTracks).max().orElse(0);
        int binSize = Math.max(1, maxTracks / 20); // ≈20 bins
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

        // --- Create chart window ---
        JFrame frame = new JFrame("Track Count Histogram – " + recording.getRecordingName());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(950, 500);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                int marginLeft = 70;
                int marginBottom = 50;
                int marginTop = 30;
                int marginRight = 40;

                int maxBinCount = 0;
                for (int c : allBins) if (c > maxBinCount) maxBinCount = c;
                if (maxBinCount == 0) maxBinCount = 1;

                double xStep = (double) (w - marginLeft - marginRight) / binCount;
                double yScale = (double) (h - marginTop - marginBottom) / maxBinCount;

                // --- Draw axes ---
                g2.setColor(Color.GRAY);
                int x0 = marginLeft;
                int y0 = h - marginBottom;
                g2.drawLine(x0, y0, w - marginRight, y0); // x-axis
                g2.drawLine(x0, y0, x0, marginTop);       // y-axis

                // --- Y-axis ticks and labels ---
                g2.setColor(Color.BLACK);
                int nTicks = 5;
                for (int i = 0; i <= nTicks; i++) {
                    int y = y0 - (int) (i * (h - marginTop - marginBottom) / nTicks);
                    int value = (int) Math.round(i * (double) maxBinCount / nTicks);
                    g2.drawLine(x0 - 5, y, x0, y);
                    g2.drawString(String.valueOf(value), x0 - 45, y + 5);
                }

                // --- Bars ---
                for (int i = 0; i < binCount; i++) {
                    int total = allBins[i];
                    int background = bgBins[i];
                    int x = (int) (x0 + i * xStep);
                    int barW = (int) (xStep * 0.8);

                    // Background portion (light blue)
                    if (background > 0) {
                        int bgHeight = (int) (background * yScale);
                        int bgY = y0 - bgHeight;
                        g2.setColor(new Color(100, 160, 255, 180));
                        g2.fillRect(x, bgY, barW, bgHeight);
                    }

                    // Foreground (non-background) portion (gray)
                    int fgHeight = (int) ((total - background) * yScale);
                    if (fgHeight > 0) {
                        int fgY = y0 - fgHeight - (int) (background * yScale);
                        g2.setColor(new Color(180, 180, 180));
                        g2.fillRect(x, fgY, barW, fgHeight);
                    }

                    // Outline
                    g2.setColor(Color.BLACK);
                    g2.drawRect(x, y0 - (int) (total * yScale), barW, (int) (total * yScale));
                }

                // --- Axis labels ---
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 12f));
                g2.drawString("Track count bins (bin size ≈ " + binSize + ")", w / 2 - 90, h - 15);
                g2.drawString("Number of squares", 10, marginTop - 10);

                // --- Summary annotation block ---
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 14f));
                g2.setColor(new Color(0, 70, 180));

                int textX = w - marginRight - 370;
                int textY = marginTop + 20;
                g2.drawString(String.format("Number of squares in recording: %d", totalSquares), textX, textY);
                g2.drawString(String.format("Number of tracks in recording: %d", totalTracks), textX, textY + 20);
                g2.drawString(String.format("Number of background squares: %d", nBackground), textX, textY + 40);
                g2.drawString(String.format("Number of tracks in background: %d", backgroundTracksTotal), textX, textY + 60);
                g2.drawString(String.format("Average number of tracks in the background: %.3f", averageTracksInBackground), textX, textY + 80);
            }
        };

        frame.add(panel);
        frame.setVisible(true);
    }
    IGNORE END    */
}