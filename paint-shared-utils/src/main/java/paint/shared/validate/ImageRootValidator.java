/*=============================================================================
 *  Class:        ImageRootValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Validates the existence of experiment image directories and checks that
 *    all recordings flagged for processing in each experiment’s metadata have
 *    matching `.nd2` files in the global image root.
 *
 *  DESCRIPTION:
 *    • Confirms the existence of an image directory for each experiment under
 *      the global image root.
 *    • Confirms that each experiment folder contains its required
 *      “Experiment Info.csv”.
 *    • Loads Experiment Info rows via the standard helper method
 *      {@code HelperIO.readExperimentInfo}, ensuring consistent schema parsing.
 *    • For each metadata row where {@code ProcessFlag == true}, verifies that a
 *      corresponding `.nd2` recording file exists in the experiment’s image
 *      directory.
 *
 *  RESPONSIBILITIES:
 *    • Cross-validate Experiment Info metadata and image repository structure.
 *    • Report missing experiment folders, missing metadata files, or missing
 *      recording files.
 *    • Provide a summarised or detailed validation report.
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
 *    – paint.shared.io.HelperIO.readExperimentInfo
 *    – paint.shared.objects.ExperimentInfo
 *    – paint.shared.validate.ValidationResult
 *    – java.nio.file.{Files, Path}
 *    – java.util.List
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
=============================================================================*/

package paint.shared.validate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import paint.shared.objects.ExperimentInfo;

import static paint.shared.constants.PaintFileNames.EXPERIMENT_INFO_CSV;
import static paint.shared.io.ExperimentDataIO.readExperimentInfo;

/**
 * Validates that all required experiment and image directories exist, and that
 * each recording flagged for processing in an Experiment Info file has a
 * corresponding `.nd2` image file in the expected location.
 */
public final class ImageRootValidator {

    /**
     * Private constructor to prevent instantiation.
     */
    private ImageRootValidator() {
        // Deliberately empty
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
     *   <li>Loads the Experiment Info table and verifies that each recording
     *       with {@code Process Flag = true} has a corresponding `.nd2` image.</li>
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
            Path imageDir      = imagesRoot.resolve(experiment);

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

            // ── 3. Load Experiment Info using standard reader (handles schema validation + entity conversion)
            List<ExperimentInfo> rows = readExperimentInfo(experimentDir);
            if (rows == null) {
                result.addError("[" + experiment + "] Cannot parse " + EXPERIMENT_INFO_CSV);
                continue;
            }

            // ── 4. Validate all ProcessFlag=true recordings by checking that an .nd2 file exists in the corresponding image directory
            for (ExperimentInfo experimentInfo : rows) {
                if (experimentInfo.isProcessFlag()) {
                    Path recordingFile = imageDir.resolve(experimentInfo.getRecordingName() + ".nd2");
                    if (!Files.exists(recordingFile)) {
                        result.addError("[" + experiment + "] Missing recording file: " + recordingFile);
                    }
                }
            }
        }

        // ── 5. Generate the summary report
        if (!result.hasErrors()) {
            result.setReport("All required image directories and files exist.");
        } else {
            result.setReport(String.join("\n", result.getErrors()));
        }

        return result;
    }
}