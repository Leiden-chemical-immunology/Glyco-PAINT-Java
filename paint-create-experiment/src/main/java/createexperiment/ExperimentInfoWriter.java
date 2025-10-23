/**
 * Part of the Paint project.
 * Copyright (c) 2025 Hans Bakker.
 * Licensed under the MIT License.
 */

package createexperiment;

import paint.shared.io.ExperimentInfoTableIO;
import paint.shared.objects.ExperimentInfo;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;

/**
 * Writes {@code Experiment Info.csv} files for Paint experiments using the
 * {@link paint.shared.io.ExperimentInfoTableIO} class for schema enforcement.
 *
 * <p>This class is used by {@link createexperiment.CreateExperimentUI} to
 * generate a CSV file describing all recordings in a new experiment directory.
 * It automatically detects condition and replicate numbers from ND2 file names
 * (if formatted according to Paint conventions) and fills in baseline metadata
 * as defined by {@code PaintConstants.EXPERIMENT_INFO_COLS}.</p>
 *
 * <p>The resulting CSV strictly conforms to the Paint schema and can be safely
 * loaded later by Paint processing tools or Fiji plugins using
 * {@link paint.shared.io.ExperimentInfoTableIO#readCsv(Path)}.</p>
 */
public class ExperimentInfoWriter {

    /**
     * Regular expression to extract condition and replicate numbers
     * from ND2 file names.
     *
     * <p>Expected example: {@code 1-Exp-2-A1-3.nd2}</p>
     * <ul>
     *     <li>Group 1 → Condition Number</li>
     *     <li>Group 2 → Replicate Number</li>
     * </ul>
     */
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^\\d+-Exp-(\\d+)-[A-Z]\\d+-(\\d+)\\.nd2$");

    /**
     * Writes a new {@code Experiment Info.csv} file in the given experiment directory.
     *
     * <p>For each provided recording file, a corresponding {@link ExperimentInfo}
     * record is created and written using {@link ExperimentInfoTableIO}.
     * Duplicate CSV names are avoided automatically by incrementing
     * {@code -1}, {@code -2}, etc.</p>
     *
     * @param experimentDir the directory where the CSV file should be created
     * @param recordings    the list of ND2 recording files to include
     * @return the created CSV file
     * @throws IOException if the directory cannot be created or written
     */
    public static File writeExperimentInfo(File experimentDir, List<File> recordings) throws IOException {
        if (!experimentDir.exists() && !experimentDir.mkdirs()) {
            throw new IOException("Failed to create experiment directory: " + experimentDir);
        }

        // --- Create ExperimentInfo entities ---
        List<ExperimentInfo> infos = new ArrayList<>();

        for (File rec : recordings) {
            String name = rec.getName();
            ExperimentInfo info = new ExperimentInfo();
            info.setRecordingName(name);

            // Try to parse condition and replicate from filename
            Matcher m = FILENAME_PATTERN.matcher(name);
            if (m.matches()) {
                info.setConditionNumber(Integer.parseInt(m.group(1)));
                info.setReplicateNumber(Integer.parseInt(m.group(2)));
            } else {
                info.setConditionNumber(0);
                info.setReplicateNumber(0);
            }

            // Default values
            info.setProcessFlag(true);
            info.setProbeName("");
            info.setProbeType("");
            info.setCellType("");
            info.setAdjuvant("");
            info.setConcentration(0.0);
            info.setThreshold(0.0);

            infos.add(info);
        }

        // --- Write table via ExperimentInfoTableIO ---
        ExperimentInfoTableIO io = new ExperimentInfoTableIO();
        Table table = io.toTable(infos);

        File csvFile = uniqueFile(experimentDir, EXPERIMENT_INFO_CSV);
        io.writeCsv(table, csvFile.toPath());

        return csvFile;
    }

    /**
     * Generates a unique file name to avoid overwriting existing CSV files.
     *
     * <p>For example, if {@code Experiment Info.csv} already exists,
     * it will generate {@code Experiment Info-1.csv}, {@code Experiment Info-2.csv}, etc.</p>
     *
     * @param dir      the directory to check for existing files
     * @param fileName the desired file name
     * @return a new {@code File} object pointing to a non-existent file
     */
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