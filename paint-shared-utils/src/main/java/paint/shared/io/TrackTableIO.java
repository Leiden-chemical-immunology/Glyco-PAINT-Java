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
        for (Track tr : tracks) {
            Row row = table.appendRow();
            row.setString("Unique Key", tr.getUniqueKey());
            row.setString("Recording Name", tr.getRecordingName());
            row.setInt("Track Id", tr.getTrackId());
            row.setString("Track Label", tr.getTrackLabel());
            row.setInt("Number of Spots", tr.getNumberOfSpots());
            row.setInt("Number of Gaps", tr.getNumberOfGaps());
            row.setInt("Longest Gap", tr.getLongestGap());
            row.setDouble("Track Duration", tr.getTrackDuration());
            row.setDouble("Track X Location", tr.getTrackXLocation());
            row.setDouble("Track Y Location", tr.getTrackYLocation());
            row.setDouble("Track Displacement", tr.getTrackDisplacement());
            row.setDouble("Track Max Speed", tr.getTrackMaxSpeed());
            row.setDouble("Track Median Speed", tr.getTrackMedianSpeed());
            row.setDouble("Diffusion Coefficient", tr.getDiffusionCoefficient());
            row.setDouble("Diffusion Coefficient Ext", tr.getDiffusionCoefficientExt());
            row.setDouble("Total Distance", tr.getTotalDistance());
            row.setDouble("Confinement Ratio", tr.getConfinementRatio());
            row.setInt("Square Number", tr.getSquareNumber());
            row.setInt("Label Number", tr.getLabelNumber());
        }
        return table;
    }

    public List<Track> toEntities(Table table) {
        List<Track> tracks = new ArrayList<>();
        for (Row row : table) {
            Track tr = new Track();
            tr.setUniqueKey(row.getString("Unique Key"));
            tr.setRecordingName(row.getString("Recording Name"));
            tr.setTrackId(row.getInt("Track Id"));
            tr.setTrackLabel(row.getString("Track Label"));
            tr.setNumberOfSpots(row.getInt("Number of Spots"));
            tr.setNumberOfGaps(row.getInt("Number of Gaps"));
            tr.setLongestGap(row.getInt("Longest Gap"));
            tr.setTrackDuration(row.getDouble("Track Duration"));
            tr.setTrackXLocation(row.getDouble("Track X Location"));
            tr.setTrackYLocation(row.getDouble("Track Y Location"));
            tr.setTrackDisplacement(row.getDouble("Track Displacement"));
            tr.setTrackMaxSpeed(row.getDouble("Track Max Speed"));
            tr.setTrackMedianSpeed(row.getDouble("Track Median Speed"));
            tr.setDiffusionCoefficient(row.getDouble("Diffusion Coefficient"));
            tr.setDiffusionCoefficientExt(row.getDouble("Diffusion Coefficient Ext"));
            tr.setTotalDistance(row.getDouble("Total Distance"));
            tr.setConfinementRatio(row.getDouble("Confinement Ratio"));
            tr.setSquareNumber(row.getInt("Square Number"));
            tr.setLabelNumber(row.getInt("Label Number"));
            tracks.add(tr);
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