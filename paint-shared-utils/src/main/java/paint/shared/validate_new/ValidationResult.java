package paint.shared.validate_new;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the outcome of a validation process.
 * Collects errors, warnings, and informational messages.
 */
public class ValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();

    // --- Adders ---

    public void addError(String message) {
        errors.add(message);
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public void addInfo(String message) {
        infos.add(message);
    }

    // --- Getters ---

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getInfos() {
        return Collections.unmodifiableList(infos);
    }

    // --- State checks ---

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    // --- Merge ---

    public void merge(ValidationResult other) {
        if (other == null) return;
        this.errors.addAll(other.getErrors());
        this.warnings.addAll(other.getWarnings());
        this.infos.addAll(other.getInfos());
    }

    // --- Formatting ---

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (errors.isEmpty() && warnings.isEmpty() && infos.isEmpty()) {
            sb.append("✔ No issues found");
        } else {
            if (!errors.isEmpty()) {
                sb.append("Errors:\n");
                errors.forEach(e -> sb.append(" - ").append(e).append("\n"));
            }
            if (!warnings.isEmpty()) {
                sb.append("Warnings:\n");
                warnings.forEach(w -> sb.append(" - ").append(w).append("\n"));
            }
            if (!infos.isEmpty()) {
                sb.append("Info:\n");
                infos.forEach(i -> sb.append(" - ").append(i).append("\n"));
            }
        }

        return sb.toString().trim();
    }
}
