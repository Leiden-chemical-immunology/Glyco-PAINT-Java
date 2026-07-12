/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package release.support;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Utility methods for updating version information in Maven pom.xml files.
 */
public final class PomUtils {
    private PomUtils() {
    }

    public static String getVersionFromPom(Path pomPath) {
        try (InputStream in = Files.newInputStream(pomPath)) {
            DocumentBuilder b    = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document        doc  = b.parse(in);
            NodeList        list = doc.getElementsByTagName("version");
            if (list.getLength() > 0) {
                return list.item(0).getTextContent().trim();
            }
        } catch (Exception e) {
            System.err.println("⚠️  Could not read version from " + pomPath + ": " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("SameParameterValue")
    public static void removeSnapshotFromAllPoms(Path base) throws IOException {
        try (java.util.stream.Stream<Path> files = Files.walk(base)) {
            files.filter(p -> p.getFileName().toString().equals("pom.xml"))
                    .forEach(p -> {
                        try {
                            String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                            String cleaned = text.replaceAll("-SNAPSHOT", "");
                            if (!text.equals(cleaned)) {
                                Files.write(p, cleaned.getBytes(StandardCharsets.UTF_8));
                                System.out.println("🧹 Cleaned -SNAPSHOT from " + p);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    public static VersionInfo computeVersions(String currentVersion) {
        String base = currentVersion.replace("-SNAPSHOT", "").trim();
        String[] parts = base.split("\\.");
        int lastNum = Integer.parseInt(parts[parts.length - 1]);
        int next = lastNum + 1;

        String prefix = "";
        if (parts.length > 1) {
            prefix = String.join(".", Arrays.copyOf(parts, parts.length - 1)) + ".";
        }

        String releaseVersion = prefix + lastNum;
        String nextDevVersion = prefix + next + "-SNAPSHOT";
        return new VersionInfo(releaseVersion, nextDevVersion);
    }
}