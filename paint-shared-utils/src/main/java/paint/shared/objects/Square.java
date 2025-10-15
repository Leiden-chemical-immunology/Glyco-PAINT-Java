package paint.shared.objects;

import tech.tablesaw.api.Table;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.IMAGE_HEIGHT;
import static paint.shared.constants.PaintConstants.IMAGE_WIDTH;
import static paint.shared.utils.Miscellaneous.round;

/**
 * Represents a square region within a recording image.
 * <p>
 * Each {@code Square} corresponds to a defined spatial subregion of an image
 * used for localized tracking or analysis of particle motion. A square contains
 * numerical descriptors such as density, variability, displacement metrics,
 * and diffusion coefficients derived from the tracks assigned to it.
 * </p>
 * <p>
 * Squares are identified by their {@code squareNumber}, position
 * ({@code rowNumber}, {@code colNumber}), and pixel boundaries
 * ({@code x0, y0, x1, y1}). They may also maintain an associated list of
 * {@link Track} objects representing motion events within the region.
 * </p>
 *
 * <p><b>Note:</b> This class is not thread-safe.</p>
 */
public class Square {

    // Attributes

    // @formatter:off
    private String  uniqueKey;                       // 0
    private String  recordingName;                   // 1
    private int     squareNumber;                    // 2
    private int     rowNumber;                       // 3
    private int     colNumber;                       // 4
    private int     labelNumber;                     // 5
    private int     cellId;                          // 6
    private boolean selected;                        // 7
    private boolean squareManuallyExcluded;          // 8
    private boolean imageExcluded;                   // 9
    private double  x0;                              // 10
    private double  y0;                              // 11
    private double  x1;                              // 12
    private double  y1;                              // 13
    private int     numberOfTracks;                  // 14
    private double  variability;                     // 15
    private double  density;                         // 16
    private double  densityRatio;                    // 17
    private double  tau;                             // 18
    private double  rSquared;                        // 19
    private double  medianDiffusionCoefficient;      // 20
    private double  medianDiffusionCoefficientExt;   // 21
    private double  medianLongTrackDuration;         // 22
    private double  medianShortTrackDuration;        // 23
    private double  medianDisplacement;              // 24
    private double  maxDisplacement;                 // 25
    private double  totalDisplacement;               // 26
    private double  medianMaxSpeed;                  // 27
    private double  maxMaxSpeed;                     // 28
    private double  medianMeanSpeed;                 // 29
    private double  maxMeanSpeed;                    // 30
    private double  maxTrackDuration;                // 31
    private double  totalTrackDuration;              // 32
    private double  medianTrackDuration;             // 33

    private List<Track> tracks = new ArrayList<>();
    private Table tracksTable = null;

    // --- Constructors ---

    /**
     * Creates an empty {@code Square}.
     * */
    public Square() {
    }

    /**
     * Creates a {@code Square} with explicit coordinate and recording information.
     *
     * @param uniqueKey     a unique identifier for this square
     * @param recordingName the recording this square belongs to
     * @param squareNumber  sequential number of this square
     * @param rowNumber     row index within the grid
     * @param colNumber     column index within the grid
     * @param x0            left coordinate in pixels
     * @param y0            top coordinate in pixels
     * @param x1            right coordinate in pixels
     * @param y1            bottom coordinate in pixels
     */
    public Square(String uniqueKey,
                  String recordingName,
                  int squareNumber,
                  int rowNumber,
                  int colNumber,
                  double x0,
                  double y0,
                  double x1,
                  double y1) {

        this.uniqueKey                     = uniqueKey;
        this.recordingName                 = recordingName;
        this.squareNumber                  = squareNumber;
        this.rowNumber                     = rowNumber;
        this.colNumber                     = colNumber;
        this.x0                            = round(x0, 2);
        this.y0                            = round(y0, 2);
        this.x1                            = round(x1, 2);
        this.y1                            = round(y1, 2);
        this.variability                   = Double.NaN;
        this.density                       = Double.NaN;
        this.densityRatio                  = Double.NaN;
        this.tau                           = Double.NaN;
        this.rSquared                      = Double.NaN;
        this.medianDiffusionCoefficient    = Double.NaN;
        this.medianDiffusionCoefficientExt = Double.NaN;
        this.medianLongTrackDuration       = Double.NaN;
        this.medianShortTrackDuration      = Double.NaN;
        this.medianDisplacement            = Double.NaN;
        this.maxDisplacement               = Double.NaN;
        this.totalDisplacement             = Double.NaN;
        this.medianMaxSpeed                = Double.NaN;
        this.maxMaxSpeed                   = Double.NaN;
        this.medianMeanSpeed               = Double.NaN;
        this.maxMeanSpeed                  = Double.NaN;
        this.totalTrackDuration            = Double.NaN;
        this.medianTrackDuration           = Double.NaN;
    }

    /**
     * Creates a {@code Square} based on its sequential number and total number of squares
     * in a recording, automatically calculating its coordinates.
     *
     * @param squareNumber               the sequential number of this square
     * @param numberOfSquaresInRecording the total number of squares in the recording
     */
    public Square(int squareNumber, int numberOfSquaresInRecording) {
        int numberSquaresInRow = (int) Math.sqrt(numberOfSquaresInRecording);
        double width           = IMAGE_WIDTH / numberSquaresInRow;
        double height          = IMAGE_HEIGHT / numberSquaresInRow;

        colNumber = squareNumber % numberSquaresInRow;
        rowNumber = squareNumber / numberSquaresInRow;

        x0 = round(colNumber * width, 2);
        x1 = round((colNumber + 1) * width, 2);
        y0 = round(rowNumber * height, 2);
        y1 = round((rowNumber + 1) * width, 2);

        this.squareNumber = squareNumber;
    }

    // --- Getters and Setters ---

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
    }

    public int getLabelNumber() {
        return labelNumber;
    }

    public void setLabelNumber(int labelNumber) {
        this.labelNumber = labelNumber;
    }

    public int getSquareNumber() {
        return squareNumber;
    }

    public void setSquareNumber(int squareNumber) {
        this.squareNumber = squareNumber;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public int getColNumber() {
        return colNumber;
    }

    public void setColNumber(int colNumber) {
        this.colNumber = colNumber;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSquareManuallyExcluded() {
        return squareManuallyExcluded;
    }

    public void setSquareManuallyExcluded(boolean squareManuallyExcluded) {
        this.squareManuallyExcluded = squareManuallyExcluded;
    }

    public boolean isImageExcluded() {
        return imageExcluded;
    }

    public void setImageExcluded(boolean imageExcluded) {
        this.imageExcluded = imageExcluded;
    }

    public double getX0() {
        return x0;
    }

    public void setX0(double x0) {
        this.x0 = round(x0, 2);
    }

    public double getY0() {
        return y0;
    }

    public void setY0(double y0) {
        this.y0 = round (y0, 2);
    }

    public double getX1() {
        return x1;
    }

    public void setX1(double x1) {
        this.x1 = round(x1, 2);
    }

    public double getY1() {
        return y1;
    }

    public void setY1(double y1) {
        this.y1 = round(y1, 2);
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public void setNumberOfTracks(int numberTracks) {
        this.numberOfTracks = numberTracks;
    }

    public double getVariability() {
        return variability;
    }

    public void setVariability(double variability) {
        this.variability = variability;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public double getDensityRatio() {
        return densityRatio;
    }

    public void setDensityRatio(double densityRatio) {
        this.densityRatio = densityRatio;
    }

    public double getTau() {
        return tau;
    }

    public void setTau(double tau) {
        this.tau = tau;
    }

    public double getRSquared() {
        return rSquared;
    }

    public void setRSquared(double rSquared) {
        this.rSquared = rSquared;
    }

    public double getMedianDiffusionCoefficient() {
        return medianDiffusionCoefficient;
    }

    public void setMedianDiffusionCoefficient(double medianDiffusionCoefficient) {
        this.medianDiffusionCoefficient = medianDiffusionCoefficient;
    }

    public double getMedianDiffusionCoefficientExt() {
        return medianDiffusionCoefficientExt;
    }

    public void setMedianDiffusionCoefficientExt(double medianDiffusionCoefficientExt) {
        this.medianDiffusionCoefficientExt = medianDiffusionCoefficientExt;
    }

    public double getMedianLongTrackDuration() {
        return medianLongTrackDuration;
    }

    public void setMedianLongTrackDuration(double medianLongTrackDuration) {
        this.medianLongTrackDuration = medianLongTrackDuration;
    }

    public double getMedianShortTrackDuration() {
        return medianShortTrackDuration;
    }

    public void setMedianShortTrackDuration(double medianShortTrackDuration) {
        this.medianShortTrackDuration = medianShortTrackDuration;
    }

    public double getTotalTrackDuration() {
        return totalTrackDuration;
    }

    public void setTotalTrackDuration(double totalTrackDuration) {
        this.totalTrackDuration = totalTrackDuration;
    }

    public double getMedianTrackDuration() {
        return medianTrackDuration;
    }

    public void setMedianTrackDuration(double medianTrackDuration) {
        this.medianTrackDuration = medianTrackDuration;
    }

    public double getMaxTrackDuration() {
        return maxTrackDuration;
    }

    public void setMaxTrackDuration(double maxTrackDuration) {
        this.maxTrackDuration = maxTrackDuration;
    }

    public double getMedianMeanSpeed() {
        return medianMeanSpeed;
    }

    public void setMedianMeanSpeed(double medianMeanSpeed) {
        this.medianMeanSpeed = medianMeanSpeed;
    }

    public double getMaxMeanSpeed() {
        return maxMeanSpeed;
    }

    public void setMaxMeanSpeed(double maxMeanSpeed) {
        this.maxMeanSpeed = maxMeanSpeed;
    }

    public double getMedianMaxSpeed() {
        return medianMaxSpeed;
    }

    public void setMedianMaxSpeed(double medianMaxSpeed) {
        this.medianMaxSpeed = medianMaxSpeed;
    }

    public double getMaxMaxSpeed() {
        return maxMaxSpeed;
    }

    public void setMaxMaxSpeed(double maxMaxSpeed) {
        this.maxMaxSpeed = maxMaxSpeed;
    }

    public double getMedianDisplacement() {
        return medianDisplacement;
    }

    public void setMedianDisplacement(double medianDisplacement) {
        this.medianDisplacement = medianDisplacement;
    }

    public double getMaxDisplacement() {
        return maxDisplacement;
    }

    public void setMaxDisplacement(double maxDisplacement) {
        this.maxDisplacement = maxDisplacement;
    }

    public double getTotalDisplacement() {
        return totalDisplacement;
    }

    public void setTotalDisplacement(double totalDisplacement) {
        this.totalDisplacement = totalDisplacement;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public Table getTracksTable() {
        return tracksTable;
    }

    public void setTracksTable(Table tracksTable) {
        this.tracksTable = tracksTable;
    }

    /** Adds a single {@link Track} to this square. */
    public void addTrack(Track track) {
        this.tracks.add(track);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n----------------------------------------------------------------------\n")
                .append("Square ").append(squareNumber)
                .append(" (Recording: ").append(recordingName != null ? recordingName : "N/A").append(")\n")
                .append("----------------------------------------------------------------------\n");

        sb.append(String.format("Row,Col Number                 : %d,%d%n", rowNumber, colNumber));
        sb.append(String.format("Coordinates [x0,y0]-[x1,y1]    : [%.2f, %.2f] - [%.2f, %.2f]%n", x0, y0, x1, y1));
        sb.append(String.format("Unique Key                     : %s%n", uniqueKey != null ? uniqueKey : "N/A"));
        sb.append(String.format("Label Number                   : %d%n", labelNumber));
        sb.append(String.format("Cell ID                        : %d%n", cellId));

        sb.append(String.format("Selected                       : %b%n", selected));
        sb.append(String.format("Square Manually Excluded       : %b%n", squareManuallyExcluded));
        sb.append(String.format("Image Excluded                 : %b%n", imageExcluded));

        sb.append(String.format("Number of Tracks               : %d%n", numberOfTracks));
        sb.append(String.format("Variability                    : %.4f%n", variability));
        sb.append(String.format("Density                        : %.4f%n", density));
        sb.append(String.format("Density Ratio                  : %.4f%n", densityRatio));
        sb.append(String.format("Tau                            : %.4f%n", tau));
        sb.append(String.format("R²                             : %.4f%n", rSquared));

        sb.append(String.format("Median Diffusion Coefficient   : %.4f%n", medianDiffusionCoefficient));
        sb.append(String.format("Median Diffusion CoefficientExt: %.4f%n", medianDiffusionCoefficientExt));
        sb.append(String.format("Median Long Track Duration     : %.4f%n", medianLongTrackDuration));
        sb.append(String.format("Median Short Track Duration    : %.4f%n", medianShortTrackDuration));
        sb.append(String.format("Median Displacement            : %.4f%n", medianDisplacement));
        sb.append(String.format("Max Displacement               : %.4f%n", maxDisplacement));
        sb.append(String.format("Total Displacement             : %.4f%n", totalDisplacement));

        sb.append(String.format("Median Max Speed               : %.4f%n", medianMaxSpeed));
        sb.append(String.format("Max Max Speed                  : %.4f%n", maxMaxSpeed));
        sb.append(String.format("Median Mean Speed              : %.4f%n", medianMeanSpeed));
        sb.append(String.format("Max Mean Speed                 : %.4f%n", maxMeanSpeed));

        sb.append(String.format("Max Track Duration             : %.4f%n", maxTrackDuration));
        sb.append(String.format("Total Track Duration           : %.4f%n", totalTrackDuration));
        sb.append(String.format("Median Track Duration          : %.4f%n", medianTrackDuration));

        if (tracks != null) {
            sb.append(String.format("Tracks attached                : %d%n", tracks.size()));
        }
        if (tracksTable != null) {
            sb.append("Tracks table available%n");
        }

        return sb.toString();
    }

    /**
     * Calculates the theoretical square area based on a fixed image size.
     *
     * @param nrSquaresInRecording number of squares in the recording
     * @return the area of a single square
     */
    public static double calcSquareArea(int nrSquaresInRecording) {
        return IMAGE_WIDTH * IMAGE_HEIGHT / nrSquaresInRecording;
    }

    /** Example driver for debugging square area calculations. */
    public static void main(String[] args) {
        List<Square> squares = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            Square square = new Square(i, 100);
            squares.add(square);
        }
        System.out.println(squares);

        double areaOriginal = calcSquareAreaOriginal(20);
        double areaNew = calcSquareArea(400);
        double difference = areaNew - areaOriginal;
        double percentualDifference = (areaNew - areaOriginal) / areaOriginal * 100;
        System.out.println("Area original: " + areaOriginal);
        System.out.println("Area new: " + areaNew);
        System.out.printf("Difference: %.6f%n", difference);
        System.out.printf("Percentual difference: %.4f%%%n", percentualDifference);
    }

    private static double calcSquareAreaOriginal(int nrSquaresInRow) {
        double micrometerPerPixel = 0.1602804;
        int pixelsPerImage = 512;
        double micrometerPerImageAxis = micrometerPerPixel * pixelsPerImage;
        double micrometerPerSquareAxis = micrometerPerImageAxis / nrSquaresInRow;
        return micrometerPerSquareAxis * micrometerPerSquareAxis;
    }
}