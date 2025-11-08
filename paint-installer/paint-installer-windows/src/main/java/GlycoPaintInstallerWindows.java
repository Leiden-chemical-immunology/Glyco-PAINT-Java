import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import paint.shared.utils.PaintPrefs;

public class GlycoPaintInstallerWindows {

    private static final String PRODUCT_NAME = "Glyco-PAINT";
    private static final String PAYLOAD_NAME = "/payload.zip";

    // Common Fiji locations on Windows
    private static final String[] FIJI_PATHS = {
            envPath("ProgramFiles") + "\\Fiji.app",
            envPath("ProgramFiles(x86)") + "\\Fiji.app",
            envPath("LOCALAPPDATA") + "\\Fiji.app"
    };

    private static String envPath(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v;
    }

    // Prefs scope for Windows
    private static final String PREF_NODE        = "InstallerWindows";
    private static final String KEY_PARENT_DIR   = "InstallDirParent"; // parent ONLY
    private static final String KEY_FIJI_DIR     = "Fiji Dir";

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;

    private Path installRoot; // <parent>\Glyco-PAINT
    private String version = "unknown";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerWindows().show());
    }

    public GlycoPaintInstallerWindows() {
        detectVersion();

        frame = new JFrame("Install " + PRODUCT_NAME + " " + version);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(650, 460);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // Center: log area
        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        frame.add(scroll, BorderLayout.CENTER);

        // Bottom: progress + Close
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
        // Remembered parent (not the Glyco-PAINT folder)
        String defaultParent = PaintPrefs.getString(
                PREF_NODE, KEY_PARENT_DIR,
                // Reasonable per-user default on Windows
                FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath()  // usually "Documents"
        );

        Path parent = Paths.get(defaultParent);
        try { Files.createDirectories(parent); } catch (IOException ignored) {}

        // Top control panel (install dir + browse + buttons)
        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JPanel dirPanel = new JPanel(new BorderLayout(6, 0));
        JLabel dirLabel = new JLabel("Install location (parent folder):");
        JTextField dirField = new JTextField(parent.toString());
        JButton browseButton = new JButton("Browse…");

        dirPanel.add(dirLabel, BorderLayout.WEST);
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);
        top.add(dirPanel, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton installButton = new JButton("Install");
        JButton cancelButton  = new JButton("Cancel");
        buttons.add(cancelButton);
        buttons.add(installButton);
        top.add(buttons, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);

        // Browse action (directories only)
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose installation parent folder for " + PRODUCT_NAME);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setApproveButtonText("Select Folder");
            chooser.setCurrentDirectory(new File(dirField.getText()));
            chooser.setSelectedFile(new File(dirField.getText()));

            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                if (f != null && f.isDirectory()) {
                    dirField.setText(f.getAbsolutePath());
                } else {
                    // fallback to whatever directory the chooser is showing
                    dirField.setText(chooser.getCurrentDirectory().getAbsolutePath());
                }
            }
        });

        // Cancel action
        cancelButton.addActionListener(e -> System.exit(0));

        // Install action
        installButton.addActionListener(e -> {
            Path chosenParent = Paths.get(dirField.getText()).normalize();
            installRoot = chosenParent.resolve(PRODUCT_NAME);

            // Persist parent for next run
            PaintPrefs.putString(PREF_NODE, KEY_PARENT_DIR, chosenParent.toString());

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

            // freeze controls during install
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

            Path tmpZip    = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTmp = Files.createTempDirectory("glyco-paint-plugin");

            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null) throw new IOException("Embedded " + PAYLOAD_NAME + " not found in JAR.");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            extractZip(tmpZip, installRoot, pluginTmp);

            boolean pluginInstalled = installFijiPlugin(pluginTmp);

            Files.deleteIfExists(tmpZip);

            if (pluginInstalled) {
                // cleanup plugin temp
                Files.walk(pluginTmp)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> p.toFile().delete());
            } else {
                // leave for manual install
                Path manualPlugin = installRoot.resolve("plugin");
                log("Fiji not found — copying plugin folder for manual installation: " + manualPlugin);
                copyDirectory(pluginTmp, manualPlugin);
            }

            SwingUtilities.invokeLater(() -> {
                progress.setVisible(false);
                closeButton.setEnabled(true);
            });
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

    /** Install plugin: try saved Fiji path first, then common locations. Save the valid path when found. */
    private boolean installFijiPlugin(Path pluginSourceRoot) throws IOException {
        Optional<Path> pluginJar = Files.walk(pluginSourceRoot)
                .filter(p -> p.getFileName().toString().startsWith("paint-fiji-plugin-") && p.toString().endsWith(".jar"))
                .findFirst();

        if (!pluginJar.isPresent()) {
            log("No Fiji plugin JAR found, skipping plugin installation.");
            return false;
        }

        Path jar = pluginJar.get();

        // 1) Saved path
        String savedFiji = PaintPrefs.getString(PREF_NODE, KEY_FIJI_DIR, null);
        if (savedFiji != null && !savedFiji.trim().isEmpty()) {
            Path pluginsDir = Paths.get(savedFiji, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Using saved Fiji path: " + savedFiji);
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString(PREF_NODE, KEY_FIJI_DIR, savedFiji); // re-save to confirm
                return true;
            } else {
                log("Saved Fiji path invalid: " + savedFiji);
            }
        }

        // 2) Common paths
        for (String base : FIJI_PATHS) {
            if (base == null || base.isEmpty()) continue;
            Path pluginsDir = Paths.get(base, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Found Fiji.app at: " + base);
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString(PREF_NODE, KEY_FIJI_DIR, base);
                return true;
            }
        }

        // 3) Not found
        log("Fiji not detected on this system.");
        return false;
    }

    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        // Remove any previous paint-*.jar
        Files.list(pluginsDir)
                .filter(p -> p.getFileName().toString().startsWith("paint-") && p.toString().endsWith(".jar"))
                .forEach(p -> {
                    try { Files.delete(p); } catch (IOException ignored) {}
                });

        Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        log("Installed plugin to: " + pluginsDir);
    }

    private void extractZip(Path zipFile, Path targetDir, Path pluginTemp) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            boolean pluginAnnounced = false;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // Skip macOS metadata
                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                // Plugin subtree
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

                // Log individual Windows executables
                if (name.toLowerCase().endsWith(".exe") && !entry.isDirectory()) {
                    log("Installing: " + Paths.get(name).getFileName());
                }

                Path out = targetDir.resolve(name);
                writeZipEntry(zis, buf, entry, out);
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
                while ((n = zis.read(buf)) > 0) {
                    os.write(buf, 0, n);
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
                log("Failed to copy " + source + " → " + target + ": " + e.getMessage());
            }
        });
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg);
            log.append("\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}