/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the aggregated result of one or more validation processes.
 * Stores categorized message lists (errors and warnings) and
 * provides convenience methods for merging and reporting.
 */
public final class ValidationResult {

    // ───────────────────────────────────────────────────────────────────────────────
    // FIELDS
    // ───────────────────────────────────────────────────────────────────────────────

    // There is deliberately no "infos" list: nothing ever added to one. The field, its getter,
    // hasInfos() and the INFO section of toString() all existed, but no addInfo() method did,
    // so the list was permanently empty and the reporting code unreachable.
    private final List<String> errors   = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private String report               = null;

    // ───────────────────────────────────────────────────────────────────────────────
    // MESSAGE ADDERS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Adds a new error message to the validation result.
     *
     * @param message descriptive text of the validation error
     */
    public void addError(String message) {
        errors.add(message);
    }

    /**
     * Adds a new warning message to the validation result.
     *
     * @param message descriptive text of the validation warning
     */
    public void addWarning(String message) {
        warnings.add(message);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns an immutable view of all recorded errors.
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Returns an immutable view of all recorded warnings.
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // STATE CHECKS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Checks if the validation result contains one or more errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Checks if the validation result contains warnings.
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Determines whether the validation result is completely valid.
     */
    public boolean isValid() {
        return errors.isEmpty();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // MERGE & REPORTING
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Merges another {@link ValidationResult} into this one.
     */
    public void merge(ValidationResult other) {
        if (other == null) {
            return;
        }

        this.errors.addAll(other.getErrors());
        this.warnings.addAll(other.getWarnings());

        if (other.report != null) {
            if (this.report == null) {
                this.report = other.report;
            } else {
                this.report += "\n" + other.report;
            }
        }
    }

    /**
     * Sets the report string for this validation result.
     */
    public void setReport(String report) {
        this.report = report;
    }

    /**
     * Returns the current report string, if set.
     */
    public String getReport() {
        return report;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // OUTPUT
    // ───────────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {

        // Prefer explicit report if set
        if (report != null) {
            return report;
        }

        StringBuilder sb = new StringBuilder();

        if (hasErrors()) {
            sb.append("❌ ERRORS:\n");
            for (String e : errors) {
                sb.append("  • ").append(e).append('\n');
            }
        }

        if (hasWarnings()) {
            sb.append("⚠️  WARNINGS:\n");
            for (String w : warnings) {
                sb.append("  • ").append(w).append('\n');
            }
        }

        if (!hasErrors() && !hasWarnings()) {
            sb.append("✔ No issues found");
        }

        return sb.toString();
    }
}