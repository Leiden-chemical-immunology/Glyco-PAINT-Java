package paint.fiji.trackmate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.CsvConcatenator.concatenateTracksFilesInDirectory;
import static paint.shared.utils.CsvUtils.countProcessed;
import static paint.shared.utils.Miscellaneous.formatDuration;

/**
 * Main class responsible for running TrackMate on an entire experiment.
 * <p>
 * It reads experiment metadata from {@code experiment_info.csv}, runs
 * tracking on selected recordings, and writes results to {@code recordings.csv}
 * and {@code tracks.csv}.
 */
public class RunTrackMateOnExperiment {

    /** Enables verbose logging to stdout (in addition to AppLogger). */
    static boolean verbose = false;

    /**
     * Runs TrackMate analysis on all recordings in a given experiment folder.
     *
     * @param experimentPath Path to the experiment folder (must contain `experiment_info.csv`)
     * @param imagesPath     Path to the base folder containing raw recording images
     * @return {@code true} if all selected recordings were processed successfully; otherwise {@code false}
     */
    public static boolean runTrackMateOnExperiment(Path experimentPath, Path imagesPath) {

        Duration totalDuration = Duration.ZERO;
        int numberRecordings = 0;
        boolean status = true;

        // Load Paint and TrackMate configuration
        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        PaintConfig paintConfig = PaintConfig.instance();
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);

        if (verbose) {
            System.out.println(paintConfig);
            System.out.println(trackMateConfig);
        }

        // Input and output CSV paths
        Path experimentFilePath = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentPath.resolve(RECORDINGS_CSV);

        if (!Files.exists(experimentFilePath)) {
            PaintLogger.errorf("Experiment info file does not exist in %s.", experimentFilePath);
            return false;
        }

        int numberRecordingsToProcess = countProcessed(experimentFilePath);
        String experimentName = experimentPath.getFileName().toString();
        String projectName = experimentPath.getParent().getFileName().toString();

        PaintLogger.infof();
        PaintLogger.infof("Processing %d %s in experiment '%s' in project '%s'.",
                numberRecordingsToProcess,
                numberRecordingsToProcess == 1 ? "recording" : "recordings",
                experimentName,
                projectName);

        // Try-with-resources for reading experiment_info.csv and writing recordings.csv
        try (
                Reader reader = Files.newBufferedReader(experimentFilePath);
                CSVParser parser = new CSVParser(reader,
                        CSVFormat.DEFAULT.builder()
                                .setHeader()                 // first record is header
                                .setSkipHeaderRecord(true)   // don’t return header as data
                                .build());
                BufferedWriter writer = Files.newBufferedWriter(allRecordingFilePath);
                CSVPrinter printer = new CSVPrinter(writer,
                        CSVFormat.DEFAULT.builder()
                                .setHeader(parser.getHeaderMap()
                                        .keySet()
                                        .toArray(new String[0])) // preserve header names
                                .build())
        ) {
            // Construct output header for recordings.csv
            List<String> header = new ArrayList<>(parser.getHeaderMap().keySet());
            header.addAll(Arrays.asList(
                    "Number of Spots", "Number of Tracks", "Number of Spots in All Tracks",
                    "Number of Frames", "Run Time", "Time Stamp", "Exclude", "Tau", "R Squared", "Density"
            ));
            printer.printRecord(header);

            for (CSVRecord record : parser) {
                try {
                    // Parse row into a map and wrap in ExperimentInfo
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String key : parser.getHeaderMap().keySet()) {
                        row.put(key, record.get(key));
                    }

                    ExperimentInfo experimentInfo = new ExperimentInfo(row);
                    String recordingName = experimentInfo.getRecordingName();

                    // Default output values
                    int numberOfSpots = 0, numberOfTracks = 0, numberOfSpotsInAllTracks = 0, numberOfFrames = 0, runTime = 0;
                    String timeStamp = "";

                    if (experimentInfo.getProcessFlag()) {
                        double threshold = experimentInfo.getThreshold();

                        // Ensure output directories exist
                        Path brightfieldImagesPath = experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES);
                        Path trackmateImagesPath = experimentPath.resolve(DIR_TRACKMATE_IMAGES);
                        if (!Files.exists(brightfieldImagesPath)) Files.createDirectories(brightfieldImagesPath);
                        if (!Files.exists(trackmateImagesPath)) Files.createDirectories(trackmateImagesPath);

                        PaintLogger.infof("   Recording '%s' (%d of %d) started TrackMate processing.",
                                recordingName, numberRecordings + 1, numberRecordingsToProcess);

                        // Run TrackMate on a single recording
                        TrackMateResults trackMateResults = RunTrackMateOnRecording.RunTrackMateOnRecording(
                                experimentPath, imagesPath, trackMateConfig, threshold, experimentInfo);

                        if (trackMateResults == null || !trackMateResults.isSuccess()) {
                            PaintLogger.errorf("   TrackMate processing failed for recording '%s'.", recordingName);
                            PaintLogger.errorf("");
                            status = false;
                            continue;
                        }
                        if (!trackMateResults.isCalculationPerformed()) {
                            numberRecordings++;
                            continue;
                        }

                        int durationInSeconds = (int) (trackMateResults.getDuration().toMillis() / 1000);
                        PaintLogger.infof("   Recording '%s' (%d of %d) processed in %s.",
                                recordingName, numberRecordings + 1, numberRecordingsToProcess,
                                formatDuration(durationInSeconds));
                        PaintLogger.infof();

                        // Update counters and values
                        totalDuration = totalDuration.plus(trackMateResults.getDuration());
                        numberRecordings++;

                        numberOfSpots = trackMateResults.getNumberOfSpots();
                        numberOfTracks = trackMateResults.getNumberOfTracks();
                        numberOfFrames = trackMateResults.getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults.getNumberOfSpotsInALlTracks();
                        runTime = durationInSeconds;
                        timeStamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    } else {
                        PaintLogger.infof();
                        PaintLogger.infof("   Recording '%s' was not selected for processing.", recordingName);
                    }

                    // Build output CSV row
                    List<String> output = new ArrayList<>();
                    for (String key : parser.getHeaderMap().keySet()) {
                        output.add(row.getOrDefault(key, ""));
                    }
                    output.addAll(Arrays.asList(
                            String.valueOf(numberOfSpots),
                            String.valueOf(numberOfTracks),
                            String.valueOf(numberOfSpotsInAllTracks),
                            String.valueOf(numberOfFrames),
                            String.valueOf(runTime),
                            timeStamp,
                            "False", "", "", ""
                    ));
                    printer.printRecord(output);

                } catch (Exception e) {
                    PaintLogger.errorf("Error processing recording: %s", e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    PaintLogger.errorf("An exception occurred:\n" + sw);
                }
            }

        } catch (IOException e) {
            PaintLogger.errorf("Error processing Experiment Info file '%s': %s", experimentFilePath, e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
            status = false;
        }

        // Concatenate all individual tracks_*.csv into tracks.csv
        Path tracksFilePath = experimentPath.resolve(TRACKS_CSV);
        try {
            concatenateTracksFilesInDirectory(experimentPath, tracksFilePath);
        } catch (IOException e) {
            PaintLogger.errorf("Error concatenating tracks file %s: %s", experimentFilePath, e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
            status = false;
        }

        // Log overall runtime
        int durationInSeconds = (int) (totalDuration.toMillis() / 1000);
        PaintLogger.infof("Processed %d recordings in %s.", numberRecordings, formatDuration(durationInSeconds));
        PaintLogger.infof();
        return status;
    }
}