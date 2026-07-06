package paint.generatesquares.regression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.generatesquares.app.GenerateSquaresHeadless;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.io.internal.SquaresTableIO;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import tech.tablesaw.api.ColumnType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.shared.constants.PaintStringConstants.BACKGROUND_PLOTS;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.TAU_FITTING_PLOTS;

/**
 * End-to-end regression gate for the Generate Squares pipeline.
 *
 * <p>Runs the real pipeline on the committed golden-master inputs
 * ({@code reference-project/221012}) with factory-default configuration, then
 * asserts that the freshly produced {@code Squares.csv} reproduces the committed
 * golden {@code Squares.csv} exactly (every square, every field, within a
 * 3-decimal tolerance). Any change that alters the scientific output fails this
 * test.</p>
 *
 * <p>A per-field difference report is printed on every run, so a failure shows
 * precisely which columns diverged, on how many squares, and by how much.</p>
 *
 * <p>(Filename retains "Spike" for historical reasons; it is now an asserting
 * gate.)</p>
 */
@DisplayName("Generate Squares — end-to-end regression gate (reference-project/221012)")
class GenerateSquaresRegressionGateTest {

    private static final String EXPERIMENT = "221012";
    private static final double TOL = 1e-3; // both files are written at 3-decimal precision

    /** A numeric square attribute to compare, with a human-readable name. */
    private static final class Field {
        final String name;
        final ToDoubleFunction<Square> get;
        Field(String name, ToDoubleFunction<Square> get) { this.name = name; this.get = get; }
    }

    private static List<Field> numericFields() {
        List<Field> f = new ArrayList<>();
        f.add(new Field("X0", Square::getX0));
        f.add(new Field("Y0", Square::getY0));
        f.add(new Field("X1", Square::getX1));
        f.add(new Field("Y1", Square::getY1));
        f.add(new Field("Variability", Square::getVariability));
        f.add(new Field("Density", Square::getDensity));
        f.add(new Field("Density Ratio", Square::getDensityRatio));
        f.add(new Field("Density Ratio Ori", Square::getDensityRatioOri));
        f.add(new Field("Tau", Square::getTau));
        f.add(new Field("R Squared", Square::getRSquared));
        f.add(new Field("Median Diffusion Coefficient", Square::getMedianDiffusionCoefficient));
        f.add(new Field("Median Displacement", Square::getMedianDisplacement));
        f.add(new Field("Max Displacement", Square::getMaxDisplacement));
        f.add(new Field("Total Displacement", Square::getTotalDisplacement));
        f.add(new Field("Median Max Speed", Square::getMedianMaxSpeed));
        f.add(new Field("Max Max Speed", Square::getMaxMaxSpeed));
        f.add(new Field("Median Median Speed", Square::getMedianMedianSpeed));
        f.add(new Field("Max Median Speed", Square::getMaxMedianSpeed));
        f.add(new Field("Max Track Duration", Square::getMaxTrackDuration));
        f.add(new Field("Total Track Duration", Square::getTotalTrackDuration));
        f.add(new Field("Median Track Duration", Square::getMedianTrackDuration));
        return f;
    }

    private static String[] headers() {
        Square.Column[] c = Square.Column.values();
        String[] h = new String[c.length];
        for (int i = 0; i < c.length; i++) h[i] = c[i].header;
        return h;
    }

    private static ColumnType[] types() {
        Square.Column[] c = Square.Column.values();
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) t[i] = c[i].type;
        return t;
    }

    private static boolean eq(double a, double b) {
        return (Double.isNaN(a) && Double.isNaN(b)) || Math.abs(a - b) <= TOL;
    }

    private List<Square> load(Path csv) throws Exception {
        SquaresTableIO io = new SquaresTableIO();
        return io.toEntities(io.readCsvWithSchema(csv, headers(), types(), false));
    }

    private static Map<String, Square> byKey(List<Square> squares) {
        Map<String, Square> m = new LinkedHashMap<>();
        for (Square s : squares) m.put(s.getUniqueKey(), s);
        return m;
    }

    @Test
    @DisplayName("produced Squares.csv reproduces the golden master exactly")
    void runAndReport(@TempDir Path projectDir) throws Exception {
        // --- Locate committed golden data on the test classpath ---
        Path goldenDir = Paths.get(getClass().getResource("/reference-project/" + EXPERIMENT).toURI());
        Path goldenSquares = goldenDir.resolve("Squares.csv");

        // --- Stage inputs into a fresh temp project: projectDir/<experiment>/{inputs} ---
        Path expDir = projectDir.resolve(EXPERIMENT);
        Files.createDirectories(expDir);
        for (String f : new String[]{"Experiment Info.csv", "Recordings.csv", "Tracks.csv"}) {
            Files.copy(goldenDir.resolve(f), expDir.resolve(f));
        }

        // --- Force factory-default configuration (isolated to the temp project) ---
        PaintLogger.initialise(projectDir, "regression-spike");
        PaintConfig.reinitialise(projectDir);
        PaintConfig.setBoolean(GENERATE_SQUARES, BACKGROUND_PLOTS, false);
        PaintConfig.setBoolean(GENERATE_SQUARES, TAU_FITTING_PLOTS, false);

        // --- Run the real pipeline ---
        GenerateSquaresHeadless.run(projectDir, Collections.singletonList(EXPERIMENT));

        Path producedSquares = expDir.resolve("Squares.csv");
        assertTrue(Files.isRegularFile(producedSquares),
                "pipeline did not produce a Squares.csv");

        // --- Load both sides through the tested production IO ---
        Map<String, Square> golden = byKey(load(goldenSquares));
        Map<String, Square> produced = byKey(load(producedSquares));

        // --- Compare ---
        List<Field> fields = numericFields();
        Map<String, Integer> fieldDiffCount = new LinkedHashMap<>();
        Map<String, Double> fieldMaxDiff = new LinkedHashMap<>();
        for (Field f : fields) { fieldDiffCount.put(f.name, 0); fieldMaxDiff.put(f.name, 0.0); }

        int missing = 0, extra = 0, identical = 0, differing = 0, visibleMismatch = 0, tracksMismatch = 0;

        for (Map.Entry<String, Square> e : golden.entrySet()) {
            Square g = e.getValue();
            Square p = produced.get(e.getKey());
            if (p == null) { missing++; continue; }

            boolean anyDiff = false;
            if (g.isVisible() != p.isVisible()) { visibleMismatch++; anyDiff = true; }
            if (g.getNumberOfTracks() != p.getNumberOfTracks()) { tracksMismatch++; anyDiff = true; }

            for (Field f : fields) {
                double gv = f.get.applyAsDouble(g), pv = f.get.applyAsDouble(p);
                if (!eq(gv, pv)) {
                    anyDiff = true;
                    fieldDiffCount.put(f.name, fieldDiffCount.get(f.name) + 1);
                    double d = Math.abs(gv - pv);
                    if (!Double.isNaN(d) && d > fieldMaxDiff.get(f.name)) fieldMaxDiff.put(f.name, d);
                }
            }
            if (anyDiff) differing++; else identical++;
        }
        for (String key : produced.keySet()) if (!golden.containsKey(key)) extra++;

        // --- Report ---
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ Generate Squares regression spike ================\n");
        sb.append(String.format("Experiment            : %s%n", EXPERIMENT));
        sb.append(String.format("Golden squares        : %d%n", golden.size()));
        sb.append(String.format("Produced squares      : %d%n", produced.size()));
        sb.append(String.format("Missing (golden only) : %d%n", missing));
        sb.append(String.format("Extra   (produced only): %d%n", extra));
        sb.append(String.format("Identical             : %d%n", identical));
        sb.append(String.format("Differing             : %d%n", differing));
        sb.append(String.format("  'Visible' mismatches : %d%n", visibleMismatch));
        sb.append(String.format("  'Number of Tracks'   : %d%n", tracksMismatch));
        sb.append(String.format("Tolerance             : %.1e%n", TOL));
        sb.append("--- per-field differences (count beyond tolerance / max abs diff) ---\n");
        for (Field f : fields) {
            int c = fieldDiffCount.get(f.name);
            if (c > 0) sb.append(String.format("  %-32s %6d   max=%.6g%n", f.name, c, fieldMaxDiff.get(f.name)));
        }
        if (differing == 0 && missing == 0 && extra == 0) {
            sb.append("RESULT: produced output matches the golden master within tolerance.\n");
        } else {
            sb.append("RESULT: differences found — see the per-field breakdown above.\n");
        }
        sb.append("==================================================================\n");
        System.out.println(sb);

        // --- Regression gate: the pipeline must reproduce the golden master exactly ---
        assertTrue(Files.isRegularFile(producedSquares), "pipeline did not produce a Squares.csv");
        assertEquals(golden.size(), produced.size(), "square count differs from the golden master");
        assertEquals(0, missing, "squares present in the golden master are missing from the output");
        assertEquals(0, extra, "extra squares produced that are not in the golden master");
        assertEquals(0, differing,
                "one or more squares differ from the golden master beyond tolerance — see the report above");
    }
}
