package paint.fiji.trackmate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.dialogs.ProjectSpecificationDialog;
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
 * Runs TrackMate analysis on all recordings in a single experiment.
 * <p>
 * This class writes results into the provided experiment path, which
 * may be the normal project directory or a sweep subdirectory.
 */
public class RunTrackMateOnExperiment {

    static boolean verbose = false;

    private static boolean runWithWatchdog(Runnable task, int maxSeconds) {
        Thread t = new Thread(task, "TrackMateThread");
        t.start();

        for (int i = 0; i < maxSeconds; i++) {
            try {
                t.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                PaintLogger.errorf("⚠️ Watchdog interrupted.");
                return false;
            }
            if (!t.isAlive()) {
                return true;
            }
            PaintLogger.raw(".");
        }

        PaintLogger.errorf("\n⏱ Task exceeded time limit, interrupting...");
        t.interrupt();
        return false;
    }

    public static boolean runTrackMateOnExperiment(Path experimentPath,
                                                   Path imagesPath,
                                                   ProjectSpecificationDialog dialog) {

        Duration totalDuration = Duration.ZERO;
        int numberRecordings = 0;
        boolean status = true;

        // --- Load Paint/TrackMate config from the correct directory ---
        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        if (Files.exists(configPath)) {
            // Re-initialise so PaintConfig points to the right directory
            PaintConfig.initialise(experimentPath.getParent());
        } else {
            PaintLogger.warningf("No PaintConfig.json found in %s, defaults will be used.",
                    experimentPath.getParent());
        }
        PaintConfig paintConfig = PaintConfig.instance();
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);
        PaintLogger.infof(trackMateConfig.toString());

        // Input/output CSV paths (written under experimentPath, sweep-compatible)
        Path experimentFilePath = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentPath.resolve(RECORDINGS_CSV);

        if (!Files.exists(experimentFilePath)) {
            PaintLogger.errorf("Experiment info file does not exist in %s.", experimentFilePath);
            return false;
        }

        int numberRecordingsToProcess = countProcessed(experimentFilePath);
        String experimentName = experimentPath.getFileName().toString();
        String projectName = experimentPath.getParent().getFileName().toString();

        PaintLogger.blankline();
        PaintLogger.infof("Processing %d %s in experiment '%s' in project '%s'.",
                numberRecordingsToProcess,
                numberRecordingsToProcess == 1 ? "recording" : "recordings",
                experimentName,
                projectName);
        PaintLogger.blankline();

        // Try-with-resources for experiment_info.csv
        try (Reader reader = Files.newBufferedReader(experimentFilePath);
             CSVParser parser = new CSVParser(reader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .build());
             BufferedWriter writer = Files.newBufferedWriter(allRecordingFilePath);
             CSVPrinter printer = new CSVPrinter(writer,
                     CSVFormat.DEFAULT.builder()
                             .setHeader(parser.getHeaderMap()
                                     .keySet()
                                     .toArray(new String[0]))
                             .build())
        ) {
            // Output header
            List<String> header = new ArrayList<>(parser.getHeaderMap().keySet());
            header.addAll(Arrays.asList(
                    "Number of Spots", "Number of Tracks", "Number of Spots in All Tracks",
                    "Number of Frames", "Run Time", "Time Stamp", "Exclude", "Tau", "R Squared", "Density"
            ));
            printer.printRecord(header);

            for (CSVRecord record : parser) {
                if (dialog != null && dialog.isCancelled()) {
                    PaintLogger.warningf("User requested cancellation. Stopping.");
                    break;
                }

                try {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String key : parser.getHeaderMap().keySet()) {
                        row.put(key, record.get(key));
                    }

                    ExperimentInfo experimentInfo = new ExperimentInfo(row);
                    String recordingName = experimentInfo.getRecordingName();

                    // Defaults
                    int numberOfSpots = 0, numberOfTracks = 0, numberOfSpotsInAllTracks = 0,
                            numberOfFrames = 0, runTime = 0;
                    String timeStamp = "";

                    if (experimentInfo.getProcessFlag()) {
                        double threshold = experimentInfo.getThreshold();

                        // Ensure subdirs exist
                        Files.createDirectories(experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES));
                        Files.createDirectories(experimentPath.resolve(DIR_TRACKMATE_IMAGES));

                        PaintLogger.infof("   Recording '%s' started TrackMate processing.", recordingName);

                        final TrackMateResults[] trackMateResults = new TrackMateResults[1];

                        boolean finished = runWithWatchdog(() -> {
                            try {
                                trackMateResults[0] = RunTrackMateOnRecording.RunTrackMateOnRecording(
                                        experimentPath, imagesPath, trackMateConfig, threshold, experimentInfo);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, 1500);

                        if (!finished || trackMateResults[0] == null || !trackMateResults[0].isSuccess()) {
                            PaintLogger.errorf("   TrackMate failed or timed out for '%s'.", recordingName);
                            status = false;
                            continue;
                        }
                        if (!trackMateResults[0].isCalculationPerformed()) {
                            numberRecordings++;
                            continue;
                        }

                        int durationInSeconds = (int) (trackMateResults[0].getDuration().toMillis() / 1000);
                        PaintLogger.infof("   Recording '%s' processed in %s.", recordingName, formatDuration(durationInSeconds));
                        PaintLogger.blankline();

                        totalDuration = totalDuration.plus(trackMateResults[0].getDuration());
                        numberRecordings++;

                        numberOfSpots = trackMateResults[0].getNumberOfSpots();
                        numberOfTracks = trackMateResults[0].getNumberOfTracks();
                        numberOfFrames = trackMateResults[0].getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults[0].getNumberOfSpotsInALlTracks();
                        runTime = durationInSeconds;
                        timeStamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else {
                        if (verbose) {
                            PaintLogger.infof("   Recording '%s' skipped.", recordingName);
                        }
                    }

                    // Build row
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
                }
            }

        } catch (IOException e) {
            PaintLogger.errorf("Error reading Experiment Info: %s", e.getMessage());
            status = false;
        }

        // Concatenate tracks
        Path tracksFilePath = experimentPath.resolve(TRACKS_CSV);
        try {
            concatenateTracksFilesInDirectory(experimentPath, tracksFilePath);
        } catch (IOException e) {
            PaintLogger.errorf("Error concatenating tracks: %s", e.getMessage());
            status = false;
        }

        PaintLogger.infof("Processed %d recordings in %s.", numberRecordings,
                formatDuration((int) (totalDuration.toMillis() / 1000)));
        PaintLogger.blankline();

        return status;
    }
}