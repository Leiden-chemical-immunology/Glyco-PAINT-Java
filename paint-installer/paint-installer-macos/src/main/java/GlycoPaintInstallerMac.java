import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import paint.shared.utils.PaintPrefs;

public class GlycoPaintInstallerMac {

    private static final String PRODUCT_NAME = "Glyco-PAINT";
    private static final String PAYLOAD_NAME = "/payload.zip";
    private static final String[] FIJI_PATHS = {
            System.getProperty("user.home") + "/Applications/Fiji.app",
            "/Applications/Fiji.app"
    };

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;
    private Path installRoot;
    private String version = "unknown";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerMac().show());
    }

    public GlycoPaintInstallerMac() {
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

        bottomPanel.add(closeButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        frame.add(bottomPanel, BorderLayout.SOUTH);
    }

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

        String savedParent = PaintPrefs.getString(
                "Installer", "InstallDirParent",
                System.getProperty("user.home") + "/Applications"
        );

        Path parent = Paths.get(savedParent);               // e.g. /Users/hans/Downloads
        Path defaultInstallParent = parent;                 // show EXACTLY what user picked last time

        try { Files.createDirectories(parent); } catch (IOException ignored) {}

        // Main panel
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(panel, BorderLayout.NORTH);

        // Top row: install directory + browse
        JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
        JLabel dirLabel = new JLabel("Install location:");

        JTextField dirField = new JTextField(defaultInstallParent.toString());
        JButton browseButton = new JButton("Browse…");

        dirPanel.add(dirLabel, BorderLayout.WEST);
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);

        panel.add(dirPanel, BorderLayout.NORTH);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton installButton = new JButton("Install");
        JButton cancelButton = new JButton("Cancel");

        buttonPanel.add(cancelButton);
        buttonPanel.add(installButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Enable choosing install directory
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setDialogTitle("Choose installation folder");
            chooser.setCurrentDirectory(parent.toFile());

            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (f != null && f.isDirectory()) {
                    dirField.setText(f.getAbsolutePath());
                }
            }
        });

        // Cancel button action
        cancelButton.addActionListener(e -> System.exit(0));

        // Install button action
        installButton.addActionListener(e -> {
            Path chosen = Paths.get(dirField.getText());

            installRoot = chosen.resolve(PRODUCT_NAME);

            PaintPrefs.putString("Installer", "InstallDirParent", chosen.toString());

            try {
                Files.createDirectories(installRoot);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Cannot create installation folder:\n" + installRoot + "\n" + ex.getMessage(),
                        "Permission error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            installButton.setEnabled(false);
            cancelButton.setEnabled(false);
            browseButton.setEnabled(false);
            dirField.setEnabled(false);

            Executors.newSingleThreadExecutor().submit(this::runInstaller);
        });

        frame.setVisible(true);
    }
    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(() -> progress.setVisible(true));
            log("Installing " + PRODUCT_NAME + " " + version + " into: " + installRoot);
            log("");

            Path tmpZip = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTemp = Files.createTempDirectory("glyco-paint-plugin");

            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null) throw new IOException("Embedded " + PAYLOAD_NAME + " not found in JAR.");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            extractZip(tmpZip, installRoot, pluginTemp);

            boolean pluginInstalled = installFijiPlugin(pluginTemp);
            removeQuarantineAttributes(installRoot);

            Files.deleteIfExists(tmpZip);

            if (pluginInstalled) {
                Files.walk(pluginTemp)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> p.toFile().delete());
            } else {
                Path manualPlugin = installRoot.resolve("plugin");
                log("Fiji.app not found, copying plugin folder for manual installation: " + manualPlugin);
                copyDirectory(pluginTemp, manualPlugin);
            }

            progress.setVisible(false);
            SwingUtilities.invokeLater(() -> closeButton.setEnabled(true));
            log("");
            log("Installation complete for " + PRODUCT_NAME + " " + version);

        } catch (Exception e) {
            log("Installation failed: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                progress.setVisible(false);
                closeButton.setEnabled(true);
            });
        }
    }

    private boolean installFijiPlugin(Path sourceRoot) throws IOException {

        Optional<Path> pluginJar = Files.walk(sourceRoot)
                .filter(p -> p.getFileName().toString().startsWith("paint-fiji-plugin-")
                        && p.toString().endsWith(".jar"))
                .findFirst();

        if (!pluginJar.isPresent()) {
            log("No Fiji plugin JAR found, skipping plugin installation.");
            return false;
        }

        Path jar = pluginJar.get();
        String savedFiji = PaintPrefs.getString("Installer", "Fiji Dir", null);

        // ✅ First try the saved Fiji path
        if (savedFiji != null) {
            Path savedPluginsDir = Paths.get(savedFiji, "plugins");
            if (Files.isDirectory(savedPluginsDir)) {
                log("Found saved Fiji path: " + savedFiji);
                installJarIntoFijiDir(jar, savedPluginsDir);

                // ✅ SAVE IT AGAIN (your requested change)
                PaintPrefs.putString("Installer", "Fiji Dir", savedFiji);

                return true;
            } else {
                log("Saved Fiji path invalid: " + savedFiji);
            }
        }

        // ✅ Otherwise search standard macOS paths
        for (String path : FIJI_PATHS) {
            Path pluginsDir = Paths.get(path, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Found Fiji.app at " + path);
                installJarIntoFijiDir(jar, pluginsDir);

                // ✅ Save for next run (already present before)
                PaintPrefs.putString("Installer", "Fiji Dir", path);
                return true;
            }
        }

        log("No Fiji.app found, plugin not installed.");
        return false;
    }

    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        Files.list(pluginsDir)
                .filter(p -> p.getFileName().toString().startsWith("paint-")
                        && p.toString().endsWith(".jar"))
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });

        Files.copy(jar,
                   pluginsDir.resolve(jar.getFileName()),
                   StandardCopyOption.REPLACE_EXISTING);

        log("Installed plugin into: " + pluginsDir);
    }

    private void removeQuarantineAttributes(Path dir) {
        try {
            Files.walk(dir)
                    .filter(p ->
                                    p.toString().endsWith(".app")
                                            || p.getFileName().toString().endsWith(".command")
                                            || p.getFileName().toString().endsWith(".sh"))
                    .forEach(p -> {
                        try {
                            new ProcessBuilder("xattr", "-dr",
                                               "com.apple.quarantine", p.toString())
                                    .inheritIO().start().waitFor();
                        } catch (Exception ignored) {}
                    });
        } catch (IOException ignored) {}
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

            while ((entry = zis.getNextEntry()) != null) {

                String name = entry.getName();
                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                // --- Plugin folder ---
                if (name.startsWith("plugin/")) {
                    if (!pluginAnnounced) {
                        log("");
                        log("Installing Fiji plugin payload...");
                        pluginAnnounced = true;
                    }
                    Path out = pluginTemp.resolve(name.substring("plugin/".length()));
                    writeZipEntry(zis, buf, entry, out);
                    continue;
                }

                // --- Detect top-level macOS apps ---
                if (name.matches("^[^/]+\\.app/$")) {
                    String appName = name.substring(0, name.length() - 1);
                    log("Installing: " + appName);
                }

                // --- Detect binaries inside MacOS folder ---
                if (name.contains("/Contents/MacOS/") && !entry.isDirectory()) {
                    String execName = name.substring(name.lastIndexOf("/") + 1);
                    // log("  Adding executable: " + execName);
                }

                // --- Write the file/directory ---
                Path out = targetDir.resolve(name);
                writeZipEntry(zis, buf, entry, out);

                // Make executables runnable
                if (name.contains("/Contents/MacOS/") && !entry.isDirectory()) {
                    out.toFile().setExecutable(true, false);
                }
            }
        }
    }

    private void writeZipEntry(ZipInputStream zis, byte[] buf, ZipEntry entry, Path out) throws IOException {
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
                log("Failed to copy " + source + " -> " + target + ": " + e.getMessage());
            }
        });
    }

    private void hideFilenameField(Component c) {
        if (c instanceof JTextField) {
            c.setVisible(false);
        } else if (c instanceof Container) {
            for (Component child : ((Container)c).getComponents()) {
                hideFilenameField(child);
            }
        }
    }

    private void hideFilenameField(JFileChooser chooser) {
        for (Component comp : chooser.getComponents()) {
            hideFilenameFieldRec(comp);
        }
    }

    private void hideFilenameFieldRec(Component c) {
        if (c instanceof JTextField) {
            c.setVisible(false);
        }
        if (c instanceof JLabel) {
            JLabel lbl = (JLabel) c;
            String txt = lbl.getText();
            if (txt != null && txt.toLowerCase().contains("file name")) {
                lbl.setVisible(false);
            }
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                hideFilenameFieldRec(child);
            }
        }
    }
}