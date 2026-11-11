package paint.shared.io.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.objects.ExperimentInfo;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.TextFiles;
import tech.tablesaw.api.ColumnType;

import java.awt.Color;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Character sets of the CSV files read and written by {@link BaseTableIO}.
 *
 * Since Java 18 the default character set is UTF-8 on every system; before, Windows used its own
 * (usually Windows-1252). Excel on Windows saves "CSV (Comma delimited)" as Windows-1252 and
 * "CSV UTF-8" as UTF-8 preceded by a byte order mark (BOM). These tests write exactly those bytes,
 * so they behave the same on macOS and Windows.
 *
 * Files are read through {@link TextFiles}: UTF-8 if valid, otherwise Windows-1252 with a warning
 * in the log; a byte order mark is skipped.
 */
@DisplayName("BaseTableIO — character sets of CSV files")
class CsvCharacterSetTest {

    private static final String PROBE = "µ-probe é 5°C";

    private static String[] headers() {
        ExperimentInfo.Column[] c = ExperimentInfo.Column.values();
        String[] h = new String[c.length];
        for (int i = 0; i < c.length; i++) h[i] = c[i].header;
        return h;
    }

    private static ColumnType[] types() {
        ExperimentInfo.Column[] c = ExperimentInfo.Column.values();
        ColumnType[] t = new ColumnType[c.length];
        for (int i = 0; i < c.length; i++) t[i] = c[i].type;
        return t;
    }

    /** The text of an Experiment Info CSV with PROBE as probe name, as written by the application. */
    private static String experimentInfoCsvText(Path dir) throws Exception {
        ExperimentInfo info = new ExperimentInfo();
        info.setExperimentName("exp");
        info.setRecordingName("rec");
        info.setConditionNumber(1);
        info.setReplicateNumber(1);
        info.setProbeName(PROBE);
        info.setProbeType("type");
        info.setCellType("cell");
        info.setAdjuvant("adj");
        info.setConcentration(1.0);
        info.setProcessFlag(true);
        info.setThreshold(1.0);

        ExperimentInfoTableIO io = new ExperimentInfoTableIO();
        Path template = dir.resolve("template.csv");
        io.writeCsv(io.toTable(Collections.singletonList(info)), template);
        return Files.readString(template, StandardCharsets.UTF_8);
    }

    private static String readProbeName(Path csv) throws Exception {
        ExperimentInfoTableIO io = new ExperimentInfoTableIO();
        List<ExperimentInfo> back = io.toEntities(io.readCsvWithSchema(csv, headers(), types(), false));
        assertEquals(1, back.size());
        return back.get(0).getProbeName();
    }

    @Test
    @DisplayName("writes UTF-8")
    void writesUtf8(@TempDir Path dir) throws Exception {
        ExperimentInfo info = new ExperimentInfo();
        info.setExperimentName("exp");
        info.setRecordingName("rec");
        info.setProbeName(PROBE);
        ExperimentInfoTableIO io = new ExperimentInfoTableIO();
        Path csv = dir.resolve("out.csv");
        io.writeCsv(io.toTable(Collections.singletonList(info)), csv);

        String text = new String(Files.readAllBytes(csv), StandardCharsets.UTF_8);
        assertEquals(true, text.contains(PROBE), "UTF-8 text of the file: " + text);
    }

    @Test
    @DisplayName("reads a UTF-8 file without BOM (as written by the application)")
    void readsUtf8(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("utf8.csv");
        Files.write(csv, experimentInfoCsvText(dir).getBytes(StandardCharsets.UTF_8));
        assertEquals(PROBE, readProbeName(csv));
    }

    @Test
    @DisplayName("reads a UTF-8 file with BOM (Excel: CSV UTF-8)")
    void readsUtf8WithBom(@TempDir Path dir) throws Exception {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = experimentInfoCsvText(dir).getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(body, 0, all, bom.length, body.length);

        Path csv = dir.resolve("utf8-bom.csv");
        Files.write(csv, all);
        assertArrayEquals(bom, java.util.Arrays.copyOf(Files.readAllBytes(csv), 3));
        assertEquals(PROBE, readProbeName(csv));
    }

    @Test
    @DisplayName("reads a Windows-1252 file (Excel on Windows: CSV (Comma delimited))")
    void readsWindows1252(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("cp1252.csv");
        Files.write(csv, experimentInfoCsvText(dir).getBytes(Charset.forName("windows-1252")));
        assertEquals(PROBE, readProbeName(csv));
    }

    /** Collects the log lines written while running {@code action}. */
    private static List<String> logDuring(ThrowingRunnable action) throws Exception {
        List<String> lines = new ArrayList<>();
        PaintLogger.setSink(new PaintLogger.Sink() {
            @Override public void log(String line, Color color) { lines.add(line); }
            @Override public void print(String text) { lines.add(text); }
        });
        try {
            action.run();
        } finally {
            PaintLogger.clearSink();
        }
        return lines;
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    @DisplayName("logs a warning when a file is read as Windows-1252")
    void warnsOnWindows1252(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("cp1252.csv");
        Files.write(csv, experimentInfoCsvText(dir).getBytes(Charset.forName("windows-1252")));

        List<String> log = logDuring(() -> assertEquals(PROBE, readProbeName(csv)));

        assertTrue(log.stream().anyMatch(l -> l.contains("Windows-1252") && l.contains("cp1252.csv")),
                   "log: " + log);
    }

    @Test
    @DisplayName("logs nothing about the character set for a UTF-8 file")
    void noWarningOnUtf8(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("utf8.csv");
        Files.write(csv, experimentInfoCsvText(dir).getBytes(StandardCharsets.UTF_8));

        List<String> log = logDuring(() -> assertEquals(PROBE, readProbeName(csv)));

        assertTrue(log.stream().noneMatch(l -> l.contains("Windows-1252")), "log: " + log);
    }

    @Test
    @DisplayName("TextFiles.readAllLines: Windows-1252 and a byte order mark")
    void readAllLines(@TempDir Path dir) throws Exception {
        Path cp1252 = dir.resolve("lines-cp1252.csv");
        Files.write(cp1252, ("a,b\n" + PROBE + ",x\n").getBytes(Charset.forName("windows-1252")));
        assertEquals(List.of("a,b", PROBE + ",x"), TextFiles.readAllLines(cp1252));

        Path bom = dir.resolve("lines-bom.csv");
        Files.write(bom, ("\uFEFFa,b\n" + PROBE + ",x\n").getBytes(StandardCharsets.UTF_8));
        List<String> lines = TextFiles.readAllLines(bom);
        assertEquals(List.of("a,b", PROBE + ",x"), lines);
        lines.add("still modifiable");
    }

    @Test
    @DisplayName("TextFiles.newReader skips a byte order mark")
    void newReaderSkipsBom(@TempDir Path dir) throws Exception {
        Path bom = dir.resolve("reader-bom.csv");
        Files.write(bom, ("\uFEFFa,b\n").getBytes(StandardCharsets.UTF_8));
        try (var reader = TextFiles.newReader(bom)) {
            assertEquals("a,b", reader.readLine());
        }
    }
}
