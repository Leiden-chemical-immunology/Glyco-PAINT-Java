/******************************************************************************
 *  Class:        ExperimentInfoWriter.java
 *  Package:      createexperiment
 *
 *  PURPOSE:
 *    Generates and writes the "Experiment Info.csv" file based on selected ND2
 *    recording files. Parses metadata such as condition and replicate numbers
 *    directly from filenames and ensures unique output file naming.
 *
 *  DESCRIPTION:
 *    This class automates the creation of experiment metadata tables by
 *    transforming ND2 file selections into structured CSV records suitable for
 *    downstream processing in the Paint workflow. Filenames are parsed using
 *    regex patterns to extract condition and replicate identifiers, and
 *    ExperimentInfo objects are serialized via Tablesaw utilities.
 *
 *  KEY FEATURES:
 *    • Regex-based filename parsing for condition and replicate extraction.
 *    • Automatic creation of experiment directories if missing.
 *    • Guarantees non-overwriting output via unique filename generation.
 *    • Integration with ExperimentInfoTableIO for CSV conversion.
 *    • Lightweight, file-based I/O without GUI dependencies.
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  MODULE:
 *    paint-create-experiment
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *    Licensed under the MIT License.
 ******************************************************************************/

package createexperiment;

import paint.shared.io.ExperimentInfoTableIO;
import paint.shared.objects.ExperimentInfo;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;

/**
 * Provides functionality to generate and write an experiment information CSV file
 * based on a list of ND2 recording files. Automatically parses and extracts condition
 * and replicate numbers from filenames, and ensures unique file names to avoid overwriting.
 */
public class ExperimentInfoWriter {

    /**
     * Regular expression pattern for extracting metadata from filenames of experiment recordings.
     * <p>
     * Expected filename format:
     * <pre>
     * [digits]-Exp-[digits]-[uppercase letter][digits]-[digits].nd2
     * </pre>
     * Groups:
     * <ul>
     *   <li>Group 1 – Condition number</li>
     *   <li>Group 2 – Replicate number</li>
     * </ul>
     */
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^\\d+-Exp-(\\d+)-[A-Z]\\d+-(\\d+)\\.nd2$");

    /**
     * Writes experiment recording metadata into a CSV file located in the specified experiment directory.
     * <p>
     * Each ND2 recording file is parsed into an {@link ExperimentInfo} instance containing structured
     * metadata such as condition and replicate numbers. The resulting table is serialized as
     * {@code Experiment Info.csv}, with numeric suffixing to avoid overwriting existing files.
     *
     * @param experimentDir the directory where the experiment CSV will be created
     * @param recordings    list of ND2 recording files to process
     * @return the created {@code File} object pointing to the resulting CSV file
     * @throws IOException if directory creation or file writing fails
     */
    public static File writeExperimentInfo(File experimentDir, List<File> recordings) throws IOException {
        if (!experimentDir.exists() && !experimentDir.mkdirs()) {
            throw new IOException("Failed to create experiment directory: " + experimentDir);
        }

        // Build ExperimentInfo objects from filenames
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
     * Ensures the output filename is unique by appending an incremented suffix if needed.
     *
     * @param dir      directory for the file
     * @param fileName desired file name
     * @return a unique file reference that does not overwrite existing files
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