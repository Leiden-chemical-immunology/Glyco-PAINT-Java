package paint.viewer.utils;

import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class RecordingEntry {

    /**
     * Represents a recording associated with a specific entry in a recording-based experiment.
     * This field stores metadata and data pertaining to the recording, which can be accessed
     * or manipulated through the associated methods and properties of the {@code RecordingEntry} class.
     * <p>
     * The {@code recording} is used to retrieve or analyze information related to the experiment
     * it belongs to, such as the recording's name, probe data, and other parameters.
     */
    // @formatter:off
    private final Recording recording;
    private final Path         trackmateImagePath;
    private final Path         brightfieldImagePath;
    private final String       experimentName;
    private final double       minRequiredDensityRatio;
    private final double       maxAllowableVariability;
    private final double       minRequiredRSquared;
    private final String       neighbourMode;
    private final ImageIcon    leftImage;
    private final ImageIcon    rightImage;
    private       List<Square> squares;
    // @formatter:on

    /**
     * Constructs a new RecordingEntry with the given parameters.
     *
     * @param recording the recording object that contains data related to the recording.
     * @param trackmateImagePath the file path to the TrackMate image.
     * @param brightfieldImagePath the file path to the Brightfield image.
     * @param experimentName the name of the experiment associated with this entry.
     * @param minRequiredDensityRatio the minimum required density ratio for processing or validation.
     * @param maxAllowableVariability the maximum allowable variability level accepted.
     * @param minRequiredRSquared the minimum required R-squared value for model fitting or analysis.
     * @param neighbourMode the mode defining neighbor calculation or analysis strategy.
     */
    public RecordingEntry(
            Recording recording,
            Path      trackmateImagePath,
            Path      brightfieldImagePath,
            String    experimentName,
            double    minRequiredDensityRatio,
            double    maxAllowableVariability,
            double    minRequiredRSquared,
            String    neighbourMode
    ) {
        // @formatter:off
        this.recording               = recording;
        this.trackmateImagePath      = trackmateImagePath;
        this.brightfieldImagePath    = brightfieldImagePath;
        this.experimentName          = experimentName;
        this.minRequiredDensityRatio = minRequiredDensityRatio;
        this.maxAllowableVariability = maxAllowableVariability;
        this.minRequiredRSquared     = minRequiredRSquared;
        this.neighbourMode           = neighbourMode;
        this.leftImage               = loadImage(trackmateImagePath, "TrackMate");
        this.rightImage              = loadImage(brightfieldImagePath, "Brightfield");
        // @formatter:on
    }

    /**
     * Loads an image from the given file path. If the image cannot be loaded using standard
     * ImageIO methods, it attempts to load the image using ImageJ's Opener. In case of a
     * failure to load the image, an error is logged, and null is returned.
     *
     * @param imagePath the file path of the image to be loaded; must not be null.
     * @param label a descriptive label used for logging, providing context about the image being loaded.
     * @return an ImageIcon instance representing the loaded image, or null if the image could not be loaded.
     */
    private static ImageIcon loadImage(Path imagePath, String label) {
        if (imagePath == null) {
            return null;
        }

        try {
            BufferedImage img = javax.imageio.ImageIO.read(imagePath.toFile());
            if (img != null) {
                PaintLogger.debugf("[%s] Loaded via ImageIO: %s", label, imagePath);
                return new ImageIcon(img);
            }
            PaintLogger.warnf("[%s] ImageIO returned null for %s", label, imagePath);
        } catch (Exception e) {
            PaintLogger.warnf("[%s] ImageIO failed for %s (%s)", label, imagePath, e.getMessage());
        }

        try {
            ij.io.Opener opener = new ij.io.Opener();
            ij.ImagePlus imp = opener.openImage(imagePath.toString());
            if (imp != null && imp.getImage() != null) {
                PaintLogger.debugf("[%s] Loaded via ImageJ Opener: %s", label, imagePath);
                return new ImageIcon(imp.getImage());
            }
            PaintLogger.warnf("[%s] ImageJ Opener returned null for %s", label, imagePath);
        } catch (Throwable t) {
            PaintLogger.warnf("[%s] ImageJ Opener threw error for %s (%s)", label, imagePath, t.getMessage());
        }

        PaintLogger.errorf("[%s] Failed to load image: %s", label, imagePath);
        return null;
    }

    // === Getters ===
    public String getRecordingName() {
        return recording.getRecordingName();
    }

    public String getExperimentName() {
        return experimentName;
    }

    public String getProbeName() {
        return recording.getProbeName();
    }

    public String getProbeType() {
        return recording.getProbeType();
    }

    public String getAdjuvant() {
        return recording.getAdjuvant();
    }

    public String getCellType() {
        return recording.getCellType();
    }

    public double getConcentration() {
        return recording.getConcentration();
    }

    public int getNumberOfSpots() {
        return recording.getNumberOfSpots();
    }

    public int getNumberOfTracks() {
        return recording.getNumberOfTracks();
    }

    public double getThreshold() {
        return recording.getThreshold();
    }

    public double getTau() {
        return recording.getTau();
    }

    public double getDensity() {
        return recording.getDensity();
    }

    public double getMinRequiredDensityRatio() {
        return minRequiredDensityRatio;
    }

    public double getMaxAllowableVariability() {
        return maxAllowableVariability;
    }

    public double getMinRequiredRSquared() {
        return minRequiredRSquared;
    }

    public String getNeighbourMode() {
        return neighbourMode;
    }

    public ImageIcon getLeftImage() {
        return leftImage;
    }

    public ImageIcon getRightImage() {
        return rightImage;
    }

    public Recording getRecording() {
        return recording;
    }

    /**
     * Retrieves a list of Square objects associated with the current recording. This method
     * caches the fetched squares per experiment to optimize performance. If the squares have
     * already been cached, it returns the cached version; otherwise, it fetches them from
     * the experiment-level cache and caches them for future use.
     *
     * @param project the project object representing the context of the current operation,
     *                used for retrieving the experiment-related data.
     * @param expectedNumberOfSquares the expected number of Square objects to retrieve,
     *                                helping ensure consistency in the loaded data.
     * @return a list of Square objects associated with the current recording. If an error
     *         occurs during retrieval, an empty list is returned.
     */
    public List<Square> getSquares(Project project, int expectedNumberOfSquares) {
        if (squares == null) {
            try {
                PaintLogger.debugf("Fetching squares (cached per experiment) for recording: %s", getRecordingName());
                squares = ExperimentSquareCache.getSquaresForRecording(
                        project.getProjectRootPath(),
                        getExperimentName(),
                        getRecordingName(),
                        expectedNumberOfSquares
                );
            } catch (Exception e) {
                e.printStackTrace();
                squares = Collections.emptyList();
            }
        } else {
            PaintLogger.debugf("Returning cached squares for recording: %s", getRecordingName());
        }
        return squares;
    }
}