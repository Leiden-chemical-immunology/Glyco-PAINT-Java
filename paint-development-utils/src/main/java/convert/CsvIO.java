/*=============================================================================
 *  Class:        CsvIO.java
 *  Package:      convert
 *
 *  PURPOSE:
 *    Utility class for low-level CSV input/output operations during conversion.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-development-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package convert;

import java.io.*;
import java.nio.file.*;
import java.util.*;

final class CsvIO {

    static List<Map<String,String>> readSimpleCsv(Path file) throws Exception {
        List<Map<String,String>> list = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(file)) {

            String head = br.readLine();
            if (head == null) {
                return list;
            }

            String[] headers = head.split(",", -1);
            for (int i = 0; i < headers.length; i++)
                headers[i] = headers[i].trim();

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                String[] vals = line.split(",", -1);
                Map<String,String> m = new LinkedHashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String h = headers[i];
                    String v = (i < vals.length ? vals[i] : "");
                    m.put(h, v.trim());
                }
                list.add(m);
            }
        }

        return list;
    }

    static void writeSimpleCsv(Path file, List<String> header, List<Map<String,String>> rows) throws Exception {
        try (BufferedWriter bw = Files.newBufferedWriter(file);
             PrintWriter pw = new PrintWriter(bw)) {

            // Write header
            boolean first = true;
            for (String h : header) {
                if (!first) {
                    pw.print(",");
                }
                pw.print(h);
                first = false;
            }

            // Write rows
            for (Map<String,String> row : rows) {
                pw.println();

                boolean firstCol = true;
                for (String col : header) {
                    if (!firstCol) {
                        pw.print(",");
                    }
                    String val = row.get(col);
                    pw.print(val == null ? "" : val);
                    firstCol = false;
                }
            }
        }
    }
}