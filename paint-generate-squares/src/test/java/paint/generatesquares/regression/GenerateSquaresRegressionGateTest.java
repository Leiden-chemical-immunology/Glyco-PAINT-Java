package paint.generatesquares.regression;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.generatesquares.app.GenerateSquaresHeadless;
import paint.compare.compare.ComparisonResult;
import paint.compare.compare.CsvSource;
import paint.compare.compare.PaintStrictComparator;
import paint.compare.compare.TableComparer;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.utils.PaintLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintFileNames.PAINT_CONFIGURATION_JSON;
import static paint.shared.constants.PaintStringConstants.BACKGROUND_PLOTS;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.TAU_FITTING_PLOTS;

/**
 * End-to-end regression gate for the Generate Squares pipeline.
 *
 * <p>Runs the real pipeline headless on the committed golden-master inputs
 * ({@code reference-project/221012}) with factory-default configuration, then
 * compares the freshly produced {@code Squares.csv} <b>and</b> {@code Recordings.csv}
 * against the committed golden files using the shared regression comparison engine
 * ({@link TableComparer} / {@link CsvSource} / {@link ComparisonResult}) — the same
 * code the regression tooling uses — in <b>exact</b> mode (zero tolerance, every
 * column). Squares are keyed on {@code Unique Key}, recordings on
 * {@code Recording Name}. Any change that alters the scientific output fails this
 * test, so it catches unintentional changes.</p>
 *
 * <p>Pass {@code -Dpaint.rules=relaxed} to compare with the Paint tolerance rules
 * instead of exactly — handy for checking whether a failing exact run's
 * differences are actually acceptable.</p>
 *
 * <p>When a change <em>is</em> intentional, re-run with
 * {@code -Dpaint.updateGolden=true} to overwrite the golden masters with the new
 * output, then review the git diff and commit it.</p>
 */
@DisplayName("Generate Squares — end-to-end regression gate (reference-project/221012)")
class GenerateSquaresRegressionGateTest {

    private static final String EXPERIMENT = "221012";

    /**
     * Set {@code -Dpaint.updateGolden=true} to overwrite the committed golden files
     * with the freshly produced output — i.e. to accept an intentional change —
     * instead of asserting.
     */
    private static final boolean UPDATE_GOLDEN = Boolean.getBoolean("paint.updateGolden");

    /**
     * Comparison mode. Default is exact (any difference fails). Pass
     * {@code -Dpaint.rules=relaxed} to use the Paint tolerance rules instead
     * (accept small changes, ignore unimportant fields) — useful to check whether
     * a failing exact run's differences are actually acceptable.
     */
    private static final boolean RELAXED = "relaxed".equalsIgnoreCase(System.getProperty("paint.rules"));

    /** Wall-clock run metadata (not scientific output); excluded in exact mode. */
    private static final Set<String> VOLATILE = new HashSet<>(Arrays.asList("Run Time", "Time Stamp"));

    @Test
    @DisplayName("produced Squares.csv and Recordings.csv reproduce the golden master exactly")
    void runAndReport(@TempDir Path projectDir) throws Exception {
        // --- Locate the reference data (kept out of git; the gate is local-only) ---
        java.net.URL ref = getClass().getResource("/reference-project/" + EXPERIMENT);
        Assumptions.assumeTrue(ref != null,
                "reference-project data not present on the classpath — local-only regression gate skipped");
        Path goldenDir = Paths.get(ref.toURI());
        for (String f : new String[]{"Experiment Info.csv", "Recordings.csv", "Tracks.csv"}) {
            Assumptions.assumeTrue(Files.isRegularFile(goldenDir.resolve(f)),
                    "reference input '" + f + "' not present — local-only regression gate skipped");
        }

        // --- Stage inputs into a fresh temp project: projectDir/<experiment>/{inputs} ---
        Path expDir = projectDir.resolve(EXPERIMENT);
        Files.createDirectories(expDir);
        for (String f : new String[]{"Experiment Info.csv", "Recordings.csv", "Tracks.csv"}) {
            Files.copy(goldenDir.resolve(f), expDir.resolve(f));
        }

        // --- Pin the run to the committed configuration that produced the golden master.
        //     Without this the gate would silently inherit whatever DefaultConfigLoader
        //     currently seeds, so a change to a default (e.g. Min Required Density Ratio)
        //     would surface as a bogus "numeric regression" — or, worse, compare output
        //     computed with different parameters than the golden was. ---
        Path goldenConfig = goldenDir.getParent().resolve(PAINT_CONFIGURATION_JSON);
        Assumptions.assumeTrue(Files.isRegularFile(goldenConfig),
                "reference '" + PAINT_CONFIGURATION_JSON + "' not present — local-only regression gate skipped");
        Files.copy(goldenConfig, projectDir.resolve(PAINT_CONFIGURATION_JSON));

        // --- Initialise against the staged project (picks up the pinned config) ---
        PaintLogger.initialise(projectDir, "regression-gate");
        PaintConfig.reinitialise(projectDir);

        // Plot generation writes only PNGs — it cannot affect the compared CSVs, but it is
        // slow. Force it off regardless of what the pinned config happens to say.
        PaintConfig.setBoolean(GENERATE_SQUARES, BACKGROUND_PLOTS, false);
        PaintConfig.setBoolean(GENERATE_SQUARES, TAU_FITTING_PLOTS, false);

        // --- Run the real pipeline (recomputes and overwrites Squares.csv and Recordings.csv) ---
        GenerateSquaresHeadless.run(projectDir, Collections.singletonList(EXPERIMENT));

        Path producedSquares    = expDir.resolve("Squares.csv");
        Path producedRecordings = expDir.resolve("Recordings.csv");
        assertTrue(Files.isRegularFile(producedSquares),    "pipeline did not produce a Squares.csv");
        assertTrue(Files.isRegularFile(producedRecordings), "pipeline did not produce a Recordings.csv");

        // --- Compare both, exactly, with the shared engine ---
        ComparisonResult squares    = compareExact(goldenDir.resolve("Squares.csv"),
                producedSquares,    "Unique Key",     true,  "Squares");
        ComparisonResult recordings = compareExact(goldenDir.resolve("Recordings.csv"),
                producedRecordings, "Recording Name", false, "Recordings");

        // --- Accept-an-intentional-change mode: overwrite the golden masters, don't assert ---
        if (UPDATE_GOLDEN) {
            blessGolden("Squares.csv",    producedSquares);
            blessGolden("Recordings.csv", producedRecordings);
            System.out.println("   Review the git diff and commit it to accept this intentional change.");
            return;
        }

        // --- Regression gate: the pipeline must reproduce both golden masters exactly ---
        assertClean(squares,    "Squares");
        assertClean(recordings, "Recordings");
    }

    /** Reads both files, compares them exactly (keyed on {@code keyColumn}), and prints the report. */
    private static ComparisonResult compareExact(Path golden, Path produced, String keyColumn,
                                                 boolean splitSquare, String label) throws Exception {
        List<Map<String, String>> g = CsvSource.read(golden);
        List<Map<String, String>> p = CsvSource.read(produced);
        Function<Map<String, String>, String> keyFn = row -> row.getOrDefault(keyColumn, "");
        ComparisonResult result = RELAXED
                ? TableComparer.compare(g, p, keyFn, new PaintStrictComparator())
                : TableComparer.compare(g, p, keyFn, VOLATILE, 0.0);
        System.out.println("==== " + label + " (" + (RELAXED ? "relaxed" : "exact") + ") ====");
        System.out.println(result.reportGrouped(splitSquare));
        return result;
    }

    /** Overwrites the committed golden file with freshly produced output, backing up the old one first. */
    private static void blessGolden(String name, Path produced) throws IOException {
        Path srcGolden = Paths.get("src", "test", "resources", "reference-project", EXPERIMENT, name);
        Files.createDirectories(srcGolden.getParent());

        // Save a timestamped copy of the existing golden before overwriting it.
        // The ".bak" suffix keeps these backups out of git (see .gitignore).
        if (Files.isRegularFile(srcGolden)) {
            String stamp  = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            Path   backup = srcGolden.resolveSibling(name + "." + stamp + ".bak");
            Files.copy(srcGolden, backup, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("🗄  Previous golden saved: " + backup.toAbsolutePath());
        }

        Files.copy(produced, srcGolden, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("✅ Golden master updated: " + srcGolden.toAbsolutePath());
    }

    private static void assertClean(ComparisonResult r, String what) {
        assertEquals(0L, r.count(ComparisonResult.Difference.Kind.MISSING),
                what + ": rows present in the golden master are missing from the output");
        assertEquals(0L, r.count(ComparisonResult.Difference.Kind.EXTRA),
                what + ": extra rows produced that are not in the golden master");
        assertEquals(0L, r.count(ComparisonResult.Difference.Kind.DUPLICATE_KEY),
                what + ": the key column is not unique — comparison is unreliable");
        assertFalse(r.hasDifferences(),
                what + ": output differs from the golden master — see the report above");
    }
}
