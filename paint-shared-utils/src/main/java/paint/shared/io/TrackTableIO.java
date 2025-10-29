/******************************************************************************
 *  Class:        TrackTableIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Provides table input/output utilities for {@link paint.shared.objects.Track}
 *    entities, handling CSV schema validation, conversion between entities
 *    and Tablesaw tables, and append operations.
 *
 *  DESCRIPTION:
 *    Defines all I/O logic for {@code tracks.csv}, using schema definitions
 *    from {@link paint.shared.constants.PaintConstants}. Each operation
 *    ensures strict type and column consistency. Supports creation of
 *    schema-compliant tables, conversion of lists of {@link Track} objects
 *    to tables, and reading or appending data with type enforcement.
 *
 *  KEY FEATURES:
 *    • Enforces consistent schema and column typing for tracks.
 *    • Converts bidirectionally between {@link Track} entities and tables.
 *    • Handles append operations with explicit type validation.
 *    • Integrates seamlessly with {@link BaseTableIO} for schema control.
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
 ******************************************************************************/

package paint.shared.io;

import paint.shared.constants.PaintConstants;
import paint.shared.objects.Track;
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
 * Provides CSV I/O and schema enforcement for {@link Track} entities.
 *
 * <p>This class encapsulates reading, writing, and conversion logic for
 * {@code tracks.csv} and guarantees alignment with the schema definitions
 * provided in {@link PaintConstants}.</p>
 */
public class TrackTableIO extends BaseTableIO {

    // ───────────────────────────────────────────────────────────────────────────────
    // TABLE CREATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates an empty {@link Table} for tracks with the correct schema.
     *
     * @return a new empty {@link Table} with all track columns defined
     */
    public Table emptyTable() {
        return newEmptyTable("Tracks", TRACKS_COLS, TRACKS_TYPES);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ENTITY ⇄ TABLE CONVERSION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Converts a list of {@link Track} objects into a {@link Table}.
     *
     * @param tracks list of {@link Track} entities to convert
     * @return a {@link Table} containing one row per track, schema validated
     */
    public Table toTable(List<Track> tracks) {
        Table table = emptyTable();
        for (Track track : tracks) {
            Row tablesawRow = table.appendRow();
            tablesawRow.setString( "Unique Key",                track.getUniqueKey());
            tablesawRow.setString( "Experiment Name",           track.getExperimentName());
            tablesawRow.setString( "Recording Name",            track.getRecordingName());
            tablesawRow.setInt(    "Track Id",                  track.getTrackId());
            tablesawRow.setInt(    "Number of Spots",           track.getNumberOfSpots());
            tablesawRow.setInt(    "Number of Gaps",            track.getNumberOfGaps());
            tablesawRow.setInt(    "Longest Gap",               track.getLongestGap());
            tablesawRow.setDouble( "Track Duration",            track.getTrackDuration());
            tablesawRow.setDouble( "Track X Location",          track.getTrackXLocation());
            tablesawRow.setDouble( "Track Y Location",          track.getTrackYLocation());
            tablesawRow.setDouble( "Track Displacement",        track.getTrackDisplacement());
            tablesawRow.setDouble( "Track Max Speed",           track.getTrackMaxSpeed());
            tablesawRow.setDouble( "Track Median Speed",        track.getTrackMedianSpeed());
            tablesawRow.setDouble( "Diffusion Coefficient",     track.getDiffusionCoefficient());
            tablesawRow.setDouble( "Diffusion Coefficient Ext", track.getDiffusionCoefficientExt());
            tablesawRow.setDouble( "Total Distance",            track.getTotalDistance());
            tablesawRow.setDouble( "Confinement Ratio",         track.getConfinementRatio());
            tablesawRow.setInt(    "Square Number",             track.getSquareNumber());
            tablesawRow.setInt(    "Label Number",              track.getLabelNumber());
        }
        return table;
    }

    /**
     * Converts a {@link Table} into a list of {@link Track} entities.
     *
     * @param table the validated {@link Table} to convert
     * @return a list of {@link Track} entities populated from the table
     */
    public List<Track> toEntities(Table table) {
        List<Track> tracks = new ArrayList<>();
        for (Row row : table) {
            Track track = new Track();
            track.setUniqueKey(              row.getString( "Unique Key"));
            track.setExperimentName(         row.getString( "Experiment Name"));
            track.setRecordingName(          row.getString( "Recording Name"));
            track.setTrackId(                row.getInt(    "Track Id"));
            track.setNumberOfSpots(          row.getInt(    "Number of Spots"));
            track.setNumberOfGaps(           row.getInt(    "Number of Gaps"));
            track.setLongestGap(             row.getInt(    "Longest Gap"));
            track.setTrackDuration(          row.getDouble( "Track Duration"));
            track.setTrackXLocation(         row.getDouble( "Track X Location"));
            track.setTrackYLocation(         row.getDouble( "Track Y Location"));
            track.setTrackDisplacement(      row.getDouble( "Track Displacement"));
            track.setTrackMaxSpeed(          row.getDouble( "Track Max Speed"));
            track.setTrackMedianSpeed(       row.getDouble( "Track Median Speed"));
            track.setDiffusionCoefficient(   row.getDouble( "Diffusion Coefficient"));
            track.setDiffusionCoefficientExt(row.getDouble( "Diffusion Coefficient Ext"));
            track.setTotalDistance(          row.getDouble( "Total Distance"));
            track.setConfinementRatio(       row.getDouble( "Confinement Ratio"));
            track.setSquareNumber(           row.getInt(    "Square Number"));
            track.setLabelNumber(            row.getInt(    "Label Number"));
            tracks.add(track);
        }
        return tracks;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CSV READ / APPEND
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Reads a CSV file into a {@link Table} using the expected track schema.
     *
     * @param csvPath path to the {@code tracks.csv} file
     * @return the parsed and validated {@link Table}
     * @throws IOException if the file cannot be read or validated
     */
    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, TRACKS, TRACKS_COLS, TRACKS_TYPES, false);
    }

    /**
     * Appends all rows from the source {@link Table} into the target {@link Table}.
     *
     * <p>This method performs manual type matching and enforces the schema
     * for all columns defined in {@code TRACKS_COLS}.</p>
     *
     * @param target the destination {@link Table}
     * @param source the source {@link Table}
     */
    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : TRACKS_COLS) {
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

    // ───────────────────────────────────────────────────────────────────────────────
    // SINGLE ROW CONVERSION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Converts a single {@link Row} into a {@link Track} entity.
     *
     * @param row the {@link Row} containing track data
     * @return a {@link Track} populated with values from the row
     */
    public Track rowToEntity(Row row) {
        Track track = new Track();
        track.setUniqueKey(                row.getString(  "Unique Key"));
        track.setExperimentName(           row.getString(  "Experiment Name"));
        track.setRecordingName(            row.getString(  "Recording Name"));
        track.setTrackId(                  row.getInt(     "Track Id"));
        track.setNumberOfSpots(            row.getInt(     "Number of Spots"));
        track.setNumberOfGaps(             row.getInt(     "Number of Gaps"));
        track.setLongestGap(               row.getInt(     "Longest Gap"));
        track.setTrackDuration(            row.getDouble(  "Track Duration"));
        track.setTrackXLocation(           row.getDouble(  "Track X Location"));
        track.setTrackYLocation(           row.getDouble(  "Track Y Location"));
        track.setTrackDisplacement(        row.getDouble(  "Track Displacement"));
        track.setTrackMaxSpeed(            row.getDouble(  "Track Max Speed"));
        track.setTrackMedianSpeed(         row.getDouble(  "Track Median Speed"));
        track.setDiffusionCoefficient(     row.getDouble(  "Diffusion Coefficient"));
        track.setDiffusionCoefficientExt(  row.getDouble(  "Diffusion Coefficient Ext"));
        track.setTotalDistance(            row.getDouble(  "Total Distance"));
        track.setConfinementRatio(         row.getDouble(  "Confinement Ratio"));
        track.setSquareNumber(             row.getInt(     "Square Number"));
        track.setLabelNumber(              row.getInt(     "Label Number"));
        return track;
    }
}