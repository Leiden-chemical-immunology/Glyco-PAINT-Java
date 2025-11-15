package paint.viewer.override;

import paint.shared.io.RecordingsTableIO;
import paint.shared.io.SquaresTableIO;
import tech.tablesaw.api.Table;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import paint.viewer.override.RecordingOverride;
import static paint.viewer.override.RecordingOverrideApplier.loadRecordingOverride;
import static paint.viewer.override.SquareOverrideApplier.loadSquareOverride;

public class OverrideTool {

    public static void main(String[] args) {

        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: java -cp paint-viewer.jar paint.viewer.cli.OverrideTool <Project-Path> <Extension>");
            System.exit(1);
        }

        if (args.length == 2) {
            String extension = args[1];
        }

        // Does the project root exist?
        Path projectPath = Paths.get(args[0]);
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            System.err.println("Error: Project path does not exist or is not a directory: " + projectPath);
            System.exit(2);
        }

        // Read the Recordings if it exists
        Table recordingsTable;
        Path  recordingsCsvPath = projectPath.resolve("Recordings.csv");

        if (!Files.exists(recordingsCsvPath)) {
            System.err.println("Info: Recordings file does not exist: " + recordingsCsvPath);
            System.exit(2);
        }

        RecordingsTableIO recordingsTableIO = new RecordingsTableIO();
        try {
            recordingsTable = recordingsTableIO.readCsv(recordingsCsvPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Read the Squares if it exists
        Table squaresTable;
        Path  squaresCsvPath = projectPath.resolve("Squares.csv");

        if (!Files.exists(squaresCsvPath)) {
            System.err.println("Info: Squares file does not exist: " + squaresCsvPath);
            System.exit(2);
        }

        SquaresTableIO squaresTableIO = new SquaresTableIO();
        try {
            squaresTable   = squaresTableIO.readCsv(squaresCsvPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Read the Recordings Override if it exists
        Path recordingOverridePath = projectPath.resolve("Recording Override.csv");
        List<RecordingOverride> recordingOverrides = null;
        if (Files.exists(recordingOverridePath)) {
            recordingOverrides = loadRecordingOverride(recordingOverridePath);
        }

        // Read the Squares Override if it exists
        Path squareOverridePath = projectPath.resolve("Square Override.csv");
        if (Files.exists(squareOverridePath)) {
            List<SquareOverride> squareOverrides = loadSquareOverride(squareOverridePath);
        }


        if (recordingOverrides != null) {

        }
    }



}