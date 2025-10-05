package viewer;

import paint.shared.objects.Project;
import paint.shared.objects.Square;
import paint.shared.io.SquareTableIO;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static paint.shared.constants.PaintConstants.SQUARES_CSV;

public class RecordingEntry {

    private final Path      leftImagePath;
    private final Path      rightImagePath;
    private final ImageIcon leftImage;
    private final ImageIcon rightImage;

    // Metadata fields
    private final String experimentName;
    private final String recordingName;
    private final String probeName;
    private final String probeType;
    private final String adjuvant;
    private final String cellType;
    private final double concentration;
    private final int    numberOfSpots;
    private final int    numberOfTracks;
    private final double threshold;
    private final double tau;
    private final double density;

    // Thresholds (from config)
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final double minRequiredRSquared;

    // Observed value from CSV
    private final double observedRSquared;

    private List<Square> squares; // ✅ unified type

    public RecordingEntry(Path leftImagePath,
                          Path rightImagePath,
                          String experimentName,
                          String probeName,
                          String probeType,
                          String adjuvant,
                          String cellType,
                          double concentration,
                          int numberOfSpots,
                          int numberOfTracks,
                          double threshold,
                          double tau,
                          double density,
                          double minRequiredDensityRatio,
                          double maxAllowableVariability,
                          double minRequiredRSquared,
                          double observedRSquared) {

        this.leftImagePath = leftImagePath;
        this.rightImagePath = rightImagePath;
        this.leftImage = loadImageIcon(leftImagePath);
        this.rightImage = loadImageIcon(rightImagePath);

        this.experimentName = experimentName;
        this.recordingName = deriveRecordingName(leftImagePath);

        this.probeName = probeName;
        this.probeType = probeType;
        this.adjuvant = adjuvant;
        this.cellType = cellType;
        this.concentration = concentration;
        this.numberOfSpots = numberOfSpots;
        this.numberOfTracks = numberOfTracks;
        this.threshold = threshold;
        this.tau = tau;
        this.density = density;
        this.minRequiredDensityRatio = minRequiredDensityRatio;
        this.maxAllowableVariability = maxAllowableVariability;
        this.minRequiredRSquared = minRequiredRSquared;
        this.observedRSquared = observedRSquared;
    }

    private static ImageIcon loadImageIcon(Path path) {
        if (path == null) return new ImageIcon();
        try {
            BufferedImage img = ImageIO.read(path.toFile());
            return new ImageIcon(img);
        } catch (Exception e) {
            e.printStackTrace();
            return new ImageIcon();
        }
    }

    // --- Helpers for recording name ---
    private static String deriveRecordingName(Path path) {
        if (path == null) return "";
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    // === Unified viewer entry loader ===
    public List<Square> getSquaresForViewer(Project project, int expectedNumberOfSquares) {
        if (squares == null) {
            try {
                squares = loadSquaresForRecording(
                        project.getProjectRootPath(),
                        getExperimentName(),
                        getRecordingName(),
                        expectedNumberOfSquares);
            } catch (Exception e) {
                e.printStackTrace();
                squares = Collections.emptyList();
            }
        }
        return squares;
    }

    // === CSV reader for unified Square ===
    private static List<Square> loadSquaresForRecording(
            Path projectPath,
            String experimentName,
            String recordingName,
            int expectedNumberOfSquares) throws Exception {

        int numberOfSquaresRead = 0;
        Path squaresCsv = projectPath.resolve(experimentName).resolve(SQUARES_CSV);

        SquareTableIO io = new SquareTableIO();
        Table table = io.readCsv(squaresCsv);

        List<Square> out = new ArrayList<>();
        for (Row row : table) {
            if (!recordingName.equals(row.getString("Recording Name"))) {
                continue;
            }
            numberOfSquaresRead++;

            Square s = new Square();
            s.setRecordingName(recordingName);
            s.setSquareNumber(row.getInt("Square Number"));
            s.setRowNumber(row.getInt("Row Number"));
            s.setColNumber(row.getInt("Column Number"));
            s.setLabelNumber(row.getInt("Label Number"));
            s.setCellId(row.getInt("Cell ID"));
            s.setSelected(row.getBoolean("Selected"));

            out.add(s);

            if (expectedNumberOfSquares != 0 && numberOfSquaresRead >= expectedNumberOfSquares) {
                break;
            }
        }

        if (expectedNumberOfSquares != 0 && numberOfSquaresRead != expectedNumberOfSquares) {
            throw new IllegalStateException(
                    "Expected " + expectedNumberOfSquares +
                            " squares, but found " + numberOfSquaresRead +
                            " in recording: " + recordingName
            );
        }

        return out;
    }

    // --- Random selection (for testing)
    public static void randomlySelectSquares(List<Square> squares) {
        Random rnd = new Random();
        for (Square sq : squares) {
            if (rnd.nextDouble() < 0.10) {
                sq.setSelected(true);
            }
        }
    }

    // === Getters ===
    public ImageIcon getLeftImage() { return leftImage; }
    public ImageIcon getRightImage() { return rightImage; }
    public String getExperimentName() { return experimentName; }
    public String getRecordingName() { return recordingName; }
    public String getProbeName() { return probeName; }
    public String getProbeType() { return probeType; }
    public String getAdjuvant() { return adjuvant; }
    public String getCellType() { return cellType; }
    public double getConcentration() { return concentration; }
    public int getNumberOfSpots() { return numberOfSpots; }
    public int getNumberOfTracks() { return numberOfTracks; }
    public double getThreshold() { return threshold; }
    public double getTau() { return tau; }
    public double getDensity() { return density; }
    public double getMinRequiredDensityRatio() { return minRequiredDensityRatio; }
    public double getMaxAllowableVariability() { return maxAllowableVariability; }
    public double getMinRequiredRSquared() { return minRequiredRSquared; }
    public double getObservedRSquared() { return observedRSquared; }
}