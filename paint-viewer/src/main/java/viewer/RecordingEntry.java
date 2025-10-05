package viewer;

import paint.shared.objects.Square;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class RecordingEntry {

    private final String recordingName;
    private final Path trackmateImagePath;
    private final Path brightfieldImagePath;
    private final String experimentName;
    private final String probeName;
    private final String probeType;
    private final String adjuvant;
    private final String cellType;
    private final double concentration;
    private final int numberOfSpots;
    private final int numberOfTracks;
    private final double threshold;
    private final double tau;
    private final double density;
    private final double minRequiredDensityRatio;
    private final double maxAllowableVariability;
    private final double minRequiredRSquared;
    private final double observedRSquared;

    private ImageIcon leftImage;
    private ImageIcon rightImage;
    private List<Square> squares;

    public RecordingEntry(
            String recordingName,
            Path trackmateImagePath,
            Path brightfieldImagePath,
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
            double observedRSquared
    ) {
        this.recordingName = recordingName;
        this.trackmateImagePath = trackmateImagePath;
        this.brightfieldImagePath = brightfieldImagePath;
        this.experimentName = experimentName;
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

        // --- Robust image loading ---
        this.leftImage = loadImage(trackmateImagePath, "TrackMate");
        this.rightImage = loadImage(brightfieldImagePath, "Brightfield");
    }

    // === Robust image loading helper ===
    private static ImageIcon loadImage(Path imagePath, String label) {
        if (imagePath == null) return null;

        try {
            BufferedImage img = javax.imageio.ImageIO.read(imagePath.toFile());
            if (img != null) {
                PaintLogger.infof("[%s] Loaded via ImageIO: %s", label, imagePath);
                return new ImageIcon(img);
            }
            PaintLogger.warningf("[%s] ImageIO returned null for %s", label, imagePath);
        } catch (Exception e) {
            PaintLogger.warningf("[%s] ImageIO failed for %s (%s)", label, imagePath, e.getMessage());
        }

        // --- Fallback: try ImageJ Opener ---
        try {
            ij.io.Opener opener = new ij.io.Opener();
            ij.ImagePlus imp = opener.openImage(imagePath.toString());
            if (imp != null && imp.getImage() != null) {
                PaintLogger.infof("[%s] Loaded via ImageJ Opener: %s", label, imagePath);
                return new ImageIcon(imp.getImage());
            }
            PaintLogger.warningf("[%s] ImageJ Opener returned null for %s", label, imagePath);
        } catch (Throwable t) {
            PaintLogger.warningf("[%s] ImageJ Opener threw error for %s (%s)", label, imagePath, t.getMessage());
        }

        PaintLogger.errorf("[%s] Failed to load image: %s", label, imagePath);
        return null;
    }

    // === Getters ===
    public String getRecordingName() { return recordingName; }
    public String getExperimentName() { return experimentName; }
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
    public ImageIcon getLeftImage() { return leftImage; }
    public ImageIcon getRightImage() { return rightImage; }

    // === Square loading and caching ===
    // === Square loading and caching (experiment-level) ===
    public List<Square> getSquares(Project project, int expectedNumberOfSquares) {
        if (squares == null) {
            try {
                PaintLogger.infof("Fetching squares (cached per experiment) for recording: %s", recordingName);
                squares = ExperimentSquareCache.getSquaresForRecording(
                        project.getProjectRootPath(),
                        experimentName,
                        recordingName,
                        expectedNumberOfSquares
                );
            } catch (Exception e) {
                e.printStackTrace();
                squares = Collections.emptyList();
            }
        } else {
            PaintLogger.infof("Returning cached squares for recording: %s", recordingName);
        }
        return squares;
    }

    public void clearCachedSquares() {
        this.squares = null;
    }
}