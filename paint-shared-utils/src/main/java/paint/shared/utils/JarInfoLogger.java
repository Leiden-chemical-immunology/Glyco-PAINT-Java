package paint.shared.utils;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class JarInfoLogger {

    public static JarInfo getJarInfo(Class<?> clazz) {

        try {
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();

            if (!classPath.startsWith("jar")) {
                return null; // Not in a JAR
            }

            String manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF";
            URL manifestUrl = new URL(manifestPath);
            try (InputStream is = manifestUrl.openStream()) {
                Manifest manifest = new Manifest(is);
                Attributes attrs = manifest.getMainAttributes();

                return new JarInfo(
                        attrs.getValue("Implementation-Title"),
                        attrs.getValue("Implementation-Version"),
                        attrs.getValue("Implementation-Vendor"),
                        attrs.getValue("Implementation-Date"),
                        attrs.getValue("Specification-Title"),
                        attrs.getValue("Specification-Version"),
                        attrs.getValue("Specification-Vendor")
                );
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        JarInfo info = getJarInfo(JarInfoLogger.class);
        if (info != null) {
            System.out.println(info);
        } else {
            System.out.println("No manifest information found.");
        }
    }
}