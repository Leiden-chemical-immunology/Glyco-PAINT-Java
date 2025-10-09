package paint.shared.io;

import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class BaseTableIO {

    /**
     * Create a new empty Table with the given schema.
     */
    protected Table newEmptyTable(String tableName, String[] colNames, ColumnType[] colTypes) {
        if (colNames.length != colTypes.length) {
            throw new IllegalArgumentException("Names and types length mismatch: "
                                                       + colNames.length + " vs " + colTypes.length);
        }
        Table t = Table.create(tableName);
        for (int i = 0; i < colNames.length; i++) {
            Column<?> c = colTypes[i].create(colNames[i]);
            t.addColumns(c);
        }
        return t;
    }

    /**
     * Append all rows from source into target, in-place.
     * Both tables must have the same schema.
     */
    public void appendInPlace(Table target, Table source) {
        if (target.columnCount() != source.columnCount()) {
            throw new IllegalArgumentException("Cannot append: column count mismatch (" +
                                                       target.columnCount() + " vs " + source.columnCount() + ")");
        }
        // Enforce same column names in order
        for (int i = 0; i < target.columnCount(); i++) {
            if (!target.column(i).name().equals(source.column(i).name())) {
                throw new IllegalArgumentException("Cannot append: schema mismatch at column " + i +
                                                           " (" + target.column(i).name() + " vs " + source.column(i).name() + ")");
            }
        }
        target.append(source);
    }

    /**
     * Build CsvReadOptions enforcing the provided column types.
     */
    protected CsvReadOptions buildCsvReadOptions(Path csvPath, ColumnType[] colTypes) {
        return CsvReadOptions.builder(csvPath.toFile())
                .header(true)
                .columnTypes(colTypes)
                .build();
    }

    /**
     * Read a CSV file into a Table with a known schema and validate its header.
     */
    public Table readCsvWithSchema(Path csvPath,
                                   String logicalName,
                                   String[] expectedCols,
                                   ColumnType[] expectedTypes,
                                   boolean allowSuperset) throws IOException {

        if (!Files.isRegularFile(csvPath)) {
            throw new IOException("CSV not found: " + csvPath);
        }

        CsvReadOptions opts = buildCsvReadOptions(csvPath, expectedTypes);
        Table table = Table.read().usingOptions(opts);

        List<String> headerErrors = validateHeader(table, expectedCols, allowSuperset);
        if (!headerErrors.isEmpty()) {
            throw new IOException("Header validation failed for " + logicalName + ":\n  - "
                                          + String.join("\n  - ", headerErrors));
        }

        List<String> typeErrors = validateTypes(table, expectedCols, expectedTypes);
        if (!typeErrors.isEmpty()) {
            throw new IOException("Type validation failed for " + logicalName + ":\n  - "
                                          + String.join("\n  - ", typeErrors));
        }

        return table;
    }

    protected List<String> validateHeader(Table t, String[] expectedCols, boolean allowSuperset) {
        List<String> errors = new ArrayList<>();
        String[] actual = t.columnNames().toArray(new String[0]);

        if (!allowSuperset && actual.length != expectedCols.length) {
            errors.add("Column count mismatch: expected " + expectedCols.length + " but found " + actual.length);
        }
        if (actual.length < expectedCols.length) {
            errors.add("CSV has fewer columns (" + actual.length + ") than expected (" + expectedCols.length + ").");
        }

        int upto = Math.min(expectedCols.length, actual.length);
        for (int i = 0; i < upto; i++) {
            if (!expectedCols[i].equals(actual[i])) {
                errors.add("At index " + i + ": expected '" + expectedCols[i] + "' but found '" + actual[i] + "'");
            }
        }

        for (String name : expectedCols) {
            if (!t.columnNames().contains(name)) {
                errors.add("Missing expected column: '" + name + "'");
            }
        }

        return errors;
    }

    protected List<String> validateTypes(Table t, String[] names, ColumnType[] types) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String colName = names[i];
            ColumnType expected = types[i];
            if (!t.columnNames().contains(colName)) {
                continue; // header validator already flagged
            }
            Column<?> col = t.column(colName);
            ColumnType actual = col.type();

            boolean compatibleNumber =
                    (expected == ColumnType.DOUBLE && actual == ColumnType.INTEGER);

            if (!actual.equals(expected) && !compatibleNumber) {
                errors.add("Type mismatch for '" + colName + "': expected " + expected + " but got " + actual);
            }
        }
        return errors;
    }

    /**
     * Write a Table as CSV (with header).
     */
    public void writeCsv(Table table, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        CsvWriteOptions opts = CsvWriteOptions.builder(target.toFile())
                .header(true)
                .separator(',')
                .build();
        table.write().usingOptions(opts);
    }

    protected static String schemaToString(String[] names, ColumnType[] types) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            parts.add(names[i] + ":" + types[i].name());
        }
        return Arrays.toString(parts.toArray(new String[0]));
    }
}