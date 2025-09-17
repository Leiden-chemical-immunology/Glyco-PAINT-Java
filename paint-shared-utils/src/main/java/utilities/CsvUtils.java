package utilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

public class CsvUtils {

    public static int countProcessed(Path filePath)  {
        int count = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(String.valueOf(filePath)))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                return 0; // empty file
            }

            String[] headers = headerLine.split(",");
            int processIdx = -1;
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("Process Flag")) {
                    processIdx = i;
                    break;
                }
            }
            if (processIdx == -1) {
                //throw new IllegalArgumentException("No 'Process Flag' column found in " + filePath);
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] fields = line.split(",", -1);
                if (processIdx < fields.length) {
                    String val = fields[processIdx].trim().toLowerCase();
                    if (val.equals("true") || val.equals("yes") || val.equals("y") || val.equals("1")) {
                        count++;
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }
}
