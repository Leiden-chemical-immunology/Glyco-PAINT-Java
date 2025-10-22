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
 * Utility for validating that experiment image directories and
 * recording files exist as required by Experiment Info files.
 */
public class ImageRootValidator {

    private static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";

    public static void main(String[] args) throws IOException {
        List<String> experiments = Arrays.asList("221108", "221122");

        ValidationResult result = ImageRootValidator.validateImageRoot(
                Paths.get("/Users/hans/Paint Test Project"),
                Paths.get("/Volumes/Extreme Pro/Omero"),
                experiments
        );

        System.out.println(result.getReport());
    }

    /**
     * Validate that all required recording files exist in the image root.
     *
     * @param projectRoot     path to the project root
     * @param imagesRoot      path to the images root
     * @param experimentNames list of experiment names to check
     * @return ValidationResult with all missing directories/files
     */
    public static ValidationResult validateImageRoot(Path projectRoot,
                                                     Path imagesRoot,
                                                     List<String> experimentNames) {
        ValidationResult result = new ValidationResult();

        for (String experiment : experimentNames) {
            Path experimentDir = projectRoot.resolve(experiment);
            Path imageDir = imagesRoot.resolve(experiment);

            // --- 1. Image directory must exist
            if (!Files.isDirectory(imageDir)) {
                result.addError("[" + experiment + "] Missing corresponding Image Root: " + imageDir);
                continue;
            }

            // --- 2. Experiment Info file must exist
            Path expInfoFile = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (!Files.exists(expInfoFile)) {
                result.addError("[" + experiment + "] Missing " + EXPERIMENT_INFO_CSV + " in " + experimentDir);
                continue;
            }

            // --- 3. Read the CSV file using Apache Commons CSV
            try (Reader reader = Files.newBufferedReader(expInfoFile);
                 CSVParser parser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .build()
                         .parse(reader)) {

                for (CSVRecord record : parser) {
                    String recordingName = record.get("Recording Name");
                    String processFlag = record.get("Process Flag").trim().toLowerCase();

                    if (processFlag.equals("true")) {
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

        if (!result.hasErrors()) {
            result.setReport("All required image directories and files exist.");
        } else {
            result.setReport(String.join("\n", result.getErrors()));
        }

        return result;
    }
}