package viewer;

import paint.shared.config.PaintConfig;
import paint.shared.objects.Project;
import paint.shared.objects.Recording;
import paint.shared.utils.PaintLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static paint.shared.io.HelperIO.readAllRecordings;

public class RecordingLoader {

    public static List<RecordingEntry> loadFromProject(Project project) {

        List<RecordingEntry> entries = new ArrayList<>();

        for (String experimentName : project.experimentNames) {

            Path experimentPath = project.getProjectRootPath().resolve(experimentName);

            List<Recording> recordings = readAllRecordings(experimentPath);
            if (recordings == null || recordings.isEmpty()) {
                continue;
            }

            for (Recording recording : recordings) {
                String recordingName = recording.getRecordingName();
                if (!recording.isProcessFlag()) {
                    continue;
                }

                // --- Image paths ---
                Path trackmateImagePath = experimentPath
                        .resolve("TrackMate Images")
                        .resolve(recordingName + ".jpg");

                if (!Files.exists(trackmateImagePath)) {
                    PaintLogger.errorf("Missing TrackMate image for '%s'", recordingName);
                    continue;
                }

                Path brightfieldDirPath = experimentPath.resolve("BrightField Images");
                Path brightfieldImagePath = null;

                if (!Files.isDirectory(brightfieldDirPath)) {
                    PaintLogger.errorf("Missing Brightfield directory '%s' image for recording '%s'", brightfieldDirPath, recordingName);
                    continue;
                }

                try {
                    for (Path p : (Iterable<Path>) Files.list(brightfieldDirPath)::iterator) {
                        String fname = p.getFileName().toString();
                        if ((fname.startsWith(recordingName + "-BF") || fname.startsWith(recordingName))
                                && fname.endsWith(".jpg")) {
                            brightfieldImagePath = p;
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (brightfieldImagePath == null) {
                    PaintLogger.errorf("Missing BrightField image for '%s'", recordingName);
                    continue;
                }

                // --- Thresholds from config ---
                // @formatter:off
                double minDensityRatio = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
                double maxVariability  = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);
                double minRSquared     = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
                String neighbourMode   = PaintConfig.getString("Generate Squares", "Neighbour Mode", "Free");
                // @formatter:on

                // --- Build final entry ---
                RecordingEntry entry = new RecordingEntry(
                        recording,
                        trackmateImagePath,
                        brightfieldImagePath,
                        experimentName,
                        minDensityRatio,
                        maxVariability,
                        minRSquared,
                        neighbourMode
                );

                entries.add(entry);
            }
        }
        return entries;
    }

}