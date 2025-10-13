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

import static paint.shared.constants.PaintConstants.SQUARE_COLS;
import static paint.shared.constants.PaintConstants.SQUARE_TYPES;
import static paint.shared.utils.Miscellaneous.round;

/**
 * Table IO for Square entities.
 */
public class SquareTableIO extends BaseTableIO {

    public Table emptyTable() {
        return newEmptyTable("Squares", SQUARE_COLS, SQUARE_TYPES);
    }

    public Table toTable(List<Square> squares) {
        Table table = emptyTable();
        for (Square square : squares) {
            Row row = table.appendRow();

            // @formatter:off
            row.setString( "Unique Key",                       square.getUniqueKey());
            row.setString( "Recording Name",                   square.getRecordingName());
            row.setInt(    "Square Number",                    square.getSquareNumber());
            row.setInt(    "Row Number",                       square.getRowNumber());
            row.setInt(    "Column Number",                    square.getColNumber());
            row.setInt(    "Label Number",                     square.getLabelNumber());
            row.setInt(    "Cell ID",                          square.getCellId());
            row.setBoolean("Selected",                         square.isSelected());
            row.setBoolean("Square Manually Excluded",         square.isSquareManuallyExcluded());
            row.setBoolean("Image Excluded",                   square.isImageExcluded());
            row.setDouble( "X0",                               round(square.getX0(),2));
            row.setDouble( "Y0",                               round(square.getY0(), 2));
            row.setDouble( "X1",                               round(square.getX1(), 2));
            row.setDouble( "Y1",                               round(square.getY1(), 2));
            row.setInt(    "Number of Tracks",                 square.getNumberOfTracks());
            row.setDouble( "Variability",                      round(square.getVariability(), 3));
            row.setDouble( "Density",                          round(square.getDensity(),4));
            row.setDouble( "Density Ratio",                    round(square.getDensityRatio(),1));
            row.setDouble( "Tau",                              round(square.getTau(), 1));
            row.setDouble( "R Squared",                        round(square.getRSquared(), 3));
            row.setDouble( "Median Diffusion Coefficient",     round(square.getMedianDiffusionCoefficient(), 2));
            row.setDouble( "Median Diffusion Coefficient Ext", round(square.getMedianDiffusionCoefficientExt(), 2));
            row.setDouble( "Median Long Track Duration",       round(square.getMedianLongTrackDuration(), 1));
            row.setDouble( "Median Short Track Duration",      round(square.getMedianShortTrackDuration(), 1));
            row.setDouble( "Median Displacement",              round(square.getMedianDisplacement(), 2));
            row.setDouble( "Max Displacement",                 round(square.getMaxDisplacement(), 2));
            row.setDouble( "Total Displacement",               round(square.getTotalDisplacement(), 2));
            row.setDouble( "Median Max Speed",                 round(square.getMedianMaxSpeed(), 2));
            row.setDouble( "Max Max Speed",                    round(square.getMaxMaxSpeed(), 2));
            row.setDouble( "Median Mean Speed",                round(square.getMedianMeanSpeed(), 2));
            row.setDouble( "Max Mean Speed",                   round(square.getMaxMeanSpeed(), 2));
            row.setDouble( "Max Track Duration",               round(square.getMaxTrackDuration(), 1));
            row.setDouble( "Total Track Duration",             round(square.getTotalTrackDuration(), 1));
            row.setDouble( "Median Track Duration",            round(square.getMedianTrackDuration(), 1));
            // @formatter:on

        }
        return table;
    }

    public List<Square> toEntities(Table table) {
        List<Square> squares = new ArrayList<>();
        for (Row row : table) {
            Square square = new Square();

            // @formatter:off
            square.setUniqueKey(                     row.getString(  "Unique Key"));
            square.setRecordingName(                 row.getString(  "Recording Name"));
            square.setSquareNumber(                  row.getInt(     "Square Number"));
            square.setRowNumber(                     row.getInt(     "Row Number"));
            square.setColNumber(                     row.getInt(     "Column Number"));
            square.setLabelNumber(                   row.getInt(     "Label Number"));
            square.setCellId(                        row.getInt(     "Cell ID"));
            square.setSelected(                      row.getBoolean( "Selected"));
            square.setSquareManuallyExcluded(        row.getBoolean( "Square Manually Excluded"));
            square.setImageExcluded(                 row.getBoolean( "Image Excluded"));
            square.setX0(                            row.getDouble(  "X0"));
            square.setY0(                            row.getDouble(  "Y0"));
            square.setX1(                            row.getDouble(  "X1"));
            square.setY1(                            row.getDouble(  "Y1"));
            square.setNumberOfTracks(                row.getInt(     "Number of Tracks"));
            square.setVariability(                   row.getDouble(  "Variability"));
            square.setDensity(                       row.getDouble(  "Density"));
            square.setDensityRatio(                  row.getDouble(  "Density Ratio"));
            square.setTau(                           row.getDouble(  "Tau"));
            square.setRSquared(                      row.getDouble(  "R Squared"));
            square.setMedianDiffusionCoefficient(    row.getDouble(  "Median Diffusion Coefficient"));
            square.setMedianDiffusionCoefficientExt( row.getDouble(  "Median Diffusion Coefficient Ext"));
            square.setMedianLongTrackDuration(       row.getDouble(  "Median Long Track Duration"));
            square.setMedianShortTrackDuration(      row.getDouble(  "Median Short Track Duration"));
            square.setMedianDisplacement(            row.getDouble(  "Median Displacement"));
            square.setMaxDisplacement(               row.getDouble(  "Max Displacement"));
            square.setTotalDisplacement(             row.getDouble(  "Total Displacement"));
            square.setMedianMaxSpeed(                row.getDouble(  "Median Max Speed"));
            square.setMaxMaxSpeed(                   row.getDouble(  "Max Max Speed"));
            square.setMedianMeanSpeed(               row.getDouble(  "Median Mean Speed"));
            square.setMaxMeanSpeed(                  row.getDouble(  "Max Mean Speed"));
            square.setMaxTrackDuration(              row.getDouble(  "Max Track Duration"));
            square.setTotalTrackDuration(            row.getDouble(  "Total Track Duration"));
            square.setMedianTrackDuration(           row.getDouble(  "Median Track Duration"));
            // @formatter:on

            squares.add(square);
        }
        return squares;
    }

    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, "Squares", SQUARE_COLS, SQUARE_TYPES, false);
    }

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : SQUARE_COLS) {
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