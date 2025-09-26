package paint.shared.validate_new;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Consistency checker for Condition Number groups.
 * Ensures all rows with the same Condition Number
 * have identical attributes.
 */
public class ConditionConsistencyChecker {

    private static final List<String> REQUIRED = Arrays.asList(
            "Condition Number", "Probe Name", "Probe Type",
            "Cell Type", "Adjuvant", "Concentration"
    );

    public static ValidationResult check(File file, String experimentName) {
        ValidationResult result = new ValidationResult();

        try (FileReader reader = new FileReader(file);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            Map<String, Map<String, String>> conditionGroups = new HashMap<>();

            for (CSVRecord record : parser) {
                String condition = record.get("Condition Number");
                String probeName = record.get("Probe Name");
                String probeType = record.get("Probe Type");
                String cellType = record.get("Cell Type");
                String adjuvant = record.get("Adjuvant");
                String concentration = record.get("Concentration");

                Map<String, String> current = new LinkedHashMap<>();
                current.put("Probe Name", probeName);
                current.put("Probe Type", probeType);
                current.put("Cell Type", cellType);
                current.put("Adjuvant", adjuvant);
                current.put("Concentration", concentration);

                if (!conditionGroups.containsKey(condition)) {
                    conditionGroups.put(condition, current);
                } else {
                    Map<String, String> baseline = conditionGroups.get(condition);

                    for (Map.Entry<String, String> entry : current.entrySet()) {
                        String col = entry.getKey();
                        String value = entry.getValue();
                        String expected = baseline.get(col);

                        if (!Objects.equals(expected, value)) {
                            result.addError("[" + experimentName + "] Condition " + condition
                                    + " - Inconsistent '" + col + "': " + expected + " / " + value);
                        }
                    }
                }
            }

        } catch (IOException e) {
            result.addError("Error reading file during consistency check: " + e.getMessage());
        }

        return result;
    }
}