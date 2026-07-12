/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.validate;
import static paint.shared.constants.PaintStringConstants.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Performs cross-row consistency checks for condition metadata in PAINT experiment CSVs.
 * <p>
 * Ensures that all rows with the same CONDITION_NUMBER share identical values for
 * critical attributes (probe, cell type, adjuvant, concentration, etc.).
 * </p>
 * The function can be called ob Experiment Info amd Recordings CSVs.
 */
public final class ConditionConsistencyChecker {

    /**
     * Private constructor to prevent instantiation.
     */
    private ConditionConsistencyChecker() {
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN VALIDATION METHOD
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates the specified CSV file for consistency among rows sharing the same
     * {@code Condition Number}.
     * <p>
     * For each unique condition number, all attribute columns are compared.
     * Any inconsistency between rows is reported
     * as an error. Duplicate errors are suppressed to reduce log noise.
     * </p>
     *
     * @param file           the experiment CSV file to validate
     * @return {@link ValidationResult} containing all consistency check results
     */
    public static ValidationResult check(File file) {
        ValidationResult result     = new ValidationResult();
        Set<String>      seenErrors = new HashSet<>(); // <-- prevent duplicates

        // Cannot use a standard reading method, because this one covers two different formats
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
             CSVParser  parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            /*
             * A condition group looks like this:
             *  Condition 3:
             *      ProbeName     = 2 Tri
             *      ProbeType     = Simple
             *      CellType      = CHO-MR
             *      Adjuvant      = LPS
             *      Concentration = 5.0
             * All entries with condition 3 need to have the same values for all attributes
             */

            Map<String, Map<String, String>> conditionGroups = new HashMap<>();

            for (CSVRecord record : parser) {
                String condition      = record.get(CONDITION_NUMBER);
                String probeName      = record.get(PROBE_NAME);
                String probeType      = record.get(PROBE_TYPE);
                String cellType       = record.get(CELL_TYPE);
                String adjuvant       = record.get(ADJUVANT);
                String concentration  = record.get(CONCENTRATION);

                Map<String, String> currentAttributes = new LinkedHashMap<>();
                currentAttributes.put(PROBE_NAME,    probeName);
                currentAttributes.put(PROBE_TYPE,    probeType);
                currentAttributes.put(CELL_TYPE,     cellType);
                currentAttributes.put(ADJUVANT,      adjuvant);
                currentAttributes.put(CONCENTRATION, concentration);

                if (!conditionGroups.containsKey(condition)) {   // First occurrence of condition — record as baseline
                    conditionGroups.put(condition, currentAttributes);
                } else {                                         // Compare against existing baseline
                    Map<String, String> baseline = conditionGroups.get(condition);
                    for (Map.Entry<String, String> entry : currentAttributes.entrySet()) {
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
}