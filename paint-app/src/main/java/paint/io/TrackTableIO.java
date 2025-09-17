package paint.io;

import objects.Track;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;


import static constants.PaintConstants.TRACK_COLS;

public class TrackTableIO extends BaseTableIO<Track> {

    public TrackTableIO() {
        super(new TrackAdapter());
    }

    /** All Track-specific mapping lives here. */
    private static class TrackAdapter implements EntityTableAdapter<Track> {

        public String tableName() {
            return "tracks";
        }

        public String[] columns() {
            return TRACK_COLS;
        }

        public ColumnType[] columnTypes() {
            // Map 1:1 to TRACK_COLS (adjust if your Track getters differ)
            return new ColumnType[] {
                    ColumnType.STRING,   // uniqueKey
                    ColumnType.STRING,   // recordingName
                    ColumnType.INTEGER,  // trackId
                    ColumnType.STRING,   // trackLabel
                    ColumnType.INTEGER,  // numberSpots
                    ColumnType.INTEGER,  // numberGaps
                    ColumnType.INTEGER,  // longestGap
                    ColumnType.DOUBLE,   // trackDuration
                    ColumnType.DOUBLE,   // trackXLocation
                    ColumnType.DOUBLE,   // trackYLocation
                    ColumnType.DOUBLE,   // trackDisplacement
                    ColumnType.DOUBLE,   // trackMaxSpeed
                    ColumnType.DOUBLE,   // trackMedianSpeed
                    ColumnType.DOUBLE,   // diffusionCoefficient
                    ColumnType.DOUBLE,   // diffusionCoefficientExt
                    ColumnType.DOUBLE,   // totalDistance
                    ColumnType.DOUBLE,   // confinementRatio
                    ColumnType.INTEGER,  // squareNumber
                    ColumnType.INTEGER   // labelNumber
            };
        }

        public List<Column<?>> newEmptyColumns() {
            List<Column<?>> cols = new ArrayList<Column<?>>(TRACK_COLS.length);
            cols.add(StringColumn.create(TRACK_COLS[0]));
            cols.add(StringColumn.create(TRACK_COLS[1]));
            cols.add(IntColumn.create   (TRACK_COLS[2]));
            cols.add(StringColumn.create(TRACK_COLS[3]));
            cols.add(IntColumn.create   (TRACK_COLS[4]));
            cols.add(IntColumn.create   (TRACK_COLS[5]));
            cols.add(IntColumn.create   (TRACK_COLS[6]));
            cols.add(DoubleColumn.create(TRACK_COLS[7]));
            cols.add(DoubleColumn.create(TRACK_COLS[8]));
            cols.add(DoubleColumn.create(TRACK_COLS[9]));
            cols.add(DoubleColumn.create(TRACK_COLS[10]));
            cols.add(DoubleColumn.create(TRACK_COLS[11]));
            cols.add(DoubleColumn.create(TRACK_COLS[12]));
            cols.add(DoubleColumn.create(TRACK_COLS[13]));
            cols.add(DoubleColumn.create(TRACK_COLS[14]));
            cols.add(DoubleColumn.create(TRACK_COLS[15]));
            cols.add(DoubleColumn.create(TRACK_COLS[16]));
            cols.add(DoubleColumn.create(TRACK_COLS[17]));
            cols.add(DoubleColumn.create(TRACK_COLS[18]));
            cols.add(DoubleColumn.create(TRACK_COLS[19]));
            cols.add(DoubleColumn.create(TRACK_COLS[20]));
            cols.add(IntColumn.create   (TRACK_COLS[21]));
            cols.add(IntColumn.create   (TRACK_COLS[22]));
            return cols;
        }

        public void appendEntity(Track t, List<Column<?>> cols) {
            ((StringColumn) cols.get(0)).append(t.getUniqueKey());
            ((StringColumn) cols.get(1)).append(t.getRecordingName());
            ((IntColumn)    cols.get(2)).append(t.getTrackId());
            ((StringColumn) cols.get(3)).append(t.getTrackLabel());
            ((IntColumn)    cols.get(4)).append(t.getNumberOfSpots());
            ((IntColumn)    cols.get(5)).append(t.getNumberOfGaps());
            ((IntColumn)    cols.get(6)).append(t.getLongestGap());
            ((DoubleColumn) cols.get(7)).append(t.getTrackDuration());
            ((DoubleColumn) cols.get(8)).append(t.getTrackXLocation());
            ((DoubleColumn) cols.get(9)).append(t.getTrackYLocation());
            ((DoubleColumn) cols.get(10)).append(t.getTrackDisplacement());
            ((DoubleColumn) cols.get(11)).append(t.getTrackMaxSpeed());
            ((DoubleColumn) cols.get(12)).append(t.getTrackMedianSpeed());
            ((DoubleColumn) cols.get(13)).append(t.getDiffusionCoefficient());
            ((DoubleColumn) cols.get(14)).append(t.getDiffusionCoefficientExt());
            ((DoubleColumn) cols.get(15)).append(t.getTotalDistance());
            ((DoubleColumn) cols.get(16)).append(t.getConfinementRatio());
            ((IntColumn)    cols.get(17)).append(t.getSquareNumber());
            ((IntColumn)    cols.get(18)).append(t.getLabelNumber());
        }

        public Track readEntity(Table table, int r) {
            Track t = new Track();
            t.setUniqueKey(table.stringColumn(TRACK_COLS[0]).get(r));
            t.setRecordingName(table.stringColumn(TRACK_COLS[1]).get(r));
            t.setTrackId(table.intColumn(TRACK_COLS[2]).getInt(r));
            t.setTrackLabel(table.stringColumn(TRACK_COLS[3]).get(r));
            t.setNumberOfSpots(table.intColumn(TRACK_COLS[4]).getInt(r));
            t.setNumberOfGaps(table.intColumn(TRACK_COLS[5]).getInt(r));
            t.setLongestGap(table.intColumn(TRACK_COLS[6]).getInt(r));
            t.setTrackDuration(table.doubleColumn(TRACK_COLS[7]).getDouble(r));
            t.setTrackXLocation(table.doubleColumn(TRACK_COLS[8]).getDouble(r));
            t.setTrackYLocation(table.doubleColumn(TRACK_COLS[9]).getDouble(r));
            t.setTrackDisplacement(table.doubleColumn(TRACK_COLS[10]).getDouble(r));
            t.setTrackMaxSpeed(table.doubleColumn(TRACK_COLS[11]).getDouble(r));
            t.setTrackMedianSpeed(table.doubleColumn(TRACK_COLS[12]).getDouble(r));
            t.setDiffusionCoefficient(table.doubleColumn(TRACK_COLS[13]).getDouble(r));
            t.setDiffusionCoefficientExt(table.doubleColumn(TRACK_COLS[14]).getDouble(r));
            t.setTotalDistance(table.doubleColumn(TRACK_COLS[15]).getDouble(r));
            t.setConfinementRatio(table.doubleColumn(TRACK_COLS[16]).getDouble(r));
            t.setSquareNumber(table.intColumn(TRACK_COLS[17]).getInt(r));
            t.setLabelNumber(table.intColumn(TRACK_COLS[18]).getInt(r));
            return t;
        }
    }
}