package viewer;

import paint.shared.config.PaintConfig;
import paint.shared.constants.PaintConstants;
import paint.shared.objects.Project;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RecordingLoader {

    public static List<RecordingEntry> loadFromProject(Project project) {
        List<RecordingEntry> entries = new ArrayList<>();

        for (String experimentName : project.experimentNames) {
            Path experimentFolder = project.getProjectRootPath().resolve(experimentName);
            Path recordingsFile = experimentFolder.resolve(PaintConstants.RECORDINGS_CSV);

            if (!Files.exists(recordingsFile)) {
                System.err.println("No recordings CSV for " + experimentName);
                continue;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(recordingsFile.toFile()))) {
                String header = br.readLine(); // skip header
                if (header == null) continue;

                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",", -1);

                    String recordingName = parts[0].trim();
                    String probeName = parts[3].trim();
                    String probeType = parts[4].trim();
                    String cellType = parts[5].trim();
                    String adjuvant = parts[6].trim();
                    double concentration = parseDouble(parts[7]);
                    boolean processFlag = Boolean.parseBoolean(parts[8].trim());

                    if (!processFlag) continue;

                    double threshold = parseDouble(parts[9]);
                    int spots = parseInt(parts[10]);
                    int tracks = parseInt(parts[11]);
                    double tau = parseDouble(parts[17]);
                    double rSquared = parseDouble(parts[18]);
                    double density = parseDouble(parts[19]);

                    // --- Paths ---
                    Path trackmateImage = experimentFolder
                            .resolve("TrackMate Images")
                            .resolve(recordingName + ".jpg");

                    if (!Files.exists(trackmateImage)) {
                        System.err.println("Missing TrackMate image for " + recordingName);
                        continue;
                    }

                    Path brightfieldDir = experimentFolder.resolve("BrightField Images");
                    Path brightfieldImage = null;

                    if (Files.isDirectory(brightfieldDir)) {
                        try {
                            for (Path p : (Iterable<Path>) Files.list(brightfieldDir)::iterator) {
                                String fname = p.getFileName().toString();
                                if ((fname.startsWith(recordingName + "-BF") || fname.startsWith(recordingName))
                                        && fname.endsWith(".jpg")) {
                                    brightfieldImage = p;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (brightfieldImage == null) {
                        System.err.println("Missing BrightField image for " + recordingName);
                        continue;
                    }

                    // --- Thresholds ---
                    double minDensityRatio = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
                    double maxVariability = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);
                    double minRSquared = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
                    String neighbourMode = PaintConfig.getString("Generate Squares", "Neighbour Mode", "Free");

                    // --- Construct entry (now includes recordingName first) ---
                    RecordingEntry entry = new RecordingEntry(
                            recordingName,
                            trackmateImage,
                            brightfieldImage,
                            experimentName,
                            probeName,
                            probeType,
                            adjuvant,
                            cellType,
                            concentration,
                            spots,
                            tracks,
                            threshold,
                            tau,
                            density,
                            minDensityRatio,
                            maxVariability,
                            minRSquared,
                            neighbourMode,
                            rSquared
                    );

                    entries.add(entry);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return entries;
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}