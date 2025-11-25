package convert;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Converter {

    // Simple test main
    public static void main(String[] args) throws Exception {
        Path inputDir = Paths.get("/Users/hans/Downloads/221012 - v39");

        new RecordingsConverter(inputDir).run();
        new TracksConverter(inputDir).run();
        new ExperimentInfoConverter(inputDir).run();
        new SquaresConverter(inputDir).run();
    }
}
