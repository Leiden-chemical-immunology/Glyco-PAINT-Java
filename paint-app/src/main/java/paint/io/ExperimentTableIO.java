package paint.io;

import objects.Experiment;
import tech.tablesaw.api.*;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.columns.Column;

import java.util.ArrayList;
import java.util.List;

import static constants.PaintConstants.EXPERIMENT_COLS;

public class ExperimentTableIO extends BaseTableIO<Experiment> {

    public ExperimentTableIO() {
        super(new ExperimentAdapter());
    }

    private static class ExperimentAdapter implements EntityTableAdapter<Experiment> {

        public String tableName() {
            return "experiments";
        }

        public String[] columns() {
            return EXPERIMENT_COLS;
        }

        public ColumnType[] columnTypes() {
            return new ColumnType[] {
                    ColumnType.STRING  // experimentName
            };
        }

        public List<Column<?>> newEmptyColumns() {
            List<Column<?>> c = new ArrayList<Column<?>>(EXPERIMENT_COLS.length);
            c.add(StringColumn.create(EXPERIMENT_COLS[0]));
            return c;
        }

        public void appendEntity(Experiment e, List<Column<?>> c) {
            ((StringColumn) c.get(0)).append(e.getExperimentName());
        }

        public Experiment readEntity(Table t, int r) {
            Experiment e = new Experiment();
            e.setExperimentName(t.stringColumn(EXPERIMENT_COLS[0]).get(r));
            return e;
        }
    }
}