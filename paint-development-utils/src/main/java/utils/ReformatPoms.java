package utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * ===============================================================
 *  POM REFORMATTER UTILITY (TRUE STRUCTURAL INDENTATION)
 * ===============================================================
 * PURPOSE:
 *   - 4-space indentation with correct nesting
 *   - Remove blank/whitespace-only lines
 *   - Add one blank line before a comment block
 *   - Add one blank line after </dependency> and </repository>
 *
 * AUTHOR: Herr Doctor
 * MODULE: paint-development-utils
 * UPDATED: 2025-11-05
 * ===============================================================
 */
public class ReformatPoms {

    private static final File ROOT_DIR =
            new File("/Users/hans/JavaPaintProjects/Glyco-PAINT-Java");

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
            if (f.isDirectory()) reformatAllPoms(f);
            else if ("pom.xml".equalsIgnoreCase(f.getName())) reformatPom(f);
        }
    }

    private static void reformatPom(File pomFile) {
        try {
            String original = new String(Files.readAllBytes(pomFile.toPath()), StandardCharsets.UTF_8);

            // Parse DOM, preserving comments
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(false);
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(original.getBytes(StandardCharsets.UTF_8)));
            doc.normalizeDocument();

            // Build formatted XML manually
            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
            formatNode(doc.getDocumentElement(), xml, 0);

            // Clean up spacing rules
            String cleaned = removeBlankLines(xml.toString());
            String withComments = addBlankBeforeCommentBlocks(cleaned);
            String withBlocks = addBlankAfterClosingTags(withComments);

            Files.write(pomFile.toPath(), withBlocks.getBytes(StandardCharsets.UTF_8));
            System.out.println("✅ Reformatted: " + pomFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error reformatting " + pomFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    /** Recursive XML printer with inline text for simple values and 4-space nesting. */
    private static void formatNode(Node node, StringBuilder out, int level) {
        String indent = repeat(' ', level * 4);

        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                out.append(indent).append("<").append(node.getNodeName());

                // Write attributes inline
                NamedNodeMap attrs = node.getAttributes();
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node a = attrs.item(i);
                    out.append(" ").append(a.getNodeName()).append("=\"")
                            .append(a.getNodeValue()).append("\"");
                }

                NodeList children = node.getChildNodes();
                if (children.getLength() == 0) {
                    out.append("/>\n");
                    return;
                }

                // If only one text node
                if (children.getLength() == 1 && children.item(0).getNodeType() == Node.TEXT_NODE) {
                    String text = children.item(0).getTextContent().trim();
                    if (text.contains("\n") || text.length() > 80) {
                        // multiline or long text -> block format
                        out.append(">\n")
                                .append(indent).append("    ").append(text).append("\n")
                                .append(indent).append("</").append(node.getNodeName()).append(">\n");
                    } else {
                        // short -> inline
                        out.append(">").append(text)
                                .append("</").append(node.getNodeName()).append(">\n");
                    }
                    return;
                }

                // Otherwise recurse for child elements
                out.append(">\n");
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child.getNodeType() == Node.TEXT_NODE &&
                            child.getTextContent().trim().isEmpty())
                        continue; // skip whitespace text
                    formatNode(child, out, level + 1);
                }
                out.append(indent).append("</").append(node.getNodeName()).append(">\n");
                break;

            case Node.COMMENT_NODE:
                out.append(indent).append("<!-- ").append(node.getNodeValue().trim()).append(" -->\n");
                break;

            default:
                break;
        }
    }
    private static String removeBlankLines(String text) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = br.readLine()) != null)
                if (!line.trim().isEmpty()) sb.append(line).append("\n");
        } catch (IOException ignore) {}
        return sb.toString();
    }

    /** Add one blank line before a comment block, not inside or after. */
    private static String addBlankBeforeCommentBlocks(String text) {
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\\r?\\n");
        boolean prevIsComment = false;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isComment = line.trim().startsWith("<!--");
            if (isComment && !prevIsComment) sb.append("\n"); // before block
            sb.append(line).append("\n");
            prevIsComment = isComment;
        }
        return sb.toString().replaceAll("(?m)\\n{3,}", "\n\n");
    }

    /** Adds one blank line after </dependency> and </repository>. */
    private static String addBlankAfterClosingTags(String text) {
        text = text.replaceAll("(?m)</dependency>\\r?\\n(?!\\r?\\n)", "</dependency>\n\n");
        text = text.replaceAll("(?m)</repository>\\r?\\n(?!\\r?\\n)", "</repository>\n\n");
        text = text.replaceAll("(?m)(</dependency>|</repository>)\\r?\\n{3,}", "$1\n\n");
        return text;
    }

    /** Java 8-compatible repeat for spaces. */
    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        char[] chars = new char[count];
        java.util.Arrays.fill(chars, c);
        return new String(chars);
    }
}