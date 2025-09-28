package createexperiment;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_COLS;
import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;

public class ExperimentInfoWriter {

    // Regex to capture condition + replicate
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^\\d+-Exp-(\\d+)-[A-Z]\\d+-(\\d+)\\.nd2$");

    public static File writeExperimentInfo(File experimentDir, List<File> recordings) throws IOException {
        if (!experimentDir.exists() && !experimentDir.mkdirs()) {
            throw new IOException("Failed to create experiment directory: " + experimentDir);
        }

        File csvFile = uniqueFile(experimentDir, EXPERIMENT_INFO_CSV);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(EXPERIMENT_INFO_COLS)
                .build();

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(csvFile), format)) {
            for (File rec : recordings) {
                Object[] row = new Object[EXPERIMENT_INFO_COLS.length];
                String name = rec.getName();

                // Try regex match
                Matcher m = FILENAME_PATTERN.matcher(name);
                Integer condition = null;
                Integer replicate = null;
                if (m.matches()) {
                    condition = Integer.valueOf(m.group(1));
                    replicate = Integer.valueOf(m.group(2));
                }

                for (int i = 0; i < EXPERIMENT_INFO_COLS.length; i++) {
                    String col = EXPERIMENT_INFO_COLS[i];
                    if ("Recording Name".equalsIgnoreCase(col)) {
                        row[i] = name;
                    } else if ("Condition Number".equalsIgnoreCase(col)) {
                        row[i] = (condition != null) ? condition : "";
                    } else if ("Replicate Number".equalsIgnoreCase(col)) {
                        row[i] = (replicate != null) ? replicate : "";
                    } else if ("Process Flag".equalsIgnoreCase(col)) {
                        row[i] = true;
                    } else {
                        row[i] = "";
                    }
                }

                printer.printRecord(row);
            }
        }

        return csvFile;
    }

    private static File uniqueFile(File dir, String fileName) {
        int dot = fileName.lastIndexOf('.');
        String stem = (dot >= 0) ? fileName.substring(0, dot) : fileName;
        String ext  = (dot >= 0) ? fileName.substring(dot) : "";

        File candidate = new File(dir, fileName);
        int n = 1;
        while (candidate.exists()) {
            candidate = new File(dir, stem + "-" + n + ext);
            n++;
        }
        return candidate;
    }
}