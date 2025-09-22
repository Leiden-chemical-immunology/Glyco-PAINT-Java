package paint.shared.validate_new;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class TracksValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.TRACK_COLS);
        if (!headersMatch(expectedHeader, actualHeader)) {
            result.addError("Header mismatch.\nExpected: " + expectedHeader + "\nActual:   " + actualHeader);
        }
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.TRACK_TYPES;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java paint.shared.validate_new.TracksValidator <path-to-tracks-file>");
            System.exit(1);
        }

        File file = new File(args[0]);
        TracksValidator validator = new TracksValidator();
        ValidationResult result = validator.validate(file);

        if (result.isValid()) {
            System.out.println("Tracks file is valid.");
        } else {
            System.out.println(result);
        }

        file = new File("/Users/hans/Paint Test Project/230613/All Tracks Java.csv");
        result = validator.validate(file);

        if (result.isValid()) {
            System.out.println("Tracks file is valid.");
        } else {
            System.out.println(result);
        }
    }
}