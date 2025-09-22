package paint.fiji.trackmate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.AppLogger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        PaintConfig paintConfig = PaintConfig.instance();
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);

        if (verbose) {
            System.out.println(paintConfig);
            System.out.println(trackMateConfig);
        }

        Path experimentFilePath = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentFilePath.getParent().resolve(RECORDINGS_CSV);
        if (!Files.exists(experimentFilePath)) {
            AppLogger.errorf("Experiment info file does not exist in %s.", experimentFilePath);
            return false;
        }

        int numberRecordingsToProcess = countProcessed(experimentFilePath);
        String experimentName = experimentPath.getFileName().toString();
        String projectName = experimentPath.getParent().getFileName().toString();

        AppLogger.infof("");
        AppLogger.infof("Processing %d recordings in experiment '%s' in project '%s'.", numberRecordingsToProcess, experimentName, projectName);

        try (
                Reader reader = Files.newBufferedReader(experimentFilePath);
                CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
                BufferedWriter writer = Files.newBufferedWriter(allRecordingFilePath);
                CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)
        ) {
            List<String> header = new ArrayList<>(parser.getHeaderMap().keySet());
            header.addAll(Arrays.asList(
                    "Number of Spots", "Number of Tracks", "Number of Spots in All Tracks",
                    "Number of Frames", "Run Time", "Time Stamp", "Exclude", "Tau", "R Squared", "Density"
            ));
            printer.printRecord(header);

            for (CSVRecord record : parser) {
                try {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String key : parser.getHeaderMap().keySet()) {
                        row.put(key, record.get(key));
                    }

                    ExperimentInfo experimentInfo = new ExperimentInfo(row);
                    String recordingName = experimentInfo.getRecordingName();
                    int numberOfSpots = 0, numberOfTracks = 0, numberOfSpotsInAllTracks = 0, numberOfFrames = 0, runTime = 0;
                    String timeStamp = "";

                    if (experimentInfo.getProcessFlag()) {
                        double threshold = experimentInfo.getThreshold();

                        Path brightfieldImagesPath = experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES);
                        Path trackmateImagesPath = experimentPath.resolve(DIR_TRACKMATE_IMAGES);
                        if (!Files.exists(brightfieldImagesPath)) Files.createDirectories(brightfieldImagesPath);
                        if (!Files.exists(trackmateImagesPath)) Files.createDirectories(trackmateImagesPath);

                        AppLogger.infof("   Recording '%s' (%d of %d) started TrackMate processing.", recordingName, numberRecordings + 1, numberRecordingsToProcess);

                        TrackMateResults trackMateResults = RunTrackMateOnRecording.RunTrackMateOnRecording(experimentPath, imagesPath, trackMateConfig, threshold, experimentInfo);
                        if (trackMateResults == null || !trackMateResults.isSuccess()) {
                            AppLogger.errorf("TrackMate processing failed for recording '%s'.", recordingName);
                            status = false;
                            continue;
                        }

                        int durationInSeconds = (int) (trackMateResults.getDuration().toMillis() / 1000);
                        AppLogger.infof("   Recording '%s' (%d of %d) processed in %s.", recordingName, numberRecordings + 1, numberRecordingsToProcess, formatDuration(durationInSeconds));

                        totalDuration = totalDuration.plus(trackMateResults.getDuration());
                        numberRecordings++;

                        numberOfSpots = trackMateResults.getNumberOfSpots();
                        numberOfTracks = trackMateResults.getNumberOfTracks();
                        numberOfFrames = trackMateResults.getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults.getNumberOfSpotsInALlTracks();
                        runTime = durationInSeconds;
                        timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } else {
                        AppLogger.infof("   Recording '%s' was deselected.", recordingName);
                    }

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
                    AppLogger.errorf("Error processing recording: %s", e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    AppLogger.errorf("An exception occurred:\n" + sw);
                }
            }
        } catch (IOException e) {
            AppLogger.errorf("Error processing Experiment Info file '%s': %s", experimentFilePath, e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw);
            status = false;
        }

        Path tracksFilePath = experimentPath.resolve(TRACKS_CSV);
        try {
            concatenateCsvFiles(experimentPath, tracksFilePath);
        } catch (IOException e) {
            AppLogger.errorf("Error concatenating tracks file %s: %s", experimentFilePath, e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw);
            status = false;
        }

        int durationInSeconds = (int) (totalDuration.toMillis() / 1000);
        AppLogger.infof("Processed %d recordings in %s.", numberRecordings, formatDuration(durationInSeconds));
        AppLogger.infof("");
        return status;
    }
}
