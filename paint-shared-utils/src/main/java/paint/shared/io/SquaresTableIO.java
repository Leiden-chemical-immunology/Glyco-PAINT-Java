/*=============================================================================
 *  Class:        SquaresTableIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Provides table input/output utilities for {@link paint.shared.objects.Square}
 *    entities, handling CSV schema validation, entity conversion, and append
 *    operations for the “Squares” data layer.
 *
 *  DESCRIPTION:
 *    This class defines I/O behavior for {@code squares.csv}, enforcing the
 *    schema defined in {@link paint.shared.schema.SquareSchema}.
 *
 *    It supports:
 *      • Creating schema-compliant Tablesaw tables.
 *      • Converting between {@link Square} objects and {@link tech.tablesaw.api.Table}.
 *      • Reading validated CSV files into tables.
 *      • Appending one table into another with safe type handling.
 *
 *  KEY FEATURES:
 *    • Enforces consistent schema across all square operations.
 *    • Converts bi-directionally between tables and Java entities.
 *    • Handles INTEGER→DOUBLE coercion where applicable.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
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

package paint.shared.io;

import static paint.shared.constants.PaintColumnNames.*;
import paint.shared.objects.Square;
import paint.shared.schema.SquareSchema;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;


/**
 * Provides all CSV input/output operations for {@link Square} entities.
 *
 * <p>Handles CSV reading, schema validation, entity conversion, and table
 * appending in a consistent manner across PAINT’s square-level datasets.</p>
 */
public class SquaresTableIO extends BaseTableIO {

    // ───────────────────────────────────────────────────────────────────────────────
    // TABLE CREATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates an empty {@link Table} for square data with the defined schema.
     *
     * @return a new empty table named “Squares”
     */
    public Table emptyTable() {
        return newEmptyTable("Squares", SquareSchema.COLUMNS, SquareSchema.TYPES);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ENTITY → TABLE CONVERSION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Converts a list of {@link Square} entities into a {@link Table}
     * matching the {@code squares.csv} schema.
     *
     * @param squares list of {@link Square} objects to convert
     * @return a schema-compliant {@link Table} populated with square data
     */
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
            tablesawRow.setDouble(  MEDIAN_MEAN_SPEED,                 square.getMedianMedianSpeed());
            tablesawRow.setDouble(  MAX_MEAN_SPEED,                    square.getMaxMedianSpeed());
            tablesawRow.setDouble(  MAX_TRACK_DURATION,                square.getMaxTrackDuration());
            tablesawRow.setDouble(  TOTAL_TRACK_DURATION,              square.getTotalTrackDuration());
            tablesawRow.setDouble(  MEDIAN_TRACK_DURATION,             square.getMedianTrackDuration());
        }

        return table;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TABLE → ENTITY CONVERSION
    // ───────────────────────────────────────────────────────────────────────────────

    public List<Square> toEntities(Table table) {
        List<Square> squares = new ArrayList<>();

        for (Row tablesawRow : table) {
            Square square = new Square();

            square.setUniqueKey(                     tablesawRow.getString(  UNIQUE_KEY));
            square.setExperimentName(                tablesawRow.getString(  EXPERIMENT_NAME));
            square.setRecordingName(                 tablesawRow.getString(  RECORDING_NAME));
            square.setSquareNumber(                  tablesawRow.getInt(     SQUARE_NUMBER));
            square.setRowNumber(                     tablesawRow.getInt(     ROW_NUMBER));
            square.setColNumber(                     tablesawRow.getInt(     COLUMN_NUMBER));
            square.setLabelNumber(                   tablesawRow.getInt(     LABEL_NUMBER));
            square.setCellId(                        tablesawRow.getInt(     CELL_ID));
            square.setVisible(                       tablesawRow.getBoolean( VISIBLE));
            square.setSquareManuallyExcluded(        tablesawRow.getBoolean( SQUARE_MANUALLY_EXCLUDED));
            square.setImageExcluded(                 tablesawRow.getBoolean( IMAGE_EXCLUDED));
            square.setX0(                            tablesawRow.getDouble(  X0));
            square.setY0(                            tablesawRow.getDouble(  Y0));
            square.setX1(                            tablesawRow.getDouble(  X1));
            square.setY1(                            tablesawRow.getDouble(  Y1));
            square.setNumberOfTracks(                tablesawRow.getInt(     NUMBER_OF_TRACKS));
            square.setVariability(                   tablesawRow.getDouble(  VARIABILITY));
            square.setDensity(                       tablesawRow.getDouble(  DENSITY));
            square.setDensityRatio(                  tablesawRow.getDouble(  DENSITY_RATIO));
            square.setDensityRatioOri(               tablesawRow.getDouble(  DENSITY_RATIO_ORI));
            square.setTau(                           tablesawRow.getDouble(  TAU));
            square.setRSquared(                      tablesawRow.getDouble(  R_SQUARED));
            square.setMedianDiffusionCoefficient(    tablesawRow.getDouble(  MEDIAN_DIFFUSION_COEFFICIENT));
            square.setMedianDiffusionCoefficientExt( tablesawRow.getDouble(  MEDIAN_DIFFUSION_COEFFICIENT_EXT));
            square.setMedianDisplacement(            tablesawRow.getDouble(  MEDIAN_DISPLACEMENT));
            square.setMaxDisplacement(               tablesawRow.getDouble(  MAX_DISPLACEMENT));
            square.setTotalDisplacement(             tablesawRow.getDouble(  TOTAL_DISPLACEMENT));
            square.setMedianMaxSpeed(                tablesawRow.getDouble(  MEDIAN_MAX_SPEED));
            square.setMaxMaxSpeed(                   tablesawRow.getDouble(  MAX_MAX_SPEED));
            square.setMedianMedianSpeed(             tablesawRow.getDouble(  MEDIAN_MEAN_SPEED));
            square.setMaxMedianSpeed(                tablesawRow.getDouble(  MAX_MEAN_SPEED));
            square.setMaxTrackDuration(              tablesawRow.getDouble(  MAX_TRACK_DURATION));
            square.setTotalTrackDuration(            tablesawRow.getDouble(  TOTAL_TRACK_DURATION));
            square.setMedianTrackDuration(           tablesawRow.getDouble(  MEDIAN_TRACK_DURATION));

            squares.add(square);
        }

        return squares;
    }


    /**
     * Appends all rows from a source {@link Table} into a target {@link Table},
     * enforcing the {@code squares.csv} schema.
     *
     * <p>Performs basic type handling (STRING, INTEGER, DOUBLE, BOOLEAN) and
     * preserves missing values.</p>
     *
     * @param target the destination table
     * @param source the source table to append
     */
    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : SquareSchema.COLUMNS) {

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