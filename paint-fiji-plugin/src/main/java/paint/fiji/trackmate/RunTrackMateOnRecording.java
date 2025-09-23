package paint.fiji.trackmate;

import debug.TrackMateSettingsDebugger;
import debug.TrackMateSettingsValidator;
import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.io.FileSaver;
import loci.common.DebugTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import paint.fiji.tracks.TrackCsvWriter;
import paint.shared.utils.AppLogger;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.ExperimentInfo;

import static paint.shared.config.PaintConfig.getBoolean;

/**
 * Utility class for executing the TrackMate plugin on a single recording.
 * This includes image loading, spot detection, tracking, visualization, and result output.
 */
public class RunTrackMateOnRecording {

    /** Verbose output flag (prints configuration to stdout when enabled). */
    final boolean verbose = false;

    /** Global debug flag (enables validation and detailed logging when true). */
    static final boolean debug = true;

    /**
     * Executes the TrackMate pipeline on the specified recording and image data.
     *
     * @param experimentPath        base path of the experiment where results will be stored
     * @param imagesPath            path containing the input ND2 image files
     * @param trackMateConfig       configuration parameters controlling detection and tracking
     * @param threshold             intensity threshold for spot detection
     * @param experimentInfoRecord  metadata about the experiment (e.g. recording name)
     * @return                      a {@link TrackMateResults} summarizing the outcome
     * @throws IOException          if an I/O error occurs while creating directories or saving files
     */
    public static TrackMateResults RunTrackMateOnRecording(Path experimentPath,
                                                           Path imagesPath,
                                                           TrackMateConfig trackMateConfig,
                                                           double threshold,
                                                           ExperimentInfo experimentInfoRecord) throws IOException {

        final boolean verbose = false;
        final boolean debug = getBoolean("Debug", "RunTrackMateOnRecording", false);

        // Record the start time
        LocalDateTime start = LocalDateTime.now();

        // Suppress Bio-Formats console output
        DebugTools.setRootLevel("OFF");

        // --- Load image from ND2 file ---
        ImagePlus imp = null;
        File nd2File = new File(imagesPath.toFile(), experimentInfoRecord.getRecordingName() + ".nd2");
        if (!nd2File.exists()) {
            AppLogger.errorf("Could not open image file: %s", nd2File.getAbsolutePath());
        }
        try {
//            ImporterOptions options = new ImporterOptions();
//            options.setId(nd2File.getAbsolutePath());
//            options.setQuiet(true); // suppress Bio-Formats dialogs
//
//            ImagePlus[] imps = BF.openImagePlus(options);
//            if (imps != null && imps.length > 0) {
//                imp = imps[0];  // first series
//            }
            imp = IJ.openImage(nd2File.getAbsolutePath());
        }
        catch (Exception e) {
            AppLogger.errorf("Could not load image file: %s", nd2File.getAbsolutePath());
        }
        if (imp == null) {
            AppLogger.errorf("The image file %s could not be opened.", nd2File);
            return new TrackMateResults(false);
        }

        // Show the image in ImageJ
        imp.show();
        IJ.wait(100); // Wait for UI window to initialize

        // Enhance contrast and convert to grayscale
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run("Grays");

        // --- Save Brightfield JPEG snapshot ---
        Path jpgPath = experimentPath.resolve("Brightfield Images")
                .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
        if (Files.notExists(jpgPath.getParent())) {
            Files.createDirectories(jpgPath.getParent());
        }
        if (!Files.exists(jpgPath)) {
            IJ.saveAs(imp, "Jpeg", jpgPath.toString());
        }

        // --- Set up TrackMate model ---
        Model model = new Model();
        model.setLogger(Logger.IJ_LOGGER);
        model.setLogger(Logger.VOID_LOGGER);

        // --- Build TrackMate settings from config ---
        Settings settings = new Settings(imp);

        if (debug && verbose) {
            System.out.println("TrackMateConfig: " + trackMateConfig);
            System.out.println("Threshold: " + threshold);
        }

        // Configure the detector settings, first set the default, but then override parameters that we know are important
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
        settings.detectorSettings.put("TARGET_CHANNEL", trackMateConfig.getTargetChannel());
        settings.detectorSettings.put("RADIUS", trackMateConfig.getRadius());
        settings.detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", trackMateConfig.isDoSubpixelLocalization());
        settings.detectorSettings.put("THRESHOLD", threshold);
        settings.detectorSettings.put("DO_MEDIAN_FILTERING", trackMateConfig.isMedianFiltering());

        // Tracker settings
        settings.trackerFactory = new SparseLAPTrackerFactory();
        settings.trackerSettings = settings.trackerFactory.getDefaultSettings();
        settings.trackerSettings.put("LINKING_MAX_DISTANCE", trackMateConfig.getLinkingMaxDistance());
        settings.trackerSettings.put("ALTERNATIVE_LINKING_COST_FACTOR", trackMateConfig.getAlternativeLinkingCostFactor());
        settings.trackerSettings.put("ALLOW_GAP_CLOSING", trackMateConfig.isAllowGapClosing());
        settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", trackMateConfig.getGapClosingMaxDistance());
        settings.trackerSettings.put("MAX_FRAME_GAP", trackMateConfig.getMaxFrameGap());
        settings.trackerSettings.put("ALLOW_TRACK_SPLITTING", trackMateConfig.isAllowTrackSplitting());
        settings.trackerSettings.put("SPLITTING_MAX_DISTANCE", trackMateConfig.getSplittingMaxDistance());
        settings.trackerSettings.put("ALLOW_TRACK_MERGING", trackMateConfig.isAllowTrackMerging());
        settings.trackerSettings.put("MERGING_MAX_DISTANCE", trackMateConfig.getMergingMaxDistance());

        // --- Filter and analyze ---
        settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));
        settings.addAllAnalyzers();
        settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS", trackMateConfig.getMinNrSpotsInTrack(), true));

        // Optional debugging
        if (debug && verbose) {
            TrackMateSettingsDebugger.logSettings(settings);
            TrackMateSettingsValidator.validate(settings);
        }

        // --- Run TrackMate pipeline ---
        TrackMate trackmate = new TrackMate(model, settings);

        if (!trackmate.checkInput()) {
            AppLogger.errorf("   TrackMate - input check failed: %s", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        // Run the spot detection step first
        if (!trackmate.execDetection()) {
            AppLogger.errorf("TrackMate - execDetection failed:", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }
        if (debug) AppLogger.debugf("      TrackMate - spot detection succeeded");

        int numberSpots = model.getSpots().getNSpots(false);
        if (numberSpots > trackMateConfig.getMaxNrSpotsInImage()) {
            AppLogger.errorf("   Too many spots detected (%d). Limit is %d.", numberSpots, trackMateConfig.getMaxNrSpotsInImage());
            ImageWindow win = imp.getWindow();
            imp.close();
            return new TrackMateResults(false);
        }
        AppLogger.infof("      TrackMate - number of spots detected: %d",  numberSpots);


        // Start the dot-printing watchdog
        Boolean dotPrint = numberSpots > 500000;
        Thread dotPrinter = null;
        final int[] dotCount = {0};

        if (dotPrint) {
            dotPrinter = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.print(".");
                    System.out.flush(); // Force immediate output
                    dotCount[0] += 1;
                    if (dotCount[0] > 80) {
                        System.out.print("\n");
                        System.out.flush(); // Force immediate output
                        dotCount[0] = 0;
                    }

                    try {
                        Thread.sleep(numberSpots / 500); // Print dot every so often
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            dotPrinter.start();
        }

        // Continue with full TrackMate processing - nr_spots is within limits
        if (!trackmate.process()) {
            AppLogger.errorf("   TrackMate process failed: %s", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        // Stop the dot printer
        if (dotPrint) {
            dotPrinter.interrupt();
            System.out.println(); // Finish the line cleanly
        }

        // --- Visualization ---
        final SelectionModel selectionModel = new SelectionModel(model);
        final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
        ds.setSpotVisible(false);
        ds.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, trackMateConfig.getTrackColoring());

        final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, ds);
        displayer.render();
        displayer.refresh();
        if (debug) AppLogger.debugf("      TrackMate - visualisation successful");

        // --- Capture overlay image ---
        final ImagePlus capture = CaptureOverlayAction.capture(imp, -1, 1, null);
        Path imagePath = experimentPath.resolve("TrackMate Images")
                .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
        if (capture != null) {
            if (!new FileSaver(capture).saveAsTiff(String.valueOf(imagePath))) {
                AppLogger.errorf("Failed to save TIFF to: %s", imagePath);
            }

        } else {
            AppLogger.infof("Overlay capture returned null.");
        }
        if (debug) AppLogger.debugf("      TrackMate - wrote trackmate image '%s'", imagePath.toString());

        // --- Write tracks to CSV ---
        String tracksName = experimentInfoRecord.getRecordingName() + "-tracks.csv";
        Path tracksPath = experimentPath.resolve(tracksName);
        if (debug) AppLogger.debugf("      TrackMate - wrote tracks file '%s'", tracksPath);

        int numberOfSpotsInALlTracks = 0;
        try {
            numberOfSpotsInALlTracks = TrackCsvWriter.writeTracksCsv(
                    trackmate,
                    experimentInfoRecord.getRecordingName(),
                    tracksPath.toFile(),
                    true);
        }
        catch (IOException e) {
            AppLogger.errorf("Failed to write tracks to 's%'", tracksPath);
        }

        // --- Summarize results ---
        int numberOfSpots = model.getSpots().getNSpots(true);
        int numberOfTracks = model.getTrackModel().nTracks(false);
        int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
        int numberOfFrames = imp.getNFrames();

        // Optional pause before closing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            AppLogger.errorf("Failed to sleep - %s", e.getMessage());
        }

        // Safely close the image window
        ImageWindow win = imp.getWindow();
        imp.close();

        Duration duration = Duration.between(start, LocalDateTime.now());

        return new TrackMateResults(true, numberOfSpots, numberOfTracks, numberOfFilteredTracks,
                numberOfFrames, duration, numberOfSpotsInALlTracks);
    }

    /**
     * Prints a debug message if the global {@link #debug} flag is set.
     *
     * @param message the debug message to print
     */
    static void debugMessage(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}
