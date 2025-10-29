/******************************************************************************
 *  Class:        RecordingTableIO.java
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
 *    in {@link paint.shared.constants.PaintConstants#RECORDINGS_COLS} and
 *    {@link paint.shared.constants.PaintConstants#RECORDINGS_TYPES}.
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
 ******************************************************************************/

package paint.shared.io;

import paint.shared.objects.Recording;
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
 * Provides table I/O utilities for {@link Recording} entities.
 *
 * <p>This class manages reading, writing, and schema enforcement for
 * {@code recordings.csv} files. Each method ensures full consistency
 * with the column definitions in PaintConstants.</p>
 */
public class RecordingTableIO extends BaseTableIO {

    // ───────────────────────────────────────────────────────────────────────────────
    // TABLE CREATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates an empty {@link Table} for recordings with the full schema applied.
     *
     * @return a new empty {@code Table} with the “Recordings” schema
     */
    public Table emptyTable() {
        return newEmptyTable("Recordings", RECORDINGS_COLS, RECORDINGS_TYPES);
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
            row.setString(  "Experiment Name",                   recording.getExperimentName());
            row.setString(  "Recording Name",                    recording.getRecordingName());
            row.setInt(     "Condition Number",                  recording.getConditionNumber());
            row.setInt(     "Replicate Number",                  recording.getReplicateNumber());
            row.setString(  "Probe Name",                        recording.getProbeName());
            row.setString(  "Probe Type",                        recording.getProbeType());
            row.setString(  "Cell Type",                         recording.getCellType());
            row.setString(  "Adjuvant",                          recording.getAdjuvant());
            row.setDouble(  "Concentration",                     recording.getConcentration());
            row.setBoolean( "Process Flag",                      recording.isProcessFlag());
            row.setDouble(  "Threshold",                         recording.getThreshold());
            row.setInt(     "Number of Spots",                   recording.getNumberOfSpots());
            row.setInt(     "Number of Tracks",                  recording.getNumberOfTracks());
            row.setInt(     "Number of Squares in Background",   recording.getNumberOfSquaresInBackground());
            row.setInt(     "Number of Tracks in Background",    recording.getNumberOfTracksInBackground());
            row.setDouble(  "Average Tracks in Background",      recording.getAverageTracksInBackGround());
            row.setInt(     "Number of Spots in All Tracks",     recording.getNumberOfSpotsInAllTracks());
            row.setInt(     "Number of Frames",                  recording.getNumberOfFrames());
            row.setDouble(  "Run Time",                          recording.getRunTime());
            row.setDateTime("Time Stamp",                        recording.getTimeStamp());
            row.setBoolean( "Exclude",                           recording.isExclude());
            row.setDouble(  "Tau",                               recording.getTau());
            row.setDouble(  "R Squared",                         recording.getRSquared());
            row.setDouble(  "Density",                           recording.getDensity());
        }
        return table;
    }

    /**
     * Converts a {@link Table} into a list of {@link Recording} entities.
     *
     * <p>The table must conform to the schema defined by
     * {@code RECORDINGS_COLS} and {@code RECORDINGS_TYPES}.
     * Each row is mapped one-to-one to a {@code Recording} object.</p>
     *
     * @param table the validated {@link Table} to convert
     * @return a list of {@code Recording} entities populated from the table
     */
    public List<Recording> toEntities(Table table) {
        List<Recording> recordings = new ArrayList<>();
        for (Row tablesawRow : table) {
            Recording recording = new Recording();
            recording.setExperimentName(              tablesawRow.getString(   "Experiment Name"));
            recording.setRecordingName(               tablesawRow.getString(   "Recording Name"));
            recording.setConditionNumber(             tablesawRow.getInt(      "Condition Number"));
            recording.setReplicateNumber(             tablesawRow.getInt(      "Replicate Number"));
            recording.setProbeName(                   tablesawRow.getString(   "Probe Name"));
            recording.setProbeType(                   tablesawRow.getString(   "Probe Type"));
            recording.setCellType(                    tablesawRow.getString(   "Cell Type"));
            recording.setAdjuvant(                    tablesawRow.getString(   "Adjuvant"));
            recording.setConcentration(               tablesawRow.getDouble(   "Concentration"));
            recording.setProcessFlag(                 tablesawRow.getBoolean(  "Process Flag"));
            recording.setThreshold(                   tablesawRow.getDouble(   "Threshold"));
            recording.setNumberOfSpots(               tablesawRow.getInt(      "Number of Spots"));
            recording.setNumberOfTracks(              tablesawRow.getInt(      "Number of Tracks"));
            recording.setNumberOfSquaresInBackground( tablesawRow.getInt(      "Number of Squares in Background"));
            recording.setNumberOfTracksInBackground(  tablesawRow.getInt(      "Number of Tracks in Background"));
            recording.setAverageTracksInBackGround(   tablesawRow.getDouble(   "Average Tracks in Background"));
            recording.setNumberOfSpotsInAllTracks(    tablesawRow.getInt(      "Number of Spots in All Tracks"));
            recording.setNumberOfFrames(              tablesawRow.getInt(      "Number of Frames"));
            recording.setRunTime(                     tablesawRow.getDouble(   "Run Time"));
            recording.setTimeStamp(                   tablesawRow.getDateTime( "Time Stamp"));
            recording.setExclude(                     tablesawRow.getBoolean(  "Exclude"));
            recording.setTau(                         tablesawRow.getDouble(   "Tau"));
            recording.setRSquared(                    tablesawRow.getDouble(   "R Squared"));
            recording.setDensity(                     tablesawRow.getDouble(   "Density"));
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
        return readCsvWithSchema(csvPath, RECORDINGS, RECORDINGS_COLS, RECORDINGS_TYPES, false);
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

            for (String col : RECORDINGS_COLS) {
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