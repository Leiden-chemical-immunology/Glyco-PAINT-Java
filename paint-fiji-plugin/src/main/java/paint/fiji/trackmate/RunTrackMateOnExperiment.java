package paint.fiji.trackmate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.scijava.app.App;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.utils.AppLogger;
import paint.shared.objects.ExperimentInfo;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.CsvConcatenator.concatenateCsvFiles;
import static paint.shared.utils.CsvUtils.countProcessed;

import static paint.shared.utils.Miscellaneous.formatDuration;

public class RunTrackMateOnExperiment {

    static boolean verbose = false;

    public static boolean runTrackMateOnExperiment(Path experimentPath, Path imagesPath) {

        Duration totalDuration = Duration.ZERO;
        int numberRecordings = 0;
        boolean status = true;

        // Read the JSON configuration file from the experiment directory
        // If it does not exist, create a new one with default values.
        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        PaintConfig paintConfig = PaintConfig.instance();
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);

        if (verbose) {
            System.out.println(paintConfig);
            System.out.println(trackMateConfig);
        }

        // Check if the Experiment info.csv file exists
        Path experimentFilePath = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentFilePath.getParent().resolve(RECORDINGS_CSV);
        if (!Files.exists(experimentFilePath)) {
            AppLogger.errorf("Experiment info file does not exist in %s.", experimentFilePath);
            return false;
        }

        // Determime how many recordings need to be processes on this experiment
        int numberRecordingsToProcess = countProcessed(experimentFilePath);

        String experimentName = experimentPath.getFileName().toString();
        String projectName = experimentPath.getParent().getFileName().toString();

        AppLogger.infof("");
        AppLogger.infof("Processing %d recordings in experiment '%s' in project '%s'.",  numberRecordingsToProcess, experimentName, projectName);

        // Open the experiment info file for reading
        try (BufferedReader reader = Files.newBufferedReader(experimentFilePath);
             BufferedWriter writer = Files.newBufferedWriter(allRecordingFilePath)) {String headerLine = reader.readLine();
            if (headerLine == null) {
                AppLogger.errorf("Experiment info file in %s is empty.", experimentFilePath);
                return false;
            }

            // Build the header for the 'All Recordings Java' file (by adding some new columns)
            String header = headerLine + "," + "Number of Spots" +
                    "," + "Number of Tracks" +
                    "," + "Number of Spots in All Tracks" +
                    "," + "Number of Frames" +
                    "," + "Run Time" +
                    "," + "Time Stamp" +
                    "," + "Exclude" +
                    "," + "Tau" +
                    "," + "R Squared" +
                    "," + "Density";

            // Write the header to the 'All Recordings Java' file
            writer.write(header);
            writer.newLine();

            // Split the header by tab into field names
            String[] headers = headerLine.split(",");

            String line;
            TrackMateResults trackMateResults;

            int numberOfSpots;
            int numberOfTracks;
            int numberOfSpotsInAllTracks;
            int numberOfFrames;
            int runTime;
            String timeStamp;
            String recordingName = null;

            // Now process row by row, recording by recording
            while ((line = reader.readLine()) != null) {

                // Put a try catch on the record processing to catch an error and continue
                try {
                    // split row into values
                    String[] fields = line.split(",", -1);  // -1 keeps empty fields

                    // Convert to map if needed
                    Map<String, String> row = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length && i < fields.length; i++) {
                        row.put(headers[i], fields[i]);
                    }

                    ExperimentInfo experimentInfoRecord = new ExperimentInfo(row);

                    recordingName = row.get("Recording Name");
                    if (experimentInfoRecord.getProcessFlag()) {
                        double threshold = experimentInfoRecord.getThreshold();

                        // Check if Brightfield Images and TrackMate Images exist and create if necessary
                        Path brightfieldImagesPath = experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES);
                        Path trackmateImagesPath = experimentPath.resolve(DIR_TRACKMATE_IMAGES);
                        if (!Files.exists(brightfieldImagesPath)) {
                            Files.createDirectories(brightfieldImagesPath);
                            AppLogger.debugf("Created missing directory: %s", brightfieldImagesPath);
                        }
                        if (!Files.exists(trackmateImagesPath)) {
                            Files.createDirectories(trackmateImagesPath);
                            AppLogger.debugf("Created missing directory: %s", trackmateImagesPath);
                        }
                        AppLogger.infof("   Recording '%s' (%d of %d) started TrackMate processing.",
                                experimentInfoRecord.getRecordingName(),
                                numberRecordings + 1,
                                numberRecordingsToProcess);

                        // Perform TrackMate processing
                        trackMateResults = RunTrackMateOnRecording.RunTrackMateOnRecording(experimentPath, imagesPath, trackMateConfig, threshold, experimentInfoRecord);

                        if (trackMateResults == null || !trackMateResults.isSuccess()) {
                            AppLogger.errorf("TrackMate processing failed for recording '%s'.", experimentInfoRecord.getRecordingName());
                            AppLogger.errorf("");
                            status = false;
                            continue;  // Process the next recording
                        }
                        int durationInSeconds = (int) (trackMateResults.getDuration().toMillis() / 1000);
                        AppLogger.infof("   Recording '%s' (%d of %d) processed in %s.",
                                experimentInfoRecord.getRecordingName(),
                                numberRecordings + 1,
                                numberRecordingsToProcess,
                                formatDuration(durationInSeconds));
                        AppLogger.infof("");
                        totalDuration = totalDuration.plus(trackMateResults.getDuration());
                        numberRecordings += 1;

                        numberOfSpots = trackMateResults.getNumberOfSpots();
                        numberOfTracks = trackMateResults.getNumberOfTracks();
                        numberOfFrames = trackMateResults.getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults.getNumberOfSpotsInALlTracks();
                        runTime = (int) trackMateResults.getDuration().toMillis() / 1000;
                        timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } else {
                        AppLogger.infof("   Recording '%s' was deselected.", experimentInfoRecord.getRecordingName());
                        numberOfSpots = 0;
                        numberOfTracks = 0;
                        numberOfFrames = 0;
                        numberOfSpotsInAllTracks = 0;
                        runTime = 0;
                        timeStamp = "";
                    }

                    // Build output row, even if no recording was processed

                    String out = line + "," + String.format("%d", numberOfSpots) +      // Number of Spots
                            "," + String.format("%d", numberOfTracks) +                 // Number of Tracks
                            "," + String.format("%d", numberOfSpotsInAllTracks) +       // Numbe of spots in all tracks
                            "," + String.format("%d", numberOfFrames) +                 // Number of Frames
                            "," + String.format("%d", runTime) +                        // Run Time needs to be an int
                            "," + String.format("%s", timeStamp) +                      // Timestamp
                            "," + "False" +                                             // Exclude
                            "," + "" +                                                  // Tau
                            "," + "" +                                                  // R Squared
                            "," + "";                                                   // Density

                    writer.write(out);
                    writer.newLine();
                }
                catch (Exception e) {
                    AppLogger.errorf("Error processing recording %s: %s", recordingName, e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    AppLogger.errorf("An exception occurred:\n" + sw.toString());
                }
            }
        } catch (IOException e) {
            AppLogger.errorf("Error processing Experiment Info file '%s': %s", experimentFilePath, e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw.toString());
            status = false;
        }

        // Concatenate the 'All Tracks Java' file
        Path tracksFilePath = experimentPath.resolve(TRACKS_CSV);
        try {
            concatenateCsvFiles(experimentPath, tracksFilePath);
        } catch (IOException e) {
            AppLogger.errorf("Error concatenating tracks file file %s: %s", experimentFilePath, e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw.toString());
            status = false;
        }

        int durationInSeconds = (int) (totalDuration.toMillis() / 1000);
        AppLogger.infof("Processed %d recordings in %s.", numberRecordings, formatDuration(durationInSeconds));
        AppLogger.infof("");
        return status;
    }
}
