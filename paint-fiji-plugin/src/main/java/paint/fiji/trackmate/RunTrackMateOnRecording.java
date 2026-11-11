/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
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
import paint.fiji.tracks.TrackDataExporter;
import paint.shared.config.TrackMateConfig;
import paint.ui.dialogs.ProjectDialog;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Provides functionality to run TrackMate analysis for a single microscopy
 * recording. Handles setup, detection, tracking, visualization, and export.
 * <p>
 * This class is typically invoked by {@link RunTrackMateOnExperiment} as part
 * of batch processing within a Paint project.
 * </p>
 */
public class RunTrackMateOnRecording {

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
     */
    public static TrackMateResults runTrackMateOnRecording(Path experimentPath,
            Path            imagesPath,
            TrackMateConfig trackMateConfig,
            double          threshold,
            ExperimentInfo  experimentInfoRecord,
            ProjectDialog   dialog) {

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

            // Loading times are logged, to see where the time goes (e.g. an external drive)
            long readStart = System.nanoTime();
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

            long displayStart = System.nanoTime();
            imp.show();
            IJ.wait(100);

            IJ.run(imp, "Enhance Contrast", "saturated=0.35");
            IJ.run("Grays");
            long brightfieldStart = System.nanoTime();

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

            Path   brightFieldPath = null;
            String baseName        = experimentInfoRecord.getRecordingName();

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
            long loadEnd = System.nanoTime();
            PaintLogger.raw(String.format(Locale.US, " (read %.1f s, display %.1f s, brightfield %.1f s) ",
                                          (displayStart - readStart) / 1e9,
                                          (brightfieldStart - displayStart) / 1e9,
                                          (loadEnd - brightfieldStart) / 1e9));

            // -----------------------------------------------------------------
            // Steps 3 and 4 – Configure and execute TrackMate (see track())
            // -----------------------------------------------------------------
            TrackMate trackmate = track(imp, trackMateConfig, threshold, dialog);
            if (trackmate == null) {
                return cancelEarly(imp, impBrightfield);
            }
            Model model                  = trackmate.getModel();
            int   numberOfFilteredTracks = model.getTrackModel().nTracks(true);

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
            final HyperStackDisplayer displayer = new HyperStackDisplayer(model,
                                                                          selectionModel,
                                                                          imp,
                                                                          ds);
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
            Path   tracksPath = experimentPath.resolve(tracksName);
            int    totalSpotsInAllTracks = 0;

            try {
                totalSpotsInAllTracks = TrackDataExporter.writeTracksCsv(
                        trackmate,
                        experimentInfoRecord.getExperimentName(),
                        experimentInfoRecord.getRecordingName(),
                        tracksPath,
                        true);
            } catch (Exception e) {
                PaintLogger.errorf("Failed to write tracks to '%s'", tracksPath);
            }

            // -----------------------------------------------------------------
            // Step 7 – Summarize and return results
            // -----------------------------------------------------------------
            int numberOfSpotsTotal     = model.getSpots().getNSpots(true);
            int numberOfTracks         = model.getTrackModel().nTracks(false);
            // int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
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
            // Any unexpected top-level exception: honor cancellation if present
            if (e instanceof InterruptedException
                    || Thread.currentThread().isInterrupted()
                    || isCancelled(Thread.currentThread(), dialog)) {
                Thread.currentThread().interrupt();
                PaintLogger.warnf("TrackMate recording interrupted (top-level cancellation).");
                return cancelEarly(imp, impBrightfield);
            }

            PaintLogger.errorf("Exception during TrackMate processing: %s", e.getMessage());
            return cancelEarly(imp, impBrightfield);
        } finally {
            closeImages(imp, impBrightfield, capture);
        }
    }

    // -------------------------------------------------------------------------
    // Utility methods
    // -------------------------------------------------------------------------

    /**
     * Configures TrackMate for the recording and runs it: spot detection, the check against
     * {@code Max Number of Spots in Image}, linking and track filtering, and (if configured) the
     * exclusion of edge tracks. Progress is logged as before.
     * <p>
     * This part needs no display, so it can run headless; the regression test for the TrackMate
     * stage uses it. Everything that needs the screen (showing the image, the overlay, the
     * captured images) stays in {@link #runTrackMateOnRecording}.
     *
     * @param imp             the recording
     * @param trackMateConfig the TrackMate parameters
     * @param threshold       the spot detection threshold
     * @param dialog          the dialog that can cancel the run, or {@code null}
     * @return the TrackMate instance after tracking, or {@code null} if the run was cancelled or
     *         failed (the reason has been logged)
     */
    static TrackMate track(ImagePlus       imp,
                           TrackMateConfig trackMateConfig,
                           double          threshold,
                           ProjectDialog   dialog) {

        // -----------------------------------------------------------------
        // Step 3 – Configure TrackMate
        // -----------------------------------------------------------------
        Model model = new Model();
        model.setLogger(Logger.VOID_LOGGER);

        Settings settings = new Settings(imp);
        // Detector configuration
        settings.detectorFactory  = new LogDetectorFactory();
        settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
        settings.detectorSettings.put("TARGET_CHANNEL",           trackMateConfig.getTargetChannel());
        settings.detectorSettings.put("RADIUS",                   trackMateConfig.getRadius());
        settings.detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", trackMateConfig.isDoSubpixelLocalization());
        settings.detectorSettings.put("THRESHOLD",                threshold);
        settings.detectorSettings.put("DO_MEDIAN_FILTERING",      trackMateConfig.isMedianFiltering());

        // Tracker configuration
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

        // Locale for consistent number formatting
        Locale.setDefault(Locale.US);
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");
        // These two had no effect on TrackMate's linking (tested 2026-11-05, see
        // doc/Change Note - TrackMate Determinism and Single Detection.md).
        // Reproducible linking is controlled by the 'Deterministic Tracking' setting.
        // System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1");
        // System.setProperty("trackmate.deterministic", "true");

        settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));
        settings.addAllAnalyzers();
        settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS",
                                                  trackMateConfig.getMinNumberOfSpotsInTrack(),
                                                  true));

        // -----------------------------------------------------------------
        // Step 4 – Execute TrackMate
        // -----------------------------------------------------------------

        if (isCancelled(Thread.currentThread(), dialog)) {
            return null;
        }

        TrackMate trackmate = new TrackMate(model, settings);
        // Multithreaded linking is fast but not reproducible; a single thread is
        // reproducible but slow. See TrackMateConfig.isDeterministicTracking().
        if (trackMateConfig.isDeterministicTracking()) {
            trackmate.setNumThreads(1);
        }
        if (!trackmate.checkInput()) {
            if (!isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.errorf("TrackMate input check failed: %s", trackmate.getErrorMessage());
            }
            return null;
        }

        // ---- Spot detection ----
        PaintLogger.raw("\n                       TrackMate - spot detection:  ");
        try {
            if (!trackmate.execDetection()) {
                // If we reach here with an interrupted thread or external cancel,
                // treat it as cancellation, not as a hard error.
                if (Thread.currentThread().isInterrupted() || isCancelled(Thread.currentThread(), dialog)) {
                    Thread.currentThread().interrupt();
                    PaintLogger.warnf("TrackMate spot detection interrupted (cancellation).");
                    return null;
                }

                PaintLogger.errorf("TrackMate - execDetection failed: %s",
                                   trackmate.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            // Special handling: InterruptedException often bubbles out of TrackMate
            // when the watchdog/worker interrupts the thread.
            if (e instanceof InterruptedException
                    || Thread.currentThread().isInterrupted()
                    || isCancelled(Thread.currentThread(), dialog)) {
                Thread.currentThread().interrupt();
                PaintLogger.warnf("TrackMate spot detection interrupted (likely cancellation).");
                return null;
            }

            PaintLogger.errorf("Unexpected error during detection: %s", e.getMessage());
            return null;
        }

        int numberOfSpots = model.getSpots().getNSpots(false);
        if (numberOfSpots > trackMateConfig.getMaxNumberOfSpotsInImage()) {
            PaintLogger.warnf("   TrackMate - Too many spots detected (%d). Limit is %d.",
                              numberOfSpots, trackMateConfig.getMaxNumberOfSpotsInImage());
            return null;
        } else {
            String numberOfSpotsString = " (" + numberOfSpots + " spots detected) ";
            PaintLogger.raw(numberOfSpotsString);
        }

        if (isCancelled(Thread.currentThread(), dialog)) {
            return null;
        }

        // ---- Track detection ----
        PaintLogger.raw("\n                       TrackMate - track detection: ");
        try {
            if (!processAfterDetection(trackmate)) {

                if (Thread.currentThread().isInterrupted() || isCancelled(Thread.currentThread(), dialog)) {
                    Thread.currentThread().interrupt();
                    PaintLogger.warnf("TrackMate track detection interrupted (cancellation).");
                    return null;
                }

                PaintLogger.errorf("TrackMate process failed: %s", trackmate.getErrorMessage());
                return null;
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException
                    || Thread.currentThread().isInterrupted()
                    || isCancelled(Thread.currentThread(), dialog)) {
                Thread.currentThread().interrupt();
                PaintLogger.warnf("TrackMate track detection interrupted (likely cancellation).");
                return null;
            }

            PaintLogger.errorf("Unexpected error during TrackMate process: %s", e.getMessage());
            return null;
        }

        // Edge tracks: present in the first or last analysed frame, so their real duration
        // is unknown. Hidden tracks are skipped by the overlay, the count and the tracks CSV.
        String edgeTracksString;
        if (trackMateConfig.isExcludeEdgeTracks()) {
            int numberOfEdgeTracks = excludeEdgeTracks(model, settings.tstart, settings.tend);
            edgeTracksString = numberOfEdgeTracks + " edge tracks dismissed";
        } else {
            edgeTracksString = "edge tracks kept";
        }

        int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
        String linkingMode          = trackMateConfig.isDeterministicTracking() ? "deterministic" : "multithreaded";
        String numberOfTracksString = " (" + numberOfFilteredTracks + " tracks detected, "
                + edgeTracksString + ", " + linkingMode + ") ";
        PaintLogger.raw(numberOfTracksString);

        return trackmate;
    }

    /**
     * Runs the TrackMate steps that follow spot detection.
     * <p>
     * This is {@link TrackMate#process()} without its first step, {@code execDetection()}.
     * Detection is run separately beforehand, so that the spot count can be checked against
     * {@code Max Number of Spots in Image} before the expensive tracking starts. Calling
     * {@code process()} would run detection a second time.
     * <p>
     * Same steps, arguments and return behaviour as {@code process()} in TrackMate 7.14.0:
     * {@code false} as soon as a step fails, {@code true} when cancelled between steps.
     * Verified to give the same results as {@code process()}; see
     * {@code doc/Change Note - TrackMate Determinism and Single Detection.md}.
     * Check this list again when TrackMate is upgraded.
     *
     * @param trackmate the TrackMate instance on which {@code execDetection()} has succeeded
     * @return {@code false} if a step failed, {@code true} otherwise
     */
    private static boolean processAfterDetection(TrackMate trackmate) {
        List<BooleanSupplier> steps = Arrays.asList(
                trackmate::execInitialSpotFiltering,
                () -> trackmate.computeSpotFeatures(true),
                () -> trackmate.execSpotFiltering(true),
                trackmate::execTracking,
                () -> trackmate.computeEdgeFeatures(true),
                () -> trackmate.computeTrackFeatures(true),
                () -> trackmate.execTrackFiltering(true)
        );
        for (BooleanSupplier step : steps) {
            if (!step.getAsBoolean()) {
                return false;
            }
            if (trackmate.isCanceled()) {
                return true;
            }
        }
        return true;
    }

    /**
     * Hides the tracks that are present in the first or the last analysed frame.
     * <p>
     * The real duration of such a track is unknown: it may have started before the recording
     * began, or continued after it ended. Only tracks that are visible, i.e. that passed
     * TrackMate's track filters, are considered. Hidden tracks are skipped by the overlay,
     * by {@code nTracks(true)} and by the tracks CSV ({@code visibleOnly = true}).
     *
     * @param model      the TrackMate model after tracking
     * @param firstFrame the first analysed frame, 0-based ({@code settings.tstart})
     * @param lastFrame  the last analysed frame, 0-based ({@code settings.tend})
     * @return the number of tracks hidden
     */
    static int excludeEdgeTracks(Model model, int firstFrame, int lastFrame) {
        TrackModel    trackModel = model.getTrackModel();
        List<Integer> edgeTracks = new ArrayList<>();

        for (Integer trackId : trackModel.trackIDs(true)) {
            int start = Integer.MAX_VALUE;
            int end   = Integer.MIN_VALUE;
            for (Spot spot : trackModel.trackSpots(trackId)) {
                int frame = (int) Math.round(spot.getFeature(Spot.FRAME));
                start = Math.min(start, frame);
                end   = Math.max(end, frame);
            }
            if (start <= firstFrame || end >= lastFrame) {
                edgeTracks.add(trackId);
            }
        }

        model.beginUpdate();
        try {
            for (Integer trackId : edgeTracks) {
                model.setTrackVisibility(trackId, false);
            }
        } finally {
            model.endUpdate();
        }
        return edgeTracks.size();
    }

    /**
     * Checks for thread interruption or user cancellation.
     */
    private static boolean isCancelled(Thread t, ProjectDialog dialog) {
        return t.isInterrupted() || (dialog != null && dialog.isCancelled());
    }

    /**
     * Safely closes visible images.
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
     * Cancels processing early and closes images.
     */
    private static TrackMateResults cancelEarly(ImagePlus... images) {
        closeImages(images);
        PaintLogger.warnf("   Recording cancelled.");
        return new TrackMateResults(false);
    }
}