package paint.shared.validate_new;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.*;

public class ExperimentInfoValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.EXPERIMENT_INFO_COLS);
        if (!headersMatch(expectedHeader, actualHeader)) {
            result.addError("Header mismatch in Experiment Info file.\nExpected: " + expectedHeader + "\nActual: " + actualHeader);
        }
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.EXPERIMENT_INFO_TYPES;
    }

    @Override
    public ValidationResult validate(File file) {
        ValidationResult result = super.validate(file);

        // Consistency check for attributes grouped by Condition Number
        List<String> keys = Arrays.asList("Probe Name", "Probe Type", "Cell Type", "Adjuvant", "Concentration");
        Map<String, Map<String, String>> conditionAttributes = new HashMap<>();

        try (Reader reader = new FileReader(file);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build())) {

            int rowNum = 2; // header is line 1

            for (CSVRecord record : parser) {
                String condition = record.get("Condition Number").trim();

                Map<String, String> current = new LinkedHashMap<>();
                for (String key : keys) {
                    if (!record.isMapped(key)) {
                        result.addError("Missing required column: " + key);
                        return result;
                    }
                    current.put(key, record.get(key).trim());
                }

                Map<String, String> existing = conditionAttributes.get(condition);
                if (existing == null) {
                    conditionAttributes.put(condition, current);
                } else {
                    List<String> mismatches = new ArrayList<>();
                    for (String key : keys) {
                        String expected = existing.get(key);
                        String actual = current.get(key);
                        if (!Objects.equals(expected, actual)) {
                            mismatches.add(key + " (expected=" + expected + ", found=" + actual + ")");
                        }
                    }
                    if (!mismatches.isEmpty()) {
                        result.addError("Inconsistent attributes for Condition Number '" + condition + "' at row " + rowNum + ":\n  → " + String.join("; ", mismatches));
                    }
                }

                rowNum++;
            }

        } catch (Exception e) {
            result.addError("Consistency check failed: " + e.getMessage());
        }

        return result;
    }

    public static void main(String[] args) {
        File file = new File("/Users/hans/Paint Test Project/230417/Experiment Info.csv");
        ExperimentInfoValidator validator = new ExperimentInfoValidator();
        ValidationResult result = validator.validate(file);

        if (result.isValid()) {
            System.out.println("✔ Experiment Info file is valid.");
        } else {
            System.out.println("⚠ Issues found:");
            System.out.println(result);
        }
    }
}