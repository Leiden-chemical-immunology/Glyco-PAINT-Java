/*=============================================================================
 *  Class:        Square.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents a rectangular spatial region ("square") within a recording or
 *    experiment in the PAINT analysis framework.
 *
 *  DESCRIPTION:
 *    The {@code Square} class models a subregion of an experimental image grid.
 *    Each square stores its spatial coordinates, computed statistics, flags,
 *    and references to related {@link Track} objects. Squares can be generated
 *    automatically from the total number of regions in a recording or defined
 *    explicitly from coordinates.
 *
 *    The class also provides utilities for calculating theoretical square areas
 *    and introspective initialization of numeric fields.
 *
 *  KEY FEATURES:
 *    • Encapsulates position, dimensions, and analysis results of a square.
 *    • Supports linking of {@link Track} objects and {@link tech.tablesaw.api.Table}.
 *    • Provides automatic coordinate calculation based on grid size.
 *    • Offers formatted diagnostic output and NaN initialization for doubles.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

package paint.shared.objects;

import tech.tablesaw.api.Table;

import static paint.shared.utils.Miscellaneous.initialiseDoublesToNaN;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintGeometry.IMAGE_HEIGHT;
import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;
import static paint.shared.utils.Miscellaneous.round;

/**
 * Represents a rectangular region (square) within a recording or experiment,
 * with its coordinates, statistics, and related tracks.
 */
public class Square {

    // ───────────────────────────────────────────────────────────────────────────────
    // ATTRIBUTES
    // ───────────────────────────────────────────────────────────────────────────────


    private String  uniqueKey;                       
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
    private double  x0;                              
    private double  y0;                              
    private double  x1;                              
    private double  y1;                              
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

    private List<Track> tracks      = new ArrayList<>();
    private Table       tracksTable = null;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates an empty {@code Square}.
     */
    public Square() {
    }

    /**
     * Creates a {@code Square} with explicit coordinates and identifiers.
     *
     * @param uniqueKey      unique identifier
     * @param experimentName experiment name
     * @param recordingName  recording name
     * @param squareNumber   sequential number
     * @param rowNumber      row index in grid
     * @param colNumber      column index in grid
     * @param x0             left coordinate in pixels
     * @param y0             top coordinate in pixels
     * @param x1             right coordinate in pixels
     * @param y1             bottom coordinate in pixels
     */
    public Square(String uniqueKey,
                  String experimentName,
                  String recordingName,
                  int squareNumber,
                  int rowNumber,
                  int colNumber,
                  double x0,
                  double y0,
                  double x1,
                  double y1) {

        initialiseDoublesToNaN(this);
        this.uniqueKey       = uniqueKey;
        this.experimentName  = experimentName;
        this.recordingName   = recordingName;
        this.squareNumber    = squareNumber;
        this.rowNumber       = rowNumber;
        this.colNumber       = colNumber;
        this.x0              = round(x0, 2);
        this.y0              = round(y0, 2);
        this.x1              = round(x1, 2);
        this.y1              = round(y1, 2);
    }

    /**
     * Creates a {@code Square} automatically based on its sequential number and
     * the total number of squares in the recording.
     *
     * @param squareNumber               sequential number of the square
     * @param numberOfSquaresInRecording total number of squares in the recording
     */
    public Square(int squareNumber, int numberOfSquaresInRecording) {
        int    numberSquaresInRow = (int) Math.sqrt(numberOfSquaresInRecording);
        double width              = IMAGE_WIDTH / numberSquaresInRow;
        double height             = IMAGE_HEIGHT / numberSquaresInRow;

        initialiseDoublesToNaN(this);

        colNumber = squareNumber % numberSquaresInRow;
        rowNumber = squareNumber / numberSquaresInRow;

        x0 = round(colNumber * width, 2);
        x1 = round((colNumber + 1) * width, 2);
        y0 = round(rowNumber * height, 2);
        y1 = round((rowNumber + 1) * width, 2);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS AND MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    public String getUniqueKey() {
        return uniqueKey;
    }

    public void setUniqueKey(String uniqueKey) {
        this.uniqueKey = uniqueKey;
    }

    public String getExperimentName() {
        return experimentName;
    }

    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    public String getRecordingName() {
        return recordingName;
    }

    public void setRecordingName(String recordingName) {
        this.recordingName = recordingName;
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

    public int getLabelNumber() {
        return labelNumber;
    }

    public void setLabelNumber(int labelNumber) {
        this.labelNumber = labelNumber;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
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
        this.y0 = round(y0, 2);
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

    public double getDensityRatioOri() {
        return densityRatioOri;
    }

    public void setDensityRatioOri(double densityRatioOri) {
        this.densityRatioOri = densityRatioOri;
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

    public double getMedianMedianSpeed() {
        return medianMedianSpeed;
    }

    public void setMedianMedianSpeed(double medianMedianSpeed) {
        this.medianMedianSpeed = medianMedianSpeed;
    }

    public double getMaxMedianSpeed() {
        return maxMedianSpeed;
    }

    public void setMaxMedianSpeed(double maxMedianSpeed) {
        this.maxMedianSpeed = maxMedianSpeed;
    }

    public double getMaxTrackDuration() {
        return maxTrackDuration;
    }

    public void setMaxTrackDuration(double maxTrackDuration) {
        this.maxTrackDuration = maxTrackDuration;
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

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracksList(List<Track> tracks) {
        this.tracks = tracks;
    }

    public Table getTracksTable() {
        return tracksTable;
    }

    public void setTracksTable(Table tracksTable) {
        this.tracksTable = tracksTable;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // UTILITIES
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Calculates the theoretical square area for a given recording grid size.
     *
     * @param nrSquares total number of squares in the recording
     * @return area of one square (in image units)
     */
    public static double calculateSquareArea(int nrSquares) {
        return IMAGE_WIDTH * IMAGE_HEIGHT / nrSquares;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // STRING REPRESENTATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a formatted textual representation of this square and its metrics.
     *
     * @return formatted string
     */
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
            sb.append("Tracks table available%n");
        }

        return sb.toString();
    }
}