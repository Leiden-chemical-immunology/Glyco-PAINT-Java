/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package createexperiment.app;

import paint.shared.objects.ExperimentInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static paint.shared.constants.PaintFileNames.EXPERIMENT_INFO_CSV;
import static paint.shared.io.MainIOInterface.writeSpecificExperimentInfoFile;

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
     * @param experimentDirPath the directory where the experiment CSV will be created
     * @param recordings        list of ND2 recording files to process
     * @return the created {@code File} object pointing to the resulting CSV file
     * @throws IOException if directory creation or file writing fails
     */
    public static Path exportExperimentInfo(Path experimentDirPath, List<File> recordings) throws IOException {
        Files.createDirectories(experimentDirPath);

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

        // --- Write table
        Path csvFilePath = uniqueFile(experimentDirPath, EXPERIMENT_INFO_CSV);
        writeSpecificExperimentInfoFile(csvFilePath, infos);

        return csvFilePath;

    }

    /**
     * Ensures the output filename is unique by appending an incremented suffix if needed.
     *
     * @param dirPath      directory for the file
     * @param fileName desired file name
     * @return a unique path reference that does not overwrite existing files
     */
    @SuppressWarnings("SameParameterValue")
    private static Path uniqueFile(Path dirPath, String fileName) {
        int    dot  = fileName.lastIndexOf('.');
        String stem = (dot >= 0) ? fileName.substring(0, dot) : fileName;
        String ext  = (dot >= 0) ? fileName.substring(dot) : "";

        Path candidate = dirPath.resolve(fileName);
        int n = 1;

        while (Files.exists(candidate)) {
            candidate = dirPath.resolve(stem + "-" + n + ext);
            n++;
        }

        return candidate;
    }
}