package trackMatePaint;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import ij.ImagePlus;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;
import loci.formats.FormatException;

import fiji.plugin.trackmate.*;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.tracking.jaqaman.SparseLAPTrackerFactory;
import fiji.plugin.trackmate.detection.LogDetectorFactory;
import fiji.plugin.trackmate.visualization.hyperstack.HyperStackDisplayer;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;


public class TrackMateLauncher {

    public static void main(String[] args) throws IOException, FormatException {

        // Process the same image 10 times and see how it starts well, but ends slowly
        for (int i=1; i < 10; i++) {
            Instant start = Instant.now();
            runTrackMate();
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            System.out.printf("✅ Time taken to process image %d: %s seconds.%n", i, duration.getSeconds());
        }
    }

    private static void runTrackMate() throws IOException, FormatException {

        ImagePlus[] imps;
        ImagePlus imp;
        Model model;
        DisplaySettings displaySettings;
        Settings settings;
        FeatureFilter spotFilter;
        FeatureFilter trackFilter;
        TrackModel trackModel;
        TrackMate trackmate;
        SelectionModel selectionModel;
        HyperStackDisplayer hyperStackDisplayer;
        ImporterOptions importerOptions;

        // Path to ND2 file
        String path = "/Users/hans/Downloads/221012 Images/221012-Exp-3-A4-2.nd2";

        // Load images using Bio-Formats
        importerOptions = new ImporterOptions();
        importerOptions.setId(path);
        importerOptions.setSplitChannels(false);
        importerOptions.setSplitTimepoints(false);
        importerOptions.setSplitFocalPlanes(false);
        imps = BF.openImagePlus(importerOptions);

        if (imps.length == 0) {
            System.err.println("No image found.");
            return;
        }
        imp = imps[0];
        imp.show();

        // Setup model and settings
        model = new Model();

        // Create DisplaySettings (default settings for your ImagePlus):
        displaySettings = DisplaySettings.defaultStyle();

        settings = new Settings(imp);

        // === DETECTOR ===
        settings.detectorFactory = new LogDetectorFactory();

        Map<String, Object> detectorSettings = new HashMap<>();
        detectorSettings.put("TARGET_CHANNEL", 1);
        detectorSettings.put("RADIUS", 0.5);
        detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", false);
        detectorSettings.put("THRESHOLD", 20.0);
        detectorSettings.put("DO_MEDIAN_FILTERING", false);
        settings.detectorSettings = detectorSettings;

        // === SPOT FILTER ===
        spotFilter = new FeatureFilter(Spot.QUALITY, 0.0, true);
        settings.addSpotFilter(spotFilter);

        // === TRACKER ===
        settings.trackerFactory = new SparseLAPTrackerFactory();

        Map<String, Object> trackerSettings = settings.trackerFactory.getDefaultSettings();
        trackerSettings.put("LINKING_MAX_DISTANCE", 0.6);
        trackerSettings.put("ALTERNATIVE_LINKING_COST_FACTOR", 1.05);

        trackerSettings.put("ALLOW_GAP_CLOSING", true);
        trackerSettings.put("GAP_CLOSING_MAX_DISTANCE", 1.2);
        trackerSettings.put("MAX_FRAME_GAP", 3);

        trackerSettings.put("ALLOW_TRACK_SPLITTING", false);
        trackerSettings.put("SPLITTING_MAX_DISTANCE", 15.0);

        trackerSettings.put("ALLOW_TRACK_MERGING", false);
        trackerSettings.put("MERGING_MAX_DISTANCE", 15.0);

        settings.trackerSettings = trackerSettings;

        // === ANALYZERS ===
        settings.addAllAnalyzers();

        // === TRACK FILTER ===
        trackFilter = new FeatureFilter("NUMBER_SPOTS", 3.0, true);
        settings.addTrackFilter(trackFilter);

        // === INSTANTIATE TrackMate ===
        trackmate = new TrackMate(model, settings);
        model.setLogger(Logger.IJ_LOGGER);

        if (!trackmate.checkInput()) {
            System.err.println("TrackMate error 1: " + trackmate.getErrorMessage());
            return;
        }

        if (!trackmate.execDetection())
        {
            System.err.println("TrackMate error 2: " + trackmate.getErrorMessage());
            return;
        }

        int nrSpots = model.getSpots().getNSpots(false);
        if (nrSpots > 1_000_000) {
            System.err.println("Too many spots");
            return;
        }

        if (!trackmate.process()) {
            System.err.println("TrackMate error 4: " + trackmate.getErrorMessage());
            return;
        }

        trackModel = model.getTrackModel();
        selectionModel = new SelectionModel(model);

        hyperStackDisplayer = new HyperStackDisplayer(model, selectionModel, imp, displaySettings);
        hyperStackDisplayer.render();
        hyperStackDisplayer.refresh();


        // Do the cleanup
        try {
            if (imp != null) {
                imp.close();  // release native image memory
                imp = null;
            }
            if (imps[0] != null) {
                imps[0].close();
                imps[0] = null;
            }

            if (selectionModel != null) {
                selectionModel.clearSelection();
                selectionModel = null;
            }

            // Nullify other references
            trackmate = null;
            model = null;
            trackModel = null;
            importerOptions = null;
            displaySettings = null;
            settings = null;
            spotFilter = null;
            trackFilter = null;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.gc();
        }
    }
}
