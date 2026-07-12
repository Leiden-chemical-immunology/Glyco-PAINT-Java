/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.override.recording_exclude;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static paint.viewer.override.ExportOverridesFromViewer.applyRecordingExcludes;
import static paint.viewer.override.recording_exclude.ImportRecordingExclude.importRecordingExcludes;
import static paint.viewer.override.recording_exclude.ImportRecordingExclude.loadRecordingExclude;
import static paint.viewer.override.recording_exclude.WriteRecordingExclude.updateExcludeRecordingsCsv;

/**
 * Pins down the contract of the recording-exclude round trip.
 * <p>
 * {@code Viewer/Recording Exclude.csv} — <em>not</em> the {@code Exclude} column of any
 * Recordings.csv — is the authoritative record of which recordings the user excluded.
 * The Viewer appends to and removes from that file as the user toggles Exclude; both
 * consumers of it ({@link ImportRecordingExclude} on load, and
 * {@link paint.viewer.override.ExportOverridesFromViewer} on export) then <em>clear the
 * whole Exclude column and rebuild it from the file</em>.
 * </p>
 * <p>
 * That clearing looks destructive, and it is: an Exclude flag set by any other means is
 * discarded. It is nevertheless deliberate — it is what makes the file authoritative and the
 * result independent of whatever the column happened to contain. The tests below fix that
 * behaviour in place so it cannot be "fixed" away by someone who meets the clearing loop
 * without the context.
 * </p>
 */
class RecordingExcludeRoundTripTest {

    private static final String NAME    = "Recording Name";
    private static final String EXCLUDE = "Exclude";

    /** Builds a Recordings table with the given names, all initially not excluded. */
    private static Table recordings(String... names) {
        StringColumn  name     = StringColumn.create(NAME);
        BooleanColumn excluded = BooleanColumn.create(EXCLUDE);
        for (String n : names) {
            name.append(n);
            excluded.append(false);
        }
        return Table.create("Recordings", name, excluded);
    }

    /** The names the exclude file currently lists, in file order. */
    private static List<String> listed(Path project) {
        return loadRecordingExclude(project.resolve("Viewer").resolve("Recording Exclude.csv"))
                .stream()
                .map(RecordingExclude::getRecordingName)
                .collect(Collectors.toList());
    }

    /** Whether the given recording is flagged excluded in the table. */
    private static boolean excluded(Table t, String recordingName) {
        StringColumn  names = t.stringColumn(NAME);
        BooleanColumn flags = t.booleanColumn(EXCLUDE);
        for (int row = 0; row < t.rowCount(); row++) {
            if (recordingName.equals(names.get(row))) {
                return flags.get(row);
            }
        }
        throw new IllegalArgumentException("No such recording in the table: " + recordingName);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Writing the exclude file
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("excluding recordings writes them to the file, and they read back")
    void writeThenReadRoundTrips(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);
        updateExcludeRecordingsCsv(project, "221012-Exp-1-B2-3", true);

        assertEquals(2, listed(project).size());
        assertTrue(listed(project).contains("221012-Exp-1-A1-1"));
        assertTrue(listed(project).contains("221012-Exp-1-B2-3"));
    }

    @Test
    @DisplayName("the Viewer folder is created if it does not exist yet")
    void createsTheViewerFolder(@TempDir Path project) {
        assertFalse(Files.exists(project.resolve("Viewer")));

        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        assertTrue(Files.exists(project.resolve("Viewer").resolve("Recording Exclude.csv")));
    }

    @Test
    @DisplayName("excluding the same recording twice does not list it twice")
    void excludingTwiceDoesNotDuplicate(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        assertEquals(1, listed(project).size());
    }

    @Test
    @DisplayName("un-excluding a recording removes it from the file")
    void unExcludingRemoves(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);
        updateExcludeRecordingsCsv(project, "221012-Exp-1-B2-3", true);

        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", false);

        assertEquals(java.util.Collections.singletonList("221012-Exp-1-B2-3"), listed(project));
    }

    @Test
    @DisplayName("un-excluding a recording that was never excluded is a no-op")
    void unExcludingSomethingAbsentIsHarmless(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        updateExcludeRecordingsCsv(project, "221012-Exp-1-Z9-9", false);

        assertEquals(java.util.Collections.singletonList("221012-Exp-1-A1-1"), listed(project));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Importing the exclude file into a Recordings table
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("import flags exactly the recordings the file lists")
    void importFlagsWhatTheFileLists(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        Table t = recordings("221012-Exp-1-A1-1", "221012-Exp-1-B2-3");
        importRecordingExcludes(t, project);

        assertTrue(excluded(t, "221012-Exp-1-A1-1"));
        assertFalse(excluded(t, "221012-Exp-1-B2-3"));
    }

    @Test
    @DisplayName("import clears an Exclude flag the file does not list (the file is authoritative)")
    void importClearsFlagsNotInTheFile(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        Table t = recordings("221012-Exp-1-A1-1", "221012-Exp-1-B2-3");
        t.booleanColumn(EXCLUDE).set(1, true);          // B2-3 excluded by some other means

        importRecordingExcludes(t, project);

        assertTrue(excluded(t, "221012-Exp-1-A1-1"));
        assertFalse(excluded(t, "221012-Exp-1-B2-3"),
                    "an Exclude flag not backed by the exclude file is deliberately discarded");
    }

    @Test
    @DisplayName("import matches the whole recording name, not a prefix of it")
    void importMatchesWholeNameNotPrefix(@TempDir Path project) {
        // -A1-1 is a prefix of -A1-10. Substring or prefix matching here would exclude both.
        // Two bugs of exactly this shape were found elsewhere in the Viewer, so guard it.
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        Table t = recordings("221012-Exp-1-A1-1", "221012-Exp-1-A1-10");
        importRecordingExcludes(t, project);

        assertTrue(excluded(t, "221012-Exp-1-A1-1"));
        assertFalse(excluded(t, "221012-Exp-1-A1-10"),
                    "-A1-10 must not be excluded merely because -A1-1 is a prefix of it");
    }

    @Test
    @DisplayName("a name in the file that no recording has is ignored")
    void unknownNamesAreIgnored(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-9-ZZ-9", true);

        Table t = recordings("221012-Exp-1-A1-1");
        importRecordingExcludes(t, project);

        assertFalse(excluded(t, "221012-Exp-1-A1-1"));
    }

    @Test
    @DisplayName("with no exclude file, import leaves the table exactly as it was")
    void noFileLeavesTheTableUntouched(@TempDir Path project) {
        Table t = recordings("221012-Exp-1-A1-1", "221012-Exp-1-B2-3");
        t.booleanColumn(EXCLUDE).set(0, true);

        importRecordingExcludes(t, project);   // no Viewer/Recording Exclude.csv exists

        assertTrue(excluded(t, "221012-Exp-1-A1-1"),
                   "absent file means 'nothing to apply', not 'clear everything'");
        assertFalse(excluded(t, "221012-Exp-1-B2-3"));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Exporting: the same rebuild, from the same file
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("export rebuilds the Exclude column from the file, exactly as import does")
    void exportRebuildsTheColumnLikeImportDoes(@TempDir Path project) {
        updateExcludeRecordingsCsv(project, "221012-Exp-1-A1-1", true);

        Table t = recordings("221012-Exp-1-A1-1", "221012-Exp-1-B2-3");
        t.booleanColumn(EXCLUDE).set(1, true);          // stale flag, not backed by the file

        applyRecordingExcludes(t, project);

        assertTrue(excluded(t, "221012-Exp-1-A1-1"));
        assertFalse(excluded(t, "221012-Exp-1-B2-3"));
    }

    @Test
    @DisplayName("a full toggle round trip: what the user excluded is what comes back")
    void fullRoundTrip(@TempDir Path project) {
        String a = "221012-Exp-1-A1-1";
        String b = "221012-Exp-1-A1-10";
        String c = "221012-Exp-1-B2-3";

        // The user excludes a and c, then changes their mind about c and excludes b instead.
        updateExcludeRecordingsCsv(project, a, true);
        updateExcludeRecordingsCsv(project, c, true);
        updateExcludeRecordingsCsv(project, c, false);
        updateExcludeRecordingsCsv(project, b, true);

        Table t = recordings(a, b, c);
        importRecordingExcludes(t, project);

        assertTrue(excluded(t, a));
        assertTrue(excluded(t, b));
        assertFalse(excluded(t, c));
    }
}
