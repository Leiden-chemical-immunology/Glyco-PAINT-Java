/*=============================================================================
 *  Class:        GlycoPaintInstallerMac.java
 *  Package:      paint.installer
 *
 *  PURPOSE:
 *    Provides a macOS installer for the Glyco-PAINT software suite. The
 *    installer unpacks the embedded application bundle payload, installs
 *    selected .app components, and optionally installs the Fiji plugin if a
 *    Fiji.app installation is found.
 *
 *  DESCRIPTION:
 *    This installer loads an embedded ZIP payload containing multiple .app
 *    bundles and a Fiji plugin JAR. The user selects which components to
 *    install and where they should be placed. The installer attempts to
 *    automatically locate Fiji.app using the most common macOS installation
 *    directories.
 *
 *    macOS Fiji search paths (checked in order):
 *      • ~/Applications/Fiji.app
 *      • /Applications/Fiji.app
 *
 *    If those directories do not contain a valid Fiji.app (i.e., a folder
 *    containing a "plugins" directory), the user is shown a warning dialog and
 *    then prompted to manually select a Fiji.app folder. If the plugin still
 *    cannot be installed, the plugin files are copied into a "plugin"
 *    directory inside the Glyco-PAINT install root for manual installation.
 *
 *  KEY FEATURES:
 *    • Installs Viewer.app, Generate Squares.app, Get Omero.app,
 *      Create Experiment.app as selected by the user.
 *    • Extracts plugin payload and attempts to install it into a valid Fiji.app.
 *    • Shows a clear warning and allows the user to manually select Fiji.app
 *      when auto-detection fails.
 *    • Remembers last installation directory and last known Fiji.app path.
 *    • Removes macOS quarantine attributes for installed applications.
 *    • Provides detailed logging inside the installer window.
 *    • Automatic version detection from payload.zip.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-installer
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
=============================================================================*/

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

    /* ======================================================================
       CONSTANTS AND SEARCH PATHS
       ====================================================================== */

    /** Product name used for directory naming and window titles. */
    private static final String PRODUCT_NAME = "Glyco-PAINT";

    /** Embedded ZIP file inside the JAR containing all .app bundles and plugin payload. */
    private static final String PAYLOAD_NAME = "/payload.zip";

    /**
     * Default macOS Fiji locations checked in order:
     *   1) ~/Applications/Fiji.app
     *   2) /Applications/Fiji.app
     */
    private static final String[] FIJI_PATHS = {
            System.getProperty("user.home") + "/Applications/Fiji.app",
            "/Applications/Fiji.app"
    };

    /** Exact .app directory names inside payload.zip (top-level). */
    private static final String APP_VIEWER             = "Viewer.app";
    private static final String APP_GENERATE_SQUARES   = "Generate Squares.app";
    private static final String APP_GET_OMERO          = "Get Omero.app";
    private static final String APP_CREATE_EXPERIMENT  = "Create Experiment.app";

    /* ======================================================================
       UI COMPONENTS
       ====================================================================== */

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;

    // Component checkboxes (default: selected)
    private final JCheckBox cbViewer           = new JCheckBox("Viewer", true);
    private final JCheckBox cbGenerateSquares  = new JCheckBox("Generate Squares", true);
    private final JCheckBox cbGetOmero         = new JCheckBox("Get Omero", true);
    private final JCheckBox cbCreateExperiment = new JCheckBox("Create Experiment", true);
    private final JCheckBox cbPlugin           = new JCheckBox("Fiji plugin", true);

    /* ======================================================================
       INSTALLER STATE
       ====================================================================== */

    /** Final installation root, e.g., ~/Applications/Glyco-PAINT */
    private Path installRoot;

    /** Version detected from names inside payload.zip */
    private String version = "unknown";

    /** User-selected .app bundles being installed */
    private Set<String> selectedApps = Collections.emptySet();

    /** Whether the Fiji plugin should be installed */
    private boolean installPlugin = true;

    /* ======================================================================
       ENTRY POINT
       ====================================================================== */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerMac().show());
    }

    /* ======================================================================
       CONSTRUCTOR
       ====================================================================== */

    /**
     * Initializes UI components and detects the application version embedded
     * in the payload ZIP.
     */
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

    /* ======================================================================
       VERSION DETECTION
       ====================================================================== */

    /**
     * Reads the embedded ZIP and extracts the version number from filenames
     * like: {@code paint-<module>-1.2.3}.
     */
    private void detectVersion() {
        try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry entry;
            Pattern versionPattern =
                    Pattern.compile("paint-[a-zA-Z-]+-([0-9]+\\.[0-9]+(\\.[0-9]+)?)");

            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = versionPattern.matcher(entry.getName());
                if (m.find()) {
                    version = m.group(1);
                    break;
                }
            }
        } catch (IOException ignored) {}
    }

    /* ======================================================================
       MAIN UI
       ====================================================================== */

    /**
     * Builds and displays the installer window, including component selection,
     * directory selection, and installation trigger logic.
     */
    private void show() {
        String savedParent = PaintPrefs.getString(
                "Installer", "InstallDirParent",
                System.getProperty("user.home") + "/Applications"
        );

        final Path parent = Paths.get(savedParent);
        try { Files.createDirectories(parent); } catch (IOException ignored) {}

        JPanel configPanel = new JPanel(new BorderLayout(10, 10));
        configPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(configPanel, BorderLayout.NORTH);

        // --- Install directory selection row
        JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
        JLabel dirLabel = new JLabel("Install location:");
        final JTextField dirField = new JTextField(parent.toString());
        JButton browseButton = new JButton("Browse…");

        dirPanel.add(dirLabel, BorderLayout.WEST);
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);
        configPanel.add(dirPanel, BorderLayout.NORTH);

        // --- Component selection (.app bundles and plugin)
        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new GridLayout(0, 1, 4, 4));
        componentsPanel.setBorder(BorderFactory.createTitledBorder("Components to install"));

        componentsPanel.add(cbViewer);
        componentsPanel.add(cbGenerateSquares);
        componentsPanel.add(cbGetOmero);
        componentsPanel.add(cbCreateExperiment);
        componentsPanel.add(cbPlugin);

        configPanel.add(componentsPanel, BorderLayout.CENTER);

        // --- Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton installButton = new JButton("Install");
        JButton cancelButton  = new JButton("Cancel");
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

        // Cancel exits
        cancelButton.addActionListener(e -> System.exit(0));

        // Install: validate and start worker
        installButton.addActionListener(e -> {
            Path chosen = Paths.get(dirField.getText()).normalize();
            installRoot = chosen.resolve(PRODUCT_NAME);

            // Capture selections
            Set<String> apps = new LinkedHashSet<>();
            if (cbViewer.isSelected())           apps.add(APP_VIEWER);
            if (cbGenerateSquares.isSelected())  apps.add(APP_GENERATE_SQUARES);
            if (cbGetOmero.isSelected())         apps.add(APP_GET_OMERO);
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

            // Persist parent dir
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

            // Disable UI and run
            installButton.setEnabled(false);
            cancelButton.setEnabled(false);
            browseButton.setEnabled(false);
            dirField.setEnabled(false);
            setComponentsEnabled(componentsPanel, false);

            Executors.newSingleThreadExecutor().submit(this::runInstaller);
        });

        frame.setVisible(true);
    }

    /**
     * Recursively enables or disables all components inside a container.
     *
     * @param c        container whose children will be toggled
     * @param enabled  whether to enable (true) or disable (false)
     */
    private void setComponentsEnabled(Container c, boolean enabled) {
        for (Component comp : c.getComponents()) {
            comp.setEnabled(enabled);
            if (comp instanceof Container) setComponentsEnabled((Container) comp, enabled);
        }
    }

    /* ======================================================================
       INSTALLATION WORKER
        ====================================================================== */

    /**
     * Runs the installation: extracts payload, installs selected apps,
     * installs (or exports) plugin, and removes quarantine attributes.
     */
    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(() -> progress.setVisible(true));
            log("Installing " + PRODUCT_NAME + " " + version + " into: " + installRoot);
            log("");

            Path tmpZip    = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTmp = Files.createTempDirectory("glyco-paint-plugin");

            // Copy embedded payload.zip to temp
            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null) throw new IOException("Embedded " + PAYLOAD_NAME + " not found in JAR.");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            // Extract selected apps and plugin payload
            extractZip(tmpZip, installRoot, pluginTmp, selectedApps, installPlugin);

            boolean pluginInstalled = false;
            if (installPlugin) {
                pluginInstalled = installFijiPlugin(pluginTmp);
            }

            // Remove macOS quarantine attributes
            removeQuarantineAttributes(installRoot);

            // Cleanup temp ZIP
            Files.deleteIfExists(tmpZip);

            if (installPlugin) {
                if (pluginInstalled) {
                    // Delete pluginTmp recursively
                    try (java.util.stream.Stream<Path> stream = Files.walk(pluginTmp)) {
                        stream.sorted(Comparator.reverseOrder())
                              .forEach(p -> p.toFile().delete());
                    } catch (IOException ignored) {}
                } else {
                    Path manual = installRoot.resolve("plugin");
                    log("Fiji.app not found — copying plugin folder for manual installation: " + manual);
                    copyDirectory(pluginTmp, manual);
                }
            } else {
                // Delete pluginTmp recursively
                try (java.util.stream.Stream<Path> stream = Files.walk(pluginTmp)) {
                    stream.sorted(Comparator.reverseOrder())
                          .forEach(p -> p.toFile().delete());
                } catch (IOException ignored) {}
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

    /* ======================================================================
       FIJI PLUGIN INSTALLATION
       ====================================================================== */

    /**
     * Installs the plugin JAR into a Fiji.app installation.
     * Search order:
     * <ol>
     *   <li>Saved Fiji path from preferences</li>
     *   <li>Standard macOS Fiji locations</li>
     *   <li>Prompt user to choose a Fiji.app folder</li>
     * </ol>
     * If all fail, returns {@code false} so the plugin payload is exported
     * to {@code <installRoot>/plugin/} for manual installation.
     *
     * @param sourceRoot temporary folder containing extracted plugin files
     * @return true if plugin installed into Fiji, false otherwise
     * @throws IOException if file operations fail
     */
    private boolean installFijiPlugin(Path sourceRoot) throws IOException {
        Optional<Path> pluginJar = findPluginJar(sourceRoot);
        if (!pluginJar.isPresent()) {
            log("No Fiji plugin JAR found in payload under " + sourceRoot);
            return false;
        }

        Path jar = pluginJar.get();
        //log("Plugin JAR detected: " + jar.getFileName());

        // 1) Try saved Fiji path
        String savedFiji = PaintPrefs.getString("Installer", "Fiji Dir", null);
        if (savedFiji != null) {
            log("Trying saved Fiji path: " + savedFiji);
            Path pluginsDir = Paths.get(savedFiji, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString("Installer", "Fiji Dir", savedFiji);
                return true;
            } else {
                log("Saved Fiji path is NOT valid: " + pluginsDir);
            }
        }

        // 2) Try standard macOS locations
        for (String base : FIJI_PATHS) {
            log("Trying auto-detected Fiji path: " + base);
            Path pluginsDir = Paths.get(base, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Found Fiji at: " + base);
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString("Installer", "Fiji Dir", base);
                return true;
            }
        }

        // 3) Ask user (warning first, then chooser)
        log("Auto-detection failed. Asking user to select Fiji.app…");
        Path manual = askUserForFijiFolder();
        if (manual != null) {
            log("User selected: " + manual);
            Path pluginsDir = manual.resolve("plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Selected folder contains plugins directory. Installing.");
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString("Installer", "Fiji Dir", manual.toString());
                return true;
            } else {
                log("Selected folder does not contain a 'plugins' directory: " + pluginsDir);
            }
        } else {
            log("User cancelled Fiji selection dialog.");
        }

        log("No valid Fiji.app folder. Plugin will be copied for manual installation.");
        return false;
    }

    /**
     * Copies the plugin JAR into Fiji's {@code plugins} folder after removing
     * any older PAINT plugin jars.
     *
     * @param jar         plugin file to install
     * @param pluginsDir  Fiji plugins directory
     * @throws IOException if IO fails
     */
    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        // Remove older paint-* jars
        try (java.util.stream.Stream<Path> stream = Files.list(pluginsDir)) {
            stream.filter(p -> {
                String fn = p.getFileName().toString();
                return fn.startsWith("paint-") && fn.endsWith(".jar");
            }).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }

        // Copy new jar
        Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        log("Installed plugin into: " + pluginsDir);
    }

    /* ======================================================================
       FILE OPERATIONS
       ====================================================================== */

    /**
     * Recursively removes macOS quarantine attributes from installed bundles
     * and scripts to avoid Gatekeeper prompts.
     *
     * @param dir root directory to scan
     */
    private void removeQuarantineAttributes(Path dir) {
        try (java.util.stream.Stream<Path> stream = Files.walk(dir)) {
            stream.filter(p -> {
                String fn = p.getFileName().toString();
                return p.toString().endsWith(".app")
                        || fn.endsWith(".command")
                        || fn.endsWith(".sh");
            }).forEach(p -> {
                try {
                    new ProcessBuilder("xattr", "-dr", "com.apple.quarantine", p.toString())
                            .inheritIO().start().waitFor();
                } catch (Exception ignored) {}
            });
        } catch (IOException ignored) {}
    }

    /**
     * Appends a line to the installer log area.
     *
     * @param msg message to append
     */
    private void log(final String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    /* ======================================================================
       ZIP EXTRACTION
       ====================================================================== */

    /**
     * Extracts the embedded ZIP payload. Installs only selected .app bundles,
     * and extracts the plugin payload into a temporary folder.
     *
     * @param zipFile        path to the copied payload zip
     * @param targetDir      install root for application bundles
     * @param pluginTemp     temp folder to receive plugin files
     * @param appsToInstall  selected app bundle names (top-level directories)
     * @param includePlugin  whether to extract plugin payload
     * @throws IOException on IO failure
     */
    private void extractZip(Path zipFile,
                            Path targetDir,
                            Path pluginTemp,
                            final Set<String> appsToInstall,
                            final boolean includePlugin) throws IOException {

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            boolean pluginAnnounced = false;
            Set<String> announcedApps = new HashSet<>();

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                // Skip macOS metadata
                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                // Plugin payload under plugin/
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

                // Determine the top-level folder (e.g., "Viewer.app")
                String top = name;
                int slash = name.indexOf('/');
                if (slash >= 0) top = name.substring(0, slash);

                // Only extract selected .app bundles
                if (top.endsWith(".app")) {
                    if (!appsToInstall.contains(top)) continue;

                    // Announce once per app
                    if (!announcedApps.contains(top) && name.equals(top + "/")) {
                        log("Installing: " + top);
                        announcedApps.add(top);
                    }

                    Path out = targetDir.resolve(name);
                    writeZipEntry(zis, buf, entry, out);

                    // Ensure binaries inside Contents/MacOS are executable
                    if (name.contains("/Contents/MacOS/") && !entry.isDirectory()) {
                        out.toFile().setExecutable(true, false);
                    }
                }
            }
        }
    }

    /**
     * Writes a single ZIP entry to disk, creating parents as required.
     */
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

    /**
     * Recursively copies a directory tree.
     *
     * @param src source directory
     * @param dst destination directory (created if needed)
     * @throws IOException on IO failure
     */
    private void copyDirectory(Path src, Path dst) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> {
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
    }

    /* ======================================================================
       PLUGIN JAR DISCOVERY
       ====================================================================== */

    /**
     * Locates the plugin JAR under the extracted plugin payload. Prefers
     * {@code paint-fiji-plugin-*.jar}; otherwise returns the first .jar found
     * when sorted descending by filename.
     *
     * @param root root directory of extracted plugin payload
     * @return optional path to plugin jar
     * @throws IOException on IO failure
     */
    private Optional<Path> findPluginJar(Path root) throws IOException {
        try (java.util.stream.Stream<Path> s = Files.walk(root)) {

            List<Path> jars = s
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .collect(java.util.stream.Collectors.toList());

            // Prefer "paint-fiji-plugin-*.jar"
            for (Path p : jars) {
                if (p.getFileName().toString().startsWith("paint-fiji-plugin-")) {
                    return Optional.of(p);
                }
            }

            // Fallback: first jar if available
            return jars.isEmpty() ? Optional.empty() : Optional.of(jars.get(0));
        }
    }

    /* ======================================================================
       USER PROMPT FOR FIJI.APP
       ====================================================================== */

    /**
     * Shows a warning first; only if the user confirms will it show a chooser
     * to select the {@code Fiji.app} folder. Returns {@code null} if the user
     * cancels at any step.
     *
     * @return selected Fiji.app folder or {@code null} if cancelled
     */
    private Path askUserForFijiFolder() {
        final Path[] result = new Path[1];

        // 1) Warning dialog
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Fiji.app not found.\nPlease select your Fiji.app folder.",
                "Fiji Not Found",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            log("User cancelled Fiji selection.");
            return null;
        }

        // 2) Folder chooser (after warning closes)
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Fiji.app folder");
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.setAcceptAllFileFilterUsed(false);

                // Accept either directories or the Fiji.app bundle
                chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().equalsIgnoreCase("Fiji.app");
                    }
                    @Override
                    public String getDescription() {
                        return "Fiji.app";
                    }
                });

                int r = chooser.showOpenDialog(frame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    result[0] = chooser.getSelectedFile().toPath();
                }
            });
        } catch (Exception ignored) {}

        return result[0];
    }
}