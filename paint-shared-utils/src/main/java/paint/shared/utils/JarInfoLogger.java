package paint.shared.utils;

import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JarInfoLogger {

    private static final ZoneId AMS = ZoneId.of("Europe/Amsterdam");
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(AMS);

    public static JarInfo getJarInfo(Class<?> clazz) {
        try {
            // 1) Try reading MANIFEST from the jar that loaded this class (works in fat jar)
            Manifest manifest = manifestFromCodeSource(clazz);
            if (manifest == null) {
                // 2) Fall back: read first MANIFEST on the classpath (works in IDE)
                manifest = manifestFromClasspath(clazz);
            }
            if (manifest == null) {
                return null;
            }

            Attributes a = manifest.getMainAttributes();

            String ts = a.getValue("Build-Timestamp"); // optional
            String formattedTs = formatAmsterdam(ts);   // returns null if absent/unparseable

            return new JarInfo(
                    a.getValue("Implementation-Title"),
                    a.getValue("Implementation-Version"),
                    a.getValue("Implementation-Vendor"),
                    formattedTs,
                    a.getValue("Specification-Title"),
                    a.getValue("Specification-Version"),
                    a.getValue("Specification-Vendor")
            );
        } catch (Exception e) {
            PaintLogger.errorf("JarInfo: %s", e);
            return null;
        }
    }

    private static Manifest manifestFromCodeSource(Class<?> clazz) {
        try {
            CodeSource cs = clazz.getProtectionDomain().getCodeSource();
            if (cs == null) {
                return null;
            }
            URL loc = cs.getLocation(); // jar:file:/.../app.jar or file:/.../classes/
            String path = loc.toString();
            if (path.endsWith(".jar") || path.contains(".jar!")) {
                // Open the jar directly
                try (JarFile jf = new JarFile(loc.getPath().replace("file:", ""))) {
                    return jf.getManifest();
                } catch (Exception ignored) {
                    // If that fails (e.g., shaded jar path nuances), try jar URL to MANIFEST
                    String jarUrl = (path.endsWith(".jar") ? "jar:" + path + "!/META-INF/MANIFEST.MF"
                            : path.substring(0, path.indexOf("!")) + "!/META-INF/MANIFEST.MF");
                    try (InputStream is = new URL(jarUrl).openStream()) {
                        return new Manifest(is);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static Manifest manifestFromClasspath(Class<?> clazz) {
        try {
            ClassLoader cl = clazz.getClassLoader();
            Enumeration<URL> en = cl.getResources("META-INF/MANIFEST.MF");
            while (en.hasMoreElements()) {
                URL u = en.nextElement();
                // Pick the first one; optionally filter by package/jar name if you have many
                try (InputStream is = u.openStream()) {
                    return new Manifest(is);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static String formatAmsterdam(String utcString) {
        if (utcString == null || utcString.trim().isEmpty()) {
            return null;
        }

        // Try strict ISO-8601 first (e.g. 2025-10-14T06:39:16Z)
        try {
            Instant ins = Instant.parse(utcString.trim());
            return OUT_FMT.format(ins);
        } catch (DateTimeParseException ignored) {}

        // Try "yyyy-MM-dd HH:mm:ss" (assume UTC)
        try {
            LocalDateTime ldt = LocalDateTime.parse(utcString.trim(),
                                                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            return OUT_FMT.format(ldt.toInstant(ZoneOffset.UTC));
        } catch (DateTimeParseException ignored) {}

        // "yyyy-MM-dd'T'HH:mm:ssXX" (e.g. +02:00)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(utcString.trim(),
                                                      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            return OUT_FMT.format(odt.toInstant());
        } catch (DateTimeParseException ignored) {}

        // Give up gracefully
        return null;
    }

    public static void main(String[] args) {
        JarInfo info = getJarInfo(JarInfoLogger.class);
        System.out.println(Objects.toString(info, "No manifest information found."));
    }
}