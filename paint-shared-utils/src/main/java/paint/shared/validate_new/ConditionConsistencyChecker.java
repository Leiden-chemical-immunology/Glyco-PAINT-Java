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
                String conc = record.get("Concentration");

                Map<String, String> attributes = conditionGroups.get(condition);
                if (attributes == null) {
                    attributes = new HashMap<>();
                    attributes.put("Probe Name", probeName);
                    attributes.put("Probe Type", probeType);
                    attributes.put("Cell Type", cellType);
                    attributes.put("Adjuvant", adjuvant);
                    attributes.put("Concentration", conc);
                    conditionGroups.put(condition, attributes);
                } else {
                    if (!Objects.equals(attributes.get("Probe Name"), probeName) ||
                            !Objects.equals(attributes.get("Probe Type"), probeType) ||
                            !Objects.equals(attributes.get("Cell Type"), cellType) ||
                            !Objects.equals(attributes.get("Adjuvant"), adjuvant) ||
                            !Objects.equals(attributes.get("Concentration"), conc)) {

                        String expected = String.format(
                                "[%s] Condition %s expected: [Probe Name=%s, Probe Type=%s, Cell Type=%s, Adjuvant=%s, Concentration=%s]",
                                experimentName, condition,
                                attributes.get("Probe Name"),
                                attributes.get("Probe Type"),
                                attributes.get("Cell Type"),
                                attributes.get("Adjuvant"),
                                attributes.get("Concentration"));

                        String found = String.format(
                                "[%s] Condition %s found: [Probe Name=%s, Probe Type=%s, Cell Type=%s, Adjuvant=%s, Concentration=%s]",
                                experimentName, condition,
                                probeName, probeType, cellType, adjuvant, conc);

                        result.addError("Inconsistent attributes for Condition Number " + condition
                                + "\n" + expected
                                + "\n" + found);
                    }
                }
            }

        } catch (IOException e) {
            result.addError("Error reading file during consistency check: " + e.getMessage());
        }

        return result;
    }
}