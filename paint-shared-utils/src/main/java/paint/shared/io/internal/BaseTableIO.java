/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.io.internal;

import paint.shared.io.MainIOInterface;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.FloatColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static paint.shared.utils.BooleanUtils.normalizeBoolean;

/**
 * Internal abstract base class for consistent CSV and {@link Table} I/O across
 * the PAINT data-layer implementations. Provides shared implementations for
 * reading, validating, appending, and writing tabular data using Tablesaw.
 *
 * <p>This class is package-private and intended to be used only by the
 * TableIO implementations in {@code paint.shared.io.internal}. External code
 * must access CSV I/O via {@link MainIOInterface}.</p>
 */
abstract class BaseTableIO {

    // ───────────────────────────────────────────────────────────────────────────────
    // TABLE CREATION HELPERS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new empty {@link Table} with the given schema.
     *
     * @param tableName name of the resulting table
     * @param colNames  column names in schema order
     * @param colTypes  column types in schema order
     * @return a new empty {@link Table} instance with all columns defined
     */
    protected static Table newEmptyTable(String tableName, String[] colNames, ColumnType[] colTypes) {
        if (colNames.length != colTypes.length) {
            throw new IllegalArgumentException("Names and types length mismatch: "
                                                       + colNames.length + " vs " + colTypes.length);
        }

        Table table = Table.create(tableName);
        for (int i = 0; i < colNames.length; i++) {
            Column<?> c = colTypes[i].create(colNames[i]);
            table.addColumns(c);
        }
        return table;
    }

    /**
     * Creates a new empty {@link Table} whose schema is derived from a column
     * enum. This replaces the per-subclass {@code getColumnHeaders()} /
     * {@code getColumnTypes()} helpers: each TableIO implementation supplies its
     * {@code Column} enum values plus accessors for the header name and column
     * type, and this method assembles the name/type arrays in enum order and
     * delegates to {@link #newEmptyTable(String, String[], ColumnType[])}.
     *
     * @param tableName name of the resulting table
     * @param columns   the schema enum's values, in schema order
     * @param headerFn  extracts the CSV header name from a column value
     * @param typeFn     extracts the {@link ColumnType} from a column value
     * @param <E>       the column enum type
     * @return a new empty {@link Table} with the derived schema
     */
    protected static <E extends Enum<E>> Table newEmptyTable(
            String tableName,
            E[] columns,
            Function<E, String> headerFn,
            Function<E, ColumnType> typeFn) {

        String[]     names = new String[columns.length];
        ColumnType[] types = new ColumnType[columns.length];
        for (int i = 0; i < columns.length; i++) {
            names[i] = headerFn.apply(columns[i]);
            types[i] = typeFn.apply(columns[i]);
        }
        return newEmptyTable(tableName, names, types);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CSV READING WITH SCHEMA ENFORCEMENT
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Reads a CSV file into a {@link Table} with a known schema and validates its
     * header and types.
     *
     * <p>This implementation uses a two-step import process:</p>
     * <ol>
     *   <li>Read the CSV with all columns forced to STRING.</li>
     *   <li>Normalize boolean values and convert types according to the schema.</li>
     * </ol>
     *
     * <p>This avoids Tablesaw’s strict Boolean parser rejecting values such as
     * "true", "false", "yes", "no", "1", "0", etc.</p>
     *
     * @param csvPath       path to the CSV file
     * @param expectedCols  expected column names in order
     * @param expectedTypes expected column types in order
     * @param allowSuperset if true, allows the CSV to contain extra columns
     * @return a typed, schema-validated {@link Table}
     * @throws IOException if the CSV is missing, invalid, or fails validation
     */
    public Table readCsvWithSchema(
            Path         csvPath,
            String[]     expectedCols,
            ColumnType[] expectedTypes,
            boolean      allowSuperset) throws IOException {

        if (!Files.isRegularFile(csvPath)) {
            throw new IOException("CSV not found: " + csvPath);
        }

        // STEP 1 — Read the CSV as STRINGS only (no type parsing!)
        ColumnType[] stringTypes = new ColumnType[expectedCols.length];
        Arrays.fill(stringTypes, ColumnType.STRING);

        CsvReadOptions rawOpts = CsvReadOptions.builder(csvPath.toFile())
                                               .header(true)
                                               .columnTypes(stringTypes)    // force everything to STRING
                                               .build();

        Table raw = Table.read().usingOptions(rawOpts);

        // STEP 2 — Validate header BEFORE type coercion
        List<String> headerErrors = validateHeader(raw, expectedCols, allowSuperset);
        if (!headerErrors.isEmpty()) {
            throw new IOException("Header validation failed:\n  - "
                                          + String.join("\n  - ", headerErrors));
        }

        // STEP 3 — Build a fresh typed table with the correct schema
        Table typed = newEmptyTable(raw.name(), expectedCols, expectedTypes);

        for (int r = 0; r < raw.rowCount(); r++) {
            Row newRow = typed.appendRow();

            for (int c = 0; c < expectedCols.length; c++) {

                String     colName = expectedCols[c];
                ColumnType type    = expectedTypes[c];

                String value = raw.stringColumn(colName).get(r);
                if (value != null) value = value.trim();
                if (value != null && value.isEmpty()) value = null;

                switch (type.name()) {
                    case "STRING":
                        newRow.setString(colName, value);
                        break;

                    case "INTEGER":
                        if (value == null) {
                            newRow.setMissing(colName);
                            break;
                        }
                        try {
                            newRow.setInt(colName, Integer.parseInt(value));
                        } catch (Exception ex) {
                            throw new IOException("Invalid INTEGER in '" + colName + "': " + value);
                        }
                        break;

                    case "DOUBLE":
                        if (value == null) {
                            newRow.setMissing(colName);
                            break;
                        }
                        try {
                            newRow.setDouble(colName, Double.parseDouble(value));
                        } catch (Exception ex) {
                            throw new IOException("Invalid DOUBLE in '" + colName + "': " + value);
                        }
                        break;

                    case "BOOLEAN": {
                        String norm = normalizeBoolean(value);
                        if (norm == null) {
                            newRow.setMissing(colName);
                        } else {
                            newRow.setBoolean(colName, Boolean.parseBoolean(norm));
                        }
                        break;
                    }

                    case "LOCAL_DATE_TIME":
                        if (value == null) {
                            newRow.setMissing(colName);
                            break;
                        }
                        try {
                            // assumes your converter now writes ISO-8601 values
                            newRow.setDateTime(colName, LocalDateTime.parse(value));
                        } catch (Exception ex) {
                            throw new IOException("Invalid LOCAL_DATE_TIME in '" +
                                                          colName + "': " + value);
                        }
                        break;

                    default:
                        throw new IOException("Unsupported type: " + type.name());
                }
            }
        }

        // STEP 4 — Type checking
        List<String> typeErrors = validateTypes(typed, expectedCols, expectedTypes);
        if (!typeErrors.isEmpty()) {
            throw new IOException("Type validation failed:\n  - "
                                          + String.join("\n  - ", typeErrors));
        }

        return typed;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER & TYPE VALIDATION HELPERS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the table header matches the expected columns.
     *
     * @param t             the table to validate
     * @param expectedCols  expected column names
     * @param allowSuperset whether extra columns are allowed
     * @return a list of human-readable error messages (empty if valid)
     */
    protected List<String> validateHeader(Table t, String[] expectedCols, boolean allowSuperset) {
        List<String> errors = new ArrayList<>();
        List<String> actualCols = new ArrayList<>();
        for (String col : t.columnNames()) {
            actualCols.add(col.toLowerCase(Locale.ROOT));
        }

        String[] expectedLower = new String[expectedCols.length];
        for (int i = 0; i < expectedCols.length; i++) {
            expectedLower[i] = expectedCols[i].toLowerCase(Locale.ROOT);
        }

        if (!allowSuperset && actualCols.size() != expectedCols.length) {
            errors.add("Column count mismatch: expected " + expectedCols.length + " but found " + actualCols.size());
        }
        if (actualCols.size() < expectedCols.length) {
            errors.add("CSV has fewer columns (" + actualCols.size() + ") than expected (" + expectedCols.length + ").");
        }

        int upto = Math.min(expectedLower.length, actualCols.size());
        for (int i = 0; i < upto; i++) {
            if (!expectedLower[i].equals(actualCols.get(i))) {
                errors.add("At index " + i + ": expected '" + expectedCols[i] + "' but found '"
                                   + t.columnNames().get(i) + "'");
            }
        }

        for (String name : expectedLower) {
            if (!actualCols.contains(name)) {
                errors.add("Missing expected column: '" + name + "'");
            }
        }

        return errors;
    }

    /**
     * Validates that the column types match the expected schema.
     *
     * @param t      table to validate
     * @param names  expected column names
     * @param types  expected column types
     * @return a list of human-readable error messages (empty if valid)
     */
    protected List<String> validateTypes(Table t, String[] names, ColumnType[] types) {
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            String colName = names[i];
            ColumnType expected = types[i];

            if (!t.columnNames().contains(colName)) {
                continue;
            }

            Column<?> col = t.column(colName);
            ColumnType actual = col.type();

            boolean compatibleNumber =
                    (expected == ColumnType.DOUBLE && actual == ColumnType.INTEGER);

            if (!actual.equals(expected) && !compatibleNumber) {
                errors.add("Type mismatch for '" + colName + "': expected "
                                   + expected + " but got " + actual);
            }
        }
        return errors;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CSV WRITING
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Writes a {@link Table} to CSV with US-locale fixed 3-decimal precision for
     * floating point values.
     *
     * @param table  table to export
     * @param target CSV file path
     * @throws IOException if writing fails
     */
    public void writeCsv(Table table, Path target) throws IOException {
        NumberFormat nf = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));

        Table export = Table.create(table.name());
        for (Column<?> col : table.columns()) {
            if (col instanceof DoubleColumn) {
                DoubleColumn dc = (DoubleColumn) col;
                StringColumn sc = StringColumn.create(col.name());
                for (int i = 0; i < dc.size(); i++) {
                    sc.append(dc.isMissing(i) ? "" : nf.format(dc.getDouble(i)));
                }
                export.addColumns(sc);
            } else if (col instanceof FloatColumn) {
                FloatColumn fc = (FloatColumn) col;
                StringColumn sc = StringColumn.create(col.name());
                for (int i = 0; i < fc.size(); i++) {
                    sc.append(fc.isMissing(i) ? "" : nf.format(fc.getFloat(i)));
                }
                export.addColumns(sc);
            } else {
                export.addColumns(col);
            }
        }

        Files.createDirectories(target.getParent());
        CsvWriteOptions opts = CsvWriteOptions.builder(target.toFile())
                                              .header(true)
                                              .separator(',')
                                              .build();

        export.write().usingOptions(opts);
    }
}