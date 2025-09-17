package paint.fijiUtilities;

import debug.TrackMateSettingsDebugger;
import debug.TrackMateSettingsValidator;
import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.action.CaptureOverlayAction;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettingsIO;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.util.LogRecorder;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import loci.common.DebugTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import utilities.AppLogger;
import config.TrackMateConfig;

public class RunTrackMate {

    final boolean verbose = false;
    static final boolean debug = true;

    public static TrackMateResults RunTrackMate(Path experimentPath, Path imagesPath, TrackMateConfig trackMateConfig, double threshold, ExperimentInfoRecord experimentInfoRecord) throws IOException {

        final boolean verbose = false;
        final boolean debug = false;

        // Record the start time
        LocalDateTime start = LocalDateTime.now();

        // Suppress Bio-Formats console output
        DebugTools.setRootLevel("OFF");

        // Open the image and show it
        File nd2File = new File(imagesPath.toFile(), experimentInfoRecord.recordingName + ".nd2");
        ImagePlus imp = IJ.openImage(nd2File.getAbsolutePath());
        if (imp == null) {
            AppLogger.errorf("The image file %s could not be opened.", nd2File);
            return new TrackMateResults(false);
        }
        imp.show();

        // Change the image color
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run("Grays");


        // Save the Brightfield image as a jpg if it does not already exist
        Path jpgPath = experimentPath
                .resolve("Brightfield Images")
                .resolve(experimentInfoRecord.recordingName + ".jpg");
        // Check if the Brightfield Images directory exists and create one if it does not exist
        if (Files.notExists(jpgPath.getParent())) {
            Files.createDirectories(jpgPath.getParent());
        }
        if (!Files.exists(jpgPath)) {
            IJ.saveAs(imp, "Jpeg", jpgPath.toString());
        }

        Model model = new Model();

        // Suppress all TrackMate logging
        model.setLogger(Logger.IJ_LOGGER);
        model.setLogger(Logger.VOID_LOGGER);

        // Prepare the Settings object
        Settings settings = new Settings(imp);

        if (verbose) {
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

        // Configure the tracker settings, first set the default, but then override parameters that we know are important
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

        // Configure spot filters - Do not filter out any nr_spots
        settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));

        // Add ALL the feature analyzers known to TrackMate.
        // They will yield numerical features for the results, such as speed, mean intensity, etc.
        settings.addAllAnalyzers();

        // Configure track filters - Only consider tracks of 3 and longer.
        settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS", trackMateConfig.getMinNrSpotsInTrack(), true));

        // Debug
        if (debug) {
            TrackMateSettingsDebugger.logSettings(settings);
            TrackMateSettingsValidator.validate(settings);
        }

        // Instantiate plugin
        TrackMate trackmate = new TrackMate(model, settings);

        if (!trackmate.checkInput()) {
            AppLogger.errorf("TrackMate - input check failed: %s", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        // Run the spot detection step first
        if (!trackmate.execDetection()) {
            AppLogger.errorf("TrackMate - execDetection failed:", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        int nrSpots = model.getSpots().getNSpots(false);
        if (nrSpots > trackMateConfig.getMaxNrSpotsInImage()) {
            AppLogger.errorf("Too many spots detected (%d). Limit is {%d}.", nrSpots, trackMateConfig.getMaxNrSpotsInImage());
            return new TrackMateResults(false);
        }

        // Continue with full TrackMate processing - nr_spots is within limits
        if (!trackmate.process()) {
            AppLogger.errorf("TrackMate process failed: %s", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        // --- Models
        final SelectionModel selectionModel = new SelectionModel(model);

        // --- Display settings (user defaults)
        final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
        ds.setSpotVisible(false);
        ds.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, trackMateConfig.getTrackColoring());

        // --- Display
        final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, ds);
        displayer.render();
        displayer.refresh();

        // --- Capture overlay and save
        final Logger tmLogger = new LogRecorder(Logger.VOID_LOGGER);
        final ImagePlus capture = CaptureOverlayAction.capture(imp, -1, 1, tmLogger);
        if (capture != null) {
            Path imagePath = experimentPath
                    .resolve("TrackMate Images")
                    .resolve(experimentInfoRecord.recordingName + ".jpg");
            // Check if the TrackMate Images directory exists and create one if it does not exist
            if (Files.notExists(imagePath.getParent())) {
                Files.createDirectories(imagePath.getParent());
            }
            if (!new FileSaver(capture).saveAsTiff(String.valueOf(imagePath))) {
                AppLogger.errorf("Failed to save TIFF to: %s", imagePath);
            }
        } else {
            AppLogger.infof("Overlay capture returned null.");
        }

        //  Write the recording tracks to a CSV file
        Path tracksPath = experimentPath.resolve(experimentInfoRecord.recordingName + "-tracks.csv");
        int numberOfSpotsInALlTracks = TrackCsvWriter.writeTracksCsv(
                trackmate,
                experimentInfoRecord.recordingName,
                tracksPath.toFile(),
                true);

        // Get the results
        int numberOfSpots = model.getSpots().getNSpots(true);  // Number of visible spots only
        int numberOfTracks = model.getTrackModel().nTracks(false); // Number of all tracks
        int numberOfFilteredTracks = model.getTrackModel().nTracks(true);      // Number of filtered tracks
        int numberOfFrames = imp.getNFrames();

        // Show the image for 2 seconds before closing it and moving on
        try {
            Thread.sleep(2000); // pause 2 seconds
        } catch (InterruptedException e) {
            AppLogger.errorf("Failed to sleep - %s", e.getMessage());
        }
        imp.close();

        // Record the end time
        LocalDateTime end = LocalDateTime.now();
        Duration duration = Duration.between(start, end);

        return new TrackMateResults(true, numberOfSpots, numberOfTracks, numberOfFilteredTracks, numberOfFrames, duration, numberOfSpotsInALlTracks);
    }

    static void debugMessage(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}