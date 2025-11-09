/******************************************************************************
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
 *    macOS Fiji search paths:
 *      • ~/Applications/Fiji.app
 *      • /Applications/Fiji.app
 *
 *    If those directories do not contain a valid Fiji.app (i.e. a folder
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
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-installer
 *
 *  UPDATED:
 *    2025-11-09
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import paint.shared.utils.PaintPrefs;

public class GlycoPaintInstallerMac {

    /* ----------------------------------------------------------------------
       CONSTANTS AND SEARCH PATHS
       ---------------------------------------------------------------------- */

    private static final String PRODUCT_NAME = "Glyco-PAINT";
    private static final String PAYLOAD_NAME = "/payload.zip";

    /* macOS Fiji search paths.
       These match the typical user installation locations:
         1. ~/Applications/Fiji.app
         2. /Applications/Fiji.app
       The installer checks these before prompting the user.
    */
    private static final String[] FIJI_PATHS = {
            System.getProperty("user.home") + "/Applications/Fiji.app",
            "/Applications/Fiji.app"
    };

    /* Names of the .app bundles inside the ZIP payload. These must match the
       top-level directory names in payload.zip exactly. */
    private static final String APP_VIEWER = "Viewer.app";
    private static final String APP_GENERATE_SQUARES = "Generate Squares.app";
    private static final String APP_GET_OMERO = "Get Omero.app";
    private static final String APP_CREATE_EXPERIMENT = "Create Experiment.app";

    /* ----------------------------------------------------------------------
       UI COMPONENTS
       ---------------------------------------------------------------------- */

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;

    private final JCheckBox cbViewer = new JCheckBox("Viewer", true);
    private final JCheckBox cbGenerateSquares = new JCheckBox("Generate Squares", true);
    private final JCheckBox cbGetOmero = new JCheckBox("Get Omero", true);
    private final JCheckBox cbCreateExperiment = new JCheckBox("Create Experiment", true);
    private final JCheckBox cbPlugin = new JCheckBox("Fiji plugin", true);

    /* ----------------------------------------------------------------------
       STATE
       ---------------------------------------------------------------------- */

    private Path installRoot;
    private String version = "unknown";

    private Set<String> selectedApps = Collections.emptySet();
    private boolean installPlugin = true;

    /* ----------------------------------------------------------------------
       ENTRY POINT
       ---------------------------------------------------------------------- */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerMac().show());
    }

    /* ----------------------------------------------------------------------
       CONSTRUCTOR
       ---------------------------------------------------------------------- */

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

    /* ----------------------------------------------------------------------
       VERSION DETECTION
       ---------------------------------------------------------------------- */

    private void detectVersion() {
        try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry entry;
            Pattern versionPattern = Pattern.compile("paint-[a-zA-Z-]+-([0-9]+\\.[0-9]+(\\.[0-9]+)?)");

            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = versionPattern.matcher(entry.getName());
                if (m.find()) {
                    version = m.group(1);
                    break;
                }
            }
        } catch (IOException ignored) {}
    }

    /* ----------------------------------------------------------------------
       MAIN UI
       ---------------------------------------------------------------------- */

    private void show() {

        String savedParent = PaintPrefs.getString(
                "Installer", "InstallDirParent",
                System.getProperty("user.home") + "/Applications"
        );

        final Path parent = Paths.get(savedParent);
        final Path defaultInstallParent = parent;

        try { Files.createDirectories(parent); } catch (IOException ignored) {}

        JPanel configPanel = new JPanel(new BorderLayout(10, 10));
        configPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(configPanel, BorderLayout.NORTH);

        JPanel dirPanel = new JPanel(new BorderLayout(5, 0));
        JLabel dirLabel = new JLabel("Install location:");
        final JTextField dirField = new JTextField(defaultInstallParent.toString());
        JButton browseButton = new JButton("Browse…");

        dirPanel.add(dirLabel, BorderLayout.WEST);
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);
        configPanel.add(dirPanel, BorderLayout.NORTH);

        JPanel componentsPanel = new JPanel();
        componentsPanel.setLayout(new GridLayout(0, 1, 4, 4));
        componentsPanel.setBorder(BorderFactory.createTitledBorder("Components to install"));

        componentsPanel.add(cbViewer);
        componentsPanel.add(cbGenerateSquares);
        componentsPanel.add(cbGetOmero);
        componentsPanel.add(cbCreateExperiment);
        componentsPanel.add(cbPlugin);

        configPanel.add(componentsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton installButton = new JButton("Install");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(cancelButton);
        buttonPanel.add(installButton);
        configPanel.add(buttonPanel, BorderLayout.SOUTH);

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

            Set<String> apps = new LinkedHashSet<>();
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

            Executors.newSingleThreadExecutor().submit(this::runInstaller);
        });

        frame.setVisible(true);
    }

    private void setComponentsEnabled(Container c, boolean enabled) {
        for (Component comp : c.getComponents()) {
            comp.setEnabled(enabled);
            if (comp instanceof Container) setComponentsEnabled((Container) comp, enabled);
        }
    }

    /* ----------------------------------------------------------------------
       INSTALLATION WORKER
       ---------------------------------------------------------------------- */

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

            extractZip(tmpZip, installRoot, pluginTemp, selectedApps, installPlugin);

            boolean pluginInstalled = false;
            if (installPlugin) {
                pluginInstalled = installFijiPlugin(pluginTemp);
            }

            removeQuarantineAttributes(installRoot);

            Files.deleteIfExists(tmpZip);

            if (installPlugin) {
                if (pluginInstalled) {
                    try {
                        Files.walk(pluginTemp)
                                .sorted(Comparator.reverseOrder())
                                .forEach(p -> p.toFile().delete());
                    } catch (IOException ignored) {}
                } else {
                    Path manualPlugin = installRoot.resolve("plugin");
                    log("Fiji.app not found, copying plugin folder for manual installation: " + manualPlugin);
                    copyDirectory(pluginTemp, manualPlugin);
                }
            } else {
                try {
                    Files.walk(pluginTemp)
                            .sorted(Comparator.reverseOrder())
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

    /* ----------------------------------------------------------------------
       FIJI PLUGIN INSTALLATION
       ---------------------------------------------------------------------- */

    private boolean installFijiPlugin(Path sourceRoot) throws IOException {
        Optional<Path> pluginJar = findPluginJar(sourceRoot);
        if (!pluginJar.isPresent()) {
            log("No Fiji plugin JAR found in payload under " + sourceRoot);
            return false;
        }

        Path jar = pluginJar.get();

        String savedFiji = PaintPrefs.getString("Installer", "Fiji Dir", null);

        if (savedFiji != null) {
            log("Trying saved Fiji path: " + savedFiji);
            Path pluginsDir = Paths.get(savedFiji, "plugins");

            if (Files.isDirectory(pluginsDir)) {
                log("Saved Fiji path is valid.");
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString("Installer", "Fiji Dir", savedFiji);
                return true;
            }
        }

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
            }
        } else {
            log("User cancelled Fiji selection dialog.");
        }

        log("No valid Fiji.app folder. Plugin will be copied for manual installation.");
        return false;
    }

    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        Files.list(pluginsDir)
                .filter(p -> {
                    String fn = p.getFileName().toString();
                    return fn.startsWith("paint-") && fn.endsWith(".jar");
                })
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });

        Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        log("Installed plugin into: " + pluginsDir);
    }

    /* ----------------------------------------------------------------------
       FILE OPERATIONS
       ---------------------------------------------------------------------- */

    private void removeQuarantineAttributes(Path dir) {
        try {
            Files.walk(dir)
                    .filter(p -> {
                        String fn = p.getFileName().toString();
                        return p.toString().endsWith(".app")
                                || fn.endsWith(".command")
                                || fn.endsWith(".sh");
                    })
                    .forEach(p -> {
                        try {
                            new ProcessBuilder("xattr", "-dr",
                                               "com.apple.quarantine", p.toString())
                                    .inheritIO().start().waitFor();
                        } catch (Exception ignored) {}
                    });
        } catch (IOException ignored) {}
    }

    private void log(final String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
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
            Set<String> announcedApps = new HashSet<>();

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

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

                String top = name;
                int slash = name.indexOf('/');
                if (slash >= 0) top = name.substring(0, slash);

                if (top.endsWith(".app")) {
                    if (!appsToInstall.contains(top)) continue;

                    if (!announcedApps.contains(top) && name.equals(top + "/")) {
                        log("Installing: " + top);
                        announcedApps.add(top);
                    }

                    Path out = targetDir.resolve(name);
                    writeZipEntry(zis, buf, entry, out);

                    if (name.contains("/Contents/MacOS/") && !entry.isDirectory()) {
                        out.toFile().setExecutable(true, false);
                    }
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

    /* ----------------------------------------------------------------------
       PLUGIN JAR DISCOVERY
       ---------------------------------------------------------------------- */

    private Optional<Path> findPluginJar(Path root) throws IOException {
        try (java.util.stream.Stream<Path> s = Files.walk(root)) {
            Optional<Path> preferred = s
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .filter(p -> p.getFileName().toString().startsWith("paint-fiji-plugin-"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .findFirst();

            if (preferred.isPresent()) return preferred;
        }

        try (java.util.stream.Stream<Path> s2 = Files.walk(root)) {
            return s2
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".jar"))
                    .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                    .findFirst();
        }
    }

    /* ----------------------------------------------------------------------
       USER PROMPT FOR FIJI.APP
       ---------------------------------------------------------------------- */

    private Path askUserForFijiFolder() {

        final Path[] result = new Path[1];

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

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Fiji.app folder");
                chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                chooser.setAcceptAllFileFilterUsed(false);

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