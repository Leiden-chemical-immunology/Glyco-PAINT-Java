/******************************************************************************
 *  Class:        RunTrackMateOnRecording.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Executes the TrackMate tracking pipeline for a single microscopy recording.
 *    Handles image loading, brightfield snapshot generation, TrackMate
 *    configuration setup, execution, visualization, and CSV result export.
 *
 *  DESCRIPTION:
 *    • Loads the ND2 image and optional brightfield reference.
 *    • Configures and runs TrackMate in deterministic, headless mode.
 *    • Applies spot and track filtering based on configuration parameters.
 *    • Exports per-recording images and tracking data to the experiment directory.
 *    • Supports cancellation and safe cleanup of ImagePlus instances.
 *
 *  RESPONSIBILITIES:
 *    • Manage end-to-end TrackMate processing for a single recording.
 *    • Integrate Paint configuration and runtime parameters.
 *    • Handle user cancellation gracefully during processing.
 *    • Collect and summarize analysis results in {@link TrackMateResults}.
 *
 *  USAGE EXAMPLE:
 *    TrackMateResults result = RunTrackMateOnRecording.runTrackMateOnRecording(
 *        experimentPath,
 *        imagesPath,
 *        trackMateConfig,
 *        threshold,
 *        experimentInfo,
 *        dialog);
 *
 *  DEPENDENCIES:
 *    – fiji.plugin.trackmate.*
 *    – paint.shared.config.TrackMateConfig
 *    – paint.shared.objects.ExperimentInfo
 *    – paint.shared.dialogs.ProjectDialog
 *    – paint.shared.utils.PaintLogger
 *    – paint.fiji.tracks.TrackCsvWriter
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
import java.util.Locale;

import static paint.shared.config.PaintConfig.getBoolean;

/**
 * Provides functionality to run TrackMate analysis for a single microscopy
 * recording. Handles setup, detection, tracking, visualization, and export.
 * <p>
 * This class is typically invoked by {@link RunTrackMateOnExperiment} as part
 * of batch processing within a Paint project.
 * </p>
 */
public class RunTrackMateOnRecording extends TrackMateHeadless {

    /** Debug flag used for conditional runtime logging. */
    static final boolean debug = true;

    /**
     * Executes the TrackMate pipeline on a given recording.
     * Loads the ND2 image, applies analysis parameters, and writes results.
     *
     * @param experimentPath       directory where results will be written
     * @param imagesPath           directory containing the ND2 image
     * @param trackMateConfig      configuration object with TrackMate parameters
     * @param threshold            threshold for spot detection
     * @param experimentInfoRecord metadata describing this recording
     * @param dialog               optional dialog for user cancellation
     * @return {@link TrackMateResults} containing analysis statistics,
     *         or a cancellation result if aborted
     * @throws IOException if file operations fail
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

        ImagePlus imp            = null;
        ImagePlus impBrightfield = null;
        ImagePlus capture        = null;

        try {
            // -----------------------------------------------------------------
            // Step 1 – Load ND2 image
            // -----------------------------------------------------------------
            PaintLogger.raw("                       TrackMate - Image Loading:   ");
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield);
            }

            File nd2File = new File(imagesPath.toFile(), experimentInfoRecord.getRecordingName() + ".nd2");
            if (!nd2File.exists()) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Could not open image file: %s", nd2File.getAbsolutePath());
                }
                return cancelEarly(imp, impBrightfield);
            }

            try {
                imp = IJ.openImage(nd2File.getAbsolutePath());
            } catch (Exception e) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Could not load image file: %s", nd2File.getAbsolutePath());
                }
                return cancelEarly(imp, impBrightfield);
            }

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield);
            }

            if (imp == null) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Unsupported format or file not found: %s", nd2File);
                }
                return cancelEarly(imp, impBrightfield);
            }

            imp.show();
            IJ.wait(100);

            IJ.run(imp, "Enhance Contrast", "saturated=0.35");
            IJ.run("Grays");

            // -----------------------------------------------------------------
            // Step 2 – Save Brightfield snapshot
            // -----------------------------------------------------------------
            Path jpgPath = experimentPath.resolve("Brightfield Images")
                    .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
            if (Files.notExists(jpgPath.getParent())) {
                Files.createDirectories(jpgPath.getParent());
            }
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield);
            }

            Path brightFieldPath = null;
            String baseName = experimentInfoRecord.getRecordingName();

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
            } catch (Exception e) {
                PaintLogger.errorf("Could not locate brightfield file: %s", candidates);
            }

            if (brightFieldPath == null || Files.notExists(brightFieldPath)) {
                PaintLogger.warnf("      Could not open brightfield file: %s",
                                  brightFieldPath == null ? "none found" : brightFieldPath.toString());
            } else {
                try {
                    impBrightfield = IJ.openImage(brightFieldPath.toString());
                    IJ.run(impBrightfield, "Enhance Contrast", "saturated=0.35");
                    IJ.saveAs(impBrightfield, "Jpeg", jpgPath.toString());
                } catch (Exception e) {
                    PaintLogger.errorf("Error handling brightfield file: %s", e.getMessage());
                }
            }

            // -----------------------------------------------------------------
            // Step 3 – Configure TrackMate
            // -----------------------------------------------------------------
            Model model = new Model();
            model.setLogger(Logger.VOID_LOGGER);

            Settings settings = new Settings(imp);
            // Detector configuration
            settings.detectorFactory   = new LogDetectorFactory();
            settings.detectorSettings  = settings.detectorFactory.getDefaultSettings();
            settings.detectorSettings.put("TARGET_CHANNEL",           trackMateConfig.getTargetChannel());
            settings.detectorSettings.put("RADIUS",                   trackMateConfig.getRadius());
            settings.detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", trackMateConfig.isDoSubpixelLocalization());
            settings.detectorSettings.put("THRESHOLD",                threshold);
            settings.detectorSettings.put("DO_MEDIAN_FILTERING",      trackMateConfig.isMedianFiltering());

            // Tracker configuration
            settings.trackerFactory    = new SparseLAPTrackerFactory();
            settings.trackerSettings   = settings.trackerFactory.getDefaultSettings();
            settings.trackerSettings.put("LINKING_MAX_DISTANCE",            trackMateConfig.getLinkingMaxDistance());
            settings.trackerSettings.put("ALTERNATIVE_LINKING_COST_FACTOR", trackMateConfig.getAlternativeLinkingCostFactor());
            settings.trackerSettings.put("ALLOW_GAP_CLOSING",               trackMateConfig.isAllowGapClosing());
            settings.trackerSettings.put("GAP_CLOSING_MAX_DISTANCE",        trackMateConfig.getGapClosingMaxDistance());
            settings.trackerSettings.put("MAX_FRAME_GAP",                   trackMateConfig.getMaxFrameGap());
            settings.trackerSettings.put("ALLOW_TRACK_SPLITTING",           trackMateConfig.isAllowTrackSplitting());
            settings.trackerSettings.put("SPLITTING_MAX_DISTANCE",          trackMateConfig.getSplittingMaxDistance());
            settings.trackerSettings.put("ALLOW_TRACK_MERGING",             trackMateConfig.isAllowTrackMerging());
            settings.trackerSettings.put("MERGING_MAX_DISTANCE",            trackMateConfig.getMergingMaxDistance());

            // Deterministic execution
            Locale.setDefault(Locale.US);
            System.setProperty("user.language", "en");
            System.setProperty("user.country", "US");
            System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");  // Disable JVM-level parallelism (limits common pool threads to 1)
            System.setProperty("trackmate.deterministic", "true");                            // Request deterministic behavior from TrackMate if supportedSo where I

            settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));
            settings.addAllAnalyzers();
            settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS", trackMateConfig.getMinNumberOfSpotsInTrack(), true));

            // -----------------------------------------------------------------
            // Step 4 – Execute TrackMate
            // -----------------------------------------------------------------

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield);
            }

            TrackMate trackmate = new TrackMate(model, settings);
            if (!trackmate.checkInput()) {
                PaintLogger.errorf("TrackMate input check failed: %s", trackmate.getErrorMessage());
                return cancelEarly(imp, impBrightfield);
            }

            PaintLogger.raw("\n                       TrackMate - spot detection:  ");
            try {
                if (!trackmate.execDetection()) {
                    PaintLogger.errorf("TrackMate - execDetection failed: %s", trackmate.getErrorMessage());
                    return cancelEarly(imp, impBrightfield);
                }
            } catch (Exception e) {
                PaintLogger.errorf("Unexpected error during detection: %s", e.getMessage());
                return cancelEarly(imp, impBrightfield);
            }

            int numberOfSpots = model.getSpots().getNSpots(false);
            if (numberOfSpots > trackMateConfig.getMaxNumberOfSpotsInImage()) {
                PaintLogger.warnf("   TrackMate - Too many spots detected (%d). Limit is %d.",
                                  numberOfSpots, trackMateConfig.getMaxNumberOfSpotsInImage());
                return cancelEarly(imp, impBrightfield);
            } else {
                String numberOfSpotsString = " (" + numberOfSpots + " spots detected).";
                PaintLogger.raw(numberOfSpotsString);
            }

            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield);
            }

            PaintLogger.raw("\n                       TrackMate - track detection: ");
            try {
                if (!trackmate.process()) {
                    PaintLogger.errorf("TrackMate process failed: %s", trackmate.getErrorMessage());
                    return cancelEarly(imp, impBrightfield);
                }
            } catch (Exception e) {
                PaintLogger.errorf("Unexpected error during TrackMate process: %s", e.getMessage());
                return cancelEarly(imp, impBrightfield);
            }

            // -----------------------------------------------------------------
            // Step 5 – Visualization
            // -----------------------------------------------------------------
            if (isCancelled(Thread.currentThread(), dialog)) {
                return cancelEarly(imp, impBrightfield);
            }

            final SelectionModel  selectionModel = new SelectionModel(model);
            final DisplaySettings ds             = DisplaySettingsIO.readUserDefault();
            ds.setSpotVisible(false);
            ds.setTrackColorBy(DisplaySettings.TrackMateObject.TRACKS,
                               trackMateConfig.getTrackColoring());
            final HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, ds);
            displayer.render();
            displayer.refresh();

            capture = CaptureOverlayAction.capture(imp, -1, 1, null);
            Path imagePath = experimentPath.resolve("TrackMate Images")
                    .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
            if (capture != null) {
                if (!new FileSaver(capture).saveAsTiff(String.valueOf(imagePath))) {
                    PaintLogger.errorf("Failed to save TIFF to: %s", imagePath);
                }
            }
            IJ.wait(2000);

            // -----------------------------------------------------------------
            // Step 6 – Write tracks CSV
            // -----------------------------------------------------------------
            String tracksName = experimentInfoRecord.getRecordingName() + "-tracks.csv";
            Path tracksPath = experimentPath.resolve(tracksName);
            int totalSpotsInAllTracks = 0;

            try {
                totalSpotsInAllTracks = TrackCsvWriter.writeTracksCsv(
                        trackmate,
                        experimentInfoRecord.getExperimentName(),
                        experimentInfoRecord.getRecordingName(),
                        tracksPath.toFile(),
                        true);
            } catch (IOException e) {
                PaintLogger.errorf("Failed to write tracks to '%s'", tracksPath);
            }

            // -----------------------------------------------------------------
            // Step 7 – Summarize and return results
            // -----------------------------------------------------------------
            int numberOfSpotsTotal     = model.getSpots().getNSpots(true);
            int numberOfTracks         = model.getTrackModel().nTracks(false);
            int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
            int numberOfFrames         = imp.getNFrames();

            Duration duration = Duration.between(start, LocalDateTime.now());

            closeImages(imp, impBrightfield, capture);

            return new TrackMateResults(true,
                                        true,
                                        numberOfSpotsTotal,
                                        numberOfTracks,
                                        numberOfFilteredTracks,
                                        numberOfFrames,
                                        duration,
                                        totalSpotsInAllTracks);

        } catch (Exception e) {
            PaintLogger.errorf("Exception during TrackMate processing: %s", e.getMessage());
            return cancelEarly(imp, impBrightfield);
        } finally {
            closeImages(imp, impBrightfield, capture);
        }
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    /** Checks for thread interruption or user cancellation. */
    private static boolean isCancelled(Thread t, ProjectDialog dialog) {
        return t.isInterrupted() || (dialog != null && dialog.isCancelled());
    }

    /** Safely closes visible images. */
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

    /** Cancels processing early and closes images. */
    private static TrackMateResults cancelEarly(ImagePlus... images) {
        closeImages(images);
        PaintLogger.warnf("   Recording cancelled.");
        return new TrackMateResults(false);
    }
}