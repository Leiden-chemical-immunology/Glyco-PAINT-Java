/*=============================================================================
 *  Class:        RecordingsTableIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Provides table input/output utilities for {@link paint.shared.objects.Recording}
 *    entities, handling CSV schema validation, conversion between entity lists
 *    and Tablesaw tables, and controlled append operations.
 *
 *  DESCRIPTION:
 *    This class defines all I/O behavior related to {@code recordings.csv}.
 *    It leverages {@link BaseTableIO} for schema validation and ensures that
 *    every read or write operation adheres strictly to the schema specified
 *    in {@link paint.shared.schema.RecordingSchema#COLUMNS} and
 *    {@link paint.shared.schema.RecordingSchema#TYPES}.
 *
 *    It supports:
 *      • Creating empty tables with the correct schema.
 *      • Converting between Recording objects and Tablesaw tables.
 *      • Reading schema-validated CSV files into tables.
 *      • Appending tables with schema-validated coercion.
 *
 *  KEY FEATURES:
 *    • Enforces consistent column names, order, and data types.
 *    • Handles missing values gracefully during append operations.
 *    • Provides strong typing for table-to-entity conversion.
 *    • Compatible with Java 8 and Tablesaw 0.43+.
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

import paint.shared.objects.Recording;
import paint.shared.schema.RecordingSchema;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintFileNames.RECORDINGS_CSV;
import static paint.shared.constants.PaintColumnNames.*;

/**
 * Provides table I/O utilities for {@link Recording} entities.
 *
 * <p>This class manages reading, writing, and schema enforcement for
 * {@code recordings.csv} files. Each method ensures full consistency
 * with the column definitions in {@link paint.shared.schema.RecordingSchema}.</p>
 */
public class RecordingsTableIO extends BaseTableIO {

    // ───────────────────────────────────────────────────────────────────────────────
    // TABLE CREATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates an empty {@link Table} for recordings with the full schema applied.
     *
     * @return a new empty {@code Table} with the “Recordings” schema
     */
    public Table emptyTable() {
        return newEmptyTable("Recordings", RecordingSchema.COLUMNS, RecordingSchema.TYPES);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ENTITY ⇄ TABLE CONVERSION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Converts a list of {@link Recording} entities into a fully typed
     * {@link Table} matching the {@code recordings.csv} schema.
     *
     * @param recordings the list of {@code Recording} objects to convert
     * @return a {@code Table} populated with recording data
     */
    public Table toTable(List<Recording> recordings) {
        Table table = emptyTable();
        for (Recording recording : recordings) {
            Row row = table.appendRow();
            row.setString(  EXPERIMENT_NAME,                 recording.getExperimentName());
            row.setString(  RECORDING_NAME,                  recording.getRecordingName());
            row.setInt(     CONDITION_NUMBER,                recording.getConditionNumber());
            row.setInt(     REPLICATE_NUMBER,                recording.getReplicateNumber());
            row.setString(  PROBE_NAME,                      recording.getProbeName());
            row.setString(  PROBE_TYPE,                      recording.getProbeType());
            row.setString(  CELL_TYPE,                       recording.getCellType());
            row.setString(  ADJUVANT,                        recording.getAdjuvant());
            row.setDouble(  CONCENTRATION,                   recording.getConcentration());
            row.setBoolean( PROCESS_FLAG,                    recording.isProcessFlag());
            row.setDouble(  THRESHOLD,                       recording.getThreshold());
            row.setInt(     NUMBER_OF_SPOTS,                 recording.getNumberOfSpots());
            row.setInt(     NUMBER_OF_TRACKS,                recording.getNumberOfTracks());
            row.setInt(     NUMBER_OF_SQUARES_IN_BACKGROUND, recording.getNumberOfSquaresInBackground());
            row.setInt(     NUMBER_OF_TRACKS_IN_BACKGROUND,  recording.getNumberOfTracksInBackground());
            row.setDouble(  AVERAGE_TRACKS_IN_BACKGROUND,    recording.getAverageTracksInBackGround());
            row.setInt(     NUMBER_OF_SPOTS_IN_ALL_TRACKS,   recording.getNumberOfSpotsInAllTracks());
            row.setInt(     NUMBER_OF_FRAMES,                recording.getNumberOfFrames());
            row.setDouble(  RUN_TIME,                        recording.getRunTime());
            row.setDateTime(TIME_STAMP,                      recording.getTimeStamp());
            row.setBoolean( EXCLUDE,                         recording.isExclude());
            row.setDouble(  TAU,                             recording.getTau());
            row.setDouble(  R_SQUARED,                       recording.getRSquared());
            row.setDouble(  DENSITY,                         recording.getDensity());
            row.setDouble(  MIN_REQUIRED_DENSITY_RATIO,      recording.getMinRequiredDensityRatio());
            row.setDouble(  MIN_REQUIRED_R_SQUARED,          recording.getMinRequiredRSquared());
            row.setDouble(  MAX_ALLOWABLE_VARIABILITY,       recording.getMaxAllowableVariability());
            row.setString(  NEIGHBOUR_MODE,                  recording.getNeighbourMode());
        }
        return table;
    }

    /**
     * Converts a {@link Table} into a list of {@link Recording} entities.
     *
     * <p>The table must conform to the schema defined by
     * {@code RecordingSchema.COLUMNS} and {@code RecordingSchema.TYPES}.
     * Each row is mapped one-to-one to a {@code Recording} object.</p>
     *
     * @param table the validated {@link Table} to convert
     * @return a list of {@code Recording} entities populated from the table
     */
    public List<Recording> toEntities(Table table) {
        List<Recording> recordings = new ArrayList<>();
        for (Row tablesawRow : table) {
            Recording recording = new Recording();
            recording.setExperimentName(              tablesawRow.getString(   EXPERIMENT_NAME));
            recording.setRecordingName(               tablesawRow.getString(   RECORDING_NAME));
            recording.setConditionNumber(             tablesawRow.getInt(      CONDITION_NUMBER));
            recording.setReplicateNumber(             tablesawRow.getInt(      REPLICATE_NUMBER));
            recording.setProbeName(                   tablesawRow.getString(   PROBE_NAME));
            recording.setProbeType(                   tablesawRow.getString(   PROBE_TYPE));
            recording.setCellType(                    tablesawRow.getString(   CELL_TYPE));
            recording.setAdjuvant(                    tablesawRow.getString(   ADJUVANT));
            recording.setConcentration(               tablesawRow.getDouble(   CONCENTRATION));
            recording.setProcessFlag(                 tablesawRow.getBoolean(  PROCESS_FLAG));
            recording.setThreshold(                   tablesawRow.getDouble(   THRESHOLD));
            recording.setNumberOfSpots(               tablesawRow.getInt(      NUMBER_OF_SPOTS));
            recording.setNumberOfTracks(              tablesawRow.getInt(      NUMBER_OF_TRACKS));
            recording.setNumberOfSquaresInBackground( tablesawRow.getInt(      NUMBER_OF_SQUARES_IN_BACKGROUND));
            recording.setNumberOfTracksInBackground(  tablesawRow.getInt(      NUMBER_OF_TRACKS_IN_BACKGROUND));
            recording.setAverageTracksInBackGround(   tablesawRow.getDouble(   AVERAGE_TRACKS_IN_BACKGROUND));
            recording.setNumberOfSpotsInAllTracks(    tablesawRow.getInt(      NUMBER_OF_SPOTS_IN_ALL_TRACKS));
            recording.setNumberOfFrames(              tablesawRow.getInt(      NUMBER_OF_FRAMES));
            recording.setRunTime(                     tablesawRow.getDouble(   RUN_TIME));
            recording.setTimeStamp(                   tablesawRow.getDateTime( TIME_STAMP));
            recording.setExclude(                     tablesawRow.getBoolean(  EXCLUDE));
            recording.setTau(                         tablesawRow.getDouble(   TAU));
            recording.setRSquared(                    tablesawRow.getDouble(   R_SQUARED));
            recording.setDensity(                     tablesawRow.getDouble(   DENSITY));

            recording.setMinRequiredDensityRatio(     tablesawRow.getDouble(   MIN_REQUIRED_DENSITY_RATIO));
            recording.setMinRequiredRSquared(         tablesawRow.getDouble(   MIN_REQUIRED_R_SQUARED));
            recording.setMaxAllowableVariability(     tablesawRow.getDouble(   MAX_ALLOWABLE_VARIABILITY));
            recording.setNeighbourMode(               tablesawRow.getString(   NEIGHBOUR_MODE));
            recordings.add(recording);
        }
        return recordings;
    }
    // ───────────────────────────────────────────────────────────────────────────────
    // CSV READ / APPEND
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Reads a CSV file containing recording data into a validated {@link Table}.
     *
     * @param csvPath path to the {@code recordings.csv} file
     * @return a {@link Table} conforming to the recordings schema
     * @throws IOException if the file cannot be read or parsed
     */
    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, RECORDINGS_CSV, RecordingSchema.COLUMNS, RecordingSchema.TYPES, false);
    }

    /**
     * Appends all rows from a source {@link Table} into a target {@link Table},
     * enforcing the recordings schema.
     *
     * <p>Behavior:</p>
     * <ul>
     *   <li>All columns are appended in schema order.</li>
     *   <li>Supports basic type coercion (INTEGER → DOUBLE).</li>
     *   <li>Missing values are preserved as missing.</li>
     *   <li>Both tables must share the same schema.</li>
     * </ul>
     *
     * @param target the destination table
     * @param source the source table to append from
     */
    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : RecordingSchema.COLUMNS) {
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