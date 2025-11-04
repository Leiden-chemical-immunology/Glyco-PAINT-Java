package utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.util.Base64;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 * MakeGlycoPaintInstaller
 *
 * PURPOSE:
 *   Creates a cross-platform, self-extracting Glyco-PAINT installer
 *   that bundles all desktop applications and the Fiji plugin into
 *   a single, portable Bash script.
 *
 * OUTPUT:
 *   ~/Downloads/Glyco-PAINT-Installer.sh
 */
public class MakeGlycoPaintInstaller {

    private static final Path MAC_APP_DIR = Paths.get(System.getProperty("user.home"), "Applications", "Glyco-PAINT");
    private static final Path WIN_APP_DIR = Paths.get(System.getProperty("user.home"), "AppData", "Local", "Glyco-PAINT");
    private static final Path FIJI_TARGET_DIR = Paths.get(System.getProperty("user.home"),
                                                          "JavaPaintProjects", "paint-fiji-plugin", "target");
    private static final Path OUTPUT_INSTALLER = Paths.get(System.getProperty("user.home"),
                                                           "Downloads", "Glyco-PAINT-Installer.sh");

    public static void main(String[] args) throws IOException {
        System.out.println("📦 Creating Glyco-PAINT self-extracting installer...");

        boolean isMac = System.getProperty("os.name").toLowerCase().contains("mac");
        Path installSource = isMac ? MAC_APP_DIR : WIN_APP_DIR;

        if (!Files.isDirectory(installSource)) {
            System.err.println("❌ Directory not found: " + installSource);
            System.exit(1);
        }

        // Find the Fiji fat JAR
        Optional<Path> fijiJarOpt = Files.list(FIJI_TARGET_DIR)
                .filter(p -> p.getFileName().toString().endsWith("-jar-with-dependencies.jar"))
                .findFirst();

        if (!fijiJarOpt.isPresent()) {
            System.err.println("❌ No fat JAR found in: " + FIJI_TARGET_DIR);
            System.exit(1);
        }

        Path fijiJar = fijiJarOpt.get();
        System.out.println("🧩 Found Fiji JAR: " + fijiJar.getFileName());

        // Create temporary directory
        Path tmpDir = Files.createTempDirectory("glyco-paint-installer");
        Path payloadDir = tmpDir.resolve("Glyco-PAINT");
        Files.createDirectories(payloadDir);

        // Copy Glyco-PAINT folder and plugin JAR
        copyRecursive(installSource, payloadDir);
        Files.copy(fijiJar, tmpDir.resolve(fijiJar.getFileName()), StandardCopyOption.REPLACE_EXISTING);

        // Create compressed payload
        Path payloadTarGz = tmpDir.resolve("payload.tar.gz");
        createTarGz(tmpDir, payloadTarGz, Arrays.asList("Glyco-PAINT", fijiJar.getFileName().toString()));

        // Generate installer script
        try (BufferedWriter writer = Files.newBufferedWriter(OUTPUT_INSTALLER)) {
            writer.write(INSTALLER_HEADER);
            writer.newLine();
            writer.write("__ARCHIVE_BELOW__");
            writer.newLine();

            byte[] payload = Files.readAllBytes(payloadTarGz);
            String encoded = Base64.getEncoder().encodeToString(payload);

            // Write Base64 in 76-character lines for readability
            for (int i = 0; i < encoded.length(); i += 76) {
                int end = Math.min(i + 76, encoded.length());
                writer.write(encoded, i, end - i);
                writer.newLine();
            }
        }

        // Make script executable
        OUTPUT_INSTALLER.toFile().setExecutable(true);

        // Clean up
        deleteRecursive(tmpDir);

        System.out.println("\n✅ Created cross-platform self-extracting installer:");
        System.out.println("   " + OUTPUT_INSTALLER);
        System.out.println("\n💡 To install, run:");
        System.out.println("   bash " + OUTPUT_INSTALLER.getFileName());
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static void copyRecursive(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path dest = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {}
                });
    }

    private static void createTarGz(Path baseDir, Path output, List<String> entries) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(output.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GZIPOutputStream gos = new GZIPOutputStream(bos);
             TarArchiveOutputStream tos = new TarArchiveOutputStream(gos)) {

            tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            for (String entryName : entries) {
                Path entryPath = baseDir.resolve(entryName);
                addToTar(tos, entryPath, entryName);
            }
        }
    }

    private static void addToTar(TarArchiveOutputStream tos, Path path, String entryName) throws IOException {
        if (Files.isDirectory(path)) {
            TarArchiveEntry dirEntry = new TarArchiveEntry(entryName + "/");
            tos.putArchiveEntry(dirEntry);
            tos.closeArchiveEntry();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path child : stream) {
                    addToTar(tos, child, entryName + "/" + child.getFileName().toString());
                }
            }
        } else {
            TarArchiveEntry entry = new TarArchiveEntry(path.toFile(), entryName);
            tos.putArchiveEntry(entry);
            Files.copy(path, tos);
            tos.closeArchiveEntry();
        }
    }

    // -------------------------------------------------------------------------
    // Embedded Bash installer header (verbatim)
    // -------------------------------------------------------------------------
    private static final String INSTALLER_HEADER = String.join("\n", Arrays.asList(
            "#!/bin/bash",
            "set -e",
            "",
            "echo \"🧬 Glyco-PAINT Installer\"",
            "echo \"=========================\"",
            "",
            "OS=\"$(uname -s)\"",
            "if [[ \"$OS\" == \"Darwin\" ]]; then",
            "  INSTALL_APPS=\"$HOME/Applications/Glyco-PAINT\"",
            "else",
            "  INSTALL_APPS=\"$HOME/AppData/Local/Glyco-PAINT\"",
            "fi",
            "",
            "TMP_EXTRACT=$(mktemp -d)",
            "cleanup() { rm -rf \"$TMP_EXTRACT\"; }",
            "trap cleanup EXIT",
            "",
            "# Extract embedded payload",
            "PAYLOAD_LINE=$(awk '/^__ARCHIVE_BELOW__/ {print NR + 1; exit 0; }' \"$0\")",
            "tail -n +$PAYLOAD_LINE \"$0\" | base64 --decode | tar -xz -C \"$TMP_EXTRACT\"",
            "",
            "echo \"\"",
            "echo \"📂 Installing Glyco-PAINT to:\"",
            "echo \"   $INSTALL_APPS\"",
            "mkdir -p \"$(dirname \"$INSTALL_APPS\")\"",
            "rm -rf \"$INSTALL_APPS\"",
            "cp -R \"$TMP_EXTRACT/Glyco-PAINT\" \"$INSTALL_APPS\"",
            "",
            "echo \"\"",
            "echo \"🔍 Looking for Fiji.app ...\"",
            "OS=\"$(uname -s)\"",
            "if [[ \"$OS\" == \"Darwin\" ]]; then",
            "  OPTIONS=(\"/Applications/Fiji.app\" \"$HOME/Applications/Fiji.app\")",
            "else",
            "  OPTIONS=(\"/c/Program Files/Fiji.app\" \"/c/Program Files (x86)/Fiji.app\" \"$HOME/AppData/Local/Fiji.app\")",
            "fi",
            "",
            "FOUND_PATH=\"\"",
            "for opt in \"${OPTIONS[@]}\"; do",
            "  if [ -d \"$opt\" ]; then",
            "    FOUND_PATH=\"$opt\"",
            "    break",
            "  fi",
            "done",
            "",
            "if [ -n \"$FOUND_PATH\" ]; then",
            "  echo \"✅ Found Fiji.app at: $FOUND_PATH\"",
            "  FIJI_APP=\"$FOUND_PATH\"",
            "else",
            "  echo \"Please choose where Fiji.app is installed:\"",
            "  read -rp \"Enter full path to Fiji.app: \" FIJI_APP",
            "fi",
            "",
            "if [ ! -d \"$FIJI_APP\" ]; then",
            "  echo \"❌ Invalid path: $FIJI_APP\"",
            "  exit 1",
            "fi",
            "",
            "FIJI_PLUGINS=\"$FIJI_APP/plugins\"",
            "mkdir -p \"$FIJI_PLUGINS\"",
            "JAR_FILE=$(find \"$TMP_EXTRACT\" -type f -name '*-jar-with-dependencies.jar' | head -n 1)",
            "echo \"🔌 Installing $(basename \"$JAR_FILE\") to $FIJI_PLUGINS ...\"",
            "cp \"$JAR_FILE\" \"$FIJI_PLUGINS/\"",
            "",
            "echo \"\"",
            "echo \"✅ Installation complete!\"",
            "echo \"   • Glyco-PAINT: $INSTALL_APPS\"",
            "echo \"   • Fiji plugin: $FIJI_PLUGINS\"",
            "exit 0"
    ));
}