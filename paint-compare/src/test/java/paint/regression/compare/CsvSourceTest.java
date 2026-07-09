package paint.regression.compare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CsvSource / TableComparer — robust CSV comparison")
class CsvSourceTest {

    private static Path write(Path dir, String name, String content) throws IOException {
        Path p = dir.resolve(name);
        Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        return p;
    }

    @Test
    @DisplayName("quoted fields containing commas are parsed as one field (not split)")
    void parsesQuotedCommas(@TempDir Path dir) throws IOException {
        Path csv = write(dir, "q.csv",
                "Unique Key,Label,Value\n"
                        + "k1,\"Smith, John\",42\n"
                        + "k2,plain,7\n");

        List<Map<String, String>> rows = CsvSource.read(csv);

        assertEquals(2, rows.size());
        // The hand-rolled split(",") would have turned this into two fields.
        assertEquals("Smith, John", rows.get(0).get("Label"));
        assertEquals("42", rows.get(0).get("Value"));
        assertEquals("plain", rows.get(1).get("Label"));
    }

    @Test
    @DisplayName("compareFiles: identical files -> no differences")
    void compareIdenticalFiles(@TempDir Path dir) throws Exception {
        String content = "Unique Key,Density\nk1,1.000\nk2,2.000\n";
        Path a = write(dir, "a.csv", content);
        Path b = write(dir, "b.csv", content);

        ComparisonResult r = compareFiles(a, b);
        assertFalse(r.hasDifferences(), r.report());
    }

    @Test
    @DisplayName("compareFiles: a changed numeric value beyond tolerance is a real difference")
    void compareChangedValue(@TempDir Path dir) throws Exception {
        Path a = write(dir, "a.csv", "Unique Key,Density\nk1,1.000\nk2,2.000\n");
        Path b = write(dir, "b.csv", "Unique Key,Density\nk1,1.000\nk2,2.500\n");

        ComparisonResult r = compareFiles(a, b);
        assertTrue(r.hasDifferences());
        assertEquals(1, r.count(ComparisonResult.Difference.Kind.VALUE));
    }

    @Test
    @DisplayName("compareFiles: reordered rows are NOT a difference (keyed on Unique Key)")
    void reorderedRowsAreEqual(@TempDir Path dir) throws Exception {
        Path a = write(dir, "a.csv", "Unique Key,Density\nk1,1.000\nk2,2.000\n");
        Path b = write(dir, "b.csv", "Unique Key,Density\nk2,2.000\nk1,1.000\n");

        assertFalse(compareFiles(a, b).hasDifferences());
    }

    /** Reads both CSVs and compares them keyed on Unique Key, tolerance 1e-3. */
    private static ComparisonResult compareFiles(Path a, Path b) throws IOException {
        return TableComparer.compare(CsvSource.read(a), CsvSource.read(b),
                row -> row.getOrDefault("Unique Key", ""), noIgnore(), 1e-3);
    }

    private static Set<String> noIgnore() {
        return Collections.emptySet();
    }
}
