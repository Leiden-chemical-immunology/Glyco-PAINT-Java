/*
 * Copyright (c) 2026 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;

import fiji.plugin.trackmate.TrackMate;
import ij.ImagePlus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.compare.ComparisonResult;
import paint.compare.CsvSource;
import paint.compare.TableComparer;
import paint.fiji.tracks.TrackDataExporter;
import paint.shared.config.TrackMateConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;

/**
 * Regression gate for the TrackMate stage of the Fiji plugin.
 *
 * <p>Runs the plugin's own TrackMate step ({@link RunTrackMateOnRecording#track}) headless on a
 * small committed recording and writes the tracks with {@link TrackDataExporter}, as the plugin
 * does. The resulting tracks are compared <b>exactly</b>, keyed on {@code Unique Key}, with the
 * committed reference {@code trackmate-reference/Tracks.csv}, using the same comparison engine
 * as the Generate Squares gate. A change to the TrackMate settings or steps, to the export, or a
 * TrackMate upgrade that changes the tracks makes this test fail.</p>
 *
 * <p>TrackMate links multithreaded by default, and then the tracks vary from run to run. This
 * test therefore always runs with {@code Deterministic Tracking} on (one thread); see
 * {@code doc/Change Note - TrackMate Determinism and Single Detection.md}. Normal runs are not
 * affected.</p>
 *
 * <p>Input: {@code 221012-Exp-1-A1-1-crop.tif}, a crop of recording {@code 221012-Exp-1-A1-1}
 * (x 224, y 224, 64 × 64 pixels, frames 0–499), made with the Bio-Formats importer so that the
 * calibration is the one Fiji uses (0.1603 µm per pixel, 0.0500 s per frame). The threshold is
 * that of the recording in {@code Experiment Info.csv} (5). The TrackMate parameters come from
 * the committed {@code Paint Configuration.json} next to it. The ND2 loading itself is not
 * covered.</p>
 *
 * <p>When a change <em>is</em> intentional, re-run with {@code -Dpaint.updateGolden=true} to
 * overwrite the reference with the new output, then review the git diff and commit it.</p>
 */
@DisplayName("TrackMate stage — regression gate (crop of 221012-Exp-1-A1-1)")
class TrackMateRegressionGateTest {

    private static final String EXPERIMENT = "221012";
    private static final String RECORDING  = "221012-Exp-1-A1-1";
    private static final String IMAGE      = RECORDING + "-crop.tif";
    private static final double THRESHOLD  = 5.0;

    private static final Path    REFERENCE_DIR = Path.of("src", "test", "resources", "trackmate-reference");
    private static final boolean UPDATE_GOLDEN = Boolean.getBoolean("paint.updateGolden");

    /** Closes the log file, so the temporary directory can be deleted (required on Windows). */
    @AfterEach
    void closeLogger() {
        PaintLogger.close();
    }

    @Test
    @DisplayName("tracks of the test recording reproduce the reference exactly (deterministic linking)")
    void tracksReproduceReference(@TempDir Path projectDir) throws Exception {
        // --- The pinned TrackMate parameters, with deterministic linking ---
        Files.copy(REFERENCE_DIR.resolve(PAINT_CONFIGURATION_JSON), projectDir.resolve(PAINT_CONFIGURATION_JSON));
        PaintLogger.initialise(projectDir, "trackmate-regression-gate");
        PaintConfig.reinitialise(projectDir);
        TrackMateConfig config = new TrackMateConfig();
        assertTrue(config.isDeterministicTracking(), "the gate must run with Deterministic Tracking on");

        // --- Run the plugin's TrackMate step on the test recording ---
        Path image = REFERENCE_DIR.resolve(IMAGE);
        assertTrue(Files.isRegularFile(image), "test recording missing: " + image.toAbsolutePath());
        ImagePlus imp = new ImagePlus(image.toString());
        assertEquals(500, imp.getNFrames(), "test recording not read as 500 frames");

        TrackMate trackmate = RunTrackMateOnRecording.track(imp, config, THRESHOLD, null);
        assertNotNull(trackmate, "TrackMate did not complete; see the log");

        Path produced = projectDir.resolve("Tracks.csv");
        TrackDataExporter.writeTracksCsv(trackmate, EXPERIMENT, RECORDING, produced, true);
        assertTrue(Files.isRegularFile(produced), "no tracks file written");

        // --- Accept-an-intentional-change mode: overwrite the reference, don't assert ---
        Path golden = REFERENCE_DIR.resolve("Tracks.csv");
        if (UPDATE_GOLDEN || !Files.isRegularFile(golden)) {
            blessReference(golden, produced);
            return;
        }

        // --- Compare exactly with the reference ---
        List<Map<String, String>> g = CsvSource.read(golden);
        List<Map<String, String>> p = CsvSource.read(produced);
        ComparisonResult result = TableComparer.compare(g, p, row -> row.getOrDefault("Unique Key", ""),
                                                        Set.of(), 0.0);
        System.out.println("==== TrackMate tracks (exact): " + g.size() + " reference, " + p.size() + " produced ====");
        System.out.println(result.reportGrouped(false));

        assertEquals(0L, result.count(ComparisonResult.Difference.Kind.MISSING),
                     "tracks in the reference are missing from the output");
        assertEquals(0L, result.count(ComparisonResult.Difference.Kind.EXTRA),
                     "extra tracks produced that are not in the reference");
        assertEquals(0L, result.count(ComparisonResult.Difference.Kind.DUPLICATE_KEY),
                     "the Unique Key column is not unique — comparison is unreliable");
        assertFalse(result.hasDifferences(), "tracks differ from the reference — see the report above");
    }

    /**
     * Writes the produced tracks as the new reference (with a backup of the old one). Without
     * {@code -Dpaint.updateGolden=true} this only happens when there is no reference yet, and the
     * test then fails, so a missing reference is never silently accepted.
     */
    private static void blessReference(Path golden, Path produced) throws IOException {
        boolean existed = Files.isRegularFile(golden);
        if (existed) {
            String stamp  = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            Path   backup = golden.resolveSibling("Tracks.csv." + stamp + ".bak");
            Files.copy(golden, backup, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("🗄  Previous reference saved: " + backup.toAbsolutePath());
        }
        Files.copy(produced, golden, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✅ Reference updated: " + golden.toAbsolutePath());
        System.out.println("   Review the git diff and commit it to accept this intentional change.");
        if (!UPDATE_GOLDEN) {
            throw new AssertionError("No reference Tracks.csv existed; one has been written from this run. "
                                             + "Review it and run the test again.");
        }
    }
}
