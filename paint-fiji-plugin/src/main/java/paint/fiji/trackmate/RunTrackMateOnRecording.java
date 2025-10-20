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
import paint.shared.dialogs.ProjectSpecificationDialog;
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
 * Executes the TrackMate pipeline on a single recording.
 */
public class RunTrackMateOnRecording {

    static final boolean debug = true;

    private static boolean isCancelled(Thread t, ProjectSpecificationDialog dialog) {
        return t.isInterrupted() || (dialog != null && dialog.isCancelled());
    }

    private static TrackMateResults cancelEarly(ImagePlus imp) {
        try {
            if (imp != null) {
                imp.close();
            }
        } catch (Exception ignored) {
            //
        }
        PaintLogger.warningf("   Recording cancelled.");
        return new TrackMateResults(false);
    }

    public static TrackMateResults runTrackMateOnRecording(Path experimentPath,
                                                           Path imagesPath,
                                                           TrackMateConfig trackMateConfig,
                                                           double threshold,
                                                           ExperimentInfo experimentInfoRecord,
                                                           ProjectSpecificationDialog dialog) throws IOException {

        final boolean debugFlag = getBoolean("Debug", "Debug RunTrackMateOnRecording", false);
        LocalDateTime start = LocalDateTime.now();
        DebugTools.setRootLevel("OFF");

        // --- Step 1: Load ND2 image ---
        PaintLogger.raw("                       TrackMate - Image Loading:   ");
        if (isCancelled(Thread.currentThread(), dialog)) {
            return new TrackMateResults(false);
        }

        File nd2File = new File(imagesPath.toFile(), experimentInfoRecord.getRecordingName() + ".nd2");
        if (!nd2File.exists()) {
            if (!isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.errorf("Could not open image file: %s", nd2File.getAbsolutePath());
            }
            return new TrackMateResults(false);
        }

        ImagePlus imp;
        try {
            imp = IJ.openImage(nd2File.getAbsolutePath());
        } catch (Exception e) {
            if (!isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.errorf("Could not load image file: %s", nd2File.getAbsolutePath());
            }
            return new TrackMateResults(false);
        }

        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        if (imp == null) {
            if (!isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.errorf("Unsupported format or file not found: %s", nd2File);
            }
            return new TrackMateResults(false);
        }

        imp.show();
        IJ.wait(100);
        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        IJ.run(imp, "Enhance Contrast", "saturated=0.35");
        IJ.run("Grays");
        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        // --- Step 2: Save Brightfield snapshot ---
        Path jpgPath = experimentPath.resolve("Brightfield Images")
                .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
        if (Files.notExists(jpgPath.getParent())) {
            Files.createDirectories(jpgPath.getParent());
        }
        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        // Find The brightfield file
        ImagePlus impBrightfield;

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
            PaintLogger.warningf("      Could not open brightfield file: %s",
                                 brightFieldPath == null ? "none found" : brightFieldPath.toString());
        }
        else {
            try {
                impBrightfield = IJ.openImage(brightFieldPath.toString());
                IJ.run(impBrightfield, "Enhance Contrast", "saturated=0.35");
            } catch (Exception e) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Could not open brightfield file: %s", brightFieldPath.toString());
                }
                impBrightfield = null;
            }

            try {
                if (impBrightfield != null) {
                    IJ.saveAs(impBrightfield, "Jpeg", jpgPath.toString());
                }
            } catch (Exception e) {
                if (!isCancelled(Thread.currentThread(), dialog))
                    PaintLogger.errorf("Failed to save Brightfield image: %s", jpgPath);
            }
        }

        // --- Step 3: Prepare TrackMate model and settings ---
        Model model = new Model();
        model.setLogger(Logger.VOID_LOGGER);

        Settings settings = new Settings(imp);
        settings.detectorFactory = new LogDetectorFactory();
        settings.detectorSettings = settings.detectorFactory.getDefaultSettings();
        settings.detectorSettings.put("TARGET_CHANNEL", trackMateConfig.getTargetChannel());
        settings.detectorSettings.put("RADIUS", trackMateConfig.getRadius());
        settings.detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", trackMateConfig.isDoSubpixelLocalization());
        settings.detectorSettings.put("THRESHOLD", threshold);
        settings.detectorSettings.put("DO_MEDIAN_FILTERING", trackMateConfig.isMedianFiltering());

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

        settings.addSpotFilter(new FeatureFilter("QUALITY", 0, true));
        settings.addAllAnalyzers();
        settings.addTrackFilter(new FeatureFilter("NUMBER_SPOTS", trackMateConfig.getMinNrSpotsInTrack(), true));

        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        // --- Step 4: Run TrackMate pipeline ---
        TrackMate trackmate = new TrackMate(model, settings);
        if (!trackmate.checkInput()) {
            PaintLogger.errorf("TrackMate input check failed: %s", trackmate.getErrorMessage());
            return cancelEarly(imp);
        }

        PaintLogger.raw("\n                       TrackMate - spot detection:  ");
        try {
            if (!trackmate.execDetection()) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("TrackMate - execDetection failed: %s", trackmate.getErrorMessage());
                }
                return cancelEarly(imp);
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException || isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.warningf("TrackMate detection interrupted by user cancel.");
            } else {
                PaintLogger.errorf("Unexpected error during detection: %s", e.getMessage());
            }
            return cancelEarly(imp);
        }

        int numberOfSpots = model.getSpots().getNSpots(false);
        if (numberOfSpots > trackMateConfig.getMaxNrSpotsInImage()) {
            if (!isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.warningf("   Trackmate - Too many spots detected (%d). Limit is %d.",
                                     numberOfSpots, trackMateConfig.getMaxNrSpotsInImage());
            }
            return cancelEarly(imp);
        }
        else {
            String numberOfSpotsString = " (" +  numberOfSpots + " spots detected).";
            PaintLogger.raw(numberOfSpotsString);
        }

        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        PaintLogger.raw("\n                       TrackMate - track detection: ");
        try {
            if (!trackmate.process()) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("TrackMate process failed: %s", trackmate.getErrorMessage());
                }
                return cancelEarly(imp);
            }
        } catch (Exception e) {
            // Detect both direct and wrapped interrupts
            if (e instanceof InterruptedException ||
                    e.getCause() instanceof InterruptedException ||
                    isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.warningf("TrackMate processing interrupted by user cancel.");
                Thread.currentThread().interrupt(); // restore flag
            } else {
                PaintLogger.errorf("Unexpected error during TrackMate process: %s", e.getMessage());
            }
            return cancelEarly(imp);
        }

        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
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
            return cancelEarly(imp);
        }

        final ImagePlus capture = CaptureOverlayAction.capture(imp, -1, 1, null);
        Path imagePath = experimentPath.resolve("TrackMate Images")
                .resolve(experimentInfoRecord.getRecordingName() + ".jpg");
        if (capture != null) {
            if (!new FileSaver(capture).saveAsTiff(String.valueOf(imagePath))) {
                if (!isCancelled(Thread.currentThread(), dialog)) {
                    PaintLogger.errorf("Failed to save TIFF to: %s", imagePath);
                }
            }
        }

        if (isCancelled(Thread.currentThread(), dialog)) return cancelEarly(imp);

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
            if (!isCancelled(Thread.currentThread(), dialog)) {
                PaintLogger.errorf("Failed to write tracks to '%s'", tracksPath);
            }
        }

        if (isCancelled(Thread.currentThread(), dialog)) {
            return cancelEarly(imp);
        }

        // --- Step 7: Summarize results ---

        // @formatter:off
        int numberOfSpotsTotal     = model.getSpots().getNSpots(true);
        int numberOfTracks         = model.getTrackModel().nTracks(false);
        int numberOfFilteredTracks = model.getTrackModel().nTracks(true);
        int numberOfFrames         = imp.getNFrames();
        // @formatter:on

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        try {
            ImageWindow win = imp.getWindow();
            if (win != null) {
                imp.close();
            }
        } catch (Exception e) {
            PaintLogger.warningf("Error while closing image: %s", e.getMessage());
        }

        Duration duration = Duration.between(start, LocalDateTime.now());

        return new TrackMateResults(true, true, numberOfSpotsTotal, numberOfTracks,
                                    numberOfFilteredTracks, numberOfFrames, duration, numberOfSpotsInALlTracks);
    }
}