package paint.shared.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> infos = new ArrayList<>();
    private String report = null;   // <-- new

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
        if (other == null) {
            return;
        }
        this.errors.addAll(other.getErrors());
        this.warnings.addAll(other.getWarnings());
        this.infos.addAll(other.getInfos());
        if (other.report != null) {
            if (this.report == null) {
                this.report = other.report;
            } else {
                this.report += "\n" + other.report;
            }
        }
    }

    // --- Report field ---
    public void setReport(String report) {
        this.report = report;
    }

    public String getReport() {
        return report;
    }

    @Override
    public String toString() {
        if (report != null) {
            return report;
        }
        if (isValid()) {
            return "✔ No issues found";
        }
        return String.join("\n", errors);
    }
}