package paint.io;

import objects.Square;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.api.*;

import java.util.ArrayList;
import java.util.List;

import static constants.PaintConstants.SQUARE_COLS;

public class SquareTableIO extends BaseTableIO<Square> {

    public SquareTableIO() { super(new SquareAdapter()); }

    private static class SquareAdapter implements EntityTableAdapter<Square> {

        public String tableName() {
            return "squares";
        }

        public String[] columns() {
            return SQUARE_COLS;
        }

        public ColumnType[] columnTypes() {
            return new ColumnType[] {
                    ColumnType.STRING,  //  0 uniqueKey
                    ColumnType.STRING,  //  1 recordingName
                    ColumnType.INTEGER, //  2 squareNumber
                    ColumnType.INTEGER, //  3 rowNumber
                    ColumnType.INTEGER, //  4 colNumber
                    ColumnType.INTEGER, //  5 labelNumber
                    ColumnType.INTEGER, //  6 cellID

                    ColumnType.BOOLEAN, //  7 selected
                    ColumnType.BOOLEAN, //  8 squareManuallyExcluded
                    ColumnType.BOOLEAN, //  9 imageExcluded

                    ColumnType.DOUBLE,  // 10 x0
                    ColumnType.DOUBLE,  // 11 y0
                    ColumnType.DOUBLE,  // 12 x1
                    ColumnType.DOUBLE,  // 13 y1

                    ColumnType.INTEGER, // 14 numberTracks
                    ColumnType.DOUBLE,  // 15 variability
                    ColumnType.DOUBLE,  // 16 density
                    ColumnType.DOUBLE,  // 17 densityRatio
                    ColumnType.DOUBLE,  // 18 tau
                    ColumnType.DOUBLE,  // 19 rSquared

                    ColumnType.DOUBLE,  // 20 medianDiffusionCoefficient
                    ColumnType.DOUBLE,  // 21 medianDiffusionCoefficientExt
                    ColumnType.DOUBLE,  // 22 medianLongTrackDuration
                    ColumnType.DOUBLE,  // 23 medianShortTrackDuration

                    ColumnType.DOUBLE,  // 24 medianDisplacement
                    ColumnType.DOUBLE,  // 25 maxDisplacement
                    ColumnType.DOUBLE,  // 26 totalDisplacement

                    ColumnType.DOUBLE,  // 27 medianMaxSpeed
                    ColumnType.DOUBLE,  // 28 maxMaxSpeed

                    ColumnType.DOUBLE,  // 29 medianMeanSpeed
                    ColumnType.DOUBLE,  // 30 maxMeanSpeed

                    ColumnType.DOUBLE,  // 31 maxTrackDuration
                    ColumnType.DOUBLE,  // 32 totalTrackDuration
                    ColumnType.DOUBLE   // 33 medianTrackDuration
            };
        }

        public List<Column<?>> newEmptyColumns() {
            List<Column<?>> c = new ArrayList<Column<?>>(SQUARE_COLS.length);
            c.add(StringColumn.create (SQUARE_COLS[0]));
            c.add(StringColumn.create (SQUARE_COLS[1]));
            c.add(IntColumn.create    (SQUARE_COLS[2]));
            c.add(IntColumn.create    (SQUARE_COLS[3]));
            c.add(IntColumn.create    (SQUARE_COLS[4]));
            c.add(IntColumn.create    (SQUARE_COLS[5]));
            c.add(IntColumn.create    (SQUARE_COLS[6]));

            c.add(BooleanColumn.create(SQUARE_COLS[7]));
            c.add(BooleanColumn.create(SQUARE_COLS[8]));
            c.add(BooleanColumn.create(SQUARE_COLS[9]));

            c.add(DoubleColumn.create (SQUARE_COLS[10]));
            c.add(DoubleColumn.create (SQUARE_COLS[11]));
            c.add(DoubleColumn.create (SQUARE_COLS[12]));
            c.add(DoubleColumn.create (SQUARE_COLS[13]));

            c.add(IntColumn.create    (SQUARE_COLS[14]));
            c.add(DoubleColumn.create (SQUARE_COLS[15]));
            c.add(DoubleColumn.create (SQUARE_COLS[16]));
            c.add(DoubleColumn.create (SQUARE_COLS[17]));
            c.add(DoubleColumn.create (SQUARE_COLS[18]));
            c.add(DoubleColumn.create (SQUARE_COLS[19]));

            c.add(DoubleColumn.create (SQUARE_COLS[20]));
            c.add(DoubleColumn.create (SQUARE_COLS[21]));
            c.add(DoubleColumn.create (SQUARE_COLS[22]));
            c.add(DoubleColumn.create (SQUARE_COLS[23]));

            c.add(DoubleColumn.create (SQUARE_COLS[24]));
            c.add(DoubleColumn.create (SQUARE_COLS[25]));
            c.add(DoubleColumn.create (SQUARE_COLS[26]));

            c.add(DoubleColumn.create (SQUARE_COLS[27]));
            c.add(DoubleColumn.create (SQUARE_COLS[28]));

            c.add(DoubleColumn.create (SQUARE_COLS[29]));
            c.add(DoubleColumn.create (SQUARE_COLS[30]));

            c.add(DoubleColumn.create (SQUARE_COLS[31]));
            c.add(DoubleColumn.create (SQUARE_COLS[32]));
            c.add(DoubleColumn.create (SQUARE_COLS[33]));
            return c;
        }

        public void appendEntity(Square s, List<Column<?>> c) {
            ((StringColumn ) c.get(0)).append(s.getUniqueKey());
            ((StringColumn ) c.get(1)).append(s.getRecordingName());
            ((IntColumn    ) c.get(2)).append(s.getSquareNumber());
            ((IntColumn    ) c.get(3)).append(s.getRowNumber());
            ((IntColumn    ) c.get(4)).append(s.getColNumber());
            ((IntColumn    ) c.get(5)).append(s.getLabelNumber());
            ((IntColumn    ) c.get(6)).append(s.getCellId());

            ((BooleanColumn) c.get(7)).append(s.isSelected());
            ((BooleanColumn) c.get(8)).append(s.isSquareManuallyExcluded());
            ((BooleanColumn) c.get(9)).append(s.isImageExcluded());

            ((DoubleColumn ) c.get(10)).append(s.getX0());
            ((DoubleColumn ) c.get(11)).append(s.getY0());
            ((DoubleColumn ) c.get(12)).append(s.getX1());
            ((DoubleColumn ) c.get(13)).append(s.getY1());

            ((IntColumn    ) c.get(14)).append(s.getNumberOfTracks());
            ((DoubleColumn ) c.get(15)).append(s.getVariability());
            ((DoubleColumn ) c.get(16)).append(s.getDensity());
            ((DoubleColumn ) c.get(17)).append(s.getDensityRatio());
            ((DoubleColumn ) c.get(18)).append(s.getTau());
            ((DoubleColumn ) c.get(19)).append(s.getRSquared());

            ((DoubleColumn ) c.get(20)).append(s.getMedianDiffusionCoefficient());
            ((DoubleColumn ) c.get(21)).append(s.getMedianDiffusionCoefficientExt());
            ((DoubleColumn ) c.get(22)).append(s.getMedianLongTrackDuration());
            ((DoubleColumn ) c.get(23)).append(s.getMedianShortTrackDuration());

            ((DoubleColumn ) c.get(24)).append(s.getMedianDisplacement());
            ((DoubleColumn ) c.get(25)).append(s.getMaxDisplacement());
            ((DoubleColumn ) c.get(26)).append(s.getTotalDisplacement());

            ((DoubleColumn ) c.get(27)).append(s.getMedianMaxSpeed());
            ((DoubleColumn ) c.get(28)).append(s.getMaxMaxSpeed());

            ((DoubleColumn ) c.get(29)).append(s.getMedianMeanSpeed());
            ((DoubleColumn ) c.get(30)).append(s.getMaxMeanSpeed());

            ((DoubleColumn ) c.get(31)).append(s.getMaxTrackDuration());
            ((DoubleColumn ) c.get(32)).append(s.getTotalTrackDuration());
            ((DoubleColumn ) c.get(33)).append(s.getMedianTrackDuration());
        }

        public Square readEntity(Table t, int r) {
            Square s = new Square();
            s.setUniqueKey(t.stringColumn(SQUARE_COLS[0]).get(r));
            s.setRecordingName(t.stringColumn(SQUARE_COLS[1]).get(r));
            s.setSquareNumber(t.intColumn(SQUARE_COLS[2]).getInt(r));
            s.setRowNumber(t.intColumn(SQUARE_COLS[3]).getInt(r));
            s.setColNumber(t.intColumn(SQUARE_COLS[4]).getInt(r));
            s.setLabelNumber(t.intColumn(SQUARE_COLS[5]).getInt(r));
            s.setCellId(t.intColumn(SQUARE_COLS[6]).getInt(r));

            s.setSelected(t.booleanColumn(SQUARE_COLS[7]).get(r));
            s.setSquareManuallyExcluded(t.booleanColumn(SQUARE_COLS[8]).get(r));
            s.setImageExcluded(t.booleanColumn(SQUARE_COLS[9]).get(r));

            s.setX0(t.doubleColumn(SQUARE_COLS[10]).getDouble(r));
            s.setY0(t.doubleColumn(SQUARE_COLS[11]).getDouble(r));
            s.setX1(t.doubleColumn(SQUARE_COLS[12]).getDouble(r));
            s.setY1(t.doubleColumn(SQUARE_COLS[13]).getDouble(r));

            s.setNumberOfTracks(t.intColumn(SQUARE_COLS[14]).getInt(r));
            s.setVariability(t.doubleColumn(SQUARE_COLS[15]).getDouble(r));
            s.setDensity(t.doubleColumn(SQUARE_COLS[16]).getDouble(r));
            s.setDensityRatio(t.doubleColumn(SQUARE_COLS[17]).getDouble(r));
            s.setTau(t.doubleColumn(SQUARE_COLS[18]).getDouble(r));
            s.setRSquared(t.doubleColumn(SQUARE_COLS[19]).getDouble(r));

            s.setMedianDiffusionCoefficient(t.doubleColumn(SQUARE_COLS[20]).getDouble(r));
            s.setMedianDiffusionCoefficientExt(t.doubleColumn(SQUARE_COLS[21]).getDouble(r));
            s.setMedianLongTrackDuration(t.doubleColumn(SQUARE_COLS[22]).getDouble(r));
            s.setMedianShortTrackDuration(t.doubleColumn(SQUARE_COLS[23]).getDouble(r));

            s.setMedianDisplacement(t.doubleColumn(SQUARE_COLS[24]).getDouble(r));
            s.setMaxDisplacement(t.doubleColumn(SQUARE_COLS[25]).getDouble(r));
            s.setTotalDisplacement(t.doubleColumn(SQUARE_COLS[26]).getDouble(r));

            s.setMedianMaxSpeed(t.doubleColumn(SQUARE_COLS[27]).getDouble(r));
            s.setMaxMaxSpeed(t.doubleColumn(SQUARE_COLS[28]).getDouble(r));

            s.setMedianMeanSpeed(t.doubleColumn(SQUARE_COLS[29]).getDouble(r));
            s.setMaxMeanSpeed(t.doubleColumn(SQUARE_COLS[30]).getDouble(r));

            s.setMaxTrackDuration(t.doubleColumn(SQUARE_COLS[31]).getDouble(r));
            s.setTotalTrackDuration(t.doubleColumn(SQUARE_COLS[32]).getDouble(r));
            s.setMedianTrackDuration(t.doubleColumn(SQUARE_COLS[33]).getDouble(r));
            return s;
        }
    }
}