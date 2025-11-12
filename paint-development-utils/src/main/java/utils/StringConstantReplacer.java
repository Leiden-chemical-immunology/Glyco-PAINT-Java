/*==============================================================================
 *  Class:        StringConstantReplacer.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Replaces specific string literals in Java files with constant names
 *    defined in PaintConstants.java. Uses an external list of approved
 *    replacements for safety.
 *
 *  DESCRIPTION:
 *    - Reads allowed strings from a text file (one per line).
 *    - Converts each to a constant name (uppercase, underscores).
 *    - Replaces those literals across the source tree.
 *    - Adds new constants to PaintConstants.java if not present.
 *    - Ensures each file has `import static paint.shared.constants.PaintConstants.*;`
 *
 *  USAGE:
 *    java paint.shared.utils.StringConstantReplacer \
 *         /path/to/PaintConstants.java \
 *         /path/to/source/root \
 *         /path/to/strings_to_replace.txt
 *
 *  AUTHOR:
 *    Hans Bakker
 *  UPDATED:
 *    2025-11-12
 *============================================================================*/

package paint.shared.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public final class StringConstantReplacer {

    private static final String IMPORT_LINE = "import static paint.shared.constants.PaintConstants.*;";
    private static final String CONST_TEMPLATE = "public static final String %s = \"%s\";";

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("Usage: java StringConstantReplacer <PaintConstantsPath> <SourceRoot> <StringListFile>");
            return;
        }

        Path constantsPath = Paths.get(args[0]);
        Path sourceRoot = Paths.get(args[1]);
        Path listFile = Paths.get(args[2]);

        if (!Files.exists(constantsPath) || !Files.exists(sourceRoot) || !Files.exists(listFile)) {
            System.err.println("❌ One or more paths are invalid.");
            return;
        }

        // Load list of approved strings
        List<String> targetStrings = Files.readAllLines(listFile);
        targetStrings.removeIf(String::isEmpty);

        // Build mapping: literal → constantName
        Map<String, String> replacements = new LinkedHashMap<>();
        for (String literal : targetStrings) {
            String constName = literal.toUpperCase()
                                      .replaceAll("[^A-Z0-9]+", "_")
                                      .replaceAll("_+", "_");
            replacements.put(literal, constName);
        }

        // Load existing constants
        Set<String> existingConstants = new HashSet<>();
        List<String> constantsLines = Files.readAllLines(constantsPath);
        for (String line : constantsLines) {
            Matcher m = Pattern.compile("public static final String (\\w+)").matcher(line);
            if (m.find()) existingConstants.add(m.group(1));
        }

        // Traverse source tree
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceRoot.toFile(), javaFiles);

        Map<String, String> newConstants = new LinkedHashMap<>();

        for (File f : javaFiles) {
            if (f.getName().equals("PaintConstants.java")) continue;
            processFile(f, replacements, existingConstants, newConstants);
        }

        // Append new constants to PaintConstants.java if needed
        if (!newConstants.isEmpty()) {
            System.out.println("\n✳ Adding new constants to PaintConstants.java:");
            BufferedWriter writer = new BufferedWriter(new FileWriter(constantsPath.toFile(), true));
            writer.write("\n// Auto-added constants\n");
            for (Map.Entry<String, String> e : newConstants.entrySet()) {
                String def = String.format(CONST_TEMPLATE, e.getKey(), e.getValue());
                writer.write(def + "\n");
                System.out.println("   + " + def);
            }
            writer.close();
        }

        System.out.println("\n✅ Done. Updated files and constants where needed.");
    }

    private static void processFile(File file,
            Map<String, String> replacements,
            Set<String> existingConstants,
            Map<String, String> newConstants) throws IOException {

        String content = new String(Files.readAllBytes(file.toPath()));
        boolean modified = false;

        for (Map.Entry<String, String> e : replacements.entrySet()) {
            String literal = e.getKey();
            String constant = e.getValue();

            String quoted = "\"" + literal + "\"";
            if (content.contains(quoted)) {
                content = content.replace(quoted, constant);
                modified = true;

                if (!existingConstants.contains(constant) && !newConstants.containsKey(constant)) {
                    newConstants.put(constant, literal);
                }
            }
        }

        if (modified) {
            // Ensure static import exists
            if (!content.contains(IMPORT_LINE)) {
                content = insertImport(content);
            }

            Files.write(file.toPath(), content.getBytes());
            System.out.println("📝 Updated: " + file.getPath());
        }
    }

    private static String insertImport(String content) {
        int idx = content.indexOf("package ");
        if (idx == -1) return content;
        int end = content.indexOf(";", idx);
        if (end == -1) return content;
        String before = content.substring(0, end + 1);
        String after = content.substring(end + 1);
        return before + "\n" + IMPORT_LINE + "\n" + after;
    }

    private static void collectJavaFiles(File dir, List<File> files) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) collectJavaFiles(f, files);
            else if (f.getName().endsWith(".java")) files.add(f);
        }
    }

    private StringConstantReplacer() {}
}