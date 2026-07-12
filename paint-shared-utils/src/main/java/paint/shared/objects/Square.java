/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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
     *
     * @param uniqueKey      a unique identifier for this square (typically exp§rec§num)
     * @param experimentName name of the experiment containing this square
     * @param recordingName  name of the recording containing this square
     * @param squareNumber   index of this square in the recording (0..N-1)
     * @param rowNumber      row index in the grid
     * @param colNumber      column index in the grid
     * @param x0             top-left x coordinate in µm
     * @param y0             top-left y coordinate in µm
     * @param x1             bottom-right x coordinate in µm
     * @param y1             bottom-right y coordinate in µm
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
     *
     * @param n total number of squares in the recording grid
     * @return the area of a single square in µm²
     */
    public static double calculateSquareArea(int n) {
        return IMAGE_WIDTH * IMAGE_HEIGHT / n;
    }

    //=========================================================================
    // ACCESSORS & MUTATORS
    //=========================================================================


    /**
     * @return the globally unique key for this square.
     */
    public String getUniqueKey() {
        return uniqueKey;
    }

    /**
     * @param key the unique key to set.
     */
    public void setUniqueKey(String key) {
        this.uniqueKey = key;
    }

    /**
     * @return the name of the experiment this square belongs to.
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * @param name the experiment name to set.
     */
    public void setExperimentName(String name) {
        this.experimentName = name;
    }

    /**
     * @return the name of the recording this square belongs to.
     */
    public String getRecordingName() {
        return recordingName;
    }

    /**
     * @param name the recording name to set.
     */
    public void setRecordingName(String name) {
        this.recordingName = name;
    }

    /**
     * @return the sequential index of this square within the recording.
     */
    public int getSquareNumber() {
        return squareNumber;
    }

    /**
     * @param n the square number to set.
     */
    public void setSquareNumber(int n) {
        this.squareNumber = n;
    }

    /**
     * @return the row index of this square in the grid.
     */
    public int getRowNumber() {
        return rowNumber;
    }

    /**
     * @param r the row number to set.
     */
    public void setRowNumber(int r) {
        this.rowNumber = r;
    }

    /**
     * @return the column index of this square in the grid.
     */
    public int getColNumber() {
        return colNumber;
    }

    /**
     * @param c the column number to set.
     */
    public void setColNumber(int c) {
        this.colNumber = c;
    }

    /**
     * @return the label index of this square (often matches square number).
     */
    public int getLabelNumber() {
        return labelNumber;
    }

    /**
     * @param n the label number to set.
     */
    public void setLabelNumber(int n) {
        this.labelNumber = n;
    }

    /**
     * @return the assigned cell ID for this square.
     */
    public int getCellId() {
        return cellId;
    }

    /**
     * @param id the cell ID to set.
     */
    public void setCellId(int id) {
        this.cellId = id;
    }

    /**
     * @return true if this square is visible in the current view.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param v the visibility status to set.
     */
    public void setVisible(boolean v) {
        this.visible = v;
    }

    /**
     * @return true if this square has been manually marked for exclusion.
     */
    public boolean isSquareManuallyExcluded() {
        return squareManuallyExcluded;
    }

    /**
     * @param b the manual exclusion status to set.
     */
    public void setSquareManuallyExcluded(boolean b) {
        this.squareManuallyExcluded = b;
    }

    /**
     * @return true if the parent image is excluded.
     */
    public boolean isImageExcluded() {
        return imageExcluded;
    }

    /**
     * @param b the image exclusion status to set.
     */
    public void setImageExcluded(boolean b) {
        this.imageExcluded = b;
    }

    /**
     * @return the top-left X coordinate of the square.
     */
    public double getX0() {
        return x0;
    }

    /**
     * @param v the top-left X coordinate to set.
     */
    public void setX0(double v) {
        this.x0 = round(v, 2);
    }

    /**
     * @return the top-left Y coordinate of the square.
     */
    public double getY0() {
        return y0;
    }

    /**
     * @param v the top-left Y coordinate to set.
     */
    public void setY0(double v) {
        this.y0 = round(v, 2);
    }

    /**
     * @return the bottom-right X coordinate of the square.
     */
    public double getX1() {
        return x1;
    }

    /**
     * @param v the bottom-right X coordinate to set.
     */
    public void setX1(double v) {
        this.x1 = round(v, 2);
    }

    /**
     * @return the bottom-right Y coordinate of the square.
     */
    public double getY1() {
        return y1;
    }

    /**
     * @param v the bottom-right Y coordinate to set.
     */
    public void setY1(double v) {
        this.y1 = round(v, 2);
    }

    /**
     * @return the number of tracks that originated in this square.
     */
    public int getNumberOfTracks() {
        return numberOfTracks;
    }

    /**
     * @param n the number of tracks to set.
     */
    public void setNumberOfTracks(int n) {
        this.numberOfTracks = n;
    }

    /**
     * @return the spatial variability metric of track density.
     */
    public double getVariability() {
        return variability;
    }

    /**
     * @param v the variability metric to set.
     */
    public void setVariability(double v) {
        this.variability = v;
    }

    /**
     * @return the track density within the square.
     */
    public double getDensity() {
        return density;
    }

    /**
     * @param d the density value to set.
     */
    public void setDensity(double d) {
        this.density = d;
    }

    /**
     * @return the ratio of density to the local background.
     */
    public double getDensityRatio() {
        return densityRatio;
    }

    /**
     * @param v the density ratio to set.
     */
    public void setDensityRatio(double v) {
        this.densityRatio = v;
    }

    /**
     * @return the original density ratio (before processing).
     */
    public double getDensityRatioOri() {
        return densityRatioOri;
    }

    /**
     * @param v the original density ratio to set.
     */
    public void setDensityRatioOri(double v) {
        this.densityRatioOri = v;
    }

    /**
     * @return the computed Tau value for kinetics in this square.
     */
    public double getTau() {
        return tau;
    }

    /**
     * @param t the Tau value to set.
     */
    public void setTau(double t) {
        this.tau = t;
    }

    /**
     * @return the R-squared value for the kinetics fit in this square.
     */
    public double getRSquared() {
        return rSquared;
    }

    /**
     * @param r the R-squared value to set.
     */
    public void setRSquared(double r) {
        this.rSquared = r;
    }

    /**
     * @return the median diffusion coefficient of tracks in this square.
     */
    public double getMedianDiffusionCoefficient() {
        return medianDiffusionCoefficient;
    }

    /**
     * @param v the median diffusion coefficient to set.
     */
    public void setMedianDiffusionCoefficient(double v) {
        this.medianDiffusionCoefficient = v;
    }

    /**
     * @return the extended median diffusion coefficient.
     */
    public double getMedianDiffusionCoefficientExt() {
        return medianDiffusionCoefficientExt;
    }

    /**
     * @param v the extended median diffusion coefficient to set.
     */
    public void setMedianDiffusionCoefficientExt(double v) {
        this.medianDiffusionCoefficientExt = v;
    }

    /**
     * @return the median displacement of spots within tracks.
     */
    public double getMedianDisplacement() {
        return medianDisplacement;
    }

    /**
     * @param v the median displacement to set.
     */
    public void setMedianDisplacement(double v) {
        this.medianDisplacement = v;
    }

    /**
     * @return the maximum displacement observed among tracks.
     */
    public double getMaxDisplacement() {
        return maxDisplacement;
    }

    /**
     * @param v the maximum displacement to set.
     */
    public void setMaxDisplacement(double v) {
        this.maxDisplacement = v;
    }

    /**
     * @return the total displacement across all tracks.
     */
    public double getTotalDisplacement() {
        return totalDisplacement;
    }

    /**
     * @param v the total displacement to set.
     */
    public void setTotalDisplacement(double v) {
        this.totalDisplacement = v;
    }

    /**
     * @return the median of maximum speeds achieved by tracks.
     */
    public double getMedianMaxSpeed() {
        return medianMaxSpeed;
    }

    /**
     * @param v the median max speed to set.
     */
    public void setMedianMaxSpeed(double v) {
        this.medianMaxSpeed = v;
    }

    /**
     * @return the absolute maximum speed observed among tracks.
     */
    public double getMaxMaxSpeed() {
        return maxMaxSpeed;
    }

    /**
     * @param v the maximum speed to set.
     */
    public void setMaxMaxSpeed(double v) {
        this.maxMaxSpeed = v;
    }

    /**
     * @return the median of median speeds of individual tracks.
     */
    public double getMedianMedianSpeed() {
        return medianMedianSpeed;
    }

    /**
     * @param v the median median speed to set.
     */
    public void setMedianMedianSpeed(double v) {
        this.medianMedianSpeed = v;
    }

    /**
     * @return the maximum of median speeds observed among tracks.
     */
    public double getMaxMedianSpeed() {
        return maxMedianSpeed;
    }

    /**
     * @param v the max median speed to set.
     */
    public void setMaxMedianSpeed(double v) {
        this.maxMedianSpeed = v;
    }

    /**
     * @return the duration of the longest track in the square.
     */
    public double getMaxTrackDuration() {
        return maxTrackDuration;
    }

    /**
     * @param v the maximum track duration to set.
     */
    public void setMaxTrackDuration(double v) {
        this.maxTrackDuration = v;
    }

    /**
     * @return the sum of durations of all tracks in the square.
     */
    public double getTotalTrackDuration() {
        return totalTrackDuration;
    }

    /**
     * @param v the total track duration to set.
     */
    public void setTotalTrackDuration(double v) {
        this.totalTrackDuration = v;
    }

    /**
     * @return the median track duration in the square.
     */
    public double getMedianTrackDuration() {
        return medianTrackDuration;
    }

    /**
     * @param v the median track duration to set.
     */
    public void setMedianTrackDuration(double v) {
        this.medianTrackDuration = v;
    }

    /**
     * @return the list of {@link Track} objects associated with this square.
     */
    public List<Track> getTracks() {
        return tracks;
    }

    /**
     * @param t the list of tracks to set.
     */
    public void setTracksList(List<Track> t) {
        this.tracks = t;
    }

    /**
     * @return the {@link Table} of track data for this square.
     */
    public Table getTracksTable() {
        return tracksTable;
    }

    /**
     * @param t the tracks table to set.
     */
    public void setTracksTable(Table t) {
        this.tracksTable = t;
    }

    //=========================================================================
    // UTILITIES
    //=========================================================================

    /**
     * Resets all calculated metrics to {@link Double#NaN}.
     * This is useful before re-running calculations or when a square fails
     * to meet the minimum requirements for analysis.
     */
    public void resetCalculatedAttributes() {
        this.tau                           = Double.NaN;
        this.rSquared                      = Double.NaN;
        this.variability                   = Double.NaN;
        this.density                       = Double.NaN;
        this.densityRatio                  = Double.NaN;
        this.densityRatioOri               = Double.NaN;
        this.medianDiffusionCoefficient    = Double.NaN;
        this.medianDiffusionCoefficientExt = Double.NaN;
        this.medianDisplacement            = Double.NaN;
        this.maxDisplacement               = Double.NaN;
        this.totalDisplacement             = Double.NaN;
        this.medianMaxSpeed                = Double.NaN;
        this.maxMaxSpeed                   = Double.NaN;
        this.medianMedianSpeed             = Double.NaN;
        this.maxMedianSpeed                = Double.NaN;
        this.maxTrackDuration              = Double.NaN;
        this.totalTrackDuration            = Double.NaN;
        this.medianTrackDuration           = Double.NaN;
    }

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