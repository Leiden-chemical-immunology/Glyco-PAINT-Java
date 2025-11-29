/*=============================================================================
 *  Class:        RecordingsTableIO.java
 *  Package:      paint.shared.io.internal
 *
 *  PURPOSE:
 *    Public-but-internal implementation of CSV and table I/O for
 *    {@link paint.shared.objects.Recording} entities. Although declared public
 *    so it can be used by {@link paint.shared.io.MainDataInterface}, this class
 *    is NOT part of the public API and must never be referenced directly by
 *    external modules.
 *
 *  DESCRIPTION:
 *    Provides all low-level logic for the recordings data layer:
 *
 *      • Creating schema-compliant Tablesaw tables
 *      • Converting {@link Recording} entities ↔ Tablesaw rows
 *      • Reading CSV files with strict header and type enforcement (via BaseTableIO)
 *      • Performing schema-aware append operations with safe type handling
 *
 *    The ONLY supported entry point for recordings I/O is
 *    {@link MainDataInterface}. This class is an internal implementation detail.
 *
 *  DESIGN NOTES:
 *    • Visibility is public ONLY because package-private classes inside
 *      'paint.shared.io.internal' cannot be accessed from
 *      'paint.shared.io.MainDataInterface'.
 *    • Despite being public, this class is treated as internal API.
 *    • All schema definitions come from {@link paint.shared.schema.RecordingSchema}.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
 *
 *  AUTHOR:       Hans Bakker
 *  MODULE:       paint-shared-utils
 *  UPDATED:      2025-10-28
 *  COPYRIGHT:    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.io.internal;

import paint.shared.io.MainIOInterface;
import paint.shared.objects.Recording;
import paint.shared.schema.RecordingSchema;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * Internal schema-validated table I/O implementation for {@link Recording}.
 *
 * <p>This class handles CSV reading, conversion, table creation, and safe
 * append operations for {@code recordings.csv}. All schema definitions are
 * taken from {@link RecordingSchema}.</p>
 *
 * <p>External callers must use {@link MainIOInterface}.</p>
 */
public class RecordingsTableIO extends BaseTableIO {

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    /**
     * Creates a new empty table with the complete Recordings schema.
     *
     * @return a schema-compliant empty {@link Table}
     */
    public Table emptyTable() {
        return newEmptyTable("Recordings",
                             RecordingSchema.COLUMNS,
                             RecordingSchema.TYPES);
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    /**
     * Converts a list of {@link Recording} entities into a schema-validated table.
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
            row.setBoolean( PROCESS_FLAG,                    recording.isProcessFlagSet());
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

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    /**
     * Converts a validated recording table into a list of {@link Recording} entities.
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

    // =====================================================================
    //  APPEND / MERGE
    // =====================================================================

    /**
     * Appends all rows from {@code source} into {@code target} while enforcing
     * the Recordings schema and preserving missing values.
     *
     * <p>Supports INTEGER → DOUBLE upcasting where needed.</p>
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