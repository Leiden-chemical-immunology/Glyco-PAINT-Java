package paint.io;

import tech.tablesaw.api.Table;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.columns.Column;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Per-entity adapter: defines schema + row mapping. (package-private on purpose) */
interface EntityTableAdapter<E> {
    String tableName();
    String[] columns();                 // fixed order
    ColumnType[] columnTypes();         // same length/order as columns()

    /** Create empty typed columns in the same order as columns()/columnTypes(). */
    List<Column<?>> newEmptyColumns();

    /** Append one entity into the provided columns (same order as columns()). */
    void appendEntity(E entity, List<Column<?>> cols);

    /** Reconstruct one entity from a table row (0-based index). */
    E readEntity(Table table, int rowIndex);
}

/** Reusable base for CSV/Table I/O across entities (Java 8 / Tablesaw 0.44.x). */
public class BaseTableIO<E> {

    private final EntityTableAdapter<E> adapter;

    public BaseTableIO(EntityTableAdapter<E> adapter) {
        this.adapter = adapter;
    }

    /**
     * Creates an empty but structured {@link Table}.
     * <p>
     * The returned table has zero rows but includes all columns defined by the adapter,
     * with the correct names and types.
     *
     * @return an empty {@code Table} with the correct schema (0 rows, all columns typed)
     */
    public Table emptyTable() {
        List<Column<?>> cols = adapter.newEmptyColumns();
        return Table.create(adapter.tableName(), cols.toArray(new Column<?>[cols.size()]));
    }

    /**
     * Converts a list of entities into a typed {@link Table}.
     * <p>
     * Each entity is appended to a new set of typed columns provided by the adapter,
     * preserving schema consistency.
     *
     * @param list the list of entities to convert; may be empty but not {@code null}
     * @return a {@code Table} containing all entities from the list with the correct schema
     */
    public Table toTable(List<E> list) {
        List<Column<?>> cols = adapter.newEmptyColumns();
        for (int i = 0; i < list.size(); i++) {
            adapter.appendEntity(list.get(i), cols);
        }
        return Table.create(adapter.tableName(), cols.toArray(new Column<?>[cols.size()]));
    }

    /**
     * Converts a {@link Table} (with a schema matching the adapter) back into a list of entities.
     * <p>
     * Each row in the table is reconstructed into an entity using the adapter.
     *
     * @param table the input table; must have the same schema as defined by the adapter
     * @return a list of entities reconstructed from the rows of the table
     * @throws IllegalArgumentException if the table header does not match the expected schema
     */
    public List<E> toEntities(Table table) {
        validateHeader(table, adapter.columns());
        List<E> out = new ArrayList<E>(table.rowCount());
        for (int r = 0; r < table.rowCount(); r++) {
            out.add(adapter.readEntity(table, r));
        }
        return out;
    }

    /**
     * Writes the given {@link Table} to a CSV file after reordering and validating
     * its columns against the expected schema.
     *
     * @param table    the table to write
     * @param filePath the path to the CSV file to write to
     * @throws IOException if an I/O error occurs while writing the file
     */
    public void writeCsv(Table table, String filePath) throws IOException {
        // adapter.columns() is String[] -> OK for varargs
        Table normalized = table.copy().retainColumns(adapter.columns());
        normalized.write().csv(filePath);
    }

    public void writeCsv(Table table, Path filePath) throws IOException {
        // adapter.columns() is String[] -> OK for varargs
        Table normalized = table.copy().retainColumns(adapter.columns());
        normalized.write().csv(filePath.toString());
    }

    /**
     * Reads a CSV file into a {@link Table}, forcing the adapter’s column types
     * and validating that the header matches the expected order.
     *
     * @param filePath the path to the CSV file to read
     * @return a {@code Table} containing the CSV data with the adapter’s schema enforced
     * @throws IOException if an error occurs while reading the file
     * @throws IllegalArgumentException if the CSV header does not match the expected schema
     */
    public Table readCsv(Path filePath) throws IOException {
        CsvReadOptions opts = CsvReadOptions.builder(filePath.toFile())
                .header(true)
                .columnTypes(adapter.columnTypes())
                .build();
        Table t = Table.read().usingOptions(opts);
        validateHeader(t, adapter.columns());
        return t;
    }

    /** Return a new table that is a copy of 'combined' with rows from 'add' appended.
     *  You can call this repeatedly to append multiple tables.
     *      combined = trackIO.appendInPlace(combined, batch1);
     *      combined = trackIO.appendInPlace(combined, batch2);
     * @param combined - the table to append to.
     * @param add - the table to append.
     * @return - combined new table with the rows from 'add' appended.
     */

    public Table appended(Table combined, Table add) {

        validateHeader(combined, adapter.columns());
        validateHeader(add, adapter.columns());
        Table copy = combined.copy();
        // add.retainColumns(...) expects varargs -> provide String[]
        String[] aCols = combined.columnNames().toArray(new String[combined.columnCount()]);
        copy.append(add.retainColumns(aCols));
        return copy;
    }

    /** Return a new table that is a copy of 'a' with rows from 'b' appended.
     * You can call this repeatedly to append multiple tables.
     * trackIO.appendInPlace(combined, batch1);  // modifies 'combined'
     * trackIO.appendInPlace(combined, batch2);  // modifies 'combined' again
     * @param target - the table to append to.
     * @param source - the table to append.
    */

    public void appendInPlace(Table target, Table source) {
        validateHeader(target, adapter.columns());
        validateHeader(source, adapter.columns());
        target.append(source);   // We have checked the headers already, so a direct 'append' is safe

        // This is the tablesaw way to append tables
        // String[] cols = target.columnNames().toArray(new String[target.columnCount()]);
        // target.append(source.retainColumns(cols));   // This is the tablesaw way to append tables
    }

    /** Ensure the header of the table matches exactly the specified list (names and order).
     * @param tableToValidate - table to validate.
     * @param expectedColumns - the expected column names.
     * @throws IllegalArgumentException if the header does not match exactly.
     * */
    private static void validateHeader(Table tableToValidate, String[] expectedColumns) {
        List<String> names = tableToValidate.columnNames();
        if (names.size() != expectedColumns.length) {
            throw new IllegalArgumentException("Unexpected column count: found=" + names.size()
                    + " expectedColumns=" + expectedColumns.length + " -> " + names);
        }
        for (int i = 0; i < expectedColumns.length; i++) {
            if (!expectedColumns[i].equals(names.get(i))) {
                throw new IllegalArgumentException("Unexpected column at index " + i
                        + ": found '" + names.get(i) + "', expectedColumns '" + expectedColumns[i] + "'");
            }
        }
    }
}