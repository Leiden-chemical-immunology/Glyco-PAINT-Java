/******************************************************************************
 *  Class:        BaseTableIO.java
 *  Package:      paint.shared.io
 *
 *  PURPOSE:
 *    Provides shared utilities for creating, validating, reading, and writing
 *    Tablesaw {@link tech.tablesaw.api.Table} objects used throughout the PAINT
 *    software ecosystem.
 *
 *  DESCRIPTION:
 *    This abstract base class centralizes common logic for:
 *      - Creating new tables with defined schemas
 *      - Validating CSV headers and column types
 *      - Reading and writing CSV files with enforced schemas
 *      - Appending tables safely with schema checks
 *      - Ensuring consistent numeric formatting and locale handling
 *
 *  KEY FEATURES:
 *    - Schema-based CSV reading and validation
 *    - Locale-stable CSV export (US locale, fixed 3-decimal precision)
 *    - Header and type consistency checking
 *    - Robust append operation for schema-aligned tables
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

import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Abstract base class for consistent CSV and {@link Table} I/O across
 * the PAINT modules. Provides shared implementations for reading, validating,
 * appending, and writing tabular data using Tablesaw.
 */
public abstract class BaseTableIO {

    /**
     * Creates a new empty {@link Table} with the given schema.
     *
     * @param tableName name of the table to create
     * @param colNames  array of column names
     * @param colTypes  array of corresponding {@link ColumnType}s
     * @return a new empty {@link Table} with the specified schema
     * @throws IllegalArgumentException if the lengths of {@code colNames} and {@code colTypes} differ
     */
    protected Table newEmptyTable(String tableName, String[] colNames, ColumnType[] colTypes) {
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
     * Appends all rows from {@code source} into {@code target} in place.
     * Both tables must have identical schemas.
     *
     * @param target the {@link Table} to append rows into
     * @param source the {@link Table} providing rows to append
     * @throws IllegalArgumentException if the tables have different schemas
     */
    public void appendInPlace(Table target, Table source) {
        if (target.columnCount() != source.columnCount()) {
            throw new IllegalArgumentException("Cannot append: column count mismatch ("
                                                       + target.columnCount() + " vs " + source.columnCount() + ")");
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
     * Builds a {@link CsvReadOptions} instance enforcing the provided column types.
     *
     * @param csvPath  path to the CSV file
     * @param colTypes expected {@link ColumnType}s for the file
     * @return configured {@link CsvReadOptions}
     */
    protected CsvReadOptions buildCsvReadOptions(Path csvPath, ColumnType[] colTypes) {
        return CsvReadOptions.builder(csvPath.toFile())
                .header(true)
                .columnTypes(colTypes)
                .build();
    }

    /**
     * Reads a CSV file into a {@link Table} with a known schema and validates its header and types.
     *
     * @param csvPath       path to the CSV file
     * @param logicalName   logical name used for error reporting
     * @param expectedCols  expected column names in order
     * @param expectedTypes expected {@link ColumnType}s in order
     * @param allowSuperset whether extra columns are allowed
     * @return a validated {@link Table}
     * @throws IOException if the file cannot be read or validation fails
     */
    public Table readCsvWithSchema(Path csvPath,
                                   String logicalName,
                                   String[] expectedCols,
                                   ColumnType[] expectedTypes,
                                   boolean allowSuperset) throws IOException {

        if (!Files.isRegularFile(csvPath)) {
            throw new IOException("CSV not found: " + csvPath);
        }

        CsvReadOptions opts  = buildCsvReadOptions(csvPath, expectedTypes);
        Table          table = Table.read().usingOptions(opts);

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

    /**
     * Validates that the table header matches the expected columns.
     *
     * @param t             the {@link Table} to validate
     * @param expectedCols  expected column names
     * @param allowSuperset whether extra columns are allowed
     * @return a list of validation error messages (empty if valid)
     */
    protected List<String> validateHeader(Table t, String[] expectedCols, boolean allowSuperset) {
        List<String> errors = new ArrayList<>();
        List<String> actualCols = new ArrayList<>();
        for (String col : t.columnNames()) {
            actualCols.add(col.toLowerCase(Locale.ROOT));
        }

        String[] expectedLower = Arrays.stream(expectedCols)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toArray(String[]::new);

        if (!allowSuperset && actualCols.size() != expectedCols.length) {
            errors.add("Column count mismatch: expected " + expectedCols.length + " but found " + actualCols.size());
        }
        if (actualCols.size() < expectedCols.length) {
            errors.add("CSV has fewer columns (" + actualCols.size() + ") than expected (" + expectedCols.length + ").");
        }

        int upto = Math.min(expectedLower.length, actualCols.size());
        for (int i = 0; i < upto; i++) {
            if (!expectedLower[i].equals(actualCols.get(i))) {
                errors.add("At index " + i + ": expected '" + expectedCols[i] + "' but found '" + t.columnNames().get(i) + "'");
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
     * Validates that the column types in the table match the expected schema.
     *
     * @param t      the {@link Table} to validate
     * @param names  expected column names
     * @param types  expected {@link ColumnType}s
     * @return a list of validation error messages (empty if valid)
     */
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
     * Writes a {@link Table} to a CSV file using a stable US locale with fixed
     * three-decimal formatting for floating-point values.
     *
     * @param table  the {@link Table} to write
     * @param target target file path
     * @throws IOException if writing fails
     */

    public void writeCsv(Table table, Path target) throws IOException {
        // Force locale-stable, fixed 3-decimal formatting
        NumberFormat nf = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.US));

        // Build an export table with numeric columns materialized as formatted strings
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
                // Non-floating types: add as-is (you can copy or reuse the same instance)
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