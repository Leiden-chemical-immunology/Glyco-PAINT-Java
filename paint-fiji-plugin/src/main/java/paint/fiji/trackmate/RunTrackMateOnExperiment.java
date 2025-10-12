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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static paint.shared.config.TrackMateConfig.trackMateConfigToFile;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.utils.CsvConcatenator.concatenateTracksFilesInDirectory;
import static paint.shared.utils.CsvUtils.countProcessed;
import static paint.shared.utils.Miscellaneous.formatDuration;

/**
 * Runs TrackMate analysis on all recordings in a single experiment.
 */
public class RunTrackMateOnExperiment {

    static boolean verbose = false;

    /**
     * Watchdog that joins in 1s slices and interrupts promptly on cancel or timeout.
     */
    private static boolean runWithWatchdog(Runnable task,
                                           int maxSecondsPerRecording,
                                           ProjectSpecificationDialog dialog) {
        Thread thread = new Thread(task, "TrackMateThread");
        thread.start();

        int numberOfInterrupts = 0;
        int numberOfDotsOnline = 0;

        for (int i = 0; i < maxSecondsPerRecording; i++) {
            try {
                thread.join(1000); // check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                PaintLogger.errorf("Watchdog thread itself was interrupted.");
                return false;
            }

            // ✅ TrackMate finished normally
            if (!thread.isAlive()) {
                return true;
            }

            // ✅ User pressed cancel
            if (dialog != null && dialog.isCancelled()) {
                PaintLogger.warningf("User requested cancellation — stopping TrackMate gracefully...");
                // ⚠️ Do NOT call thread.interrupt(), just mark as finished
                return false;
            }

            // progress dots
            numberOfInterrupts++;
            if (numberOfInterrupts >= 1) {
                PaintLogger.raw(".");
                numberOfDotsOnline++;
                numberOfInterrupts = 0;
            }
            if (numberOfDotsOnline >= 80) {
                PaintLogger.raw("\n                                                    ")     ;
                numberOfDotsOnline = 0;
            }
        }

        // ⏱ Timeout reached
        PaintLogger.errorf("   TrackMate - exceeded time limit of %d seconds.", maxSecondsPerRecording);
        // ⚠️ Instead of thread.interrupt(), just log and exit cleanly
        // PaintLogger.warningf("Not interrupting TrackMate threads to avoid InterruptedException.");
        return false;
    }

    public static boolean runTrackMateOnExperiment(Path experimentPath,
                                                   Path imagesPath,
                                                   ProjectSpecificationDialog dialog) {

        Duration totalDuration = Duration.ZERO;
        int numberRecordings = 0;
        boolean status = true;

        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        if (Files.exists(configPath)) {
            PaintConfig.initialise(experimentPath.getParent());
        } else {
            PaintLogger.warningf("No PaintConfig.json found in %s, defaults will be used.",
                                 experimentPath.getParent());
        }
        PaintConfig paintConfig = PaintConfig.instance();
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);

        int maxSecondsPerRecording = paintConfig.getIntValue("TrackMate", "Max Seconds Per Recording", 2000);
        try {
            Path filePath = experimentPath.resolve("Output").resolve("ParametersUsed.txt");
            Files.createDirectories(filePath.getParent());
            trackMateConfigToFile(trackMateConfig, filePath);
        } catch (Exception ex) {
            PaintLogger.errorf("Could not write file '%s'", "ParametersUsed.txt");
        }

        Path experimentFilePath  = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentPath.resolve(RECORDING_CSV);
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

        try (Reader reader = Files.newBufferedReader(experimentFilePath);
             CSVParser parser = new CSVParser(reader,
                                              CSVFormat.DEFAULT.builder()
                                                      .setHeader()
                                                      .setSkipHeaderRecord(true)
                                                      .build());
             BufferedWriter writer = Files.newBufferedWriter(allRecordingFilePath);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().build())
        ) {
            List<String> header = new ArrayList<>(parser.getHeaderMap().keySet());
            header.addAll(Arrays.asList(
                    "Number of Spots",
                    "Number of Tracks",
                    "Number of Tracks in Background",
                    "Number of Squares in Background",
                    "Average Tracks in Background",
                    "Number of Spots in All Tracks",
                    "Number of Frames",
                    "Run Time",
                    "Time Stamp",
                    "Exclude",
                    "Tau",
                    "R Squared",
                    "Density"
            ));
            printer.printRecord(header);

            for (CSVRecord record : parser) {
                if (dialog != null && dialog.isCancelled()) {
                    PaintLogger.warningf("User requested cancellation. Stopping.");
                    break;
                }

                try {
                    Map<String, String> row = new LinkedHashMap<String, String>();
                    for (String key : parser.getHeaderMap().keySet()) {
                        row.put(key, record.get(key));
                    }

                    ExperimentInfo experimentInfo = new ExperimentInfo(row);
                    String recordingName = experimentInfo.getRecordingName();

                    // @formatter:off
                    int numberOfSpots            = 0;
                    int numberOfTracks           = 0;
                    int numberOfFilteredTracks   = 0;
                    int numberOfSpotsInAllTracks = 0;
                    int numberOfFrames           = 0;
                    int runTime                  = 0;
                    String timeStamp             = "";
                    // @formatter:on

                    if (experimentInfo.isProcessFlag()) {
                        double threshold = experimentInfo.getThreshold();

                        Files.createDirectories(experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES));
                        Files.createDirectories(experimentPath.resolve(DIR_TRACKMATE_IMAGES));

                        PaintLogger.infof("   Recording '%s' started TrackMate processing.", recordingName);

                        final TrackMateResults[] trackMateResults = new TrackMateResults[1];

                        boolean finished = runWithWatchdog(() -> {
                            try {
                                trackMateResults[0] = RunTrackMateOnRecording.RunTrackMateOnRecording(
                                        experimentPath, imagesPath, trackMateConfig, threshold, experimentInfo, dialog);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, maxSecondsPerRecording, dialog);

                        if (!finished) {
                            if (dialog != null && dialog.isCancelled()) {
                                PaintLogger.infof("Recording '%s' cancelled cleanly.", recordingName);
                                break;
                            } else {
                                PaintLogger.errorf("   TrackMate failed or timed out for '%s'.", recordingName);
                                PaintLogger.infof();
                                status = false;
                                continue;
                            }
                        }

                        if (trackMateResults[0] == null || !trackMateResults[0].isSuccess()) {
                            PaintLogger.errorf("   TrackMate failed for '%s'.", recordingName);
                            status = false;
                            continue;
                        }
                        if (!trackMateResults[0].isCalculationPerformed()) {
                            numberRecordings++;
                            continue;
                        }

                        int durationInSeconds = (int) (trackMateResults[0].getDuration().toMillis() / 1000);
                        PaintLogger.infof("   Recording '%s' processed in %s.",
                                          recordingName, formatDuration(durationInSeconds));
                        PaintLogger.blankline();

                        totalDuration = totalDuration.plus(trackMateResults[0].getDuration());
                        numberRecordings++;

                        // @formatter:off
                        numberOfSpots            = trackMateResults[0].getNumberOfSpots();
                        numberOfTracks           = trackMateResults[0].getNumberOfTracks();
                        numberOfFilteredTracks   = trackMateResults[0].getNumberOfFilteredTracks();
                        numberOfFrames           = trackMateResults[0].getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults[0].getNumberOfSpotsInALlTracks();
                        runTime                  = durationInSeconds;
                        timeStamp                = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        // @formatter:off

                    } else if (verbose) {
                        PaintLogger.infof("   Recording '%s' skipped.", recordingName);
                    }

                    List<String> output = new ArrayList<String>();
                    for (String key : parser.getHeaderMap().keySet()) {
                        output.add(row.get(key));
                    }
                    output.addAll(Arrays.asList(
                            String.valueOf(numberOfSpots),
                            String.valueOf(numberOfFilteredTracks),
                            "",
                            "",
                            "",
                            String.valueOf(numberOfSpotsInAllTracks),
                            String.valueOf(numberOfFrames),
                            String.valueOf(runTime),
                            timeStamp,
                            "False",
                            "",
                            "",
                            ""
                    ));
                    printer.printRecord(output);

                } catch (Exception e) {
                    if (dialog == null || !dialog.isCancelled()) {
                        PaintLogger.errorf("Error processing recording: %s", e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            if (dialog == null || !dialog.isCancelled()) {
                PaintLogger.errorf("Error reading Experiment Info: %s", e.getMessage());
            }
            status = false;
        }

        if (dialog != null && dialog.isCancelled()) {
            PaintLogger.warningf("Cancellation processed.");
            PaintLogger.blankline();
            return false;
        }

        Path tracksFilePath = experimentPath.resolve(TRACK_CSV);
        try {
            concatenateTracksFilesInDirectory(experimentPath, tracksFilePath);
        } catch (IOException e) {
            PaintLogger.errorf("Error concatenating tracks: %s", e.getMessage());
            status = false;
        }

        PaintLogger.infof("Processed %d recordings in %s.",
                          numberRecordings, formatDuration((int) (totalDuration.toMillis() / 1000)));
        PaintLogger.blankline();
        return status;
    }
}