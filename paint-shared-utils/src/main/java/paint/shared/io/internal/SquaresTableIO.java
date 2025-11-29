package paint.shared.io.internal;

import static paint.shared.constants.PaintStringConstants.*;

import paint.shared.objects.Square;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal schema-validated table I/O implementation for {@link Square}.
 */
public class SquaresTableIO extends BaseTableIO {

    // =====================================================================
    //  INTERNAL HELPERS (schema extracted from Square.Column)
    // =====================================================================

    private String[] getColumnHeaders() {
        Square.Column[] cols = Square.Column.values();
        String[] headers = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            headers[i] = cols[i].header;
        }
        return headers;
    }

    private ColumnType[] getColumnTypes() {
        Square.Column[] cols = Square.Column.values();
        ColumnType[] types = new ColumnType[cols.length];
        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }
        return types;
    }

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    public Table emptyTable() {
        return newEmptyTable(
                "Squares",
                getColumnHeaders(),
                getColumnTypes()
        );
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION  (UNCHANGED)
    // =====================================================================

    public Table toTable(List<Square> squares) {
        Table table = emptyTable();

        for (Square square : squares) {
            Row tablesawRow = table.appendRow();

            tablesawRow.setString(  UNIQUE_KEY,                        square.getUniqueKey());
            tablesawRow.setString(  EXPERIMENT_NAME,                   square.getExperimentName());
            tablesawRow.setString(  RECORDING_NAME,                    square.getRecordingName());
            tablesawRow.setInt(     SQUARE_NUMBER,                     square.getSquareNumber());
            tablesawRow.setInt(     ROW_NUMBER,                        square.getRowNumber());
            tablesawRow.setInt(     COLUMN_NUMBER,                     square.getColNumber());
            tablesawRow.setInt(     LABEL_NUMBER,                      square.getLabelNumber());
            tablesawRow.setInt(     CELL_ID,                           square.getCellId());
            tablesawRow.setBoolean( VISIBLE,                           square.isVisible());
            tablesawRow.setBoolean( SQUARE_MANUALLY_EXCLUDED,          square.isSquareManuallyExcluded());
            tablesawRow.setBoolean( IMAGE_EXCLUDED,                    square.isImageExcluded());
            tablesawRow.setDouble(  X0,                                square.getX0());
            tablesawRow.setDouble(  Y0,                                square.getY0());
            tablesawRow.setDouble(  X1,                                square.getX1());
            tablesawRow.setDouble(  Y1,                                square.getY1());
            tablesawRow.setInt(     NUMBER_OF_TRACKS,                  square.getNumberOfTracks());
            tablesawRow.setDouble(  VARIABILITY,                       square.getVariability());
            tablesawRow.setDouble(  DENSITY,                           square.getDensity());
            tablesawRow.setDouble(  DENSITY_RATIO,                     square.getDensityRatio());
            tablesawRow.setDouble(  DENSITY_RATIO_ORI,                 square.getDensityRatioOri());
            tablesawRow.setDouble(  TAU,                               square.getTau());
            tablesawRow.setDouble(  R_SQUARED,                         square.getRSquared());
            tablesawRow.setDouble(  MEDIAN_DIFFUSION_COEFFICIENT,      square.getMedianDiffusionCoefficient());
            tablesawRow.setDouble(  MEDIAN_DIFFUSION_COEFFICIENT_EXT,  square.getMedianDiffusionCoefficientExt());
            tablesawRow.setDouble(  MEDIAN_DISPLACEMENT,               square.getMedianDisplacement());
            tablesawRow.setDouble(  MAX_DISPLACEMENT,                  square.getMaxDisplacement());
            tablesawRow.setDouble(  TOTAL_DISPLACEMENT,                square.getTotalDisplacement());
            tablesawRow.setDouble(  MEDIAN_MAX_SPEED,                  square.getMedianMaxSpeed());
            tablesawRow.setDouble(  MAX_MAX_SPEED,                     square.getMaxMaxSpeed());
            tablesawRow.setDouble(  MEDIAN_MEDIAN_SPEED,               square.getMedianMedianSpeed());
            tablesawRow.setDouble(  MAX_MEDIAN_SPEED,                  square.getMaxMedianSpeed());
            tablesawRow.setDouble(  MAX_TRACK_DURATION,                square.getMaxTrackDuration());
            tablesawRow.setDouble(  TOTAL_TRACK_DURATION,              square.getTotalTrackDuration());
            tablesawRow.setDouble(  MEDIAN_TRACK_DURATION,             square.getMedianTrackDuration());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION (UNCHANGED)
    // =====================================================================

    public List<Square> toEntities(Table table) {
        List<Square> squares = new ArrayList<>();

        for (Row row : table) {
            Square square = new Square();

            square.setUniqueKey(                     row.getString( UNIQUE_KEY));
            square.setExperimentName(                row.getString( EXPERIMENT_NAME));
            square.setRecordingName(                 row.getString( RECORDING_NAME));
            square.setSquareNumber(                  row.getInt(    SQUARE_NUMBER));
            square.setRowNumber(                     row.getInt(    ROW_NUMBER));
            square.setColNumber(                     row.getInt(    COLUMN_NUMBER));
            square.setLabelNumber(                   row.getInt(    LABEL_NUMBER));
            square.setCellId(                        row.getInt(    CELL_ID));
            square.setVisible(                       row.getBoolean(VISIBLE));
            square.setSquareManuallyExcluded(        row.getBoolean(SQUARE_MANUALLY_EXCLUDED));
            square.setImageExcluded(                 row.getBoolean(IMAGE_EXCLUDED));
            square.setX0(                            row.getDouble( X0));
            square.setY0(                            row.getDouble( Y0));
            square.setX1(                            row.getDouble( X1));
            square.setY1(                            row.getDouble( Y1));
            square.setNumberOfTracks(                row.getInt(    NUMBER_OF_TRACKS));
            square.setVariability(                   row.getDouble( VARIABILITY));
            square.setDensity(                       row.getDouble( DENSITY));
            square.setDensityRatio(                  row.getDouble( DENSITY_RATIO));
            square.setDensityRatioOri(               row.getDouble( DENSITY_RATIO_ORI));
            square.setTau(                           row.getDouble( TAU));
            square.setRSquared(                      row.getDouble( R_SQUARED));
            square.setMedianDiffusionCoefficient(    row.getDouble( MEDIAN_DIFFUSION_COEFFICIENT));
            square.setMedianDiffusionCoefficientExt( row.getDouble( MEDIAN_DIFFUSION_COEFFICIENT_EXT));
            square.setMedianDisplacement(            row.getDouble( MEDIAN_DISPLACEMENT));
            square.setMaxDisplacement(               row.getDouble( MAX_DISPLACEMENT));
            square.setTotalDisplacement(             row.getDouble( TOTAL_DISPLACEMENT));
            square.setMedianMaxSpeed(                row.getDouble( MEDIAN_MAX_SPEED));
            square.setMaxMaxSpeed(                   row.getDouble( MAX_MAX_SPEED));
            square.setMedianMedianSpeed(             row.getDouble( MEDIAN_MEDIAN_SPEED));
            square.setMaxMedianSpeed(                row.getDouble( MAX_MEDIAN_SPEED));
            square.setMaxTrackDuration(              row.getDouble( MAX_TRACK_DURATION));
            square.setTotalTrackDuration(            row.getDouble( TOTAL_TRACK_DURATION));
            square.setMedianTrackDuration(           row.getDouble( MEDIAN_TRACK_DURATION));

            squares.add(square);
        }

        return squares;
    }

    // =====================================================================
    //  APPEND / MERGE (UPDATED FOR EMBEDDED SCHEMA)
    // =====================================================================

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();

            for (Square.Column colEnum : Square.Column.values()) {
                String col = colEnum.header;
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