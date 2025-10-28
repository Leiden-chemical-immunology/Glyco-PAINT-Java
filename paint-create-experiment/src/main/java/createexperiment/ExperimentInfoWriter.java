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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;

/**
 * The {@code ExperimentInfoWriter} class provides functionality to generate
 * and write an experiment information CSV file based on a list of ND2 recording
 * files. It automatically parses and extracts condition and replicate numbers
 * from file names, and ensures unique file names to avoid overwriting.
 */
public class ExperimentInfoWriter {

    /**
     * A compiled regular expression pattern used to validate and extract metadata
     * from filenames of experiment recording files.
     *
     * The expected filename format is:
     * <code>[digits]-Exp-[digits]-[uppercase letter][digits]-[digits].nd2</code>
     * where:
     * - The initial digits segment represents a primary identifier.
     * - "Exp" is a constant string in the filename.
     * - The subsequent digits represent a secondary identifier.
     * - The uppercase letter followed by digits is another segment of the metadata.
     * - The final digits (before the file extension) represent an additional identifier.
     *
     * Groups within the pattern:
     * 1. Captures part of the secondary identifier.
     * 2. Captures the final identifier from the filename.
     *
     * This pattern is used to parse filenames and extract metadata for further processing
     * when generating experiment information.
     */
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("^\\d+-Exp-(\\d+)-[A-Z]\\d+-(\\d+)\\.nd2$");

    /**
     * Writes information about a set of experiment recordings into a CSV file in the specified directory.
     *
     * The method processes a list of recording files to generate experiment information, such as condition
     * numbers, replicate numbers, and other metadata. It wraps this information in a tabular format and
     * writes it to a file named "Experiment Info.csv" (or a unique variant to avoid overwriting existing files).
     *
     * @param experimentDir the directory where the experiment information file will be created; if it does not
     *                      exist, it will attempt to create the directory
     * @param recordings    the list of recording files to process and extract metadata from
     * @return a {@code File} object pointing to the written CSV file containing the experiment information
     * @throws IOException if the experiment directory cannot be created, or if the file writing process fails
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
     * Creates a unique file within the specified directory by appending a numerical suffix to the file name if
     * a file with the given name already exists. This ensures that the returned file does not overwrite any
     * existing file in the directory.
     *
     * @param dir the directory in which the file will be created
     * @param fileName the desired name of the file (including extension, if any)
     * @return a {@code File} object representing the unique file within the specified directory
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