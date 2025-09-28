package viewer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

public class RecordingEntry {
    private final Path leftImagePath;
    private final Path rightImagePath;
    private final ImageIcon leftImage;
    private final ImageIcon rightImage;

    // Metadata fields
    private final String experimentName;   // 🔹 new
    private final String recordingName;    // 🔹 new (base name without extension)
    private final String probeName;
    private final String adjuvant;
    private final String cellType;
    private final int numberOfSpots;
    private final int numberOfTracks;
    private final double threshold;
    private final double tau;
    private final double density;

    // Thresholds (from config)
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final double minRequiredRSquared;

    // Observed value from CSV
    private final double observedRSquared;

    /** Constructor for real image files */
    public RecordingEntry(Path leftImagePath,
                          Path rightImagePath,
                          String experimentName,
                          String probeName,
                          String adjuvant,
                          String cellType,
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
        this.recordingName = deriveRecordingName(leftImagePath); // drop extension
        this.probeName = probeName;
        this.adjuvant = adjuvant;
        this.cellType = cellType;
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

    /** Constructor for prebuilt ImageIcons (testing) */
    public RecordingEntry(ImageIcon leftImage,
                          ImageIcon rightImage,
                          String experimentName,
                          String probeName,
                          String adjuvant,
                          String cellType,
                          int numberOfSpots,
                          int numberOfTracks,
                          double threshold,
                          double tau,
                          double density,
                          double minRequiredDensityRatio,
                          double maxAllowableVariability,
                          double minRequiredRSquared,
                          double observedRSquared) {
        this.leftImagePath = null;
        this.rightImagePath = null;
        this.leftImage = leftImage;
        this.rightImage = rightImage;

        this.experimentName = experimentName;
        this.recordingName = "(dummy)";
        this.probeName = probeName;
        this.adjuvant = adjuvant;
        this.cellType = cellType;
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

    // --- Helper to load images safely ---
    private static ImageIcon loadImageIcon(Path path) {
        if (path == null) return new ImageIcon();
        try {
            BufferedImage img = ImageIO.read(path.toFile());
            if (img != null) {
                return new ImageIcon(img);
            } else {
                System.err.println("Unable to read image: " + path);
                return new ImageIcon();
            }
        } catch (Exception e) {
            System.err.println("Error loading image: " + path);
            e.printStackTrace();
            return new ImageIcon();
        }
    }

    // --- Helpers for recording name ---
    private static String deriveRecordingName(Path path) {
        if (path == null) return "";
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return (dot > 0) ? fileName.substring(0, dot) : fileName;
    }

    // --- Getters ---
    public ImageIcon getLeftImage() { return leftImage; }
    public ImageIcon getRightImage() { return rightImage; }

    public String getExperimentName() { return experimentName; }
    public String getRecordingName() { return recordingName; }
    public String getProbeName() { return probeName; }
    public String getAdjuvant() { return adjuvant; }
    public String getCellType() { return cellType; }
    public int getNumberOfSpots() { return numberOfSpots; }
    public int getNumberOfTracks() { return numberOfTracks; }
    public double getThreshold() { return threshold; }
    public double getTau() { return tau; }
    public double getDensity() { return density; }

    public double getMinRequiredDensityRatio() { return minRequiredDensityRatio; }
    public double getMaxAllowableVariability() { return maxAllowableVariability; }
    public double getMinRequiredRSquared() { return minRequiredRSquared; }
    public double getObservedRSquared() { return observedRSquared; }

    public Path getLeftImagePath() { return leftImagePath; }
    public Path getRightImagePath() { return rightImagePath; }
}