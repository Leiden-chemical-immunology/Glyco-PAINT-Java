/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.compare;

import java.util.Map;

/**
 * Decides, per column, whether two cell values should count as a difference.
 *
 * <p>This is the extension point that lets {@link TableComparer} stay a generic,
 * pure diff engine while domain-specific comparison rules (ignored columns,
 * per-field tolerances, sentinel equivalences, cross-column corrections) live in
 * their own, individually testable implementation.</p>
 */
public interface FieldComparator {

    /** @return {@code true} if this column should be skipped entirely. */
    boolean isIgnored(String column);

    /**
     * @param column        the column being compared
     * @param baselineValue the (trimmed) value from the baseline row
     * @param testValue     the (trimmed) value from the test row
     * @param baselineRow   the full baseline row, for rules needing cross-column context
     * @param testRow       the full test row, for rules needing cross-column context
     * @return {@code true} if the two values are considered equal (i.e. NOT a difference)
     */
    boolean equal(String column,
                  String baselineValue,
                  String testValue,
                  Map<String, String> baselineRow,
                  Map<String, String> testRow);
}
