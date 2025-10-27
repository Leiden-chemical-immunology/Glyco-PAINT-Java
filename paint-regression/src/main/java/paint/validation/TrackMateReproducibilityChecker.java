/******************************************************************************
 *  Class:        TrackMateReproducibilityChecker.java
 *  Package:      paint.validation
 *
 *  PURPOSE:
 *    Verifies reproducibility of the TrackMate pipeline by running it twice
 *    on the same recording and comparing the resulting CSV outputs.
 *
 *  DESCRIPTION:
 *    This helper executes RunTrackMateOnRecording twice using identical inputs
 *    and parameters, stores the two generated CSV files under distinct names,
 *    and compares them byte-for-byte using MD5 checksums.
 *
 *  USAGE:
 *    TrackMateReproducibilityChecker.verify(
 *        experimentPath, imagesPath, trackMateConfig, threshold,
 *        experimentInfoRecord, dialog
 *    );
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  UPDATED:
 *    2025-10-27
 ******************************************************************************/

package paint.validation;

import paint.fiji.trackmate.RunTrackMateOnRecording;
import paint.shared.config.TrackMateConfig;
import paint.shared.dialogs.ProjectDialog;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;

import java.nio.file.*;
import java.security.MessageDigest;

public class TrackMateReproducibilityChecker {

    /**
     * Runs the TrackMate pipeline twice on the same data and checks whether
     * the generated CSV outputs are bit-identical.
     *
     * @param experimentPath Path to the experiment folder.
     * @param imagesPath Path to the folder containing ND2 images.
     * @param config TrackMate configuration used for processing.
     * @param threshold Detection threshold value.
     * @param info Experiment metadata for the current recording.
     * @param dialog Project dialog for progress/cancellation.
     * @throws Exception if file or I/O operations fail.
     */
    public static void verify(Path experimentPath,
                              Path imagesPath,
                              TrackMateConfig config,
                              double threshold,
                              ExperimentInfo info,
                              ProjectDialog dialog) throws Exception {

        PaintLogger.raw("\n🧪 Verifying TrackMate reproducibility...\n");

        Path csv1 = experimentPath.resolve(info.getRecordingName() + "-tracks-run1.csv");
        Path csv2 = experimentPath.resolve(info.getRecordingName() + "-tracks-run2.csv");
        Path original = experimentPath.resolve(info.getRecordingName() + "-tracks.csv");

        // --- Run TrackMate first time ---
        PaintLogger.raw("▶ First run...");
        RunTrackMateOnRecording.runTrackMateOnRecording(experimentPath, imagesPath, config, threshold, info, dialog);
        if (Files.exists(original))
            Files.move(original, csv1, StandardCopyOption.REPLACE_EXISTING);

        // --- Run TrackMate second time ---
        PaintLogger.raw("▶ Second run...");
        RunTrackMateOnRecording.runTrackMateOnRecording(experimentPath, imagesPath, config, threshold, info, dialog);
        if (Files.exists(original))
            Files.move(original, csv2, StandardCopyOption.REPLACE_EXISTING);

        // --- Compute and compare checksums ---
        String md5_1 = md5sum(csv1);
        String md5_2 = md5sum(csv2);

        PaintLogger.raw(String.format("  ➤ First run checksum : %s%n", md5_1));
        PaintLogger.raw(String.format("  ➤ Second run checksum: %s%n", md5_2));

        if (md5_1.equals(md5_2)) {
            PaintLogger.raw("✅ Reproducibility verified — files are identical.\n");
        } else {
            PaintLogger.raw("⚠️  Non-identical results detected — further investigation needed.\n");
        }

        // Optional cleanup to avoid clutter:
        // Files.deleteIfExists(csv1);
        // Files.deleteIfExists(csv2);
    }

    private static String md5sum(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(file));
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        try {
            // Example paths — adjust these to your actual experiment/test setup
            Path experimentPath = Paths.get("/Users/hans/Paint Test Project/221012");
            Path imagesPath     = experimentPath; // or Paths.get("/path/to/images");

            // Example config and metadata
            TrackMateConfig config = new TrackMateConfig();
            double threshold = 5.0; // use your usual detection threshold

            ExperimentInfo info = new ExperimentInfo();
            info.setExperimentName("221012-Exp-1");
            info.setRecordingName("221012-Exp-1-A1-1");

            ProjectDialog dialog = null; // or a real instance if you need UI feedback

            // Run reproducibility check
            verify(experimentPath, imagesPath, config, threshold, info, dialog);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}