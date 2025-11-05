package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

/**
 * ===============================================================
 *  POM REFORMATTER UTILITY (BLOCK-AWARE, 4-SPACE INDENT)
 * ===============================================================
 * PURPOSE:
 *   - 4-space indentation (forced)
 *   - Remove all blank/whitespace-only lines
 *   - Add one blank line before a consecutive comment block
 *   - Add one blank line after </dependency> and </repository>
 *
 * AUTHOR: Herr Doctor
 * MODULE: paint-development-utils
 * UPDATED: 2025-11-05
 * ===============================================================
 */
public class ReformatPoms {

    private static final File ROOT_DIR =
            new File("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java-clean");

    public static void main(String[] args) {
        System.out.println("🔍 Searching for pom.xml files under: " + ROOT_DIR.getAbsolutePath());
        reformatAllPoms(ROOT_DIR);
        System.out.println("✨ All POMs processed.");
    }

    private static void reformatAllPoms(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                reformatAllPoms(f);
            } else if ("pom.xml".equalsIgnoreCase(f.getName())) {
                reformatPom(f);
            }
        }
    }

    private static void reformatPom(File pomFile) {
        try {
            String original = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);

            // Validate XML
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(false);
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(original.getBytes(StandardCharsets.UTF_8)));
            doc.normalizeDocument();

            // Pretty-print
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            t.setOutputProperty(OutputKeys.METHOD, "xml");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.transform(new DOMSource(doc), new StreamResult(baos));
            String pretty = baos.toString("UTF-8");

            // Post-process spacing
            String cleaned = removeBlankLines(pretty);
            String withCommentBlocks = addBlankBeforeCommentBlocks(cleaned);
            String withBlockSpacing = addBlankAfterClosingTags(withCommentBlocks);
            String finalText = adjustIndentationWidth(withBlockSpacing, 4);

            Files.write(pomFile.toPath(), finalText.getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ Reformatted: " + pomFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error reformatting " + pomFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    /** Removes blank or whitespace-only lines. */
    private static String removeBlankLines(String text) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) sb.append(line).append("\n");
            }
        } catch (IOException ignore) {}
        return sb.toString();
    }

    /**
     * Adds exactly one blank line before a block of consecutive comments.
     * Does NOT add blank lines inside or after the comment block.
     */
    private static String addBlankBeforeCommentBlocks(String text) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\\r?\\n");
        boolean previousWasComment = false;
        boolean inCommentBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isComment = line.trim().startsWith("<!--");

            // Detect start of a comment block
            if (isComment && !previousWasComment) {
                // Add a blank line before this block (if not at top)
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') sb.append('\n');
                sb.append('\n');
                inCommentBlock = true;
            }

            sb.append(line).append('\n');
            previousWasComment = isComment;

            // End of comment block if next line is not a comment
            if (inCommentBlock) {
                if (i + 1 < lines.length && !lines[i + 1].trim().startsWith("<!--")) {
                    inCommentBlock = false;
                }
            }
        }

        // Normalize excessive newlines
        return sb.toString().replaceAll("(?m)\\n{3,}", "\n\n");
    }


    /** Adds exactly one blank line after </dependency> and </repository>. */
    private static String addBlankAfterClosingTags(String text) {
        text = text.replaceAll("(?m)</dependency>\\r?\\n(?!\\r?\\n)", "</dependency>\n\n");
        text = text.replaceAll("(?m)</repository>\\r?\\n(?!\\r?\\n)", "</repository>\n\n");
        text = text.replaceAll("(?m)(</dependency>|</repository>)\\r?\\n{3,}", "$1\n\n");
        return text;
    }

    /**
     * Ensures consistent 4-space indentation, regardless of the transformer.
     */
    private static String adjustIndentationWidth(String text, int targetWidth) {
        int[] counts = new int[9];
        String[] lines = text.split("\\r?\\n", -1);
        for (String line : lines) {
            int i = 0;
            while (i < line.length() && line.charAt(i) == ' ') i++;
            if (i > 0 && i < counts.length) counts[i]++;
        }
        int base = 0, max = 0;
        for (int i = 1; i < counts.length; i++) {
            if (counts[i] > max) { max = counts[i]; base = i; }
        }
        if (base == 0) base = 2;

        StringBuilder out = new StringBuilder(text.length());
        for (String line : lines) {
            int i = 0;
            while (i < line.length() && line.charAt(i) == ' ') i++;
            if (i == 0) {
                out.append(line).append('\n');
                continue;
            }
            int level = Math.round((float) i / base);
            int newSpaces = level * targetWidth;
            for (int s = 0; s < newSpaces; s++) out.append(' ');
            out.append(line.substring(i)).append('\n');
        }
        return out.toString();
    }
}