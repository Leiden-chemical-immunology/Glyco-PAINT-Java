package paint.fiji.utils;

import java.io.IOException;
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
                continue; // skip to next test
            }

            // --- 2. Experiment Info file must exist
            Path expInfoFile = experimentDir.resolve(EXPERIMENT_INFO_CSV);
            if (!Files.exists(expInfoFile)) {
                report.add("[Experiment " + experiment + "] Missing " + EXPERIMENT_INFO_CSV + " in " + experimentDir);
                continue;
            }

            // ---3. Read the recordings to determine which image file should be there
            try {
                List<String> lines = Files.readAllLines(expInfoFile);
                if (lines.size() <= 1) {
                    continue; // no recordings
                }

                // Header line
                String[] headers = lines.get(0).split(",");
                int recordingIdx = findColumnIndex(headers, "Recording Name");
                int flagIdx = findColumnIndex(headers, "Process Flag");

                if (recordingIdx < 0 || flagIdx < 0) {
                    report.add("[Experiment " + experiment + "] Missing required columns in Experiment Info");
                    continue;
                }

                // Process rows
                for (int i = 1; i < lines.size(); i++) {
                    String[] cols = lines.get(i).split(",");
                    if (cols.length <= Math.max(recordingIdx, flagIdx)) {
                        continue; // malformed row
                    }

                    String recordingName = cols[recordingIdx].trim();
                    String processFlag = cols[flagIdx].trim().toLowerCase();

                    if (processFlag.equals("true")) {
                        Path recordingFile = imageDir.resolve(recordingName + ".nd2");
                        if (!Files.exists(recordingFile)) {
                            report.add("[Experiment " + experiment + "] Missing recording file: " + recordingFile);
                        }
                    }
                }

            } catch (IOException e) {
                report.add("[Experiment " + experiment + "] Error reading " + expInfoFile + ": " + e.getMessage());
            }
        }

        return report;
    }

    private static int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(columnName)) {
                return i;
            }
        }
        return -1;
    }
}