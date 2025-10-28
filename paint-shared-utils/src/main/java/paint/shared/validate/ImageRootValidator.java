/******************************************************************************
 *  Class:        ImageRootValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the existence and integrity of experiment image directories and
 *    corresponding recording files referenced by Experiment Info CSV files.
 *
 *  DESCRIPTION:
 *    • Ensures each experiment listed in the project has a matching image
 *      directory under the global image root.
 *    • Checks that each experiment folder contains its required
 *      “Experiment Info.csv” metadata file.
 *    • Parses each CSV file and confirms that all recordings marked for
 *      processing (Process Flag = true) have corresponding `.nd2` image files
 *      present in the expected image directory.
 *
 *  RESPONSIBILITIES:
 *    • Detect missing experiment directories or image files.
 *    • Cross-validate metadata and file system structure.
 *    • Provide detailed reporting via {@link ValidationResult}.
 *
 *  USAGE EXAMPLE:
 *    List<String> experiments = Arrays.asList("221108", "221122");
 *    ValidationResult result = ImageRootValidator.validateImageRoot(
 *        Paths.get("/Users/hans/Paint Test Project"),
 *        Paths.get("/Volumes/Extreme Pro/Omero"),
 *        experiments
 *    );
 *    System.out.println(result.getReport());
 *
 *  DEPENDENCIES:
 *    – org.apache.commons.csv.{CSVFormat, CSVParser, CSVRecord}
 *    – paint.shared.validate.ValidationResult
 *    – java.nio.file.{Files, Path, Paths}
 *    – java.util.{List, Arrays}
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

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Validates that all required experiment and image directories exist, and that
 * each recording flagged for processing in an Experiment Info file has a
 * corresponding `.nd2` image file in the expected location.
 */
public final class ImageRootValidator {

    /** Standard file name for experiment metadata. */
    private static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";

    // ───────────────────────────────────────────────────────────────────────────────
    // MAIN ENTRY POINT (TEST HARNESS)
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Standalone test harness for local validation of project/image directory pairs.
     *
     * @param args command-line arguments (unused)
     * @throws IOException if any I/O error occurs while reading CSV files
     */
    public static void main(String[] args) throws IOException {
        List<String> experiments = Arrays.asList("221108", "221122");

        ValidationResult result = ImageRootValidator.validateImageRoot(
                Paths.get("/Users/hans/Paint Test Project"),
                Paths.get("/Volumes/Extreme Pro/Omero"),
                experiments
        );

        System.out.println(result.getReport());
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // CORE VALIDATION LOGIC
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that all required recording image files exist under the specified
     * image root for each experiment in the provided list.
     *
     * <p>For each experiment:
     * <ul>
     *   <li>Confirms the corresponding image directory exists under {@code imagesRoot}.</li>
     *   <li>Confirms the experiment folder under {@code projectRoot} contains
     *       the {@code Experiment Info.csv} file.</li>
     *   <li>Parses the CSV to locate all recordings marked with
     *       {@code Process Flag = true}, and verifies each has a matching
     *       {@code .nd2} file in the image directory.</li>
     * </ul>
     *
     * @param projectRoot     path to the local PAINT project root
     * @param imagesRoot      path to the global image repository root
     * @param experimentNames list of experiment identifiers to check
     * @return a {@link ValidationResult} containing all missing files/directories
     */
    public static ValidationResult validateImageRoot(Path projectRoot,
                                                     Path imagesRoot,
                                                     List<String> experimentNames) {
        ValidationResult result = new ValidationResult();

        for (String experiment : experimentNames) {
            Path experimentDir = projectRoot.resolve(experiment);
            Path imageDir = imagesRoot.resolve(experiment);

            // ── 1. Check image directory existence
            if (!Files.isDirectory(imageDir)) {
                result.addError("[" + experiment + "] Missing Image Root: " + imageDir);
                continue;
            }

            // ── 2. Check Experiment Info CSV presence
            Path expInfoFile = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (!Files.exists(expInfoFile)) {
                result.addError("[" + experiment + "] Missing " + EXPERIMENT_INFO_CSV + " in " + experimentDir);
                continue;
            }

            // ── 3. Parse and verify each recording entry - this is a try with resources construct
            try (Reader reader = Files.newBufferedReader(expInfoFile);
                 CSVParser parser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .build()
                         .parse(reader)) {

                for (CSVRecord record : parser) {
                    String recordingName = record.get("Recording Name");
                    String processFlag   = record.get("Process Flag").trim().toLowerCase();

                    if ("true".equals(processFlag)) {
                        Path recordingFile = imageDir.resolve(recordingName + ".nd2");
                        if (!Files.exists(recordingFile)) {
                            result.addError("[" + experiment + "] Missing recording file: " + recordingFile);
                        }
                    }
                }

            } catch (IOException e) {
                result.addError("[" + experiment + "] Error reading " + expInfoFile + ": " + e.getMessage());
            }
        }

        // ── 4. Generate the summary report
        if (!result.hasErrors()) {
            result.setReport("All required image directories and files exist.");
        } else {
            result.setReport(String.join("\n", result.getErrors()));
        }

        return result;
    }

    /** Private constructor to prevent instantiation. */
    private ImageRootValidator() {
        // Deliberately empty
    }
}