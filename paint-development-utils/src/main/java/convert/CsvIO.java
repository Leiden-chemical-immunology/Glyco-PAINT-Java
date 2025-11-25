package convert;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class CsvIO {

    public static List<Map<String,String>> readCsv(Path file) throws Exception {
        List<Map<String,String>> list = new ArrayList<Map<String,String>>();
        BufferedReader br = Files.newBufferedReader(file);

        try {
            String head = br.readLine();
            if (head == null) return list;

            String[] headers = head.split(",", -1);
            for (int i = 0; i < headers.length; i++)
                headers[i] = headers[i].trim();

            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) continue;

                String[] vals = line.split(",", -1);
                Map<String,String> m = new LinkedHashMap<String,String>();
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i];
                    String v = (i < vals.length ? vals[i] : "");
                    m.put(h, v.trim());
                }
                list.add(m);
            }
        } finally {
            br.close();
        }
        return list;
    }

    public static void writeCsv(Path file, List<String> header, List<Map<String,String>> rows) throws Exception {
        try (BufferedWriter bw = Files.newBufferedWriter(file);
             PrintWriter pw = new PrintWriter(bw)) {

            // Write header
            for (int i = 0; i < header.size(); i++) {
                if (i > 0) pw.print(",");
                pw.print(header.get(i));
            }
            // No println() here → no stray newline yet

            // Write rows
            for (int r = 0; r < rows.size(); r++) {
                pw.println();  // newline before the row (not after)
                Map<String,String> row = rows.get(r);

                for (int c = 0; c < header.size(); c++) {
                    if (c > 0) pw.print(",");
                    String col = header.get(c);
                    String val = row.get(col);
                    pw.print(val == null ? "" : val);
                }
            }
        }
    }
}