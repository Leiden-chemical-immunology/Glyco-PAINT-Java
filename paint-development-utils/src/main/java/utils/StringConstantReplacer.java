/*==============================================================================
 *  Class:        StringConstantReplacer.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Replaces specific string literals in Java files with constant names
 *    defined in PaintConstants.java. Uses an external list of approved
 *    replacements for safety.
 *
 *  DRY RUN MODE:
 *    If invoked with `--dry-run` as the first argument, no files are changed.
 *    The tool only reports which replacements and additions would occur.
 *
 *  FILE LIST MODE:
 *    If a file called "files_to_update.txt" exists (one filename per line),
 *    only those Java files are processed.
 *
 *  USAGE:
 *    java paint.shared.utils.StringConstantReplacer \
 *         [--dry-run] \
 *         /path/to/PaintConstants.java \
 *         /path/to/source/root \
 *         /path/to/strings_to_replace.txt
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

public final class StringConstantReplacer {

    private static final String IMPORT_LINE    = "import static paint.shared.constants.PaintConstants.*;";
    private static final String CONST_TEMPLATE = "public static final String %s = \"%s\";";
    private static boolean applyAllRemaining   = false; // for interactive approval

    public static void main(String[] args) throws Exception {
        boolean dryRun = false;
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        if (!arguments.isEmpty() && arguments.get(0).equals("--dry-run")) {
            dryRun = true;
            arguments.remove(0);
            System.out.println("🔎 Running in DRY-RUN mode — no files will be modified.\n");
        }

        Path constantsPath = Paths.get(
                "/Users/hans/JavaPaintProjects/Glyco-PAINT-Java/paint-shared-utils/src/main/java/paint/shared/constants/PaintConstants.java"
        );
        Path sourceRoot    = Paths.get("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");
        Path listFile      = Paths.get("/users/Hans/Downloads/strings_to_replace.txt");

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

        // Optional restriction: files_to_update.txt
        Path includeListPath = Paths.get("/Users/hans/Downloads/files_to_update.txt");
        Set<String> includeFiles = new HashSet<>();
        if (Files.exists(includeListPath)) {
            for (String line : Files.readAllLines(includeListPath)) {
                line = line.trim();
                if (!line.isEmpty()) includeFiles.add(line.toLowerCase());
            }
            System.out.println("📂 Restricting to files from list file: " + includeFiles + "\n");
        } else {
            System.out.println("⚠️  No files_to_update.txt found — all .java files will be scanned.\n");
        }

        // Traverse source tree
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceRoot.toFile(), javaFiles);

        Map<String, String> newConstants = new LinkedHashMap<>();

        for (File f : javaFiles) {
            if (f.getName().equals("PaintConstants.java")) continue;
            if (!includeFiles.isEmpty() && !includeFiles.contains(f.getName().toLowerCase())) continue;
            processFile(f, replacements, existingConstants, newConstants, dryRun);
        }

        // Insert new constants at correct section
        if (!newConstants.isEmpty()) {
            System.out.println("\n✳ Constants that would be added to PaintConstants.java:");
            for (Map.Entry<String, String> e : newConstants.entrySet()) {
                String def = String.format(CONST_TEMPLATE, e.getKey(), e.getValue());
                System.out.println("   + " + def);
            }

            if (!dryRun) {
                List<String> lines = Files.readAllLines(constantsPath);
                List<String> updated = new ArrayList<>();
                boolean inserted = false;

                for (String line : lines) {
                    // Detect the Filenames section marker (case-insensitive)
                    if (!inserted && line.toLowerCase().contains("filenames")) {
                        // Insert constants just BEFORE the Filenames section
                        updated.add("// Auto-added constants (Column Names)");
                        for (Map.Entry<String, String> e : newConstants.entrySet()) {
                            String def = String.format(CONST_TEMPLATE, e.getKey(), e.getValue());
                            updated.add(def);
                        }
                        updated.add(""); // blank line after inserted constants
                        inserted = true;
                    }
                    updated.add(line);
                }

                if (!inserted) {
                    // fallback: append at end of file
                    updated.add("\n// Auto-added constants (fallback)");
                    for (Map.Entry<String, String> e : newConstants.entrySet()) {
                        String def = String.format(CONST_TEMPLATE, e.getKey(), e.getValue());
                        updated.add(def);
                    }
                }

                Files.write(constantsPath, updated, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("✅ Constants inserted before the 'Filenames' section in PaintConstants.java");
            }
        }

        if (dryRun) {
            System.out.println("\n✅ DRY-RUN complete — no files were changed.");
        } else {
            System.out.println("\n✅ Done. Updated files and constants where needed.");
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────

    private static void processFile(File file,
            Map<String, String> replacements,
            Set<String> existingConstants,
            Map<String, String> newConstants,
            boolean dryRun) throws IOException {

        if (!file.getName().endsWith(".java")) return;

        String content = new String(Files.readAllBytes(file.toPath()));
        boolean modified = false;
        boolean needsImport = false;
        StringBuilder report = new StringBuilder();

        for (Map.Entry<String, String> e : replacements.entrySet()) {
            String literal = e.getKey();
            String constant = e.getValue();

            String quoted = "\"" + literal + "\"";
            if (content.contains(quoted)) {
                modified = true;
                report.append("   → Replace ").append(quoted)
                      .append(" → ").append(constant).append("\n");

                if (!existingConstants.contains(constant) && !newConstants.containsKey(constant)) {
                    newConstants.put(constant, literal);
                }

                if (!dryRun) {
                    content = content.replace(quoted, constant);
                }
            }
        }

        if (modified) {
            if (!content.contains(IMPORT_LINE)) needsImport = true;

            System.out.println("\n📝 " + (dryRun ? "[DRY]" : "[MOD]") + " " + file.getPath());
            System.out.print(report.toString());
            if (needsImport) System.out.println("   → Would add import: " + IMPORT_LINE);

            // Interactive confirmation (only in real mode)
            if (!dryRun && !applyAllRemaining) {
                System.out.print("Apply changes to this file? [y]es / [n]o / [a]ll / [q]uit: ");
                Scanner sc = new Scanner(System.in);
                String answer = sc.nextLine().trim().toLowerCase();

                if (answer.equals("q")) {
                    System.out.println("❌ Aborted by user.");
                    System.exit(0);
                } else if (answer.equals("a")) {
                    applyAllRemaining = true;
                } else if (!answer.equals("y")) {
                    System.out.println("⏭ Skipped: " + file.getName());
                    return;
                }
            }

            if (!dryRun) {
                if (needsImport) content = insertImport(content);
                Files.write(file.toPath(), content.getBytes());
                System.out.println("✅ Changes applied to: " + file.getName());
            }
        }
    }

    private static String insertImport(String content) {
        int idx = content.indexOf("package ");
        if (idx == -1) return content;
        int end = content.indexOf(";", idx);
        if (end == -1) return content;
        String before = content.substring(0, end + 1);
        String after  = content.substring(end + 1);
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