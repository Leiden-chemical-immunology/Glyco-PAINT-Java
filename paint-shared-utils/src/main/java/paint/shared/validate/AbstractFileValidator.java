/******************************************************************************
 *  Class:        AbstractFileValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Serves as an abstract base class for validating CSV files used within the
 *    PAINT application. Provides shared logic for validating headers, column
 *    data types, and optional value integrity checks.
 *
 *  DESCRIPTION:
 *    • Handles parsing of CSV files with Apache Commons CSV.
 *    • Performs header and column-type validation.
 *    • Provides flexible timestamp parsing for multiple date formats.
 *    • Reports all detected errors and warnings through {@link ValidationResult}.
 *    • Concrete subclasses define expected headers and data types.
 *
 *  RESPONSIBILITIES:
 *    • Abstract superclass for experiment CSV validators.
 *    • Ensure format consistency across PAINT CSV datasets.
 *    • Offer reusable parsing and type-checking logic for all validators.
 *
 *  USAGE EXAMPLE:
 *    ValidationResult result = new ExperimentInfoValidator().validate(file);
 *    if (result.hasErrors()) { result.printSummary(); }
 *
 *  DEPENDENCIES:
 *    – org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
 *    – tech.tablesaw.api.ColumnType
 *    – paint.shared.validate.ValidationResult
 *    – paint.shared.utils.Miscellaneous
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

package paint.shared.validate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import tech.tablesaw.api.ColumnType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static paint.shared.utils.Miscellaneous.checkBooleanValue;

/**
 * Abstract superclass for all CSV file validators in PAINT.
 * <p>
 * Provides header, type, and optional value-validation mechanisms that are
 * common across multiple experiment file formats.
 * Concrete subclasses specify expected headers and types.
 * </p>
 */
public abstract class AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // INTERNAL STATE
    // ───────────────────────────────────────────────────────────────────────────────

    /** Tracks which column type errors have already been reported to avoid duplicates. */
    private final Set<String> reportedTypeErrors = new HashSet<>();

    /** Flexible timestamp parser supporting multiple formats with optional fractions. */
    private static final DateTimeFormatter FLEXIBLE_DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd'T'HH:mm:ss][dd/MM/yyyy'T'HH:mm:ss]")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    // ───────────────────────────────────────────────────────────────────────────────
    // VALIDATION ENTRY POINTS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates both headers and row values of the specified CSV file.
     *
     * @param file        CSV file to validate
     * @param checkValues whether to validate row values against expected types
     * @return a {@link ValidationResult} containing all detected issues
     */
    public ValidationResult validate(File file, boolean checkValues) {
        ValidationResult result = new ValidationResult();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(',')
                .setHeader()
                .setSkipHeaderRecord(false)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            List<String> header = parser.getHeaderNames();
            List<CSVRecord> records = parser.getRecords();

            validateHeader(header, result);

            if (!checkValues || result.hasErrors()) {
                return result; // Stop early if only header check or header invalid
            }

            ColumnType[] types = getExpectedTypes();
            boolean reportedMismatch = false;

            for (int i = 0; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                String[]  row    = new String[header.size()];

                for (int j = 0; j < header.size(); j++) {
                    row[j] = record.get(j);
                }

                if (row.length != types.length) {
                    if (!reportedMismatch) {
                        result.addError("Row " + (i + 2) + " has " + row.length
                                                + " values; expected " + types.length
                                                + ". Possible formatting issue (e.g., extra/missing commas).");
                        reportedMismatch = true;
                    }
                    continue;
                }
                rowMatchesTypes(row, types, header, i + 2, result);
            }
        } catch (IOException e) {
            result.addError("Error reading file: " + e.getMessage());
        }

        return result;
    }

    /**
     * Validates headers only (no row data).
     *
     * @param file CSV file to validate
     * @return {@link ValidationResult} with header validation results
     */
    public ValidationResult validateHeadersOnly(File file) {
        return validate(file, false);
    }

    /**
     * Validates both headers and values.
     *
     * @param file CSV file to validate
     * @return {@link ValidationResult} describing validation results
     */
    public ValidationResult validate(File file) {
        return validate(file, true);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ABSTRACT DEFINITIONS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Subclasses implement header validation logic for a specific CSV schema.
     *
     * @param actualHeader header names read from the CSV
     * @param result       result accumulator for recording mismatches
     */
    protected abstract void validateHeader(List<String> actualHeader, ValidationResult result);

    /**
     * Returns the expected column types for the CSV file.
     *
     * @return array of {@link ColumnType} values
     */
    protected abstract ColumnType[] getExpectedTypes();

    // ───────────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Compares expected and actual header sequences, recording mismatches.
     *
     * @param expected expected header names
     * @param actual   actual header names
     * @param result   validation result for error reporting
     * @return {@code true} if headers match exactly; otherwise {@code false}
     */
    protected boolean headersMatch(List<String> expected, List<String> actual, ValidationResult result) {
        if (expected.equals(actual)) {
            return true;
        }

        List<String> missing = expected.stream()
                .filter(h -> !actual.contains(h))
                .collect(Collectors.toList());

        List<String> unexpected = actual.stream()
                .filter(h -> !expected.contains(h))
                .collect(Collectors.toList());

        StringBuilder error = new StringBuilder("Header mismatch:");

        if (!missing.isEmpty()) {
            error.append("\n- Missing headers: ").append(missing);
        }

        if (!unexpected.isEmpty()) {
            error.append("\n- Unexpected headers: ").append(unexpected);
        }

        if (false) {
            error.append("\n\nExpected: ").append(expected);
            error.append("\nActual:   ").append(actual);
        }
        result.addError(error.toString());
        return false;
    }

    /**
     * Checks whether all row values conform to expected column types.
     *
     * @param row       string array of CSV values
     * @param types     expected column types
     * @param headers   header names
     * @param rowIndex  row index (1-based)
     * @param result    validation accumulator
     * @return {@code true} if all types match; otherwise {@code false}
     */
    protected boolean rowMatchesTypes(String[]         row,
                                      ColumnType[]     types,
                                      List<String>     headers,
                                      int              rowIndex,
                                      ValidationResult result) {
        for (int i = 0; i < types.length; i++) {
            String value = row[i];
            ColumnType type = types[i];
            if (!canParse(value, type)) {
                String colName = headers.size() > i ? headers.get(i) : "Column " + (i + 1);
                String key = colName + " type " + type.name();
                if (!reportedTypeErrors.contains(key)) {
                    result.addError("Some values in column '" + colName +
                                            "' are invalid for type " + type.name() + ".");
                    reportedTypeErrors.add(key);
                }
            }
        }
        return true;
    }

    /**
     * Determines if a value can be parsed as the given column type.
     *
     * @param value raw string value
     * @param type  expected type
     * @return {@code true} if value is parseable or empty; otherwise {@code false}
     */
    private boolean canParse(String value, ColumnType type) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }

        try {
            switch (type.name()) {
                case "INTEGER":
                    Integer.parseInt(value);
                    break;
                case "DOUBLE":
                    Double.parseDouble(value);
                    break;
                case "BOOLEAN":
                    String v = value.trim().toLowerCase();
                    if (!checkBooleanValue(v)) {
                        throw new IllegalArgumentException("Invalid boolean: " + value);
                    }
                    break;
                case "LOCAL_DATE_TIME":
                    LocalDateTime.parse(value, FLEXIBLE_DATE_TIME);
                    break;
                case "STRING":
                    break;
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}