/*=============================================================================
 *  Class:        SquaresTableIO.java
 *  Package:      paint.shared.io.internal
 *
 *  PURPOSE:
 *    Public-but-internal implementation of CSV and table I/O for
 *    {@link paint.shared.objects.Square} entities. Although declared public so
 *    that {@link paint.shared.io.MainDataInterface} may access it across
 *    package boundaries, this class is NOT part of PAINT’s public API and must
 *    not be referenced directly by external modules.
 *
 *  DESCRIPTION:
 *    Provides all low-level functionality for the squares data layer:
 *
 *       • Creating schema-compliant Tablesaw tables
 *       • Converting {@link Square} ↔ Tablesaw rows
 *       • Reading CSV files with strict schema validation (via BaseTableIO)
 *       • Performing schema-aware append operations with safe type handling
 *
 *    The only supported public entry point for square I/O is
 *    {@link paint.shared.io.MainDataInterface}.  This class is an internal
 *    implementation detail despite being declared public.
 *
 *  DESIGN NOTES:
 *    • Visibility is public only because package-private classes in
 *      paint.shared.io.internal cannot be accessed by MainDataInterface.
 *    • All column names, order, and data types come from {@link SquareSchema}.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
 *
 *  AUTHOR:       Hans Bakker
 *  MODULE:       paint-shared-utils
 *  UPDATED:      2025-10-28
 *  COPYRIGHT:    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.io.internal;

import static paint.shared.constants.PaintStringConstants.*;

import paint.shared.io.MainIOInterface;
import paint.shared.objects.Square;
import paint.shared.schema.SquareSchema;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal schema-validated table I/O implementation for {@link Square}.
 *
 * <p>This class supports CSV reading, schema enforcement, entity-row conversion,
 * and schema-aware append operations.  All external callers must go through
 * {@link MainIOInterface}.</p>
 */
public class SquaresTableIO extends BaseTableIO {

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    /**
     * Creates a new empty table with the full Squares schema.
     *
     * @return a schema-compliant empty {@link Table}
     */
    public Table emptyTable() {
        return newEmptyTable("Squares",
                             SquareSchema.COLUMNS,
                             SquareSchema.TYPES);
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    /**
     * Converts a list of {@link Square} entities into a schema-validated table.
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
            tablesawRow.setDouble(  MEDIAN_MEDIAN_SPEED,               square.getMedianMedianSpeed());
            tablesawRow.setDouble(  MAX_MEDIAN_SPEED,                  square.getMaxMedianSpeed());
            tablesawRow.setDouble(  MAX_TRACK_DURATION,                square.getMaxTrackDuration());
            tablesawRow.setDouble(  TOTAL_TRACK_DURATION,              square.getTotalTrackDuration());
            tablesawRow.setDouble(  MEDIAN_TRACK_DURATION,             square.getMedianTrackDuration());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    /**
     * Converts a validated Squares table into a list of {@link Square} entities.
     */
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
            square.setMedianMedianSpeed(             tablesawRow.getDouble(  MEDIAN_MEDIAN_SPEED));
            square.setMaxMedianSpeed(                tablesawRow.getDouble(  MAX_MEDIAN_SPEED));
            square.setMaxTrackDuration(              tablesawRow.getDouble(  MAX_TRACK_DURATION));
            square.setTotalTrackDuration(            tablesawRow.getDouble(  TOTAL_TRACK_DURATION));
            square.setMedianTrackDuration(           tablesawRow.getDouble(  MEDIAN_TRACK_DURATION));

            squares.add(square);
        }

        return squares;
    }

    // =====================================================================
    //  APPEND / MERGE
    // =====================================================================

    /**
     * Appends all rows from {@code source} into {@code target} while enforcing
     * the Squares schema and preserving missing values.
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