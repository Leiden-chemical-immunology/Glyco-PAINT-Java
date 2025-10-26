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
import ij.io.FileSaver;
import loci.common.DebugTools;
import paint.fiji.tracks.TrackCsvWriter;
import paint.shared.config.TrackMateConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static paint.shared.config.PaintConfig.getBoolean;

/**
 * The RunTrackMateOnRecording class provides functionality to run the TrackMate
 * tracking algorithm on a specified recording. It handles the configuration,
 * image loading, progress checks, and results management for the tracking process.
 *
 * This class is designed to work with experimental data stored in specified paths and
 * utilizes the TrackMate framework for analyzing particle trajectories.
 *
 * It also handles early cancellation of processing, cleanup of resources, and
 * debugging information if enabled.
 */
public class RunTrackMateOnRecording extends TrackMateHeadless {

    static final boolean debug = true;



    /**
     * Runs the TrackMate analysis pipeline on a given microscopy recording.
     * This method processes an ND2 recording, detects spots using TrackMate,
     * applies user-specified filtering rules and extracts tracking results.
     *
     * @param experimentPath The path to the experiment directory where results will be stored.
     * @param imagesPath The path to the directory containing image files.
     * @param trackMateConfig Configuration settings for TrackMate, including detection and tracking parameters.
     * @param threshold The threshold value to use for spot detection in the TrackMate pipeline.
     * @param experimentInfoRecord Metadata regarding the experiment, including the recording name.
     * @param dialog The dialog instance used for providing progress feedback and handling user cancellation.
     * @return A {@code TrackMateResults} object containing the results of the spot detection and tracking, or null if the process is cancelled or fails.
     * @throws IOException If an I/O error occurs during file operations.
     */
    public static TrackMateResults runTrackMateOnRecording(Path experimentPath,
                                                           Path imagesPath,
                                                           TrackMateConfig trackMateConfig,
                                                           double threshold,
                                                           ExperimentInfo experimentInfoRecord,
                                                           ProjectDialog dialog) throws IOException {

        final boolean debugFlag = getBoolean("Debug", "Debug RunTrackMateOnRecording", false);
        LocalDateTime start = LocalDateTime.now();
        DebugTools.setRootLevel("OFF");

        ImagePlus imp = null;
        ImagePlus impBrightfield = null;
        ImagePlus capture = null;

        try {
            // --- Step 1: Load ND2 image ---
            PaintLogger.raw("                       TrackMate - Image Loading:   ");
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            File nd2File = new File(imagesPath.toFile(), experimentInfoRecord.getRecordingName() + ".nd2");
            if (!nd2File.exists()) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Could not open image file: %s", nd2File.getAbsolutePath());
                }
                return cancelEarly(imp, impBrightfield, capture);
            }

            try {
                imp = IJ.openImage(nd2File.getAbsolutePath());
            } catch (Exception e) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Could not load image file: %s", nd2File.getAbsolutePath());
                }
                return cancelEarly(imp, impBrightfield, capture);
            }

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            if (imp == null) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Unsupported format or file not found: %s", nd2File);
                }
                return cancelEarly(imp, impBrightfield, capture);
            }

            imp.show();
            IJ.wait(100);
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            IJ.run(imp, "Enhance Contrast", "saturated=0.35");
            IJ.run("Grays");
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            // --- Step 2: Save Brightfield snapshot ---
            Path jpgPath = experimentPath.resolve("Brightfield Images")
                    .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
            if (Files.notExists(jpgPath.getParent())) {
                Files.createDirectories(jpgPath.getParent());
            }
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            Path brightFieldPath = null;
            String baseName      = experimentInfoRecord.getRecordingName();

            List<String> candidates = Arrays.asList(
                    baseName + "-BF.nd2",
                    baseName + "-BF1.nd2",
                    baseName + "-BF2.nd2");
            try {
                brightFieldPath = candidates.stream()
                        .map(imagesPath::resolve)
                        .filter(Files::exists)
                        .findFirst()
                        .orElse(null);
            }
            catch (Exception e) {
                PaintLogger.errorf("Could not find Brightfield image file: %s", candidates);
            }

            if (brightFieldPath == null || Files.notExists(brightFieldPath)) {
                PaintLogger.warnf("      Could not open brightfield file: %s",
                                     brightFieldPath == null ? "none found" : brightFieldPath.toString());
            }
            else {
                try {
                    impBrightfield = IJ.openImage(brightFieldPath.toString());
                    IJ.run(impBrightfield, "Enhance Contrast", "saturated=0.35");
                    IJ.saveAs(impBrightfield, "Jpeg", jpgPath.toString());
                } catch (Exception e) {
                    PaintLogger.errorf("Error handling brightfield file: %s", e.getMessage());
                }
            }

            // --- Step 3: Prepare TrackMate model and settings ---
            Model model = new Model();
            model.setLogger(Logger.VOID_LOGGER);

            Settings settings = new Settings(imp);
            settings.detectorFactory = new LogDetectorFactory();
            settings.detectorSettings = settings.detectorFactory.getDefaultSettings();

            // @formatter:off
            settings.detectorSettings.put("TARGET_CHANNEL",           trackMateConfig.getTargetChannel());
            settings.detectorSettings.put("RADIUS",                   trackMateConfig.getRadius());
            settings.detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", trackMateConfig.isDoSubpixelLocalization());
            settings.detectorSettings.put("THRESHOLD",                threshold);
            settings.detectorSettings.put("DO_MEDIAN_FILTERING",      trackMateConfig.isMedianFiltering());

            settings.trackerFactory  = new SparseLAPTrackerFactory();
            settings.trackerSettings = settings.trackerFactory.getDefaultSettings();

            settings.trackerSettings.put("LINKING_MAX_DISTANCE",            trackMateConfig.getLinkingMaxDistance());
            settings.trackerSettings.put("ALTERNATIVE_LINKING_COST_FACTOR", trackMateConfig.getAlternativeLinkingCostFactor());
            settings.trackerSettings.put("ALLOW_GAP_CLOSING",               trackMateConfig.isAllowGapClosing());
            settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE",        trackMateConfig.getGapClosingMaxDistance());
            settings.trackerSettings.put("MAX_FRAME_GAP",                   trackMateConfig.getMaxFrameGap());
            settings.trackerSettings.put("ALLOW_TRACK_SPLITTING",           trackMateConfig.isAllowTrackSplitting());
            settings.trackerSettings.put("SPLITTING_MAX_DISTANCE",          trackMateConfig.getSplittingMaxDistance());
            settings.trackerSettings.put("ALLOW_TRACK_MERGING",             trackMateConfig.isAllowTrackMerging());
            settings.trackerSettings.put("MERGING_MAX_DISTANCE",            trackMateConfig.getMergingMaxDistance());
            // @formatter:on

            settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));
            settings.addAllAnalyzers();
            settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS", trackMateConfig.getMinNumberOfSpotsInTrack(), true));

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            // --- Step 4: Run TrackMate pipeline ---
            TrackMate trackmate = new TrackMate(model, settings);
            if (!trackmate.checkInput()) {
                PaintLogger.errorf("TrackMate input check failed: %s", trackmate.getErrorMessage());
                return cancelEarly(imp, impBrightfield, capture);
            }

            PaintLogger.raw("\n                       TrackMate - spot detection:  ");
            try {
                if (!trackmate.execDetection()) {
                    PaintLogger.errorf("TrackMate - execDetection failed: %s", trackmate.getErrorMessage());
                    return cancelEarly(imp, impBrightfield, capture);
                }
            } catch (Exception e) {
                PaintLogger.errorf("Unexpected error during detection: %s", e.getMessage());
                return cancelEarly(imp, impBrightfield, capture);
            }

            int numberOfSpots = model.getSpots().getNSpots(false);
            if (numberOfSpots > trackMateConfig.getMaxNumberOfSpotsInImage()) {
                PaintLogger.warnf("   Trackmate - Too many spots detected (%d). Limit is %d.",
                                     numberOfSpots, trackMateConfig.getMaxNumberOfSpotsInImage());
                return cancelEarly(imp, impBrightfield, capture);
            }
            else {
                String numberOfSpotsString = " (" +  numberOfSpots + " spots detected).";
                PaintLogger.raw(numberOfSpotsString);
            }

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            PaintLogger.raw("\n                       TrackMate - track detection: ");
            try {
                if (!trackmate.process()) {
                    PaintLogger.errorf("TrackMate process failed: %s", trackmate.getErrorMessage());
                    return cancelEarly(imp, impBrightfield, capture);
                }
            } catch (Exception e) {
                PaintLogger.errorf("Unexpected error during TrackMate process: %s", e.getMessage());
                return cancelEarly(imp, impBrightfield, capture);
            }

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            // --- Step 5: Visualization ---
            final SelectionModel selectionModel = new SelectionModel(model);
            final DisplaySettings ds = DisplaySettingsIO.readUserDefault();
            ds.setSpotVisible(false);
            ds.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS, trackMateConfig.getTrackColoring());
            final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, ds);
            displayer.render();
            displayer.refresh();


            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield, capture);
            }

            capture = CaptureOverlayAction.capture(imp, -1, 1, null);
            Path imagePath = experimentPath.resolve("TrackMate Images")
                    .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
            if (capture != null) {
                if (!new FileSaver(capture).saveAsTiff(String.valueOf(imagePath))) {
                    PaintLogger.errorf("Failed to save TIFF to: %s", imagePath);
                }
            }
            IJ.wait(2000);

            // --- Step 6: Write tracks CSV ---
            String tracksName = experimentInfoRecord.getRecordingName() + "-tracks.csv";
            Path tracksPath = experimentPath.resolve(tracksName);
            int numberOfSpotsInALlTracks = 0;
            try {
                numberOfSpotsInALlTracks = TrackCsvWriter.writeTracksCsv(
                        trackmate,
                        experimentInfoRecord.getExperimentName(),
                        experimentInfoRecord.getRecordingName(),
                        tracksPath.toFile(),
                        true);
            } catch (IOException e) {
                PaintLogger.errorf("Failed to write tracks to '%s'", tracksPath);
            }

            // --- Step 7: Summarize results ---
            int numberOfSpotsTotal     = model.getSpots().getNSpots(true);
            int numberOfTracks         = model.getTrackModel().nTracks(false);
            int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
            int numberOfFrames         = imp.getNFrames();

            Duration duration = Duration.between(start, LocalDateTime.now());

            closeImages(imp, impBrightfield, capture);

            return new TrackMateResults(true, true, numberOfSpotsTotal, numberOfTracks,
                                        numberOfFilteredTracks, numberOfFrames, duration, numberOfSpotsInALlTracks);

        } catch (Exception e) {
            PaintLogger.errorf("Exception during TrackMate processing: %s", e.getMessage());
            return cancelEarly(imp, impBrightfield, capture);
        } finally {
            closeImages(imp, impBrightfield, capture);
        }
    }

    /**
     * Checks if the given thread has been interrupted or if the provided dialog indicates a cancellation.
     *
     * @param t the thread whose interruption status is to be checked
     * @param dialog the dialog to check for a cancellation flag
     * @return true if the thread is interrupted or the dialog is cancelled, false otherwise
     */
    private static boolean isCancelled(Thread t, ProjectDialog dialog) {
        return t.isInterrupted() || (dialog != null && dialog.isCancelled());
    }

    /**
     * Closes the provided images if they are visible and handles any exceptions that may occur during the closing.
     *
     * @param images Varargs parameter accepting one or more ImagePlus objects to be closed.
     */
    private static void closeImages(ImagePlus... images) {
        for (ImagePlus img : images) {
            try {
                if (img != null && img.isVisible()) {
                    img.close();
                }
            } catch (Exception e) {
                PaintLogger.warnf("Error closing image: %s", e.getMessage());
            }
        }
    }

    /**
     * Cancels the current operation early, closing any provided images and
     * returning a result indicating that the process was cancelled.
     *
     * @param images one or more {@code ImagePlus} objects to be closed during the cancellation process.
     * @return a {@code TrackMateResults} instance indicating that the process was not successfully completed.
     */
    private static TrackMateResults cancelEarly(ImagePlus... images) {
        closeImages(images);
        PaintLogger.warnf("   Recording cancelled.");
        return new TrackMateResults(false);
    }
}