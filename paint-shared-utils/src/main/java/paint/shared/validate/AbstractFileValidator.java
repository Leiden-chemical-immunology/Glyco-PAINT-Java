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

/**
 * Abstract base class for validating CSV files used in Paint.
 * <p>
 * Provides header and type validation logic for CSV-based experiment data.
 * Subclasses define the expected header layout and column data types.
 * </p>
 */
public abstract class AbstractFileValidator {

    private final Set<String> reportedTypeErrors = new HashSet<>();

    // Flexible timestamp parser: supports yyyy-MM-dd and dd/MM/yyyy with optional fractions
    private static final DateTimeFormatter FLEXIBLE_DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd'T'HH:mm:ss][dd/MM/yyyy'T'HH:mm:ss]")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
            .optionalEnd()
            .toFormatter();

    /**
     * Validates both headers and values of the given CSV file.
     *
     * @param file        CSV file to validate
     * @param checkValues whether to validate row values against expected types
     * @return a {@link ValidationResult} describing all detected errors and warnings
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
                return result; // stop if only header check or headers already invalid
            }

            ColumnType[] types = getExpectedTypes();
            boolean reportedMismatch = false;

            for (int i = 0; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                String[] row = new String[header.size()];

                for (int j = 0; j < header.size(); j++) {
                    row[j] = record.get(j);
                }

                if (row.length != types.length) {
                    if (!reportedMismatch) {
                        result.addError("Row " + (i + 2) + " has " + row.length
                                                + " values; expected " + types.length
                                                + ". This may indicate a formatting error (e.g. extra or missing commas).");
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
     * Validates only the headers of a CSV file, without checking row data.
     *
     * @param file CSV file to validate
     * @return a {@link ValidationResult} containing header validation results
     */
    public ValidationResult validateHeadersOnly(File file) {
        return validate(file, false);
    }

    /**
     * Validates both headers and data values of the given CSV file.
     *
     * @param file CSV file to validate
     * @return a {@link ValidationResult} describing validation results
     */
    public ValidationResult validate(File file) {
        return validate(file, true);
    }

    /**
     * Checks whether the actual CSV header matches the expected header.
     *
     * @param actualHeader list of header names from the CSV file
     * @param result       validation result accumulator for recording mismatches
     */
    protected abstract void validateHeader(List<String> actualHeader, ValidationResult result);

    /**
     * Returns the expected column types for the CSV file.
     *
     * @return an array of {@link ColumnType} values defining expected types
     */
    protected abstract ColumnType[] getExpectedTypes();

    /**
     * Compares expected and actual headers, adding descriptive errors if mismatched.
     *
     * @param expected list of expected header names
     * @param actual   list of actual header names
     * @param result   validation result for error accumulation
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
     * Checks if all values in a given row conform to the expected column types.
     *
     * @param row       the row values as strings
     * @param types     the expected column types
     * @param headers   header names for reference
     * @param rowIndex  current row index (1-based for readability)
     * @param result    validation result for accumulating errors
     * @return {@code true} if all values match their expected types; otherwise {@code false}
     */
    protected boolean rowMatchesTypes(String[] row, ColumnType[] types, List<String> headers, int rowIndex, ValidationResult result) {
        for (int i = 0; i < types.length; i++) {
            String value = row[i];
            ColumnType type = types[i];
            if (!canParse(value, type)) {
                String colName = headers.size() > i ? headers.get(i) : ("Column " + (i + 1));
                String key = colName + " type " + type.name();
                if (!reportedTypeErrors.contains(key)) {
                    result.addError("Some values in column '" + colName + "' are invalid for type " + type.name() + ".");
                    reportedTypeErrors.add(key);
                }
            }
        }
        return true;
    }

    /**
     * Determines whether a value can be parsed according to the specified column type.
     *
     * @param value the raw string value from the CSV
     * @param type  the expected {@link ColumnType}
     * @return {@code true} if parsing succeeds or the value is empty; otherwise {@code false}
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
                    if (!(v.equals("true") || v.equals("false")
                            || v.equals("1") || v.equals("0")
                            || v.equals("yes") || v.equals("no")
                            || v.equals("y") || v.equals("n")
                            || v.equals("t") || v.equals("f"))) {
                        throw new IllegalArgumentException("Invalid boolean: " + value);
                    }
                    break;
                case "LOCAL_DATE_TIME":
                    LocalDateTime.parse(value, FLEXIBLE_DATE_TIME);
                    break;
                case "STRING":
                    break; // Always valid
                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}