package paint.shared.validate;

import paint.shared.objects.Track;
import tech.tablesaw.api.ColumnType;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the schema of {@code tracks.csv} by checking header correctness
 * and column data types according to {@link Track.Column}.
 *
 * <p>This version no longer depends on TrackSchema; it uses the embedded
 * schema directly inside the Track class.</p>
 */
public final class TracksValidator extends AbstractFileValidator {

    // ───────────────────────────────────────────────────────────────────────────────
    // HEADER VALIDATION
    // ───────────────────────────────────────────────────────────────────────────────

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