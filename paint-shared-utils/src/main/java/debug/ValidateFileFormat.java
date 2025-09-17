package debug;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import constants.PaintConstants;

public class ValidateFileFormat {

    public static void main(String[] args) {
        ValidateFileFormat validator = new ValidateFileFormat();

        String root = "/Users/hans/Paint Test Project/221012/";

        // Validate tracks
        String tracksFile = root + "Experiment Info.csv";
        List<String> trackReport = validator.validateCSV(
                tracksFile,
                PaintConstants.EXPERIMENT_INFO_COLS,
                ValidateFileFormat.EXPERIMENT_INFO_TYPES
        );
        System.out.println(tracksFile);
        trackReport.forEach(System.out::println);
        System.out.println("\n\n");

        // Validate tracks
        String infoFile = root + "All Tracks Java.csv";
        List<String> infoReport = validator.validateCSV(
                infoFile,
                PaintConstants.TRACK_COLS,
                ValidateFileFormat.TRACK_TYPES
        );
        System.out.println(infoFile);
        infoReport.forEach(System.out::println);
        System.out.println("\n\n");

        // Validate recordings\
        String recordingsFile = root + "All Recordings Java.csv";
        List<String> recordingReport = validator.validateCSV(
                recordingsFile,
                PaintConstants.RECORDING_COLS,
                ValidateFileFormat.RECORDING_TYPES
        );
        System.out.println(recordingsFile);
        recordingReport.forEach(System.out::println);
        System.out.println("\n\n");

        // Validate squares
        String squaresFile = root + "All Squares Java.csv";
        List<String> squareReport = validator.validateCSV(
                squaresFile,
                PaintConstants.SQUARE_COLS,
                ValidateFileFormat.SQUARE_TYPES
        );
        System.out.println(squaresFile);
        squareReport.forEach(System.out::println);
        System.out.println("\n\n");
    }

    private enum ColumnType {
        STRING, INT, DOUBLE, BOOLEAN
    }

    public static final ColumnType[] TRACK_TYPES = {
            ColumnType.STRING,         // 0    Unique Key 
            ColumnType.STRING,         // 1    Recording Name
            ColumnType.INT,            // 2    Track Id
            ColumnType.STRING,         // 3    Track Label
            ColumnType.INT,            // 4    Number of Spots
            ColumnType.INT,            // 5    Number of Gaps
            ColumnType.INT,            // 6    Longest Gap
            ColumnType.DOUBLE,         // 7    Track Duration
            ColumnType.DOUBLE,         // 8    Track X Location
            ColumnType.DOUBLE,         // 9    Track Y Location
            ColumnType.DOUBLE,         // 10   Track Displacement
            ColumnType.DOUBLE,         // 11   Track Max Speed
            ColumnType.DOUBLE,         // 12   Track Median Speed
            ColumnType.DOUBLE,         // 13   Diffusion Coefficient
            ColumnType.DOUBLE,         // 14   Diffusion Coefficient Ext
            ColumnType.DOUBLE,         // 15   Total Distance
            ColumnType.DOUBLE,         // 16   Confinement Ratio
            ColumnType.INT,            // 17   Square Number
            ColumnType.INT             // 18   Label Number
    };

    public static final ColumnType[] SQUARE_TYPES = {
            ColumnType.STRING,        // 0      Unique Key 
            ColumnType.STRING,        // 1      Recording Name
            ColumnType.INT,           // 2      Square Number
            ColumnType.INT,           // 3      Row Number
            ColumnType.INT,           // 4      Column Number
            ColumnType.INT,           // 5      Label Number
            ColumnType.INT,           // 6      Cell ID
            ColumnType.BOOLEAN,       // 7      Selected
            ColumnType.BOOLEAN,       // 8      Square Manually Excluded
            ColumnType.BOOLEAN,       // 9      Image Excluded
            ColumnType.DOUBLE,        // 10     X0
            ColumnType.DOUBLE,        // 11     Y0
            ColumnType.DOUBLE,        // 12     X1
            ColumnType.DOUBLE,        // 13     Y1
            ColumnType.INT,           // 14     Number of Tracks
            ColumnType.DOUBLE,        // 15     Variability
            ColumnType.DOUBLE,        // 16     Density
            ColumnType.DOUBLE,        // 17     Density Ratio
            ColumnType.DOUBLE,        // 18     Tau
            ColumnType.DOUBLE,        // 19     R Squared
            ColumnType.DOUBLE,        // 20     Median Diffusion Coefficient
            ColumnType.DOUBLE,        // 21     Median Diffusion Coefficient EXT
            ColumnType.DOUBLE,        // 22     Median Long Track Duration
            ColumnType.DOUBLE,        // 23     Median Short Track Duration
            ColumnType.DOUBLE,        // 24     Median Displacement
            ColumnType.DOUBLE,        // 25     Max Displacement
            ColumnType.DOUBLE,        // 26     Total Displacement
            ColumnType.DOUBLE,        // 27     Median Max Speed
            ColumnType.DOUBLE,        // 28     Max Max Speed
            ColumnType.DOUBLE,        // 29     Median Mean Speed
            ColumnType.DOUBLE,        // 30     Max Mean Speed
            ColumnType.DOUBLE,        // 31     Max Track Duration
            ColumnType.DOUBLE,        // 32     Total Track Duration
            ColumnType.DOUBLE,        // 33     Median Track Duration
    };

    public static final ColumnType[] RECORDING_TYPES = {
            ColumnType.STRING,       // 0       Recording Name 
            ColumnType.INT,          // 1       Condition Number
            ColumnType.INT,          // 2       Replicate Number
            ColumnType.STRING,       // 3       Probe Name
            ColumnType.STRING,       // 4       Probe Type
            ColumnType.STRING,       // 5       Cell Type
            ColumnType.STRING,       // 6       Adjuvant
            ColumnType.DOUBLE,       // 7       Concentration
            ColumnType.BOOLEAN,      // 8       Process Flag
            ColumnType.DOUBLE,       // 9       Threshold
            ColumnType.INT,          // 10      Number of Spots
            ColumnType.INT,          // 11      Number of Tracks
            ColumnType.INT,          // 12      Number of Spots in All Tracks
            ColumnType.INT,          // 13      Number of Frames
            ColumnType.DOUBLE,       // 14      Run Time
            ColumnType.STRING,       // 15      Time Stamp
            ColumnType.BOOLEAN,      // 16      Exclude
            ColumnType.DOUBLE,       // 17      Tau
            ColumnType.DOUBLE,       // 18      R Squared
            ColumnType.DOUBLE        // 19      Density
    };

    public static final ColumnType[] EXPERIMENT_INFO_TYPES = {
            ColumnType.STRING,       // 0       Recording Name
            ColumnType.INT,          // 1       Condition Number
            ColumnType.INT,          // 2       Replicate Number
            ColumnType.STRING,       // 3       Probe Name
            ColumnType.STRING,       // 4       Probe Type
            ColumnType.STRING,       // 5       Cell Type
            ColumnType.STRING,       // 6       Adjuvant
            ColumnType.DOUBLE,       // 7       Concentration
            ColumnType.BOOLEAN,      // 8       Process Flag
            ColumnType.DOUBLE,       // 9       Threshold
    };


    /**
     * Validates the structure and contents of a CSV file against expected columns and types.
     * <p>
     * The method checks:
     * <ul>
     *   <li>That the header row contains all expected column names in the correct order.</li>
     *   <li>That each data row has values matching the expected types (STRING, INT, DOUBLE, BOOLEAN).</li>
     * </ul>
     * A compact list of report messages is returned, summarizing issues found.
     *
     * @param filePath      the path to the CSV file to validate
     * @param expectedCols  the expected header names in the correct order
     * @param expectedTypes the expected type for each column
     * @return a list of report lines indicating validation results; if the list contains only
     *         success messages, the file passed validation
     * @throws IOException if an I/O error occurs while reading the file
     */
    public List<String> validateCSV(String filePath, String[] expectedCols, ColumnType[] expectedTypes) {
        List<String> report = new ArrayList<>();
        boolean[] badColumn = new boolean[expectedTypes.length];

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line == null) {
                report.add("File is empty.");
                return report;
            }

            // validate header
            String[] headers = line.split(",", -1);
            validateHeaders(headers, expectedCols, report);

            // validate rows: only track if a column has bad numbers
            int rowNumber = 1;
            while ((line = br.readLine()) != null) {
                rowNumber++;
                String[] values = line.split(",", -1);
                int limit = Math.min(values.length, expectedTypes.length);
                for (int i = 0; i < limit; i++) {
                    if (!badColumn[i] && !validateValue(values[i], expectedTypes[i])) {
                        badColumn[i] = true;
                    }
                }
            }

            // add compact number format errors
            for (int i = 0; i < badColumn.length; i++) {
                if (badColumn[i]) {
                    report.add("Invalid value format detected in column: " + expectedCols[i]);
                }
            }

        } catch (IOException e) {
            report.add("I/O error: " + e.getMessage());
        }

        if (report.isEmpty()) {
            report.add("Headers are correct ✅");
            report.add("All numeric columns are well-formed ✅");
        }

        return report;
    }

    private void validateHeaders(String[] headers, String[] expectedCols, List<String> report) {
        boolean headerOk = true;

        // unexpected headers
        Set<String> expectedSet = new HashSet<>();
        for (String col : expectedCols) {
            expectedSet.add(col);
        }
        for (String header : headers) {
            if (!expectedSet.contains(header.trim())) {
                report.add("Unexpected header: '" + header + "'");
                headerOk = false;
            }
        }

        // missing headers
        Set<String> headerSet = new HashSet<>();
        for (String header : headers) {
            headerSet.add(header.trim());
        }
        for (String expected : expectedCols) {
            if (!headerSet.contains(expected)) {
                report.add("Missing expected header: '" + expected + "'");
                headerOk = false;
            }
        }

        // order mismatch check only if same length
        if (headers.length == expectedCols.length) {
            for (int i = 0; i < headers.length; i++) {
                if (!expectedCols[i].equals(headers[i].trim())) {
                    report.add("Header order mismatch at column " + i
                            + ": expected '" + expectedCols[i]
                            + "', found '" + headers[i] + "'");
                    headerOk = false;
                }
            }
        }

        if (headerOk) {
            report.add("Headers are correct ✅");
        }
    }

    private boolean validateValue(String value, ColumnType type) {
        switch (type) {
            case STRING:
                return true; // accept anything
            case INT:
                try {
                    if (!value.isEmpty()) Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case DOUBLE:
                try {
                    if (!value.isEmpty()) Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case BOOLEAN:
                String v = value.trim().toLowerCase();
                return v.isEmpty() || v.equals("true") || v.equals("false");
            default:
                return false;
        }
    }
}