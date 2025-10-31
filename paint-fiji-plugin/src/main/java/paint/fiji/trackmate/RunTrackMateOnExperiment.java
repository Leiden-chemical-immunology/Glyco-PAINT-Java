/******************************************************************************
 *  Class:        RunTrackMateOnExperiment.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Executes the TrackMate analysis workflow for a single experiment within
 *    the Paint project framework. Handles experiment-level validation,
 *    configuration setup, per-recording execution, and aggregation of results.
 *
 *  DESCRIPTION:
 *    • Reads and validates the experiment configuration and info files.
 *    • Executes TrackMate on each recording defined in the experiment.
 *    • Monitors progress using a watchdog with timeouts and user cancellation.
 *    • Collects per-recording results and concatenates them into summary CSVs.
 *    • Records execution parameters and logs runtime details.
 *
 *  RESPONSIBILITIES:
 *    • Manage experiment-level execution lifecycle.
 *    • Handle user interaction via {@link ProjectDialog}.
 *    • Orchestrate per-recording analysis via {@link RunTrackMateOnRecording}.
 *    • Maintain consistency in experiment result aggregation.
 *
 *  USAGE EXAMPLE:
 *    Path experiment = Paths.get("/Paint Project/221108");
 *    Path images     = Paths.get("/Omero/221108");
 *    boolean ok = RunTrackMateOnExperiment.runTrackMateOnExperiment(
 *                     experiment, images, dialog);
 *
 *  DEPENDENCIES:
 *    – paint.fiji.trackmate.RunTrackMateOnRecording
 *    – paint.shared.config.PaintConfig
 *    – paint.shared.config.TrackMateConfig
 *    – paint.shared.dialogs.ProjectDialog
 *    – paint.shared.utils.PaintLogger
 *    – paint.shared.utils.CsvUtils
 *    – paint.shared.objects.ExperimentInfo
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.fiji.trackmate;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;

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
import static paint.shared.utils.CsvUtils.concatenateCsvFiles;
import static paint.shared.utils.CsvUtils.countProcessed;
import static paint.shared.utils.Miscellaneous.formatDuration;

/**
 * Provides functionality to execute the TrackMate analysis workflow for a single
 * experiment. Each experiment typically contains one or more recordings that
 * are processed sequentially.
 * <p>
 * The workflow includes:
 * <ul>
 *   <li>Initializing configuration and runtime settings.</li>
 *   <li>Running TrackMate per recording in a monitored thread.</li>
 *   <li>Recording runtime statistics and aggregating results.</li>
 *   <li>Supporting user cancellation and timeouts via watchdog control.</li>
 * </ul>
 */
public class RunTrackMateOnExperiment extends RunTrackMateOnRecording {

    /**
     * Verbosity flag derived from {@link PaintRuntime}.
     */
    static final boolean verbose = PaintRuntime.isVerbose();

    /**
     * Executes a given task within a monitored thread using a watchdog.
     * The watchdog ensures that the task completes within a specified time limit
     * or terminates early if the user cancels processing.
     *
     * @param task                   the {@link Runnable} task to execute
     * @param maxSecondsPerRecording time limit for execution in seconds
     * @param dialog                 optional {@link ProjectDialog} that can signal cancellation
     * @return {@code true} if the task completed successfully;
     * {@code false} if cancelled or timed out
     */
    private static boolean runWithWatchdog(Runnable task,
                                           int maxSecondsPerRecording,
                                           ProjectDialog dialog) {

        Thread thread = new Thread(task, "TrackMateThread");
        thread.start();

        int numberOfInterrupts = 0;
        int numberOfDotsOnline = 0;

        for (int i = 0; i < maxSecondsPerRecording; i++) {
            try {
                thread.join(1000); // check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                PaintLogger.errorf("Watchdog thread was interrupted.");
                return false;
            }

            // ✅ Finished normally
            if (!thread.isAlive()) {
                return true;
            }

            // ✅ User requested cancellation
            if (dialog != null && dialog.isCancelled()) {
                PaintLogger.warnf("User requested cancellation — stopping TrackMate gracefully...");
                return false;
            }

            // Print progress dots
            numberOfInterrupts++;
            if (numberOfInterrupts >= 1) {
                PaintLogger.raw(".");
                numberOfDotsOnline++;
                numberOfInterrupts = 0;
            }
            if (numberOfDotsOnline >= 80) {
                PaintLogger.raw("\n                                                    ");
                numberOfDotsOnline = 0;
            }
        }

        // ⏱ Timeout reached
        PaintLogger.errorf("   TrackMate - exceeded time limit of %d seconds.", maxSecondsPerRecording);
        return false;
    }

    /**
     * Runs the TrackMate workflow for a single experiment.
     * Reads experiment configuration, processes each recording in sequence,
     * and aggregates results into consolidated output files.
     *
     * @param experimentPath the directory of the experiment (contains CSV files)
     * @param imagesPath     the directory containing corresponding image data
     * @param dialog         optional {@link ProjectDialog} for user cancellation
     * @return {@code true} if all recordings processed successfully;
     *         {@code false} if errors or cancellations occurred
     */
    public static boolean runTrackMateOnExperiment(Path experimentPath,
                                                   Path imagesPath,
                                                   ProjectDialog dialog) {

        // ---------------------------------------------------------------------
        // Initial setup
        // ---------------------------------------------------------------------
        Duration totalDuration           = Duration.ZERO;
        int numberRecordings             = 0;
        boolean status                   = true;
        List<Path> processedTrackFiles   = new ArrayList<>();

        // Initialize configuration
        Path configPath = experimentPath.getParent().resolve(PAINT_CONFIGURATION_JSON);
        if (Files.exists(configPath)) {
            PaintConfig.initialise(experimentPath.getParent());
        } else {
            PaintLogger.warnf("No PaintConfig.json found in %s, defaults will be used.",
                              experimentPath.getParent());
        }

        TrackMateConfig trackMateConfig = new TrackMateConfig();
        int maxSecondsPerRecording      = trackMateConfig.getMaxNumberOfSecondsPerImage();

        PaintLogger.debugf(trackMateConfig.toString());
        if (verbose) {
            PaintLogger.infof(trackMateConfig.toString());
        }

        // Save configuration snapshot
        try {
            Path filePath = experimentPath.resolve("Output").resolve("ParametersUsed.txt");
            Files.createDirectories(filePath.getParent());
            trackMateConfigToFile(trackMateConfig, filePath);
        } catch (Exception ex) {
            PaintLogger.errorf("Could not write file '%s'", "ParametersUsed.txt");
        }

        // ---------------------------------------------------------------------
        // Validate input files
        // ---------------------------------------------------------------------
        Path experimentFilePath   = experimentPath.resolve(EXPERIMENT_INFO_CSV);
        Path allRecordingFilePath = experimentPath.resolve(RECORDINGS_CSV);

        if (!Files.exists(experimentFilePath)) {
            PaintLogger.errorf("Experiment info file does not exist: %s", experimentFilePath);
            return false;
        }

        int numberRecordingsToProcess = countProcessed(experimentFilePath);
        String experimentName         = experimentPath.getFileName().toString();
        String projectName            = experimentPath.getParent().getFileName().toString();

        PaintLogger.blankline();
        PaintLogger.infof("Processing %d %s in experiment '%s' (project '%s').",
                          numberRecordingsToProcess,
                          numberRecordingsToProcess == 1 ? "recording" : "recordings",
                          experimentName,
                          projectName);
        PaintLogger.blankline();

        // ---------------------------------------------------------------------
        // Process Experiment Info and Recordings
        // ---------------------------------------------------------------------
        try (Reader experimentInfoReader = Files.newBufferedReader(experimentFilePath);
             CSVParser experimentInfoParser = new CSVParser(experimentInfoReader,
                                                            CSVFormat.DEFAULT.builder()
                                                                    .setHeader()
                                                                    .setSkipHeaderRecord(true)
                                                                    .build());
             BufferedWriter allRecordingsWriter = Files.newBufferedWriter(allRecordingFilePath);
             CSVPrinter allRecordingsPrinter = new CSVPrinter(allRecordingsWriter, CSVFormat.DEFAULT.builder().build())) {

            // Extend the Experiment Info header for RECORDINGS_CSV
            List<String> header = new ArrayList<>(experimentInfoParser.getHeaderMap().keySet());
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
            allRecordingsPrinter.printRecord(header);

            // Iterate through experiment records
            for (CSVRecord experientInfoRecord : experimentInfoParser) {

                if (dialog != null && dialog.isCancelled()) {
                    PaintLogger.warnf("User requested cancellation. Stopping.");
                    break;
                }

                try {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String key : experimentInfoParser.getHeaderMap().keySet()) {
                        row.put(key, experientInfoRecord.get(key));
                    }

                    ExperimentInfo experimentInfo           = new ExperimentInfo(row);
                    String         recordingName            = experimentInfo.getRecordingName();
                    int            numberOfSpots            = 0;
                    int            numberOfFilteredTracks   = 0;
                    int            numberOfSpotsInAllTracks = 0;
                    int            numberOfFrames           = 0;
                    int            runTime                  = 0;
                    String         timeStamp                = "";

                    if (experimentInfo.isProcessFlag()) {
                        double threshold = experimentInfo.getThreshold();

                        Files.createDirectories(experimentPath.resolve(DIR_BRIGHTFIELD_IMAGES));
                        Files.createDirectories(experimentPath.resolve(DIR_TRACKMATE_IMAGES));

                        PaintLogger.infof("   Recording '%s' started TrackMate processing.", recordingName);

                        // The following is necessary because of how Java handles variable capture inside lambdas or inner classes.
                        // We are not changing the variable trackMateResults itself (the reference to the array never changes),
                        // We pass the address of the array that does not change, but the contents can change.
                        final TrackMateResults[] trackMateResults = new TrackMateResults[1];

                        // Run TrackMate in a monitored thread
                        boolean finished = runWithWatchdog(() -> {
                            try {
                                trackMateResults[0] = RunTrackMateOnRecording.runTrackMateOnRecording(
                                        experimentPath, imagesPath, trackMateConfig, threshold, experimentInfo, dialog);
                            } catch (Exception e) {
                            }
                        }, maxSecondsPerRecording, dialog);

                        // Handle failures and cancellations
                        if (!finished) {
                            if (dialog != null && dialog.isCancelled()) {
                                PaintLogger.infof("Recording '%s' cancelled cleanly.", recordingName);
                                break;
                            } else {
                                PaintLogger.errorf("   TrackMate failed or timed out for '%s'.", recordingName);
                                PaintLogger.blankline();
                                status = false;
                                continue;
                            }
                        }

                        // Validate processing results
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
                        PaintLogger.infof("   Recording '%s' processed in %s.", recordingName, formatDuration(durationInSeconds));
                        PaintLogger.blankline();

                        Path trackFilePath = experimentPath.resolve(recordingName + "-tracks.csv");
                        processedTrackFiles.add(trackFilePath);
                        totalDuration = totalDuration.plus(trackMateResults[0].getDuration());
                        numberRecordings++;

                        // Extract output values
                        numberOfSpots            = trackMateResults[0].getNumberOfSpots();
                        numberOfFilteredTracks   = trackMateResults[0].getNumberOfFilteredTracks();
                        numberOfFrames           = trackMateResults[0].getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults[0].getNumberOfSpotsInAllTracks();
                        runTime                  = durationInSeconds;
                        timeStamp                = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                    } else if (verbose) {
                        PaintLogger.infof("   Recording '%s' skipped.", recordingName);
                    }

                    // Write summary output for this recording
                    List<String> output = new ArrayList<>();
                    for (String key : experimentInfoParser.getHeaderMap().keySet()) {
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
                    allRecordingsPrinter.printRecord(output);

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

        // ---------------------------------------------------------------------
        // Finalize outputs and summaries
        // ---------------------------------------------------------------------
        if (dialog != null && dialog.isCancelled()) {
            PaintLogger.warnf("Cancellation processed.");
            PaintLogger.blankline();
            return false;
        }

        // Merge all per-recording tracks into a single CSV
        Path tracksFilePath = experimentPath.resolve(TRACKS_CSV);
        try {
            concatenateCsvFiles(processedTrackFiles, tracksFilePath, true);
        } catch (IOException e) {
            PaintLogger.errorf("Error concatenating tracks: %s", e.getMessage());
            status = false;
        }

        // Log final summary
        PaintLogger.infof("Processed %d recordings in %s.",
                          numberRecordings, formatDuration((int) (totalDuration.toMillis() / 1000)));
        PaintLogger.blankline();
        return status;
    }
}