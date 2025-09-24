package paint.shared.validate_new;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import tech.tablesaw.api.ColumnType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractFileValidator {

    private final Set<String> reportedTypeErrors = new HashSet<>();

    public ValidationResult validate(File file) {
        ValidationResult result = new ValidationResult();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(',')              // Use '\t' for TSV
                .setHeader()                    // expect header from the input
                .setSkipHeaderRecord(false)
                .setIgnoreSurroundingSpaces(true)
                .build();

        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
             CSVParser parser = format.parse(reader)) {

            List<String> header = parser.getHeaderNames();
            List<CSVRecord> records = parser.getRecords();

            validateHeader(header, result);

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
                        result.addError("Row " + (i + 2) + " has " + row.length + " values; expected " + types.length + ". This may indicate a formatting error (e.g. extra or missing commas).");
                        reportedMismatch = true;
                    }
                    continue;
                }

                rowMatchesTypes(row, types, i + 2, result);
            }

        } catch (IOException e) {
            result.addError("Error reading file: " + e.getMessage());
        }

        return result;
    }

    protected abstract void validateHeader(List<String> actualHeader, ValidationResult result);

    protected abstract ColumnType[] getExpectedTypes();

    protected boolean headersMatch(List<String> expected, List<String> actual) {
        if (expected.equals(actual)) {
            return true;
        }

        List<String> missing = expected.stream()
                .filter(h -> !actual.contains(h))
                .collect(Collectors.toList());

        List<String> unexpected = actual.stream()
                .filter(h -> !expected.contains(h))
                .collect(Collectors.toList());

        StringBuilder error = new StringBuilder("Header mismatch.");

        if (!missing.isEmpty()) {
            error.append("\nMissing headers: ").append(missing);
        }

        if (!unexpected.isEmpty()) {
            error.append("\nUnexpected headers: ").append(unexpected);
        }

        error.append("\nExpected: ").append(expected);
        error.append("\nActual:   ").append(actual);

        return false;
    }

    protected boolean rowMatchesTypes(String[] row, ColumnType[] types, int rowIndex, ValidationResult result) {
        for (int i = 0; i < types.length; i++) {
            String value = row[i];
            ColumnType type = types[i];
            if (!canParse(value, type)) {
                String key = "Column " + (i + 1) + " type " + type.name();
                if (!reportedTypeErrors.contains(key)) {
                    result.addError("Some values in column " + (i + 1) + " are invalid for type " + type.name() + ".");
                    reportedTypeErrors.add(key);
                }
            }
        }
        return true;
    }

    private boolean canParse(String value, ColumnType type) {
        if (value == null || value.trim().isEmpty()) return true;

        try {
            switch (type.name()) {
                case "INTEGER":
                    Integer.parseInt(value);
                    break;
                case "DOUBLE":
                    Double.parseDouble(value);
                    break;
                case "BOOLEAN":
                    if (!(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")))
                        throw new IllegalArgumentException();
                    break;
                case "LOCAL_DATE_TIME":
                    java.time.LocalDateTime.parse(value);
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