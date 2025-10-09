package paint.shared.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

public class CsvUtils {

    public static int countProcessed(Path filePath) {
        int count = 0;

        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()                 // first record is the header
                    .setSkipHeaderRecord(true)   // don't treat header as a row
                    .build();

            try (CSVParser parser = CSVParser.parse(filePath.toFile(),
                                                    java.nio.charset.StandardCharsets.UTF_8, format)) {

                String processFlagKey = null;
                for (String header : parser.getHeaderMap().keySet()) {
                    if (header.trim().equalsIgnoreCase("Process Flag")) {
                        processFlagKey = header;
                        break;
                    }
                }

                if (processFlagKey == null) {
                    return 0;
                }

                for (CSVRecord record : parser) {
                    String val = record.get(processFlagKey).trim().toLowerCase();
                    if (val.equals("true") || val.equals("yes") || val.equals("y") || val.equals("1")) {
                        count++;
                    }
                }
            }
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
        }

        return count;
    }
}
