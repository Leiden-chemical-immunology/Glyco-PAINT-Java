/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package convert;

import paint.shared.validate.ValidationResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static paint.shared.constants.PaintFileNames.*;
import static paint.shared.validate.ValidationHandler.validateExperiments;

/**
 * Base class for data conversion utilities within the Glyco-PAINT framework.
 */
public class Converter {

    // Simple test main
    public static void main(String[] args) throws Exception {

        Path projectPath = Paths.get("/Users/hans/Downloads");
        Path inputDir    = projectPath.resolve("221012 - v39");

        new RecordingsConverter(inputDir).run();
        new TracksConverter(inputDir).run();
        new ExperimentInfoConverter(inputDir).run();
        new SquaresConverter(inputDir).run();

        List<String> experimentNames = Collections.singletonList(
                "221012 - v39"
        );

        List<String> fileNames = Arrays.asList(
                EXPERIMENT_INFO_CSV,
                RECORDINGS_CSV,
                SQUARES_CSV,
                TRACKS_CSV
        );

        ValidationResult result =  validateExperiments(projectPath, experimentNames, fileNames);
        if (result.hasErrors() || result.hasWarnings()) {
            System.out.println(result);
        }
        else {
            System.out.println("No Issues found");
        }
    }
}
