package paint.fijiUtilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import config.PaintConfig;
import config.TrackMateConfig;
import utilities.AppLogger;
import utilities.Miscellaneous;

import static constants.PaintConstants.*;
import static paint.fijiUtilities.CsvConcatenator.concatenateCsvFiles;
import static utilities.CsvUtils.countProcessed;

public class RunTrackMateOnExperiment {

    static boolean verbose = false;

    public static void cycleThroughRecordings(Path experimentPath, Path imagesPath) {

        Duration totalDuration = Duration.ZERO;
        int numberRecordings = 0;

        // Read the JSON configuration file from the experiment directory
        // If it does not exist, a new one will be created with default values.
        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        PaintConfig paintConfig = PaintConfig.from(configPath);
        config.TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);

        if (verbose) {
            System.out.println(paintConfig);
            System.out.println(trackMateConfig);
        }


        // Read the Experiment info.csv file and process the indicated recordings
        Path experimentFilePath = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentFilePath.getParent().resolve(RECORDINGS_CSV);
        if (!Files.exists(experimentFilePath)) {
            AppLogger.errorf("Experiment info file does not exist in %s.", experimentFilePath);
            return;
        }

        int numberRecordingsToProcess = countProcessed(experimentFilePath);

        String experimentName = experimentPath.getFileName().toString();
        String projectName = experimentPath.getParent().getFileName().toString();

        AppLogger.infof("");
        AppLogger.infof("Processing %d recordings in experiment '%s' in project '%s'.",  numberRecordingsToProcess, experimentName, projectName);
        try (BufferedReader reader = Files.newBufferedReader(experimentFilePath);
             BufferedWriter writer = Files.newBufferedWriter(allRecordingFilePath)) {String headerLine = reader.readLine();
            if (headerLine == null) {
                AppLogger.errorf("Experiment info file in %s is empty.", experimentFilePath);
                return;
            }

            // Build the header for the 'All Recordings Java' file
            StringBuilder header = new StringBuilder(headerLine);
            header.append(",").append("Number of Spots");
            header.append(",").append("Number of Tracks");
            header.append(",").append("Number of Spots in All Tracks");
            header.append(",").append("Number of Frames");
            header.append(",").append("Run Time");
            header.append(",").append("Time Stamp");
            header.append(",").append("Exclude");
            header.append(",").append("Tau");
            header.append(",").append("R Squared");
            header.append(",").append("Density");

            // Write the header to the 'All Recordings Java' file
            writer.write(header.toString());
            writer.newLine();

            // Split the header by tab into field names
            String[] headers = headerLine.split(",");

            String line;
            TrackMateResults trackMateResults = null;

            int numberOfSpots;
            int numberOfTracks;
            int numberOfSpotsInAllTracks;
            int numberOfFrames;
            int runTime;
            String timeStamp;

            while ((line = reader.readLine()) != null) {
                // split row into values
                String[] fields = line.split(",", -1);  // -1 keeps empty fields

                // Convert to map if needed
                Map<String, String> row = new LinkedHashMap<>();
                // String[] headers = headerLine.split(",");
                for (int i = 0; i < headers.length && i < fields.length; i++) {
                    row.put(headers[i], fields[i]);
                }

                ExperimentInfoRecord experimentInfoRecord = ExperimentInfoRecord.fromRow(row);

                if (experimentInfoRecord.process) {
                    double threshold = experimentInfoRecord.threshold;

                    // Perform TrackMate processing

                    trackMateResults = RunTrackMate.RunTrackMate(experimentPath, imagesPath, trackMateConfig, threshold, experimentInfoRecord);

                    if (trackMateResults == null || !trackMateResults.isSuccess()) {
                        AppLogger.errorf("TrackMate processing failed for recording '%s'.", experimentInfoRecord.recordingName);
                        return; //ToDo
                    }
                    AppLogger.infof("   Recording '%s' (%d of %d) processed in %s.",
                            experimentInfoRecord.recordingName,
                            numberRecordings + 1,
                            numberRecordingsToProcess,
                            Miscellaneous.formatDuration(trackMateResults.getDuration()));
                    totalDuration = totalDuration.plus(trackMateResults.getDuration());
                    numberRecordings += 1;

                    numberOfSpots = trackMateResults.getNumberOfSpots();
                    numberOfTracks = trackMateResults.getNumberOfTracks();
                    numberOfFrames = trackMateResults.getNumberOfFrames();
                    numberOfSpotsInAllTracks = trackMateResults.getNumberOfSpotsInALlTracks();
                    runTime = (int) trackMateResults.getDuration().toMillis() / 1000;
                    timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                else {
                    AppLogger.infof("   Skipped processing of recording '%s'.", experimentInfoRecord.recordingName);
                    numberOfSpots = 0;
                    numberOfTracks = 0;
                    numberOfFrames = 0;
                    numberOfSpotsInAllTracks = 0;
                    runTime = 0;
                    timeStamp = "";
                }

                // Build output row

                StringBuilder out = new StringBuilder(line);
                     out.append(",").append(String.format("%d", numberOfSpots))                  // Number of Spots
                        .append(",").append(String.format("%d", numberOfTracks))                 // Number of Tracks
                        .append(",").append(String.format("%d", numberOfSpotsInAllTracks))       // Numbe of spots in all tracks
                        .append(",").append(String.format("%d", numberOfFrames))                 // Number of Frames
                        .append(",").append(String.format("%d", runTime))                        // Run Time needs to be an int
                        .append(",").append(String.format("%s", timeStamp))                      // Timestamp
                        .append(",").append("False")                                                  // Exclude
                        .append(",").append("")                                                  // Tau
                        .append(",").append("")                                                  // R Squared
                        .append(",").append("");                                                 // Density

                writer.write(out.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            AppLogger.errorf("Error reading Experiment Info file '%s': %s", experimentFilePath, e.getMessage());
            e.printStackTrace();
        }

        // Concatenate the 'All Tracks Java' file
        Path tracksFilePath = experimentPath.resolve(TRACKS_CSV);
        try {
            concatenateCsvFiles(experimentPath, tracksFilePath);
        } catch (IOException e) {
            AppLogger.errorf("Error concatenating tracks file file %s: %s", experimentFilePath, e.getMessage());
            e.printStackTrace();
        }

        AppLogger.infof("Processed %d recordings in %s.", numberRecordings, Miscellaneous.formatDuration(totalDuration));
        AppLogger.infof("");
    }
}
