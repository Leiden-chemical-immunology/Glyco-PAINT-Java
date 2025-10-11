package paint.fiji.trackmate.features;

/* IGNORE START
import fiji.plugin.trackmate.Dimension;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.features.track.TrackAnalyzer;
import paint.fiji.tracks.TrackAttributeCalculations;
import paint.fiji.tracks.TrackAttributes;

import javax.swing.ImageIcon;
import java.util.*;

import static paint.shared.constants.PaintConstants.TIME_INTERVAL;

public class PaintTrackFeatureAnalyzer implements TrackAnalyzer {

    public static final String KEY = "PAINT_TRACK_FEATURES";

    public static final String DIFFUSION_COEFF     = "PAINT_DIFFUSION_COEFF";
    public static final String DIFFUSION_COEFF_EXT = "PAINT_DIFFUSION_COEFF_EXT";
    public static final String TOTAL_DISTANCE      = "PAINT_TOTAL_DISTANCE";
    public static final String CONFINEMENT_RATIO   = "PAINT_CONFINEMENT_RATIO";

    private static final List<String> FEATURES = Arrays.asList(
            DIFFUSION_COEFF,
            DIFFUSION_COEFF_EXT,
            TOTAL_DISTANCE,
            CONFINEMENT_RATIO
    );

    private static final Map<String, String> FEATURE_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> FEATURE_SHORT_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> FEATURE_UNITS = new LinkedHashMap<>();
    private static final Map<String, Boolean> IS_INT = new LinkedHashMap<>();
    private static final Map<String, Dimension> FEATURE_DIMENSIONS = new LinkedHashMap<>();

    static {
        FEATURE_NAMES.put(DIFFUSION_COEFF, "Diffusion Coefficient");
        FEATURE_NAMES.put(DIFFUSION_COEFF_EXT, "Extended Diffusion Coefficient");
        FEATURE_NAMES.put(TOTAL_DISTANCE, "Total Distance");
        FEATURE_NAMES.put(CONFINEMENT_RATIO, "Confinement Ratio");

        FEATURE_SHORT_NAMES.put(DIFFUSION_COEFF, "D");
        FEATURE_SHORT_NAMES.put(DIFFUSION_COEFF_EXT, "Dext");
        FEATURE_SHORT_NAMES.put(TOTAL_DISTANCE, "Dist");
        FEATURE_SHORT_NAMES.put(CONFINEMENT_RATIO, "ConfR");

        FEATURE_UNITS.put(DIFFUSION_COEFF, "µm²/s");
        FEATURE_UNITS.put(DIFFUSION_COEFF_EXT, "µm²/s");
        FEATURE_UNITS.put(TOTAL_DISTANCE, "µm");
        FEATURE_UNITS.put(CONFINEMENT_RATIO, "ratio");

        FEATURE_DIMENSIONS.put(DIFFUSION_COEFF, Dimension.NONE);
        FEATURE_DIMENSIONS.put(DIFFUSION_COEFF_EXT, Dimension.NONE);
        FEATURE_DIMENSIONS.put(TOTAL_DISTANCE, Dimension.LENGTH);
        FEATURE_DIMENSIONS.put(CONFINEMENT_RATIO, Dimension.NONE);

        for (String f : FEATURES)
            IS_INT.put(f, false);
    }

    // -----------------------------------------------------------------
    // Implementation
    // -----------------------------------------------------------------

    @Override
    public void process(Collection<Integer> trackIDs, Model model) {
        for (Integer trackId : trackIDs) {
            TrackAttributes ca = TrackAttributeCalculations.calculateTrackAttributes(
                    model.getTrackModel(), trackId, TIME_INTERVAL);

            model.getFeatureModel().putTrackFeature(trackId, DIFFUSION_COEFF, ca.diffusionCoeff);
            model.getFeatureModel().putTrackFeature(trackId, DIFFUSION_COEFF_EXT, ca.diffusionCoeffExt);
            model.getFeatureModel().putTrackFeature(trackId, TOTAL_DISTANCE, ca.totalDistance);
            model.getFeatureModel().putTrackFeature(trackId, CONFINEMENT_RATIO, ca.confinementRatio);
        }
    }

    @Override
    public boolean isLocal() {
        return true;
    }

    @Override
    public Collection<String> getFeatures() {
        return FEATURES;
    }

    @Override
    public Map<String, String> getFeatureNames() {
        return FEATURE_NAMES;
    }

    @Override
    public Map<String, String> getFeatureShortNames() {
        return FEATURE_SHORT_NAMES;
    }

    @Override
    public Map<String, String> getFeatureUnits() {
        return FEATURE_UNITS;
    }

    @Override
    public Map<String, Boolean> getIsIntFeature() {
        return IS_INT;
    }

    @Override
    public Map<String, Dimension> getFeatureDimensions() {
        return FEATURE_DIMENSIONS;
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public void setNumThreads() {
        // Not multithreaded
    }

    @Override
    public void setLogger(Logger logger) {
        // Optional: not used
    }

    @Override
    public long getProcessingTime() {
        return 0;
    }

    @Override
    public boolean isManualFeature() {
        return false;
    }

    @Override
    public String getInfoText() {
        return "Adds Paint-specific motion features (diffusion, distance, confinement ratio).";
    }

    @Override
    public String getName() {
        return "Paint Track Feature Analyzer";
    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}

IGNORE END*/