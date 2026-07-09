package paint.regression.compare;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Java-vs-Java regression comparison of two CSV files, keyed 1:1 and
 * order-independent. Reads both files robustly ({@link CsvSource}), compares
 * them ({@link TableComparer}), prints a structured report, and exits non-zero
 * if any difference is found — so it can gate a build or CI run.
 *
 * <p>Replaces the old {@code CsvComparatorRegression} for the Java-vs-Java case:
 * no hardcoded paths (everything comes from arguments), robust CSV parsing, and
 * a pass/fail contract instead of eyeballed diagnostics.</p>
 *
 * <pre>
 * Usage:
 *   RegressionComparator &lt;baseline.csv&gt; &lt;test.csv&gt; &lt;keyColumn&gt; [tolerance] [ignoreCol,ignoreCol,...]
 * </pre>
 *
 * For example, key squares/tracks on the {@code "Unique Key"} column so ordering
 * differences are never reported as content differences.
 */
public final class RegressionComparator {

    private static final double DEFAULT_TOLERANCE = 1e-3;

    private RegressionComparator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: RegressionComparator <baseline.csv> <test.csv> <keyColumn> "
                                       + "[tolerance] [ignoreCol,ignoreCol,...]");
            System.exit(2);
            return;
        }

        Path        baseline = Paths.get(args[0]);
        Path        test     = Paths.get(args[1]);
        String      keyCol   = args[2];
        double      tol      = args.length >= 4 ? Double.parseDouble(args[3]) : DEFAULT_TOLERANCE;
        Set<String> ignore   = args.length >= 5
                ? new HashSet<>(Arrays.asList(args[4].split(",")))
                : Collections.emptySet();

        ComparisonResult result = compareFiles(baseline, test, keyCol, ignore, tol);
        System.out.println(result.report());
        System.exit(result.hasDifferences() ? 1 : 0);
    }

    /**
     * Reads both CSV files and compares them keyed on {@code keyColumn}.
     *
     * @return the structured comparison result
     */
    public static ComparisonResult compareFiles(Path baseline,
                                                Path test,
                                                String keyColumn,
                                                Set<String> ignoreColumns,
                                                double tolerance) throws Exception {
        List<Map<String, String>> base = CsvSource.read(baseline);
        List<Map<String, String>> tst  = CsvSource.read(test);
        Function<Map<String, String>, String> keyFn = row -> row.getOrDefault(keyColumn, "");
        return TableComparer.compare(base, tst, keyFn, ignoreColumns, tolerance);
    }
}
