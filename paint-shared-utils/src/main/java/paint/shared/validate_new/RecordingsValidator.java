package paint.shared.validate_new;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RecordingsValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.RECORDING_COLS);
        if (!headersMatch(expectedHeader, actualHeader)) {
            result.addError("Header mismatch.\nExpected: " + expectedHeader + "\nActual:   " + actualHeader);
        }
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.RECORDING_TYPES;
    }

    public static void main(String[] args) {
//        if (args.length != 1) {
//            System.err.println("Usage: java paint.shared.validate_new.RecordingsValidator <path-to-recordings-file>");
//            System.exit(1);
//        }

        File file;
        RecordingsValidator validator;
        ValidationResult result;

        file = new File("/Users/hans/Paint Test Project/230417/All Recordings Java.csv");
        validator = new RecordingsValidator();
        result = validator.validate(file);

        if (result.isValid()) {
            System.out.println("Recordings file is valid.");
        } else {
            System.out.println(result);
        }

//        file = new File("/Users/hans/Paint Test Project/230417/All Recordings Java.csv");
//        validator = new RecordingsValidator();
//        result = validator.validate(file);
//
//        if (result.isValid()) {
//            System.out.println("Recordings file is valid.");
//        } else {
//            System.out.println(result);
//        }

    }
}