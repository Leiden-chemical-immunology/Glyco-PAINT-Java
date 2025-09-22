package paint.shared.validate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.util.*;

/**
 * Utility for validating that experiment image directories and
 * recording files exist as required by Experiment Info files.
 */
public class ImageRootValidator {

    public static void main(String[] args) throws IOException {

        List<String> experiments = Arrays.asList("221108", "221122");

        List<String> report = ImageRootValidator.validateImageRoot(
                Paths.get("/Users/hans/Paint Test Project"),
                Paths.get("/Volumes/Extreme Pro/Omero"),
                experiments
        );
        report.forEach(System.out::println);
    }

    private static final String EXPERIMENT_INFO_CSV = "Experiment Info.csv";

    /**
     * Validate image root consistency.
     *
     * @param projectRoot path to the project root
     * @param imagesRoot  path to the images root
     * @param experimentNames list of experiment names to check
     * @return list of report lines (missing dirs/files)
     */
    public static List<String> validateImageRoot(Path projectRoot,
                                                 Path imagesRoot,
                                                 List<String> experimentNames) {
        List<String> report = new ArrayList<>();

        for (String experiment : experimentNames) {
            Path experimentDir = projectRoot.resolve(experiment);
            Path imageDir = imagesRoot.resolve(experiment);

            // --- 1. Image directory must exist
            if (!Files.isDirectory(imageDir)) {
                report.add("[Experiment " + experiment + "] Missing image directory: " + imageDir);
                continue;
            }

            // --- 2. Experiment Info file must exist
            Path expInfoFile = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (!Files.exists(expInfoFile)) {
                report.add("[Experiment " + experiment + "] Missing " + EXPERIMENT_INFO_CSV + " in " + experimentDir);
                continue;
            }

            // --- 3. Read the CSV file using Apache Commons CSV
            try (Reader reader = Files.newBufferedReader(expInfoFile);
                 CSVParser parser = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                         .setHeader()
                         .setSkipHeaderRecord(true)
                         .build()
                         .parse(reader)) {

                Map<String, Map<String, String>> conditionGroups = new HashMap<>();

                for (CSVRecord record : parser) {
                    String recordingName = record.get("Recording Name");
                    String processFlag = record.get("Process Flag").trim().toLowerCase();

                    if (processFlag.equals("true")) {
                        Path recordingFile = imageDir.resolve(recordingName + ".nd2");
                        if (!Files.exists(recordingFile)) {
                            report.add("[Experiment " + experiment + "] Missing recording file: " + recordingFile);
                        }
                    }

                    String condition = record.get("Condition Number");
                    String probeName = record.get("Probe Name");
                    String probeType = record.get("Probe Type");
                    String cellType  = record.get("Cell Type");
                    String adjuvant  = record.get("Adjuvant");
                    String conc      = record.get("Concentration");

                    Map<String, String> currentAttributes = new LinkedHashMap<>();
                    currentAttributes.put("Probe Name", probeName);
                    currentAttributes.put("Probe Type", probeType);
                    currentAttributes.put("Cell Type", cellType);
                    currentAttributes.put("Adjuvant", adjuvant);
                    currentAttributes.put("Concentration", conc);

                    if (!conditionGroups.containsKey(condition)) {
                        conditionGroups.put(condition, currentAttributes);
                    } else {
                        Map<String, String> expectedAttributes = conditionGroups.get(condition);
                        if (!expectedAttributes.equals(currentAttributes)) {
                            report.add("[Experiment " + experiment + "] Inconsistent attributes for Condition Number: " + condition +
                                    "\n → Expected: " + expectedAttributes +
                                    "\n → Found:    " + currentAttributes);
                        }
                    }
                }

            } catch (IOException e) {
                report.add("[Experiment " + experiment + "] Error reading " + expInfoFile + ": " + e.getMessage());
            }
        }

        return report;
    }
}
