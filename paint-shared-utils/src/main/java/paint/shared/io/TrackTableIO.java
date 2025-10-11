package paint.shared.io;

import paint.shared.objects.Track;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintConstants.TRACK_COLS;
import static paint.shared.constants.PaintConstants.TRACK_TYPES;

/**
 * Table IO for Track entities.
 */
public class TrackTableIO extends BaseTableIO {

    public Table emptyTable() {
        return newEmptyTable("Tracks", TRACK_COLS, TRACK_TYPES);
    }

    public Table toTable(List<Track> tracks) {
        Table table = emptyTable();
        for (Track track : tracks) {
            Row row = table.appendRow();

            // @formatter:off
            row.setString("Unique Key",                track.getUniqueKey());
            row.setString("Recording Name",            track.getRecordingName());
            row.setInt(   "Track Id",                  track.getTrackId());
            row.setString("Track Label",               track.getTrackLabel());
            row.setInt(   "Number of Spots",           track.getNumberOfSpots());
            row.setInt(   "Number of Gaps",            track.getNumberOfGaps());
            row.setInt(   "Longest Gap",               track.getLongestGap());
            row.setDouble("Track Duration",            track.getTrackDuration());
            row.setDouble("Track X Location",          track.getTrackXLocation());
            row.setDouble("Track Y Location",          track.getTrackYLocation());
            row.setDouble("Track Displacement",        track.getTrackDisplacement());
            row.setDouble("Track Max Speed",           track.getTrackMaxSpeed());
            row.setDouble("Track Median Speed",        track.getTrackMedianSpeed());
            row.setDouble("Diffusion Coefficient",     track.getDiffusionCoefficient());
            row.setDouble("Diffusion Coefficient Ext", track.getDiffusionCoefficientExt());
            row.setDouble("Total Distance",            track.getTotalDistance());
            row.setDouble("Confinement Ratio",         track.getConfinementRatio());
            row.setInt(   "Square Number",             track.getSquareNumber());
            row.setInt(   "Label Number",              track.getLabelNumber());
            // @formatter:on

        }
        return table;
    }

    public List<Track> toEntities(Table table) {
        List<Track> tracks = new ArrayList<>();
        for (Row row : table) {
            Track track = new Track();

            // @formatter:off
            track.setUniqueKey(              row.getString( "Unique Key"));
            track.setRecordingName(          row.getString( "Recording Name"));
            track.setTrackId(                row.getInt(    "Track Id"));
            track.setTrackLabel(             row.getString( "Track Label"));
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
            track.setConfinementRatio(       row.getDouble("Confinement Ratio"));
            track.setSquareNumber(           row.getInt(    "Square Number"));
            track.setLabelNumber(            row.getInt(    "Label Number"));
            // @formatter:on

            tracks.add(track);
        }
        return tracks;
    }

    public Table readCsv(Path csvPath) throws IOException {
        return readCsvWithSchema(csvPath, "Tracks", TRACK_COLS, TRACK_TYPES, false);
    }

    public void appendInPlace(Table target, Table source) {
        for (Row row : source) {
            Row newRow = target.appendRow();
            for (String col : TRACK_COLS) {
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

    /**
     * Convert a single row into a Track.
     */
    public Track rowToEntity(Row row) {
        Track track = new Track();

        // Adapt column names to match your CSV/table headers
        track.setUniqueKey(row.getString("Unique Key"));
        track.setRecordingName(row.getString("Recording Name"));
        track.setTrackId(row.getInt("Track ID"));
        track.setTrackLabel(row.getString("Track Label"));
        track.setNumberOfSpots(row.getInt("Number of Spots"));
        track.setNumberOfGaps(row.getInt("Number of Gaps"));
        track.setLongestGap(row.getInt("Longest Gap"));
        track.setTrackDuration(row.getDouble("Track Duration"));
        track.setTrackXLocation(row.getDouble("Track X Location"));
        track.setTrackYLocation(row.getDouble("Track Y Location"));
        track.setTrackDisplacement(row.getDouble("Track Displacement"));
        track.setTrackMaxSpeed(row.getDouble("Track Max Speed"));
        track.setTrackMedianSpeed(row.getDouble("Track Median Speed"));
        track.setDiffusionCoefficient(row.getDouble("Diffusion Coefficient"));
        track.setDiffusionCoefficientExt(row.getDouble("Diffusion Coefficient Ext"));
        track.setTotalDistance(row.getDouble("Total Distance"));
        track.setConfinementRatio(row.getDouble("Confinement Ratio"));
        track.setSquareNumber(row.getInt("Square Number"));
        track.setLabelNumber(row.getInt("Label Number"));

        return track;
    }
}