/*==============================================================================
 *  Class:        StringLiteralCollector.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Scans all Java files in a source tree and extracts every string literal
 *    found in double quotes.  Produces a deduplicated, alphabetically sorted
 *    list that you can review before replacement.
 *
 *  DESCRIPTION:
 *    - Recursively traverses all .java files under the given root.
 *    - Collects all literals inside double quotes ("...").
 *    - Writes them to a plain text file (one per line).
 *    - Ignores escaped quotes and comments.
 *
 *  USAGE:
 *    java paint.shared.utils.StringLiteralCollector \
 *         /path/to/source/root \
 *         /path/to/output/strings_found.txt
 *
 *  AUTHOR:
 *    Hans Bakker
 *  UPDATED:
 *    2025-11-12
 *============================================================================*/

package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class StringLiteralCollector {

    // Match content between quotes — handles escaped quotes
    private static final Pattern STRING_PATTERN =
            Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");

    public static void main(String[] args) throws IOException {

//        if (args.length != 2) {
//            System.out.println("Usage: java StringLiteralCollector <SourceRoot> <OutputFile>");
//            return;
//        }

        Path sourceRoot = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");
        Path outputFile = Paths.get("/Users/hans/Downloads/strings_to_replace_ori.txt");

        if (!Files.exists(sourceRoot)) {
            System.err.println("❌ Source path does not exist: " + sourceRoot);
            return;
        }

        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceRoot.toFile(), javaFiles);

        Set<String> foundStrings = new TreeSet<>(); // automatically sorted

        for (File file : javaFiles) {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                Matcher m = STRING_PATTERN.matcher(line);
                while (m.find()) {
                    String literal = m.group(1)
                                      .replace("\\\"", "\"")     // unescape
                                      .trim();
                    if (!literal.isEmpty()) {
                        foundStrings.add(literal);
                    }
                }
            }
        }

        Files.write(outputFile, foundStrings);
        System.out.println("✅ Found " + foundStrings.size() + " unique string literals.");
        System.out.println("📝 Written to: " + outputFile);
    }

    private static void collectJavaFiles(File dir, List<File> files) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectJavaFiles(f, files);
            } else if (f.getName().endsWith(".java")) {
                files.add(f);
            }
        }
    }

    private StringLiteralCollector() {}
}