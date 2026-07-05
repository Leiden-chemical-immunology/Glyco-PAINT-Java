/*=============================================================================
 *  Class:        TracksTableIO.java
 *  Package:      paint.shared.io.internal
 *
 *  PURPOSE:
 *    Public-but-internal implementation of CSV and table I/O for
 *    {@link paint.shared.objects.Track} entities. Although declared public so
 *    that {@link paint.shared.io.MainDataInterface} can access it across
 *    package boundaries, this class is NOT part of PAINT’s public API and must
 *    not be used directly by external modules.
 *
 *  DESCRIPTION:
 *    Handles all low-level operations for the “tracks.csv” data layer:
 *
 *       • Creating schema-compliant Tablesaw tables
 *       • Converting {@link Track} ↔ Tablesaw rows
 *       • Reading CSV files with strict schema validation (via BaseTableIO)
 *       • Performing safe, schema-aware append operations
 *
 *    All external callers must use {@link MainDataInterface}, which exposes the
 *    official high-level API for Track reading and writing.
 *
 *  DESIGN NOTES:
 *    • Visibility is public only because package-private classes in
 *      paint.shared.io.internal cannot be accessed from MainDataInterface.
 *    • Despite being public, this class is considered internal API.
 *    • All column names, order, and types are defined by {@link TrackSchema}.
 *    • Fully compatible with Java 8 and Tablesaw 0.43+.
 *
 *  AUTHOR:       Hans Bakker
 *  MODULE:       paint-shared-utils
 *  UPDATED:      2025-10-28
 *  COPYRIGHT:    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package paint.shared.io.internal;

import static paint.shared.constants.PaintStringConstants.*;

import paint.shared.io.MainIOInterface;
import paint.shared.objects.Track;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal schema-validated table I/O implementation for {@link Track}.
 *
 * <p>This class supports CSV reading, strict schema enforcement, entity-row
 * conversion, and table append operations. External callers must use
 * {@link MainIOInterface}.</p>
 */
public class TracksTableIO extends BaseTableIO {

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    /**
     * @return a new empty {@link Table} with the Tracks schema.
     */
    public Table emptyTable() {
        return newEmptyTable("Tracks", Track.Column.values(), c -> c.header, c -> c.type);
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    /**
     * Converts a list of {@link Track} entities into a Tablesaw {@link Table}.
     *
     * @param tracks list of track objects to convert
     * @return a table containing the track data
     */
    public Table toTable(List<Track> tracks) {
        Table table = emptyTable();

        for (Track track : tracks) {
            Row tablesawRow = table.appendRow();

            tablesawRow.setString( UNIQUE_KEY,                track.getUniqueKey());
            tablesawRow.setString( EXPERIMENT_NAME,           track.getExperimentName());
            tablesawRow.setString( RECORDING_NAME,            track.getRecordingName());
            tablesawRow.setInt(    TRACK_ID,                  track.getTrackId());
            tablesawRow.setInt(    NUMBER_OF_SPOTS,           track.getNumberOfSpots());
            tablesawRow.setInt(    NUMBER_OF_GAPS,            track.getNumberOfGaps());
            tablesawRow.setInt(    LONGEST_GAP,               track.getLongestGap());
            tablesawRow.setDouble( TRACK_DURATION,            track.getTrackDuration());
            tablesawRow.setDouble( TRACK_X_LOCATION,          track.getTrackXLocation());
            tablesawRow.setDouble( TRACK_Y_LOCATION,          track.getTrackYLocation());
            tablesawRow.setDouble( TRACK_DISPLACEMENT,        track.getTrackDisplacement());
            tablesawRow.setDouble( TRACK_MAX_SPEED,           track.getTrackMaxSpeed());
            tablesawRow.setDouble( TRACK_MEDIAN_SPEED,        track.getTrackMedianSpeed());
            tablesawRow.setDouble( DIFFUSION_COEFFICIENT,     track.getDiffusionCoefficient());
            tablesawRow.setDouble( DIFFUSION_COEFFICIENT_EXT, track.getDiffusionCoefficientExt());
            tablesawRow.setDouble( TOTAL_DISTANCE,            track.getTotalDistance());
            tablesawRow.setDouble( CONFINEMENT_RATIO,         track.getConfinementRatio());
            tablesawRow.setInt(    SQUARE_NUMBER,             track.getSquareNumber());
            tablesawRow.setInt(    LABEL_NUMBER,              track.getLabelNumber());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    /**
     * Converts a validated Tracks {@link Table} into a list of {@link Track} entities.
     *
     * @param table a schema-validated table
     * @return list of Track objects
     */
    public List<Track> toEntities(Table table) {
        List<Track> tracks = new ArrayList<>();

        for (Row row : table) {
            Track track = new Track();

            track.setUniqueKey(              row.getString( UNIQUE_KEY));
            track.setExperimentName(         row.getString( EXPERIMENT_NAME));
            track.setRecordingName(          row.getString( RECORDING_NAME));
            track.setTrackId(                row.getInt(    TRACK_ID));
            track.setNumberOfSpots(          row.getInt(    NUMBER_OF_SPOTS));
            track.setNumberOfGaps(           row.getInt(    NUMBER_OF_GAPS));
            track.setLongestGap(             row.getInt(    LONGEST_GAP));
            track.setTrackDuration(          row.getDouble( TRACK_DURATION));
            track.setTrackXLocation(         row.getDouble( TRACK_X_LOCATION));
            track.setTrackYLocation(         row.getDouble( TRACK_Y_LOCATION));
            track.setTrackDisplacement(      row.getDouble( TRACK_DISPLACEMENT));
            track.setTrackMaxSpeed(          row.getDouble( TRACK_MAX_SPEED));
            track.setTrackMedianSpeed(       row.getDouble( TRACK_MEDIAN_SPEED));
            track.setDiffusionCoefficient(   row.getDouble( DIFFUSION_COEFFICIENT));
            track.setDiffusionCoefficientExt(row.getDouble( DIFFUSION_COEFFICIENT_EXT));
            track.setTotalDistance(          row.getDouble( TOTAL_DISTANCE));
            track.setConfinementRatio(       row.getDouble( CONFINEMENT_RATIO));
            track.setSquareNumber(           row.getInt(    SQUARE_NUMBER));
            track.setLabelNumber(            row.getInt(    LABEL_NUMBER));

            tracks.add(track);
        }

        return tracks;
    }

    // =====================================================================
    //  APPEND / MERGE
    // =====================================================================

    /**
     * Appends all rows from {@code source} into {@code target} while enforcing
     * the Tracks schema and preserving missing values.
     *
     * @param target the destination table
     * @param source the source table
     */
    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();

            for (Track.Column colEnum : Track.Column.values()) {
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