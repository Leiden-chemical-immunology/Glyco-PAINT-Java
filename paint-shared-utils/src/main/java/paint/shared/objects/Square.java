/*=============================================================================
 *  Class:        Square.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents a spatial analysis region (square) within a PAINT recording.
 *    A square models:
 *
 *       • Its grid location (row/column)
 *       • Pixel coordinates (x0,y0,x1,y1)
 *       • Associated track statistics
 *       • Visibility and exclusion flags
 *       • A list of Track objects and (optional) a Tablesaw track table
 *
 *  DESCRIPTION:
 *    Each square corresponds to a rectangular region extracted from a
 *    recording frame. It forms the basis for spatial analysis across:
 *
 *       • Single-molecule densities
 *       • Variability and diffusion metrics
 *       • Track-based temporal properties
 *
 *    This version embeds the schema through the {@link Column} enum,
 *    replacing the old SquareSchema class entirely. All table I/O classes now
 *    rely on Square.Column.values() for headers and column types.
 *
 *  KEY FEATURES:
 *    • Full embedded schema enum with CSV headers + Tablesaw types
 *    • Rich metadata including geometry, track metrics, and flags
 *    • Utility constructors for fully-specified and grid-generated squares
 *    • Integration with PAINT’s track pipeline via a Track list and table
 *    • Clean, formatted debugging/diagnostic output via toString()
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-11-30
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.objects;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintGeometry.IMAGE_HEIGHT;
import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;
import static paint.shared.utils.Miscellaneous.initialiseDoublesToNaN;
import static paint.shared.utils.Miscellaneous.round;

/**
 * Represents a rectangular analysis region (“square”) in a PAINT recording.
 * <p>
 * Each square stores its geometric origin, statistics derived from associated
 * tracks, and flags describing visibility and exclusion status.
 */
public class Square {

    private String  uniqueKey;

    /*=========================================================================
     *  CORE ATTRIBUTES
     *=========================================================================
     */
    private String  experimentName;
    private String  recordingName;
    private int     squareNumber;
    private int     rowNumber;
    private int     colNumber;
    private int     labelNumber;
    private int     cellId;
    private boolean visible;
    private boolean squareManuallyExcluded;
    private boolean imageExcluded;

    /* Spatial coordinates: top-left (x0,y0), bottom-right (x1,y1) */
    private double  x0;
    private double  y0;
    private double  x1;
    private double  y1;

    /* Track-derived metrics */
    private int     numberOfTracks;
    private double  variability;
    private double  density;
    private double  densityRatio;
    private double  densityRatioOri;
    private double  tau;
    private double  rSquared;
    private double  medianDiffusionCoefficient;
    private double  medianDiffusionCoefficientExt;
    private double  medianDisplacement;
    private double  maxDisplacement;
    private double  totalDisplacement;
    private double  medianMaxSpeed;
    private double  maxMaxSpeed;
    private double  medianMedianSpeed;
    private double  maxMedianSpeed;
    private double  maxTrackDuration;
    private double  totalTrackDuration;
    private double  medianTrackDuration;

    /* Associated objects */
    private List<Track> tracks      = new ArrayList<>();
    private Table       tracksTable = null;

    /**
     * Creates an empty {@code Square}.
     */
    public Square() {
    }

    //=========================================================================
    // CONSTRUCTORS
    // =========================================================================

    /**
     * Creates a fully-specified {@code Square} with geometric coordinates.
     */
    public Square(String uniqueKey,
            String experimentName,
            String recordingName,
            int squareNumber,
            int rowNumber,
            int colNumber,
            double x0, double y0,
            double x1, double y1) {

        initialiseDoublesToNaN(this);
        this.uniqueKey      = uniqueKey;
        this.experimentName = experimentName;
        this.recordingName  = recordingName;
        this.squareNumber   = squareNumber;
        this.rowNumber      = rowNumber;
        this.colNumber      = colNumber;
        this.x0             = round(x0, 2);
        this.y0             = round(y0, 2);
        this.x1             = round(x1, 2);
        this.y1             = round(y1, 2);
    }

    /**
     * Computes the theoretical square area for a given grid size.
     */
    public static double calculateSquareArea(int n) {
        return IMAGE_WIDTH * IMAGE_HEIGHT / n;
    }

    //=========================================================================
    // ACCESSORS & MUTATORS
    //=========================================================================


    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String key) {
        this.uniqueKey = key;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String name) {
        this.experimentName = name;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String name) {
        this.recordingName = name;
    }

    public int getSquareNumber() {
        return squareNumber;
    }

    public void setSquareNumber(int n) {
        this.squareNumber = n;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int r) {
        this.rowNumber = r;
    }

    public int getColNumber() {
        return colNumber;
    }

    public void setColNumber(int c) {
        this.colNumber = c;
    }

    public int getLabelNumber() {
        return labelNumber;
    }

    public void setLabelNumber(int n) {
        this.labelNumber = n;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int id) {
        this.cellId = id;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean v) {
        this.visible = v;
    }

    public boolean isSquareManuallyExcluded() {
        return squareManuallyExcluded;
    }

    public void setSquareManuallyExcluded(boolean b) {
        this.squareManuallyExcluded = b;
    }

    public boolean isImageExcluded() {
        return imageExcluded;
    }

    public void setImageExcluded(boolean b) {
        this.imageExcluded = b;
    }

    public double getX0() {
        return x0;
    }

    public void setX0(double v) {
        this.x0 = round(v, 2);
    }

    public double getY0() {
        return y0;
    }

    public void setY0(double v) {
        this.y0 = round(v, 2);
    }

    public double getX1() {
        return x1;
    }

    public void setX1(double v) {
        this.x1 = round(v, 2);
    }

    public double getY1() {
        return y1;
    }

    public void setY1(double v) {
        this.y1 = round(v, 2);
    }

    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    public void setNumberOfTracks(int n) {
        this.numberOfTracks = n;
    }

    public double getVariability() {
        return variability;
    }

    public void setVariability(double v) {
        this.variability = v;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double d) {
        this.density = d;
    }

    public double getDensityRatio() {
        return densityRatio;
    }

    public void setDensityRatio(double v) {
        this.densityRatio = v;
    }

    public double getDensityRatioOri() {
        return densityRatioOri;
    }

    public void setDensityRatioOri(double v) {
        this.densityRatioOri = v;
    }

    public double getTau() {
        return tau;
    }

    public void setTau(double t) {
        this.tau = t;
    }

    public double getRSquared() {
        return rSquared;
    }

    public void setRSquared(double r) {
        this.rSquared = r;
    }

    public double getMedianDiffusionCoefficient() {
        return medianDiffusionCoefficient;
    }

    public void setMedianDiffusionCoefficient(double v) {
        this.medianDiffusionCoefficient = v;
    }

    public double getMedianDiffusionCoefficientExt() {
        return medianDiffusionCoefficientExt;
    }

    public void setMedianDiffusionCoefficientExt(double v) {
        this.medianDiffusionCoefficientExt = v;
    }

    public double getMedianDisplacement() {
        return medianDisplacement;
    }

    public void setMedianDisplacement(double v) {
        this.medianDisplacement = v;
    }

    public double getMaxDisplacement() {
        return maxDisplacement;
    }

    public void setMaxDisplacement(double v) {
        this.maxDisplacement = v;
    }

    public double getTotalDisplacement() {
        return totalDisplacement;
    }

    public void setTotalDisplacement(double v) {
        this.totalDisplacement = v;
    }

    public double getMedianMaxSpeed() {
        return medianMaxSpeed;
    }

    public void setMedianMaxSpeed(double v) {
        this.medianMaxSpeed = v;
    }

    public double getMaxMaxSpeed() {
        return maxMaxSpeed;
    }

    public void setMaxMaxSpeed(double v) {
        this.maxMaxSpeed = v;
    }

    public double getMedianMedianSpeed() {
        return medianMedianSpeed;
    }

    public void setMedianMedianSpeed(double v) {
        this.medianMedianSpeed = v;
    }

    public double getMaxMedianSpeed() {
        return maxMedianSpeed;
    }

    public void setMaxMedianSpeed(double v) {
        this.maxMedianSpeed = v;
    }

    public double getMaxTrackDuration() {
        return maxTrackDuration;
    }

    public void setMaxTrackDuration(double v) {
        this.maxTrackDuration = v;
    }

    public double getTotalTrackDuration() {
        return totalTrackDuration;
    }

    public void setTotalTrackDuration(double v) {
        this.totalTrackDuration = v;
    }

    public double getMedianTrackDuration() {
        return medianTrackDuration;
    }

    public void setMedianTrackDuration(double v) {
        this.medianTrackDuration = v;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracksList(List<Track> t) {
        this.tracks = t;
    }

    public Table getTracksTable() {
        return tracksTable;
    }

    public void setTracksTable(Table t) {
        this.tracksTable = t;
    }

    //=========================================================================
    // UTILITIES
    //=========================================================================

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n----------------------------------------------------------------------\n")
          .append("Square ").append(squareNumber)
          .append(" (Experiment: ").append(experimentName != null ? experimentName : "N/A").append(")\n")
          .append(" (Recording: ").append(recordingName != null ? recordingName : "N/A").append(")\n")
          .append("----------------------------------------------------------------------\n");

        sb.append(String.format("Row,Col Number                 : %d,%d%n", rowNumber, colNumber));
        sb.append(String.format("Coordinates [x0,y0]-[x1,y1]    : [%.2f, %.2f] - [%.2f, %.2f]%n", x0, y0, x1, y1));
        sb.append(String.format("Unique Key                     : %s%n", uniqueKey != null ? uniqueKey : "N/A"));
        sb.append(String.format("Label Number                   : %d%n", labelNumber));
        sb.append(String.format("Cell ID                        : %d%n", cellId));

        sb.append(String.format("Visible                        : %b%n", visible));
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
        sb.append(String.format("Median Displacement            : %.4f%n", medianDisplacement));
        sb.append(String.format("Max Displacement               : %.4f%n", maxDisplacement));
        sb.append(String.format("Total Displacement             : %.4f%n", totalDisplacement));

        sb.append(String.format("Median Max Speed               : %.4f%n", medianMaxSpeed));
        sb.append(String.format("Max Max Speed                  : %.4f%n", maxMaxSpeed));
        sb.append(String.format("Median Median Speed            : %.4f%n", medianMedianSpeed));
        sb.append(String.format("Max Median Speed               : %.4f%n", maxMedianSpeed));

        sb.append(String.format("Max Track Duration             : %.4f%n", maxTrackDuration));
        sb.append(String.format("Total Track Duration           : %.4f%n", totalTrackDuration));
        sb.append(String.format("Median Track Duration          : %.4f%n", medianTrackDuration));

        if (tracks != null) {
            sb.append(String.format("Tracks attached                : %d%n", tracks.size()));
        }
        if (tracksTable != null) {
            sb.append("Tracks table available\n");
        }

        return sb.toString();
    }

    //=========================================================================
    // EMBEDDED SCHEMA ENUM
    //=========================================================================

    public enum Column {

        UNIQUE_KEY(                       "Unique Key",                       ColumnType.STRING),
        EXPERIMENT_NAME(                  "Experiment Name",                  ColumnType.STRING),
        RECORDING_NAME(                   "Recording Name",                   ColumnType.STRING),
        SQUARE_NUMBER(                    "Square Number",                    ColumnType.INTEGER),
        ROW_NUMBER(                       "Row Number",                       ColumnType.INTEGER),
        COLUMN_NUMBER(                    "Column Number",                    ColumnType.INTEGER),
        LABEL_NUMBER(                     "Label Number",                     ColumnType.INTEGER),
        CELL_ID(                          "Cell Id",                          ColumnType.INTEGER),
        VISIBLE(                          "Visible",                          ColumnType.BOOLEAN),
        SQUARE_MANUALLY_EXCLUDED(         "Square Manually Excluded",         ColumnType.BOOLEAN),
        IMAGE_EXCLUDED(                   "Image Excluded",                   ColumnType.BOOLEAN),
        X0(                               "X0",                               ColumnType.DOUBLE),
        Y0(                               "Y0",                               ColumnType.DOUBLE),
        X1(                               "X1",                               ColumnType.DOUBLE),
        Y1(                               "Y1",                               ColumnType.DOUBLE),
        NUMBER_OF_TRACKS(                 "Number of Tracks",                 ColumnType.INTEGER),
        VARIABILITY(                      "Variability",                      ColumnType.DOUBLE),
        DENSITY(                          "Density",                          ColumnType.DOUBLE),
        DENSITY_RATIO(                    "Density Ratio",                    ColumnType.DOUBLE),
        DENSITY_RATIO_ORI(                "Density Ratio Ori",                ColumnType.DOUBLE),
        TAU(                              "Tau",                              ColumnType.DOUBLE),
        R_SQUARED(                        "R Squared",                        ColumnType.DOUBLE),
        MEDIAN_DIFFUSION_COEFFICIENT(     "Median Diffusion Coefficient",     ColumnType.DOUBLE),
        MEDIAN_DIFFUSION_COEFFICIENT_EXT( "Median Diffusion Coefficient Ext", ColumnType.DOUBLE),
        MEDIAN_DISPLACEMENT(              "Median Displacement",              ColumnType.DOUBLE),
        MAX_DISPLACEMENT(                 "Max Displacement",                 ColumnType.DOUBLE),
        TOTAL_DISPLACEMENT(               "Total Displacement",               ColumnType.DOUBLE),
        MEDIAN_MAX_SPEED(                 "Median Max Speed",                 ColumnType.DOUBLE),
        MAX_MAX_SPEED(                    "Max Max Speed",                    ColumnType.DOUBLE),
        MEDIAN_MEDIAN_SPEED(              "Median Median Speed",              ColumnType.DOUBLE),
        MAX_MEDIAN_SPEED(                 "Max Median Speed",                 ColumnType.DOUBLE),
        MAX_TRACK_DURATION(               "Max Track Duration",               ColumnType.DOUBLE),
        TOTAL_TRACK_DURATION(              "Total Track Duration",            ColumnType.DOUBLE),
        MEDIAN_TRACK_DURATION(             "Median Track Duration",           ColumnType.DOUBLE);

        public final String     header;
        public final ColumnType type;

        Column(String header, ColumnType type) {
            this.header = header;
            this.type = type;
        }
    }
}