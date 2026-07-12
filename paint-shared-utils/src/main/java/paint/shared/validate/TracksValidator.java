/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.validate;

import paint.shared.objects.Track;
import tech.tablesaw.api.ColumnType;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the schema of {@code tracks.csv} by checking header correctness
 * and column data types according to {@link paint.shared.objects.Track.Column Track.Column}.
 *
 * <p>This version no longer depends on TrackSchema; it uses the embedded
 * schema directly inside the Track class.</p>
 */
public final class TracksValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Validates that the actual CSV headers match the expected headers defined in
     * {@link paint.shared.objects.Track.Column Track.Column}.
     *
     * @param actualHeader the list of headers found in the CSV file
     * @param result       the {@link ValidationResult} to record any mismatches
     */
    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {

        // Build expected header from Track.Column enum
        List<String> expected = new ArrayList<>();
        for (Track.Column col : Track.Column.values()) {
            expected.add(col.header);
        }

        headersMatch(expected, actualHeader, result);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TYPE VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns the expected {@link ColumnType} for each column in the tracks CSV,
     * as defined in {@link paint.shared.objects.Track.Column Track.Column}.
     *
     * @return an array of expected column types
     */
    @Override
    protected ColumnType[] getExpectedTypes() {

        Track.Column[] cols = Track.Column.values();
        ColumnType[]  types = new ColumnType[cols.length];

        for (int i = 0; i < cols.length; i++) {
            types[i] = cols[i].type;
        }

        return types;
    }
}