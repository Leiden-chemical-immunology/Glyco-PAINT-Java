import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GlycoPaintInstallerWindows {

    private static final String PRODUCT_NAME = "Glyco-PAINT";
    private static final String PAYLOAD_NAME = "/payload.zip";
    private static final String[] FIJI_PATHS = {
            System.getenv("ProgramFiles") + "\\Fiji.app",
            System.getenv("ProgramFiles(x86)") + "\\Fiji.app",
            System.getenv("LOCALAPPDATA") + "\\Fiji.app"
    };

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;
    private Path installRoot;
    private String version = "unknown";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerWindows().show());
    }

    private GlycoPaintInstallerWindows() {
        detectVersion();

        frame = new JFrame("Install " + PRODUCT_NAME + " " + version);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 400);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        frame.add(scroll, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));

        progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);

        closeButton = new JButton("Close");
        closeButton.setEnabled(false);
        closeButton.addActionListener(e -> System.exit(0));

        bottomPanel.add(progress, BorderLayout.CENTER);
        bottomPanel.add(closeButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

    /** Detect version number from embedded JAR filenames inside payload.zip */
    private void detectVersion() {
        try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME);
             ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            Pattern versionPattern = Pattern.compile("paint-[a-zA-Z-]+-([0-9]+\\.[0-9]+(\\.[0-9]+)?)");
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                Matcher m = versionPattern.matcher(name);
                if (m.find()) {
                    version = m.group(1);
                    break;
                }
            }
        } catch (IOException ignored) {}
    }

    private void show() {
        // Default suggestion: ~/Applications/Glyco-PAINT
        Path defaultDir = Paths.get(System.getProperty("user.home"), "Applications", PRODUCT_NAME);
        try { Files.createDirectories(defaultDir.getParent()); } catch (IOException ignored) {}

        JFileChooser chooser = new JFileChooser(defaultDir.getParent().toFile());
        chooser.setDialogTitle("Choose installation location for " + PRODUCT_NAME);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setSelectedFile(defaultDir.toFile());

        int result = chooser.showDialog(frame, "Install here");
        if (result != JFileChooser.APPROVE_OPTION) {
            System.exit(0);
            return;
        }

        // --- Determine the actual chosen directory correctly ---
        File selected = chooser.getSelectedFile();
        File current  = chooser.getCurrentDirectory();

        // If the user created a new directory or typed one manually
        if (selected == null || !selected.exists()) {
            selected = current;
        }

        // Make sure we end up with a valid directory
        if (selected != null && selected.isDirectory()) {
            installRoot = selected.toPath();
        } else {
            installRoot = current.toPath();
        }

        log("Selected install root: " + installRoot);

        try {
            Files.createDirectories(installRoot);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                                          "Cannot create installation folder:\n" + installRoot + "\n" + e.getMessage(),
                                          "Permission error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        frame.setVisible(true);
        Executors.newSingleThreadExecutor().submit(this::runInstaller);
    }

    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(() -> progress.setVisible(true));
            log("Installing " + PRODUCT_NAME + " " + version + " into: " + installRoot);
            Files.createDirectories(installRoot);

            Path tmpZip = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTemp = Files.createTempDirectory("glyco-paint-plugin");

            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null)
                    throw new IOException("Embedded " + PAYLOAD_NAME + " not found in JAR.");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            progress.setIndeterminate(true);
            extractZip(tmpZip, installRoot, pluginTemp);
            log("");

            boolean pluginInstalled = installFijiPlugin(pluginTemp);
            Files.deleteIfExists(tmpZip);

            if (pluginInstalled) {
                Files.walk(pluginTemp)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> p.toFile().delete());
            } else {
                Path manualPlugin = installRoot.resolve("plugin");
                log("⚠️  Fiji.app not found — copying plugin folder for manual installation: " + manualPlugin);
                copyDirectory(pluginTemp, manualPlugin);
            }

            progress.setIndeterminate(false);
            progress.setVisible(false);
            SwingUtilities.invokeLater(() -> closeButton.setEnabled(true));
            log("\nInstallation complete for " + PRODUCT_NAME + " " + version);

        } catch (Exception e) {
            log("❌ Installation failed: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                progress.setVisible(false);
                closeButton.setEnabled(true);
            });
        }
    }

    private boolean installFijiPlugin(Path sourceRoot) throws IOException {
        Optional<Path> pluginJar = Files.walk(sourceRoot)
                .filter(p -> p.getFileName().toString().startsWith("paint-fiji-plugin-") && p.toString().endsWith(".jar"))
                .findFirst();

        if (!pluginJar.isPresent()) {
            log("⚠️  No Fiji plugin JAR found, skipping plugin installation.");
            return false;
        }

        Path jar = pluginJar.get();
        for (String path : FIJI_PATHS) {
            if (path == null) continue;
            Path pluginsDir = Paths.get(path, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Found Fiji.app at " + path);
                Files.createDirectories(pluginsDir);

                Files.list(pluginsDir)
                        .filter(p -> p.getFileName().toString().startsWith("paint-") && p.toString().endsWith(".jar"))
                        .forEach(p -> {
                            try { Files.delete(p); } catch (IOException ignored) {}
                        });

                Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                log("Installed plugin to " + pluginsDir);
                return true;
            }
        }
        log("⚠️  No Fiji.app found; plugin not installed.");
        return false;
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    private void extractZip(Path zipFile, Path targetDir, Path pluginTemp) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            boolean pluginAnnounced = false;
            String currentApp = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                // Handle plugin folder separately
                if (name.startsWith("plugin/")) {
                    if (!pluginAnnounced) {
                        log("Installing Fiji plugin payload...");
                        pluginAnnounced = true;
                    }
                    Path out = pluginTemp.resolve(name.substring("plugin/".length()));
                    if (entry.isDirectory()) {
                        Files.createDirectories(out);
                    } else {
                        Files.createDirectories(out.getParent());
                        try (OutputStream os = Files.newOutputStream(out)) {
                            int n;
                            while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
                        }
                    }
                    continue;
                }

                if (name.matches("^[^/]+\\.exe$")) {
                    currentApp = name;
                    log("Installing executable: " + currentApp);
                }

                Path out = targetDir.resolve(name);
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    try (OutputStream os = Files.newOutputStream(out)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
                    }
                }
            }
        }
    }

    private void copyDirectory(Path src, Path dst) throws IOException {
        Files.walk(src).forEach(source -> {
            Path target = dst.resolve(src.relativize(source));
            try {
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                log("⚠️ Failed to copy " + source + " → " + target + ": " + e.getMessage());
            }
        });
    }
}