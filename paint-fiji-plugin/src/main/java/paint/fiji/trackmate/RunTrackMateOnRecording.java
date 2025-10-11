package paint.fiji.trackmate;

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
import paint.fiji.tracks.TrackCsvWriter;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;

import static paint.shared.config.PaintConfig.getBoolean;

/**
 * Executes the TrackMate pipeline on a single recording.
 * <p>
 * Handles loading of an ND2 image, spot detection, tracking, visualization,
 * result export (overlay images, tracks CSV), and returns a {@link TrackMateResults}
 * object with key statistics.
 */
public class RunTrackMateOnRecording {

    /**
     * Verbose flag to optionally print extra config information.
     */
    final boolean verbose = false;

    /**
     * Global debug flag for enabling extra validation/logging.
     */
    static final boolean debug = true;

    /**
     * Run the TrackMate workflow on a single recording.
     *
     * @param experimentPath       base path of the experiment where results are written
     * @param imagesPath           path to input ND2 image files
     * @param trackMateConfig      TrackMate configuration settings
     * @param threshold            intensity threshold for spot detection
     * @param experimentInfoRecord metadata about the current experiment/recording
     * @return {@link TrackMateResults} with run outcome and statistics
     * @throws IOException if directories or files cannot be created/written
     */
    public static TrackMateResults RunTrackMateOnRecording(Path experimentPath,
                                                           Path imagesPath,
                                                           TrackMateConfig trackMateConfig,
                                                           double threshold,
                                                           ExperimentInfo experimentInfoRecord) throws IOException {

        final boolean verbose = false;
        final boolean debug = getBoolean("Debug", "RunTrackMateOnRecording", false);

        // Start timestamp
        LocalDateTime start = LocalDateTime.now();

        // Disable noisy Bio-Formats console output
        DebugTools.setRootLevel("OFF");

        // --- Step 1: Load ND2 image ---
        PaintLogger.raw("                       TrackMate - Image Loading: ");
        ImagePlus imp = null;
        File nd2File = new File(imagesPath.toFile(), experimentInfoRecord.getRecordingName() + ".nd2");
        if (!nd2File.exists()) {
            PaintLogger.errorf("Could not open image file: %s", nd2File.getAbsolutePath());
        }
        try {
            // Using ImageJ to load ND2 instead of Bio-Formats importer (simplified)
            imp = IJ.openImage(nd2File.getAbsolutePath());
        } catch (Exception e) {
            PaintLogger.errorf("Could not load image file: %s", nd2File.getAbsolutePath());
        }
        if (imp == null) {
            PaintLogger.errorf("The image file %s could not be opened.", nd2File);
            return new TrackMateResults(false);
        }

        // Show the image in ImageJ
        imp.show();
        IJ.wait(100); // small delay to let window initialize

        // Enhance contrast & convert to grayscale
        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run("Grays");

        // --- Step 2: Save Brightfield snapshot ---
        Path jpgPath = experimentPath.resolve("Brightfield Images")
                .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
        if (Files.notExists(jpgPath.getParent())) {
            Files.createDirectories(jpgPath.getParent());
        }
        if (!Files.exists(jpgPath)) {
            IJ.saveAs(imp, "Jpeg", jpgPath.toString());
        }

        // --- Step 3: Prepare TrackMate model and settings ---
        Model model = new Model();
        model.setLogger(Logger.IJ_LOGGER);
        model.setLogger(Logger.VOID_LOGGER);

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

        // Configure tracker
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

        // Filters and analyzers
        settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));
        settings.addAllAnalyzers();
        settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS", trackMateConfig.getMinNrSpotsInTrack(), true));

        // --- Step 4: Run TrackMate pipeline ---
        TrackMate trackmate = new TrackMate(model, settings);

        if (!trackmate.checkInput()) {
            PaintLogger.errorf("   TrackMate - input check failed: %s", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        // Spot detection
        PaintLogger.raw("\n                       Trackmate - spot detection: ");
        if (!trackmate.execDetection()) {
            PaintLogger.errorf("TrackMate - execDetection failed:", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }
        if (debug) {
            PaintLogger.debugf("                  TrackMate - spot detection succeeded");
        }

        int numberSpots = model.getSpots().getNSpots(false);
        // Stop if too many spots
        if (numberSpots > trackMateConfig.getMaxNrSpotsInImage()) {
            PaintLogger.warningf("   Too many spots detected (%d). Limit is %d.", numberSpots, trackMateConfig.getMaxNrSpotsInImage());
            PaintLogger.warningf("");
            try {
                ImageWindow win = imp.getWindow();
                if (win != null) {
                    imp.close();
                }
            } catch (Exception e) {
                PaintLogger.warningf("Error while closing image: %s", e.getMessage());
            }
            return new TrackMateResults(true, false);
        }
        PaintLogger.raw("\n                       TrackMate - number of spots detected: " + numberSpots);

        // Track building
        PaintLogger.raw("\n                       Trackmate - track detection: ");
        if (!trackmate.process()) {
            PaintLogger.errorf("   TrackMate process failed: %s", trackmate.getErrorMessage());
            return new TrackMateResults(false);
        }

        // --- Step 5: Visualization ---
        final SelectionModel selectionModel = new SelectionModel(model);
        final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
        ds.setSpotVisible(false);
        ds.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, trackMateConfig.getTrackColoring());

        final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, ds);
        displayer.render();
        displayer.refresh();
        if (debug) {
            PaintLogger.debugf("      TrackMate - visualisation successful");
        }

        // Capture track overlay image
        final ImagePlus capture = CaptureOverlayAction.capture(imp, -1, 1, null);
        Path imagePath = experimentPath.resolve("TrackMate Images")
                .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
        if (capture != null) {
            if (!new FileSaver(capture).saveAsTiff(String.valueOf(imagePath))) {
                PaintLogger.errorf("Failed to save TIFF to: %s", imagePath);
            }
        } else {
            PaintLogger.infof("Overlay capture returned null.");
        }
        if (debug) {
            PaintLogger.debugf("      TrackMate - wrote trackmate image '%s'", imagePath.toString());
        }

        // --- Step 6: Write tracks CSV ---
        String tracksName = experimentInfoRecord.getRecordingName() + "-tracks.csv";
        Path tracksPath = experimentPath.resolve(tracksName);
        if (debug) {
            PaintLogger.debugf("      TrackMate - wrote tracks file '%s'", tracksPath);
        }

        int numberOfSpotsInALlTracks = 0;
        try {
            numberOfSpotsInALlTracks = TrackCsvWriter.writeTracksCsv(
                    trackmate,
                    experimentInfoRecord.getRecordingName(),
                    tracksPath.toFile(),
                    true);
        } catch (IOException e) {
            PaintLogger.errorf("Failed to write tracks to 's%'", tracksPath);
        }

        // --- Step 7: Summarize results ---
        int numberOfSpotsTotal = model.getSpots().getNSpots(true);
        int numberOfTracks = model.getTrackModel().nTracks(false);
        int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
        int numberOfFrames = imp.getNFrames();

        // Small pause before cleanup
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            PaintLogger.errorf("Failed to sleep - %s", e.getMessage());
        }

        // Safely close image window
        try {
            ImageWindow win = imp.getWindow();
            if (win != null) {
                imp.close();
            }
        } catch (Exception e) {
            PaintLogger.warningf("Error while closing image: %s", e.getMessage());
        }

        Duration duration = Duration.between(start, LocalDateTime.now());

        // Return encapsulated results
        return new TrackMateResults(true, true, numberOfSpotsTotal, numberOfTracks,
                                    numberOfFilteredTracks, numberOfFrames, duration, numberOfSpotsInALlTracks);
    }

    /**
     * Print debug message to stdout if {@link #debug} is enabled.
     *
     * @param message the debug message
     */
    static void debugMessage(String message) {
        if (debug) {
            System.out.println(message);
        }
    }
}