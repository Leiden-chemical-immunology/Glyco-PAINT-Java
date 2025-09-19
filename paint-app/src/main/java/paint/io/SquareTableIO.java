package paint.io;

import paint.shared.objects.Square;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.SQUARE_COLS;
import static paint.shared.constants.PaintConstants.SQUARE_TYPES;

/**
 * Table IO for Square entities.
 */
public class SquareTableIO extends BaseTableIO {

    public Table emptyTable() {
        return newEmptyTable("Squares", SQUARE_COLS, SQUARE_TYPES);
    }

    public Table toTable(List<Square> squares) {
        Table table = emptyTable();
        for (Square sq : squares) {
            Row row = table.appendRow();
            row.setString("Unique Key", sq.getUniqueKey());
            row.setString("Recording Name", sq.getRecordingName());
            row.setInt("Square Number", sq.getSquareNumber());
            row.setInt("Row Number", sq.getRowNumber());
            row.setInt("Column Number", sq.getColNumber());
            row.setInt("Label Number", sq.getLabelNumber());
            row.setInt("Cell ID", sq.getCellId());
            row.setBoolean("Selected", sq.isSelected());
            row.setBoolean("Square Manually Excluded", sq.isSquareManuallyExcluded());
            row.setBoolean("Image Excluded", sq.isImageExcluded());
            row.setDouble("X0", sq.getX0());
            row.setDouble("Y0", sq.getY0());
            row.setDouble("X1", sq.getX1());
            row.setDouble("Y1", sq.getY1());
            row.setInt("Number of Tracks", sq.getNumberOfTracks());
            row.setDouble("Variability", sq.getVariability());
            row.setDouble("Density", sq.getDensity());
            row.setDouble("Density Ratio", sq.getDensityRatio());
            row.setDouble("Tau", sq.getTau());
            row.setDouble("R Squared", sq.getRSquared());
            row.setDouble("Median Diffusion Coefficient", sq.getMedianDiffusionCoefficient());
            row.setDouble("Median Diffusion Coefficient Ext", sq.getMedianDiffusionCoefficientExt());
            row.setDouble("Median Long Track Duration", sq.getMedianLongTrackDuration());
            row.setDouble("Median Short Track Duration", sq.getMedianShortTrackDuration());
            row.setDouble("Median Displacement", sq.getMedianDisplacement());
            row.setDouble("Max Displacement", sq.getMaxDisplacement());
            row.setDouble("Total Displacement", sq.getTotalDisplacement());
            row.setDouble("Median Max Speed", sq.getMedianMaxSpeed());
            row.setDouble("Max Max Speed", sq.getMaxMaxSpeed());
            row.setDouble("Median Mean Speed", sq.getMedianMeanSpeed());
            row.setDouble("Max Mean Speed", sq.getMaxMeanSpeed());
            row.setDouble("Max Track Duration", sq.getMaxTrackDuration());
            row.setDouble("Total Track Duration", sq.getTotalTrackDuration());
            row.setDouble("Median Track Duration", sq.getMedianTrackDuration());
        }
        return table;
    }

    public List<Square> toEntities(Table table) {
        List<Square> squares = new ArrayList<>();
        for (Row row : table) {
            Square sq = new Square();
            sq.setUniqueKey(row.getString("Unique Key"));
            sq.setRecordingName(row.getString("Recording Name"));
            sq.setSquareNumber(row.getInt("Square Number"));
            sq.setRowNumber(row.getInt("Row Number"));
            sq.setColNumber(row.getInt("Column Number"));
            sq.setLabelNumber(row.getInt("Label Number"));
            sq.setCellId(row.getInt("Cell ID"));
            sq.setSelected(row.getBoolean("Selected"));
            sq.setSquareManuallyExcluded(row.getBoolean("Square Manually Excluded"));
            sq.setImageExcluded(row.getBoolean("Image Excluded"));
            sq.setX0(row.getDouble("X0"));
            sq.setY0(row.getDouble("Y0"));
            sq.setX1(row.getDouble("X1"));
            sq.setY1(row.getDouble("Y1"));
            sq.setNumberOfTracks(row.getInt("Number of Tracks"));
            sq.setVariability(row.getDouble("Variability"));
            sq.setDensity(row.getDouble("Density"));
            sq.setDensityRatio(row.getDouble("Density Ratio"));
            sq.setTau(row.getDouble("Tau"));
            sq.setRSquared(row.getDouble("R Squared"));
            sq.setMedianDiffusionCoefficient(row.getDouble("Median Diffusion Coefficient"));
            sq.setMedianDiffusionCoefficientExt(row.getDouble("Median Diffusion Coefficient Ext"));
            sq.setMedianLongTrackDuration(row.getDouble("Median Long Track Duration"));
            sq.setMedianShortTrackDuration(row.getDouble("Median Short Track Duration"));
            sq.setMedianDisplacement(row.getDouble("Median Displacement"));
            sq.setMaxDisplacement(row.getDouble("Max Displacement"));
            sq.setTotalDisplacement(row.getDouble("Total Displacement"));
            sq.setMedianMaxSpeed(row.getDouble("Median Max Speed"));
            sq.setMaxMaxSpeed(row.getDouble("Max Max Speed"));
            sq.setMedianMeanSpeed(row.getDouble("Median Mean Speed"));
            sq.setMaxMeanSpeed(row.getDouble("Max Mean Speed"));
            sq.setMaxTrackDuration(row.getDouble("Max Track Duration"));
            sq.setTotalTrackDuration(row.getDouble("Total Track Duration"));
            sq.setMedianTrackDuration(row.getDouble("Median Track Duration"));
            squares.add(sq);
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