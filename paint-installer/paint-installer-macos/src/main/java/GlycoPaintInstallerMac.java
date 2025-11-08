import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
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

    // App folder names as they appear at the top level of the ZIP
    private static final String APP_VIEWER = "Viewer.app";
    private static final String APP_GENERATE_SQUARES = "Generate Squares.app";
    private static final String APP_GET_OMERO = "Get Omero.app";
    private static final String APP_CREATE_EXPERIMENT = "Create Experiment.app";

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;

    // selection UI
    private final JCheckBox cbViewer = new JCheckBox("Viewer", true);
    private final JCheckBox cbGenerateSquares = new JCheckBox("Generate Squares", true);
    private final JCheckBox cbGetOmero = new JCheckBox("Get Omero", true);
    private final JCheckBox cbCreateExperiment = new JCheckBox("Create Experiment", true);
    private final JCheckBox cbPlugin = new JCheckBox("Fiji plugin", true);

    private Path installRoot;
    private String version = "unknown";

    // selections captured at install time
    private Set<String> selectedApps = Collections.emptySet();
    private boolean installPlugin = true;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { new GlycoPaintInstallerMac().show(); }
        });
    }

    public GlycoPaintInstallerMac() {
        detectVersion();

        frame = new JFrame("Install " + PRODUCT_NAME + " " + version);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(580, 520);
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

        final Path parent = Paths.get(savedParent);
        final Path defaultInstallParent = parent;

        try { Files.createDirectories(parent); } catch (IOException ignored) {}

        // Top configuration area
        JPanel configPanel = new JPanel(new BorderLayout(10, 10));
        configPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(configPanel, BorderLayout.NORTH);

        // Install directory row
        JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
        JLabel dirLabel = new JLabel("Install location:");
        final JTextField dirField = new JTextField(defaultInstallParent.toString());
        JButton browseButton = new JButton("Browse…");

        dirPanel.add(dirLabel, BorderLayout.WEST);
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);
        configPanel.add(dirPanel, BorderLayout.NORTH);

        // Components selection (checkboxes)
        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new GridLayout(0, 1, 4, 4));
        componentsPanel.setBorder(BorderFactory.createTitledBorder("Components to install"));

        componentsPanel.add(cbViewer);
        componentsPanel.add(cbGenerateSquares);
        componentsPanel.add(cbGetOmero);
        componentsPanel.add(cbCreateExperiment);
        componentsPanel.add(cbPlugin);

        configPanel.add(componentsPanel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton installButton = new JButton("Install");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        buttonPanel.add(installButton);
        configPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Directory chooser wiring
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

        cancelButton.addActionListener(e -> System.exit(0));

        installButton.addActionListener(e -> {
            Path chosen = Paths.get(dirField.getText());
            installRoot = chosen.resolve(PRODUCT_NAME);

            // capture selections
            Set<String> apps = new LinkedHashSet<String>();
            if (cbViewer.isSelected()) apps.add(APP_VIEWER);
            if (cbGenerateSquares.isSelected()) apps.add(APP_GENERATE_SQUARES);
            if (cbGetOmero.isSelected()) apps.add(APP_GET_OMERO);
            if (cbCreateExperiment.isSelected()) apps.add(APP_CREATE_EXPERIMENT);
            selectedApps = Collections.unmodifiableSet(apps);
            installPlugin = cbPlugin.isSelected();

            if (selectedApps.isEmpty() && !installPlugin) {
                JOptionPane.showMessageDialog(
                        frame,
                        "Nothing selected to install.\nPlease select at least one app or the Fiji plugin.",
                        "Nothing to do",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

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
            setComponentsEnabled(componentsPanel, false);

            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override public void run() { runInstaller(); }
            });
        });

        frame.setVisible(true);
    }

    private void setComponentsEnabled(Container c, boolean enabled) {
        for (Component comp : c.getComponents()) {
            comp.setEnabled(enabled);
            if (comp instanceof Container) setComponentsEnabled((Container) comp, enabled);
        }
    }

    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() { progress.setVisible(true); }
            });
            log("Installing " + PRODUCT_NAME + " " + version + " into: " + installRoot);
            log("");

            Path tmpZip = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTemp = Files.createTempDirectory("glyco-paint-plugin");

            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null) throw new IOException("Embedded " + PAYLOAD_NAME + " not found in JAR.");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            extractZip(tmpZip, installRoot, pluginTemp, selectedApps, installPlugin);

            boolean pluginInstalled = false;
            if (installPlugin) {
                pluginInstalled = installFijiPlugin(pluginTemp);
            }

            removeQuarantineAttributes(installRoot);

            Files.deleteIfExists(tmpZip);

            if (installPlugin) {
                if (pluginInstalled) {
                    // cleanup plugin temp if installed automatically
                    try {
                        Files.walk(pluginTemp)
                                .sorted(new Comparator<Path>() {
                                    @Override public int compare(Path a, Path b) { return b.compareTo(a); }
                                })
                                .forEach(new java.util.function.Consumer<Path>() {
                                    @Override public void accept(Path p) { p.toFile().delete(); }
                                });
                    } catch (IOException ignored) {}
                } else {
                    Path manualPlugin = installRoot.resolve("plugin");
                    log("Fiji.app not found, copying plugin folder for manual installation: " + manualPlugin);
                    copyDirectory(pluginTemp, manualPlugin);
                }
            } else {
                // plugin not selected: discard any extracted plugin temp content
                try {
                    Files.walk(pluginTemp)
                            .sorted(new Comparator<Path>() {
                                @Override public int compare(Path a, Path b) { return b.compareTo(a); }
                            })
                            .forEach(new java.util.function.Consumer<Path>() {
                                @Override public void accept(Path p) { p.toFile().delete(); }
                            });
                } catch (IOException ignored) {}
            }

            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    progress.setVisible(false);
                    closeButton.setEnabled(true);
                }
            });
            log("");
            log("Installation complete for " + PRODUCT_NAME + " " + version);

        } catch (Exception e) {
            log("Installation failed: " + e.getMessage());
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    progress.setVisible(false);
                    closeButton.setEnabled(true);
                }
            });
        }
    }

    private boolean installFijiPlugin(Path sourceRoot) throws IOException {
        Optional<Path> pluginJar = Files.walk(sourceRoot)
                .filter(new java.util.function.Predicate<Path>() {
                    @Override public boolean test(Path p) {
                        String fn = p.getFileName().toString();
                        return fn.startsWith("paint-fiji-plugin-") && fn.endsWith(".jar");
                    }
                })
                .findFirst();

        if (!pluginJar.isPresent()) {
            log("No Fiji plugin JAR found, skipping plugin installation.");
            return false;
        }

        Path jar = pluginJar.get();
        String savedFiji = PaintPrefs.getString("Installer", "Fiji Dir", null);

        // First try saved Fiji path
        if (savedFiji != null) {
            Path savedPluginsDir = Paths.get(savedFiji, "plugins");
            if (Files.isDirectory(savedPluginsDir)) {
                log("Found saved Fiji path: " + savedFiji);
                installJarIntoFijiDir(jar, savedPluginsDir);
                PaintPrefs.putString("Installer", "Fiji Dir", savedFiji);
                return true;
            } else {
                log("Saved Fiji path invalid: " + savedFiji);
            }
        }

        // Otherwise search standard macOS paths
        for (String path : FIJI_PATHS) {
            Path pluginsDir = Paths.get(path, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Found Fiji.app at " + path);
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString("Installer", "Fiji Dir", path);
                return true;
            }
        }

        log("No Fiji.app found, plugin not installed.");
        return false;
    }

    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        // Delete old plugin(s)
        Files.list(pluginsDir)
                .filter(new java.util.function.Predicate<Path>() {
                    @Override public boolean test(Path p) {
                        String fn = p.getFileName().toString();
                        return fn.startsWith("paint-") && fn.endsWith(".jar");
                    }
                })
                .forEach(new java.util.function.Consumer<Path>() {
                    @Override public void accept(Path p) {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    }
                });

        // Copy new
        Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        log("Installed plugin into: " + pluginsDir);
    }

    private void removeQuarantineAttributes(Path dir) {
        try {
            Files.walk(dir)
                    .filter(new java.util.function.Predicate<Path>() {
                        @Override public boolean test(Path p) {
                            String fn = p.getFileName().toString();
                            return p.toString().endsWith(".app")
                                    || fn.endsWith(".command")
                                    || fn.endsWith(".sh");
                        }
                    })
                    .forEach(new java.util.function.Consumer<Path>() {
                        @Override public void accept(Path p) {
                            try {
                                new ProcessBuilder("xattr", "-dr",
                                                   "com.apple.quarantine", p.toString())
                                        .inheritIO().start().waitFor();
                            } catch (Exception ignored) {}
                        }
                    });
        } catch (IOException ignored) {}
    }

    private void log(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }

    private void extractZip(Path zipFile,
                            Path targetDir,
                            Path pluginTemp,
                            final Set<String> appsToInstall,
                            final boolean includePlugin) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            boolean pluginAnnounced = false;
            Set<String> announcedApps = new HashSet<String>();

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // Skip macOS fluff
                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                // Plugin payload
                if (name.startsWith("plugin/")) {
                    if (!includePlugin) continue;
                    if (!pluginAnnounced) {
                        log("");
                        log("Installing Fiji plugin payload...");
                        pluginAnnounced = true;
                    }
                    Path out = pluginTemp.resolve(name.substring("plugin/".length()));
                    writeZipEntry(zis, buf, entry, out);
                    continue;
                }

                // Determine top-level component (e.g., "Viewer.app")
                String top = name;
                int slash = name.indexOf('/');
                if (slash >= 0) top = name.substring(0, slash);

                // Only handle .app components and only if selected
                if (top.endsWith(".app")) {
                    if (!appsToInstall.contains(top)) continue;

                    // Announce once per app
                    if (!announcedApps.contains(top) && name.equals(top + "/")) {
                        log("Installing: " + top);
                        announcedApps.add(top);
                    }

                    Path out = targetDir.resolve(name);
                    writeZipEntry(zis, buf, entry, out);

                    // Make executables in Contents/MacOS runnable
                    if (name.contains("/Contents/MacOS/") && !entry.isDirectory()) {
                        out.toFile().setExecutable(true, false);
                    }
                }
                // Any other top-level files/dirs are ignored
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
        Files.walk(src).forEach(new java.util.function.Consumer<Path>() {
            @Override public void accept(Path source) {
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
            }
        });
    }

    // (Unused helper kept for completeness; safe to remove if you like)
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