package paint.shared.validate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ExperimentInfoValidator {

    public static List<String> validate(Path experimentInfoCsv, String experimentName) throws IOException {
        List<String> report = new ArrayList<>();

        try (Reader reader = Files.newBufferedReader(experimentInfoCsv);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {

            List<String> required = Arrays.asList(
                    "Condition Number", "Probe Name", "Probe Type", "Cell Type", "Adjuvant", "Concentration"
            );

            Map<String, Map<String, String>> conditionGroups = new HashMap<>();

            for (CSVRecord record : parser) {
                boolean missingField = required.stream().anyMatch(f -> !record.isMapped(f));
                if (missingField) {
                    for (String key : required) {
                        if (!record.isMapped(key)) {
                            report.add("[Experiment " + experimentName + "] Missing required column: " + key);
                        }
                    }
                    return report;
                }

                String condition = record.get("Condition Number");
                String probeName = record.get("Probe Name");
                String probeType = record.get("Probe Type");
                String cellType  = record.get("Cell Type");
                String adjuvant  = record.get("Adjuvant");
                String conc      = record.get("Concentration");

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

                        report.add("[Experiment " + experimentName + "] Inconsistent attributes for Condition Number: " + condition +
                                "\n→ Expected: [Probe Name=" + attributes.get("Probe Name") +
                                ", Probe Type=" + attributes.get("Probe Type") +
                                ", Cell Type=" + attributes.get("Cell Type") +
                                ", Adjuvant=" + attributes.get("Adjuvant") +
                                ", Concentration=" + attributes.get("Concentration") + "]" +
                                "\n→ Found:    [Probe Name=" + probeName +
                                ", Probe Type=" + probeType +
                                ", Cell Type=" + cellType +
                                ", Adjuvant=" + adjuvant +
                                ", Concentration=" + conc + "]");
                    }
                }
            }
        }

        return report;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java paint.fiji.utils.ExperimentInfoValidator <ExperimentInfo.csv>");
            System.exit(1);
        }

        Path path = Paths.get(args[0]);
        String name = path.getParent() != null ? path.getParent().getFileName().toString() : "Experiment";

        List<String> report = validate(path, name);
        if (report.isEmpty()) {
            System.out.println("✔ No inconsistencies found in " + path.getFileName());
        } else {
            System.out.println("⚠ Inconsistencies found:");
            for (String line : report) {
                System.out.println(line);
            }
        }
    }
}
