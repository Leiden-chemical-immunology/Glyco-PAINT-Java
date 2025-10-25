package paint.shared.io;

import paint.shared.objects.Square;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.*;

/**
 * Table IO for Square entities.
 */
public class SquareTableIO extends BaseTableIO {

    public Table emptyTable() {
        return newEmptyTable("Squares", SQUARES_COLS, SQUARES_TYPES);
    }

    public Table toTable(List<Square> squares) {
        Table table = emptyTable();
        for (Square square : squares) {
            Row tablesawRow = table.appendRow();

            // @formatter:off
            tablesawRow.setString( "Unique Key",                       square.getUniqueKey());
            tablesawRow.setString( "Experiment Name",                  square.getExperimentName());
            tablesawRow.setString( "Recording Name",                   square.getRecordingName());
            tablesawRow.setInt(    "Square Number",                    square.getSquareNumber());
            tablesawRow.setInt(    "Row Number",                       square.getRowNumber());
            tablesawRow.setInt(    "Column Number",                    square.getColNumber());
            tablesawRow.setInt(    "Label Number",                     square.getLabelNumber());
            tablesawRow.setInt(    "Cell ID",                          square.getCellId());
            tablesawRow.setBoolean("Selected",                         square.isSelected());
            tablesawRow.setBoolean("Square Manually Excluded",         square.isSquareManuallyExcluded());
            tablesawRow.setBoolean("Image Excluded",                   square.isImageExcluded());
            tablesawRow.setDouble( "X0",                               square.getX0());
            tablesawRow.setDouble( "Y0",                               square.getY0());
            tablesawRow.setDouble( "X1",                               square.getX1());
            tablesawRow.setDouble( "Y1",                               square.getY1());
            tablesawRow.setInt(    "Number of Tracks",                 square.getNumberOfTracks());
            tablesawRow.setDouble( "Variability",                      square.getVariability());
            tablesawRow.setDouble( "Density",                          square.getDensity());
            tablesawRow.setDouble( "Density Ratio",                    square.getDensityRatio());
            tablesawRow.setDouble( "Density Ratio Ori",                square.getDensityRatioOri());
            tablesawRow.setDouble( "Tau",                              square.getTau());
            tablesawRow.setDouble( "R Squared",                        square.getRSquared());
            tablesawRow.setDouble( "Median Diffusion Coefficient",     square.getMedianDiffusionCoefficient());
            tablesawRow.setDouble( "Median Diffusion Coefficient Ext", square.getMedianDiffusionCoefficientExt());
            tablesawRow.setDouble( "Median Long Track Duration",       square.getMedianLongTrackDuration());
            tablesawRow.setDouble( "Median Short Track Duration",      square.getMedianShortTrackDuration());
            tablesawRow.setDouble( "Median Displacement",              square.getMedianDisplacement());
            tablesawRow.setDouble( "Max Displacement",                 square.getMaxDisplacement());
            tablesawRow.setDouble( "Total Displacement",               square.getTotalDisplacement());
            tablesawRow.setDouble( "Median Max Speed",                 square.getMedianMaxSpeed());
            tablesawRow.setDouble( "Max Max Speed",                    square.getMaxMaxSpeed());
            tablesawRow.setDouble( "Median Mean Speed",                square.getMedianMeanSpeed());
            tablesawRow.setDouble( "Max Mean Speed",                   square.getMaxMeanSpeed());
            tablesawRow.setDouble( "Max Track Duration",               square.getMaxTrackDuration());
            tablesawRow.setDouble( "Total Track Duration",             square.getTotalTrackDuration());
            tablesawRow.setDouble( "Median Track Duration",            square.getMedianTrackDuration());
            // @formatter:on

        }
        return table;
    }

    public List<Square> toEntities(Table table) {
        List<Square> squares = new ArrayList<>();
        for (Row tablesawRow : table) {
            Square square = new Square();

            // @formatter:off
            square.setUniqueKey(                     tablesawRow.getString(  "Unique Key"));
            square.setExperimentName(                tablesawRow.getString(  "Experiment Name"));
            square.setRecordingName(                 tablesawRow.getString(  "Recording Name"));
            square.setSquareNumber(                  tablesawRow.getInt(     "Square Number"));
            square.setRowNumber(                     tablesawRow.getInt(     "Row Number"));
            square.setColNumber(                     tablesawRow.getInt(     "Column Number"));
            square.setLabelNumber(                   tablesawRow.getInt(     "Label Number"));
            square.setCellId(                        tablesawRow.getInt(     "Cell ID"));
            square.setSelected(                      tablesawRow.getBoolean( "Selected"));
            square.setSquareManuallyExcluded(        tablesawRow.getBoolean( "Square Manually Excluded"));
            square.setImageExcluded(                 tablesawRow.getBoolean( "Image Excluded"));
            square.setX0(                            tablesawRow.getDouble(  "X0"));
            square.setY0(                            tablesawRow.getDouble(  "Y0"));
            square.setX1(                            tablesawRow.getDouble(  "X1"));
            square.setY1(                            tablesawRow.getDouble(  "Y1"));
            square.setNumberOfTracks(                tablesawRow.getInt(     "Number of Tracks"));
            square.setVariability(                   tablesawRow.getDouble(  "Variability"));
            square.setDensity(                       tablesawRow.getDouble(  "Density"));
            square.setDensityRatio(                  tablesawRow.getDouble(  "Density Ratio"));
            square.setDensityRatioOri(               tablesawRow.getDouble(  "Density Ratio Ori"));
            square.setTau(                           tablesawRow.getDouble(  "Tau"));
            square.setRSquared(                      tablesawRow.getDouble(  "R Squared"));
            square.setMedianDiffusionCoefficient(    tablesawRow.getDouble(  "Median Diffusion Coefficient"));
            square.setMedianDiffusionCoefficientExt( tablesawRow.getDouble(  "Median Diffusion Coefficient Ext"));
            square.setMedianLongTrackDuration(       tablesawRow.getDouble(  "Median Long Track Duration"));
            square.setMedianShortTrackDuration(      tablesawRow.getDouble(  "Median Short Track Duration"));
            square.setMedianDisplacement(            tablesawRow.getDouble(  "Median Displacement"));
            square.setMaxDisplacement(               tablesawRow.getDouble(  "Max Displacement"));
            square.setTotalDisplacement(             tablesawRow.getDouble(  "Total Displacement"));
            square.setMedianMaxSpeed(                tablesawRow.getDouble(  "Median Max Speed"));
            square.setMaxMaxSpeed(                   tablesawRow.getDouble(  "Max Max Speed"));
            square.setMedianMeanSpeed(               tablesawRow.getDouble(  "Median Mean Speed"));
            square.setMaxMeanSpeed(                  tablesawRow.getDouble(  "Max Mean Speed"));
            square.setMaxTrackDuration(              tablesawRow.getDouble(  "Max Track Duration"));
            square.setTotalTrackDuration(            tablesawRow.getDouble(  "Total Track Duration"));
            square.setMedianTrackDuration(           tablesawRow.getDouble(  "Median Track Duration"));
            // @formatter:on

            squares.add(square);
        }
        return squares;
    }

    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, SQUARES, SQUARES_COLS, SQUARES_TYPES, false);
    }

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : SQUARES_COLS) {
                Column<?> targetCol = target.column(col);
                if (targetCol.type() == ColumnType.STRING) {
                    newRow.setString(col, row.getString(col));
                } else if (targetCol.type() == ColumnType.INTEGER) {
                    newRow.setInt(col, row.getInt(col));
                } else if (targetCol.type() == ColumnType.DOUBLE) {
                    newRow.setDouble(col, row.getDouble(col));
                } else if (targetCol.type() == ColumnType.BOOLEAN) {
                    newRow.setBoolean(col, row.getBoolean(col));
                }
            }
        }
    }
}