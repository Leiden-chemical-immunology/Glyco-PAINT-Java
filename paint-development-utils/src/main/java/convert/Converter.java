package convert;

import paint.shared.validate.ValidationResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static paint.shared.constants.PaintFileNames.*;
import static paint.shared.constants.PaintFileNames.TRACKS_CSV;
import static paint.shared.validate.ValidationHandler.validateExperiments;

public class Converter {

    // Simple test main
    public static void main(String[] args) throws Exception {

        Path projectPath = Paths.get("/Users/hans/Downloads");
        Path inputDir    = projectPath.resolve("221012 - v39");

        new RecordingsConverter(inputDir).run();
        new TracksConverter(inputDir).run();
        new ExperimentInfoConverter(inputDir).run();
        new SquaresConverter(inputDir).run();

        List<String> experimentNames = Arrays.asList(
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
            System.out.println(result.toString());
        }
        else {
            System.out.println("No Issues found");
        }
    }
}
