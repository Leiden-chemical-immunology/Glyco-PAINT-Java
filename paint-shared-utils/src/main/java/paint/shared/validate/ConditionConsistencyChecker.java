/******************************************************************************
 *  Class:        ConditionConsistencyChecker.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Performs consistency validation across condition groups in experiment
 *    metadata CSV files. Ensures that all rows sharing the same
 *    "Condition Number" have identical key attributes such as probe and cell
 *    properties.
 *
 *  DESCRIPTION:
 *    • Groups all records by "Condition Number".
 *    • Compares attribute values (Probe Name, Probe Type, Cell Type,
 *      Adjuvant, Concentration) across rows of each group.
 *    • Reports inconsistencies if any attribute differs within a group.
 *    • Prevents duplicate error reporting for repeated issues.
 *
 *  RESPONSIBILITIES:
 *    • Validate intra-condition consistency of experiment metadata.
 *    • Detect deviations in descriptive attributes for the same condition.
 *    • Return results encapsulated in a {@link ValidationResult}.
 *
 *  USAGE EXAMPLE:
 *    File csv = new File("Experiment_Info.csv");
 *    ValidationResult result = ConditionConsistencyChecker.check(csv, "Experiment 001");
 *    if (result.hasErrors()) {
 *        result.printSummary();
 *    }
 *
 *  DEPENDENCIES:
 *    – org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
 *    – paint.shared.validate.ValidationResult
 *    – java.util.{Map, Set, List, HashMap, HashSet, Arrays, LinkedHashMap}
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Performs cross-row consistency checks for condition metadata in PAINT experiment CSVs.
 * <p>
 * Ensures that all rows with the same "Condition Number" share identical values for
 * critical attributes (probe, cell type, adjuvant, concentration, etc.).
 * </p>
 */
public final class ConditionConsistencyChecker {

    /**
     * Required columns used for consistency comparison.
     */
    private static final List<String> REQUIRED = Arrays.asList(
            "Condition Number", "Probe Name", "Probe Type",
            "Cell Type", "Adjuvant", "Concentration"
    );

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN VALIDATION METHOD
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates the specified CSV file for consistency among rows sharing the same
     * {@code Condition Number}.
     * <p>
     * For each unique condition number, all attribute columns listed in
     * {@link #REQUIRED} are compared. Any inconsistency between rows is reported
     * as an error. Duplicate errors are suppressed to reduce log noise.
     * </p>
     *
     * @param file           the experiment CSV file to validate
     * @return {@link ValidationResult} containing all consistency check results
     */
    public static ValidationResult check(File file) {
        ValidationResult result = new ValidationResult();
        Set<String> seenErrors = new HashSet<>(); // <-- prevent duplicates

        try (FileReader reader = new FileReader(file);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            Map<String, Map<String, String>> conditionGroups = new HashMap<>();

            for (CSVRecord record : parser) {
                String condition      = record.get("Condition Number");
                String probeName      = record.get("Probe Name");
                String probeType      = record.get("Probe Type");
                String cellType       = record.get("Cell Type");
                String adjuvant       = record.get("Adjuvant");
                String concentration  = record.get("Concentration");

                Map<String, String> current = new LinkedHashMap<>();
                current.put("Probe Name",    probeName);
                current.put("Probe Type",    probeType);
                current.put("Cell Type",     cellType);
                current.put("Adjuvant",      adjuvant);
                current.put("Concentration", concentration);

                // First occurrence of condition — record as baseline
                if (!conditionGroups.containsKey(condition)) {
                    conditionGroups.put(condition, current);
                } else {
                    // Compare against existing baseline
                    Map<String, String> baseline = conditionGroups.get(condition);

                    for (Map.Entry<String, String> entry : current.entrySet()) {
                        String col      = entry.getKey();
                        String value    = entry.getValue();
                        String expected = baseline.get(col);

                        if (!Objects.equals(expected, value)) {
                            String msg = "Condition " + condition
                                    + " - Inconsistent '" + col + "': "
                                    + expected + " / " + value;

                            // Prevent duplicate messages
                            if (seenErrors.add(msg)) {
                                result.addError(msg);
                            }
                        }
                    }
                }
            }

        } catch (IOException e) {
            result.addError("Error reading file during consistency check: " + e.getMessage());
        }

        return result;
    }

    /**
     * Private constructor to prevent instantiation.
     */
    private ConditionConsistencyChecker() {
    }
}