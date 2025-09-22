package paint.shared.validate_new;

import paint.shared.constants.PaintConstants;
import tech.tablesaw.api.ColumnType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class SquaresValidator extends AbstractFileValidator {

    @Override
    protected void validateHeader(List<String> actualHeader, ValidationResult result) {
        List<String> expectedHeader = Arrays.asList(PaintConstants.SQUARE_COLS);
        if (!headersMatch(expectedHeader, actualHeader)) {
            result.addError("Header mismatch.\nExpected: " + expectedHeader + "\nActual:   " + actualHeader);
        }
    }

    @Override
    protected ColumnType[] getExpectedTypes() {
        return PaintConstants.SQUARE_TYPES;
    }

    public static void main(String[] args) {
//        if (args.length != 1) {
//            System.err.println("Usage: java paint.shared.validate_new.SquaresValidator <path-to-squares-file>");
//            System.exit(1);
//        }

        File file = new File("/Users/hans/Paint Test Project/250430/All Squares.csv");
        SquaresValidator validator = new SquaresValidator();
        ValidationResult result = validator.validate(file);

        if (result.isValid()) {
            System.out.println("Squares file is valid.");
        } else {
            System.out.println(result);
        }

        // file = new File("/Users/hans/Paint Test Project/250430/All Squares Java.csv");
        validator = new SquaresValidator();
        result = validator.validate(file);

        if (result.isValid()) {
            System.out.println("Squares file is valid.");
        } else {
            System.out.println(result);
        }
    }
}