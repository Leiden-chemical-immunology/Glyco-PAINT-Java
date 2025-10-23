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

/**
 * Utility for extracting and displaying build metadata from a JAR’s {@code MANIFEST.MF}.
 * <p>
 * Reads manifest entries such as:
 * <ul>
 *   <li>{@code Implementation-Title}</li>
 *   <li>{@code Implementation-Version}</li>
 *   <li>{@code Implementation-Vendor}</li>
 *   <li>{@code Build-Timestamp}</li>
 * </ul>
 * Converts UTC timestamps to Europe/Amsterdam local time for display.
 * Works in both IDE (classpath) and packaged JAR executions.
 */
public class JarInfoLogger {

    private static final ZoneId AMS = ZoneId.of("Europe/Amsterdam");
    private static final DateTimeFormatter OUT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(AMS);

    /** Prevent instantiation of this static utility class. */
    private JarInfoLogger() {
        // static utility
    }

    /**
     * Reads JAR manifest information for the given class.
     *
     * @param clazz any class from the JAR whose manifest should be read
     * @return a {@link JarInfo} record containing manifest data, or {@code null} if not found
     */
    public static JarInfo getJarInfo(Class<?> clazz) {
        try {
            // Try to load from the JAR that defined this class
            Manifest manifest = manifestFromCodeSource(clazz);
            if (manifest == null) {
                // Fallback: first MANIFEST.MF on classpath
                manifest = manifestFromClasspath(clazz);
            }
            if (manifest == null) {
                return null;
            }

            Attributes attribute = manifest.getMainAttributes();
            String ts = attribute.getValue("Build-Timestamp"); // optional
            String formattedTs = formatAmsterdam(ts);

            return new JarInfo(
                    attribute.getValue("Implementation-Title"),
                    attribute.getValue("Implementation-Version"),
                    attribute.getValue("Implementation-Vendor"),
                    formattedTs,
                    attribute.getValue("Specification-Title"),
                    attribute.getValue("Specification-Version"),
                    attribute.getValue("Specification-Vendor")
            );
        } catch (Exception e) {
            PaintLogger.errorf("JarInfoLogger.getJarInfo(): %s", e);
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    /**
     * Attempts to read a manifest directly from the JAR file associated with the given class.
     *
     * @param clazz the class whose code source will be used to locate the JAR
     * @return the parsed {@link Manifest}, or {@code null} if unavailable
     */
    private static Manifest manifestFromCodeSource(Class<?> clazz) {
        try {
            CodeSource cs = clazz.getProtectionDomain().getCodeSource();
            if (cs == null) return null;

            URL loc = cs.getLocation(); // jar:file:/.../app.jar or file:/.../classes/
            String path = loc.toString();

            if (path.endsWith(".jar") || path.contains(".jar!")) {
                try (JarFile jf = new JarFile(loc.getPath().replace("file:", ""))) {
                    return jf.getManifest();
                } catch (Exception ignored) {
                    String jarUrl = (path.endsWith(".jar")
                            ? "jar:" + path + "!/META-INF/MANIFEST.MF"
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

    /**
     * Attempts to read the first manifest file found on the classpath.
     *
     * @param clazz the class providing a {@link ClassLoader} to search with
     * @return the first {@link Manifest} found, or {@code null} if none
     */
    private static Manifest manifestFromClasspath(Class<?> clazz) {
        try {
            ClassLoader cl = clazz.getClassLoader();
            Enumeration<URL> en = cl.getResources("META-INF/MANIFEST.MF");
            while (en.hasMoreElements()) {
                URL u = en.nextElement();
                try (InputStream is = u.openStream()) {
                    return new Manifest(is);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Converts a UTC timestamp string to Amsterdam local time.
     * <p>
     * Supported input formats:
     * <ul>
     *   <li>ISO 8601 (e.g., {@code 2025-10-14T06:39:16Z})</li>
     *   <li>{@code yyyy-MM-dd HH:mm:ss} (assumed UTC)</li>
     *   <li>{@code yyyy-MM-dd'T'HH:mm:ssXXX}</li>
     * </ul>
     *
     * @param utcString raw timestamp string from the manifest
     * @return formatted string (e.g., {@code "2025-10-14 08:39:16 CET"}), or {@code null} if invalid
     */
    public static String formatAmsterdam(String utcString) {
        if (utcString == null || utcString.trim().isEmpty()) return null;

        // Try ISO 8601 (e.g. 2025-10-14T06:39:16Z)
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

        // Try "yyyy-MM-dd'T'HH:mm:ssXXX"
        try {
            OffsetDateTime odt = OffsetDateTime.parse(utcString.trim(),
                                                      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
            return OUT_FMT.format(odt.toInstant());
        } catch (DateTimeParseException ignored) {}

        return null;
    }

    /**
     * Prints manifest information for this utility’s own class (for manual CLI testing).
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        JarInfo info = getJarInfo(JarInfoLogger.class);
        System.out.println(Objects.toString(info, "No manifest information found."));
    }

    // ---------------------------------------------------------------------
    // Nested data record
    // ---------------------------------------------------------------------

    /**
     * Immutable data container for JAR manifest metadata.
     */
    public static class JarInfo {
        public final String implementationTitle;
        public final String implementationVersion;
        public final String implementationVendor;
        public final String implementationDate;

        public final String specificationTitle;
        public final String specificationVersion;
        public final String specificationVendor;

        /**
         * Constructs a new {@code JarInfo} instance.
         *
         * @param implTitle    implementation title
         * @param implVersion  implementation version
         * @param implVendor   implementation vendor
         * @param implDate     implementation build date/time
         * @param specTitle    specification title
         * @param specVersion  specification version
         * @param specVendor   specification vendor
         */
        public JarInfo(String implTitle,
                       String implVersion,
                       String implVendor,
                       String implDate,
                       String specTitle,
                       String specVersion,
                       String specVendor) {

            // @formatter:off
            this.implementationTitle   = implTitle;
            this.implementationVersion = implVersion;
            this.implementationVendor  = implVendor;
            this.implementationDate    = implDate;

            this.specificationTitle    = specTitle;
            this.specificationVersion  = specVersion;
            this.specificationVendor   = specVendor;
            // @formatter:on
        }

        /** @return a human-readable short summary of implementation metadata. */
        @Override
        public String toString() {
            return String.format(
                    "Implementation Title  : %s%n" +
                            "Implementation Version: %s%n" +
                            "Implementation Vendor : %s%n" +
                            "Implementation Date   : %s%n",
                    implementationTitle, implementationVersion,
                    implementationVendor, implementationDate
            );
        }

        /** @return a detailed summary including specification metadata. */
        public String toStringLONG() {
            return String.format(
                    "Implementation Title  : %s%n" +
                            "Implementation Version: %s%n" +
                            "Implementation Vendor : %s%n" +
                            "Implementation Date   : %s%n" +
                            "Specification Title   : %s%n" +
                            "Specification Version : %s%n" +
                            "Specification Vendor  : %s",
                    implementationTitle, implementationVersion, implementationVendor,
                    implementationDate, specificationTitle, specificationVersion, specificationVendor
            );
        }
    }
}