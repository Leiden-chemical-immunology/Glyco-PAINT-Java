package utils;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/**
 * ===============================================================
 *  POM REFORMATTER UTILITY (FORCE REINDENT)
 * ===============================================================
 * PURPOSE:
 *   Fully reformat all pom.xml files under a fixed root directory.
 *
 * DESCRIPTION:
 *   Removes all indentation whitespace nodes, then reprints
 *   the document with consistent 2-space indentation.
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

        for (File file : files) {
            if (file.isDirectory()) {
                reformatAllPoms(file);
            } else if ("pom.xml".equalsIgnoreCase(file.getName())) {
                reformatPom(file);
            }
        }
    }

    private static void reformatPom(File pomFile) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(false);
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(pomFile);
            doc.normalizeDocument();

            // Remove all indentation text nodes
            removeWhitespaceNodes(doc.getDocumentElement());

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            try (FileOutputStream out = new FileOutputStream(pomFile)) {
                transformer.transform(new DOMSource(doc), new StreamResult(out));
            }

            System.out.println("✅ Reformatted: " + pomFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("❌ Error reformatting " + pomFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private static void removeWhitespaceNodes(Element element) {
        NodeList children = element.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getTextContent().trim().isEmpty()) {
                    element.removeChild(child);
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeWhitespaceNodes((Element) child);
            }
        }
    }
}