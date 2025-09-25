package paint.shared.validate_new;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds the outcome of a validation process.
 * Collects errors (and potentially warnings in the future).
 */
public class ValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();

    public void addError(String message) {
        errors.add(message);
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public void addInfo(String message) {
        infos.add(message);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<String> getInfos() {
        return Collections.unmodifiableList(infos);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ValidationResult:\n");
        if (!errors.isEmpty()) {
            sb.append("Errors:\n").append(String.join("\n", errors)).append("\n");
        }
        if (!warnings.isEmpty()) {
            sb.append("Warnings:\n").append(String.join("\n", warnings)).append("\n");
        }
        if (!infos.isEmpty()) {
            sb.append("Infos:\n").append(String.join("\n", infos)).append("\n");
        }
        if (errors.isEmpty() && warnings.isEmpty() && infos.isEmpty()) {
            sb.append("no issues");
        }
        return sb.toString();
    }

    public void merge(ValidationResult other) {
        if (other == null) return;
        this.errors.addAll(other.getErrors());
        this.warnings.addAll(other.getWarnings());
        this.infos.addAll(other.getInfos());
    }
}