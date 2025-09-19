package trackMateTest;

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TrackMateLauncher {

    public static void runTrackMateOnPath(String path) throws IOException, FormatException {

        // Load image using Bio-Formats
        ImporterOptions options = new ImporterOptions();
        options.setId(path);
        options.setSplitChannels(false);
        options.setSplitTimepoints(false);
        options.setSplitFocalPlanes(false);

        ImagePlus[] imps = BF.openImagePlus(options);
        if (imps.length == 0) {
            System.err.println("No image found.");
            return;
        }
        ImagePlus imp = imps[0];
        // imp.show(); // optional: show the image in a window

        // Setup model and settings
        Model model = new Model();
        DisplaySettings displaySettings = DisplaySettings.defaultStyle();
        Settings settings = new Settings(imp);

        // Detector
        settings.detectorFactory = new LogDetectorFactory();
        Map<String, Object> detectorSettings = new HashMap<>();
        detectorSettings.put("TARGET_CHANNEL", 1);
        detectorSettings.put("RADIUS", 0.5);
        detectorSettings.put("DO_SUBPIXEL_LOCALIZATION", false);
        detectorSettings.put("THRESHOLD", 20.0);
        detectorSettings.put("DO_MEDIAN_FILTERING", false);
        settings.detectorSettings = detectorSettings;

        // Spot Filter
        FeatureFilter spotFilter = new FeatureFilter(Spot.QUALITY, 0.0, true);
        settings.addSpotFilter(spotFilter);

        // Tracker
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

        // Analyzers and filters
        settings.addAllAnalyzers();
        FeatureFilter trackFilter = new FeatureFilter("NUMBER_SPOTS", 3.0, true);
        settings.addTrackFilter(trackFilter);

        // Instantiate TrackMate
        TrackMate trackmate = new TrackMate(model, settings);
        model.setLogger(Logger.IJ_LOGGER);

        if (!trackmate.checkInput()) {
            System.err.println("TrackMate error: " + trackmate.getErrorMessage());
            return;
        }

        if (!trackmate.execDetection()) {
            System.err.println("TrackMate detection error: " + trackmate.getErrorMessage());
            return;
        }

        int nrSpots = model.getSpots().getNSpots(false);
        if (nrSpots > 1_000_000) {
            System.err.println("Too many spots detected.");
            return;
        }

        if (!trackmate.process()) {
            System.err.println("TrackMate process error: " + trackmate.getErrorMessage());
            return;
        }

        // Optional: render
        SelectionModel selectionModel = new SelectionModel(model);
        HyperStackDisplayer displayer = new HyperStackDisplayer(model, selectionModel, imp, displaySettings);
        displayer.render();
        displayer.refresh();

        // Cleanup
        imp.close();
    }
}