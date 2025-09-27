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
                PaintLogger.errorf("\u26a0\ufe0f Watchdog interrupted.");
                return false;
            }
            if (!t.isAlive()) {
                return true;
            }
            PaintLogger.raw(".");
        }

        PaintLogger.errorf("\n\u23f1 Task exceeded time limit, interrupting...");
        t.interrupt();
        return false;
    }

    public static boolean runTrackMateOnExperiment(Path experimentPath,
                                                   Path imagesPath,
                                                   ProjectSpecificationDialog dialog) {

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

        PaintLogger.blankline();
        PaintLogger.infof("Processing %d %s in experiment '%s' in project '%s'.",
                numberRecordingsToProcess,
                numberRecordingsToProcess == 1 ? "recording" : "recordings",
                experimentName,
                projectName);
        PaintLogger.blankline();

        // Try-with-resources for reading experiment_info.csv and writing recordings.csv
        try (
                Reader reader = Files.newBufferedReader(experimentFilePath);
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
            // Construct output header for recordings.csv
            List<String> header = new ArrayList<>(parser.getHeaderMap().keySet());
            header.addAll(Arrays.asList(
                    "Number of Spots", "Number of Tracks", "Number of Spots in All Tracks",
                    "Number of Frames", "Run Time", "Time Stamp", "Exclude", "Tau", "R Squared", "Density"
            ));
            printer.printRecord(header);

            for (CSVRecord record : parser) {
                // 🔹 Check for cancel request before starting a new recording
                if (dialog != null && dialog.isCancelled()) {
                    PaintLogger.warningf("User requested cancellation. Stopping after current recording.");
                    break;
                }

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

                        final TrackMateResults[] trackMateResults = new TrackMateResults[1];

                        boolean finished = runWithWatchdog(() -> {
                            try {
                                trackMateResults[0] = RunTrackMateOnRecording.RunTrackMateOnRecording(
                                        experimentPath,
                                        imagesPath,
                                        trackMateConfig,
                                        threshold,
                                        experimentInfo);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }, 1500); // 1500 seconds max

                        if (!finished || trackMateResults[0] == null || !trackMateResults[0].isSuccess()) {
                            PaintLogger.errorf("   TrackMate processing failed or timed out for recording '%s'.", recordingName);
                            PaintLogger.errorf("");
                            status = false;
                            continue;
                        }
                        if (!trackMateResults[0].isCalculationPerformed()) {
                            numberRecordings++;
                            continue;
                        }

                        int durationInSeconds = (int) (trackMateResults[0].getDuration().toMillis() / 1000);
                        PaintLogger.infof("   Recording '%s' (%d of %d) processed in %s.",
                                recordingName, numberRecordings + 1, numberRecordingsToProcess,
                                formatDuration(durationInSeconds));
                        PaintLogger.blankline();

                        // Update counters and values
                        totalDuration = totalDuration.plus(trackMateResults[0].getDuration());
                        numberRecordings++;

                        numberOfSpots = trackMateResults[0].getNumberOfSpots();
                        numberOfTracks = trackMateResults[0].getNumberOfTracks();
                        numberOfFrames = trackMateResults[0].getNumberOfFrames();
                        numberOfSpotsInAllTracks = trackMateResults[0].getNumberOfSpotsInALlTracks();
                        runTime = durationInSeconds;
                        timeStamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else {
                        PaintLogger.blankline();
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
        PaintLogger.blankline();
        return status;
    }
}