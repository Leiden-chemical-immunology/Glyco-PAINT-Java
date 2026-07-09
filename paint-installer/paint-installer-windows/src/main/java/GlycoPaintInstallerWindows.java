/*=============================================================================
 *  Class:        GlycoPaintInstallerWindows.java
 *  Package:      paint.installer
 *
 *  PURPOSE:
 *    Provides a Windows installer for the Glyco-PAINT suite. The installer
 *    unpacks the embedded payload (EXE launchers plus the Fiji plugin) and
 *    installs the user-selected components. If a Fiji installation is found,
 *    the plugin JAR is installed automatically.
 *
 *  DESCRIPTION:
 *    The installer extracts an embedded ZIP (payload.zip) containing:
 *      • Viewer.exe
 *      • Generate Squares.exe
 *      • Get Omero.exe
 *      • Create Experiment.exe
 *      • A Fiji plugin payload in payload/plugin/
 *
 *    The user selects which EXE launchers to install and chooses a parent
 *    installation directory, under which a folder "Glyco-PAINT" is created.
 *
 *    Fiji plugin installation uses the following search order:
 *
 *      1) Saved Fiji path from preferences (if any)
 *      2) Auto-detected paths composed from these base directories:
 *           - %ProgramFiles%
 *           - %ProgramFiles(x86)%
 *           - %LOCALAPPDATA%   (usually C:\Users\<user>\AppData\Local)
 *         combined with these directory names:
 *           - Fiji
 *           - Fiji-win64
 *           - Fiji-win32
 *           - Fiji-Windows
 *      3) Ask the user: first show a warning dialog (OK/Cancel); if OK,
 *         open a directory chooser to select the Fiji folder.
 *
 *    If no valid Fiji directory (containing a "plugins" subfolder) is found,
 *    the plugin payload is copied to <installRoot>\plugin\ for manual install.
 *
 *  KEY FEATURES:
 *    • Installs selected EXE launchers from the payload.
 *    • Attempts automatic Fiji detection with full logging of attempted paths.
 *    • Warn-then-choose flow for manual Fiji selection (matches mac behavior).
 *    • Remembers last install parent and last known Fiji path.
 *    • Extracts plugin payload and installs/exports it as appropriate.
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
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import paint.shared.utils.PaintPrefs;

public class GlycoPaintInstallerWindows {

    /* ======================================================================
       CONSTANTS & SEARCH PATHS
       ====================================================================== */

    /** Product name used for folder naming and window titles. */
    private static final String PRODUCT_NAME = "Glyco-PAINT";

    /** Embedded ZIP resource name containing the payload. */
    private static final String PAYLOAD_NAME = "/payload.zip";

    /**
     * Windows Fiji directory names to try under each base path.
     * NOTE: Windows file systems are case-insensitive by default.
     */
    private static final String[] FIJI_DIR_NAMES = {
            "Fiji",
            "Fiji-win64",
            "Fiji-win32",
            "Fiji-Windows"
    };

    /**
     * Constructs the list of candidate Fiji directories by combining
     * these environment variables (if defined) with FIJI_DIR_NAMES:
     *   - ProgramFiles
     *   - ProgramFiles(x86)
     *   - LOCALAPPDATA
     * <p>
     * Examples (depending on system):
     *   C:\Program Files\Fiji
     *   C:\Program Files (x86)\Fiji-win64
     *   C:\Users\<user>\AppData\Local\Fiji-Windows
     */
    private static String[] buildFijiPaths() {
        String[] bases = {
                envPath("ProgramFiles"),
                envPath("ProgramFiles(x86)"),
                envPath("LOCALAPPDATA")
        };

        java.util.List<String> out = new java.util.ArrayList<>();
        for (String base : bases) {
            for (String name : FIJI_DIR_NAMES) {
                out.add(base + File.separator + name);
            }
        }
        return out.toArray(new String[0]);
    }

    /** All auto-detected candidate Fiji paths (constructed at class load). */
    private static final String[] FIJI_PATHS = buildFijiPaths();

    /** Helper to read an environment variable, returning empty string if absent. */
    private static String envPath(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v;
    }

    /** Preferences node/keys for this installer flavor. */
    private static final String PREF_NODE        = "InstallerWindows";
    private static final String KEY_PARENT_DIR   = "InstallDirParent";
    private static final String KEY_FIJI_DIR     = "Fiji Dir";

    /* ======================================================================
       UI COMPONENTS
       ====================================================================== */

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;

    private JCheckBox cbViewer;
    private JCheckBox cbGenerate;
    private JCheckBox cbOmero;
    private JCheckBox cbExperiment;
    private JCheckBox cbPlugin;

    /* ======================================================================
       STATE
       ====================================================================== */

    /** Final install root (parent chosen by user + PRODUCT_NAME). */
    private Path installRoot;

    /** Version string extracted from payload. */
    private String version = "unknown";

    /* ======================================================================
       ENTRY POINT
       ====================================================================== */

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerWindows().show());
    }

    /* ======================================================================
       CONSTRUCTOR & VERSION DETECTION
       ====================================================================== */

    public GlycoPaintInstallerWindows() {
        detectVersion();

        frame = new JFrame("Install " + PRODUCT_NAME + " " + version);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 520);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        frame.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setVisible(false);

        closeButton = new JButton("Close");
        closeButton.setEnabled(false);
        closeButton.addActionListener(e -> System.exit(0));

        bottom.add(progress, BorderLayout.CENTER);
        bottom.add(closeButton, BorderLayout.EAST);
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        frame.add(bottom, BorderLayout.SOUTH);
    }

    /**
     * Extracts a version string from filenames in payload.zip matching:
     *   paint-<module>-X.Y[.Z]
     */
    private void detectVersion() {
        try (InputStream inputStream = getClass().getResourceAsStream(PAYLOAD_NAME);
             ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            Pattern pat = Pattern.compile("paint-[a-zA-Z-]+-([0-9]+\\.[0-9]+(\\.[0-9]+)?)");
            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = pat.matcher(entry.getName());
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

    private void show() {
        // Use user.home as the default parent (matches the macOS installer). The
        // previous FileSystemView.getDefaultDirectory() call resolves Windows shell
        // "special" folders and can NPE (e.g. on VMs / redirected or network
        // profiles), crashing the installer before its window even appears.
        String defaultParent = PaintPrefs.getString(
                PREF_NODE,
                KEY_PARENT_DIR,
                System.getProperty("user.home")
        );

        Path parent = Paths.get(defaultParent);
        try { Files.createDirectories(parent); } catch (IOException ignored) {}

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        /* Install directory selection */
        JPanel dirPanel = new JPanel(new BorderLayout(6, 0));
        JLabel dirLabel = new JLabel("Install location (parent folder):");
        JTextField dirField = new JTextField(parent.toString());
        JButton browseButton = new JButton("Browse…");

        dirPanel.add(dirLabel, BorderLayout.WEST);
        dirPanel.add(dirField, BorderLayout.CENTER);
        dirPanel.add(browseButton, BorderLayout.EAST);
        top.add(dirPanel, BorderLayout.NORTH);

        /* Component selection */
        JPanel compPanel = new JPanel();
        compPanel.setLayout(new GridLayout(0, 1));
        compPanel.setBorder(BorderFactory.createTitledBorder("Select components to install"));

        cbViewer     = new JCheckBox("Viewer.exe", true);
        cbGenerate   = new JCheckBox("Generate Squares.exe", true);
        cbOmero      = new JCheckBox("Get Omero.exe", true);
        cbExperiment = new JCheckBox("Create Experiment.exe", true);
        cbPlugin     = new JCheckBox("Fiji Plugin", true);

        compPanel.add(cbViewer);
        compPanel.add(cbGenerate);
        compPanel.add(cbOmero);
        compPanel.add(cbExperiment);
        compPanel.add(cbPlugin);

        top.add(compPanel, BorderLayout.CENTER);

        /* Action buttons */
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton installButton = new JButton("Install");
        JButton cancelButton  = new JButton("Cancel");
        buttons.add(cancelButton);
        buttons.add(installButton);
        top.add(buttons, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);

        /* Directory chooser */
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Choose installation parent folder");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(dirField.getText()));
            chooser.setSelectedFile(new File(dirField.getText()));
            chooser.setAcceptAllFileFilterUsed(false);

            int r = chooser.showOpenDialog(frame);
            if (r == JFileChooser.APPROVE_OPTION) {
                File f = chooser.getSelectedFile();
                dirField.setText(f.getAbsolutePath());
            }
        });

        cancelButton.addActionListener(e -> System.exit(0));

        /* Start installation */
        installButton.addActionListener(e -> {
            Path chosenParent = Paths.get(dirField.getText()).normalize();
            installRoot = chosenParent.resolve(PRODUCT_NAME);

            PaintPrefs.putString(PREF_NODE, KEY_PARENT_DIR, chosenParent.toString());

            try {
                Files.createDirectories(installRoot);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame,
                                              "Cannot create folder:\n" + installRoot + "\n" + ex.getMessage(),
                                              "Permission error",
                                              JOptionPane.ERROR_MESSAGE);
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

    /* ======================================================================
       INSTALLATION WORKER
       ====================================================================== */

    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(() -> progress.setVisible(true));
            log("Installing " + PRODUCT_NAME + " " + version + " into " + installRoot);
            log("");

            Path tmpZip    = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTmp = Files.createTempDirectory("glyco-paint-plugin");

            /* Copy embedded payload to temp */
            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null) throw new IOException("Missing embedded payload.zip");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            /* Extract EXEs and plugin payload */
            extractZip(tmpZip, installRoot, pluginTmp);

            /* Try to install Fiji plugin (if selected) */
            boolean pluginInstalled = installFijiPlugin(pluginTmp);

            /* Cleanup temp ZIP */
            Files.deleteIfExists(tmpZip);

            if (pluginInstalled) {
                try (java.util.stream.Stream<Path> walk = Files.walk(pluginTmp)) {
                    walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> p.toFile().delete());
                }
            } else {
                if (cbPlugin.isSelected()) {
                    Path manual = installRoot.resolve("plugin");
                    log("Fiji not found — copying plugin for manual installation: " + manual);
                    copyDirectory(pluginTmp, manual);
                }
            }

            SwingUtilities.invokeLater(() -> {
                progress.setVisible(false);
                closeButton.setEnabled(true);
            });
            log("");
            log("Installation complete.");

        } catch (Exception e) {
            log("Installation failed: " + e.getMessage());
            SwingUtilities.invokeLater(() -> {
                progress.setVisible(false);
                closeButton.setEnabled(true);
            });
        }
    }

    /* ======================================================================
       FIJI PLUGIN INSTALLATION (Windows)
       ====================================================================== */

    /**
     * Attempts to install the Fiji plugin only if the user selected the
     * "Fiji Plugin" checkbox. Search order:
     *  1) Saved path
     *  2) Auto-detected paths (see header for full list)
     *  3) Ask the user (warning first, then chooser if OK)
     * <p>
     * Returns true if installed into a valid Fiji\plugins directory, else false.
     */
    private boolean installFijiPlugin(Path pluginSourceRoot) throws IOException {
        if (!cbPlugin.isSelected()) {
            log("Fiji plugin skipped by user.");
            return false;
        }

        Optional<Path> pluginJar = findPluginJar(pluginSourceRoot);
        if (!pluginJar.isPresent()) {
            log("No Fiji plugin JAR found in payload under " + pluginSourceRoot);
            return false;
        }

        Path jar = pluginJar.get();
        log("Found Fiji plugin JAR: " + jar.getFileName());

        /* 1) Saved path */
        String saved = PaintPrefs.getString(PREF_NODE, KEY_FIJI_DIR, null);
        if (saved != null) {
            log("Trying saved Fiji path: " + saved);
            Path pluginsDir = Paths.get(saved, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                log("Saved Fiji path is valid.");
                installJarIntoFijiDir(jar, pluginsDir);
                return true;
            } else {
                log("Saved Fiji path is NOT valid: " + pluginsDir);
            }
        }

        /* 2) Auto-detected list */
        for (String base : FIJI_PATHS) {
            if (base == null || base.isEmpty()) continue;

            log("Trying auto-detected Fiji path: " + base);
            Path pluginsDir = Paths.get(base, "plugins");

            if (Files.isDirectory(pluginsDir)) {
                log("Found Fiji at: " + base);
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString(PREF_NODE, KEY_FIJI_DIR, base);
                return true;
            }
        }

        /* 3) Ask the user — show warning, then chooser only if OK */
        Path manual = askUserForFijiFolder();
        if (manual != null) {
            Path pluginsDir = manual.resolve("plugins");
            if (Files.isDirectory(pluginsDir)) {
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString(PREF_NODE, KEY_FIJI_DIR, manual.toString());
                return true;
            }
        }

        log("No valid Fiji folder. Plugin will be copied for manual installation.");
        return false;
    }

    /** Deletes old paint-*.jar and copies the new plugin into Fiji\plugins. */
    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.list(pluginsDir)) {
            stream
                    .filter(p -> p.getFileName().toString().startsWith("paint-") &&
                            p.getFileName().toString().endsWith(".jar"))
                    .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        }

        Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        log("Installed plugin to: " + pluginsDir);
    }

    /* ======================================================================
       ZIP EXTRACTION & FILE UTILITIES
       ====================================================================== */

    /**
     * Extracts the payload, installing only selected EXEs and extracting
     * plugin payload into a temp directory.
     */
    private void extractZip(Path zipFile, Path targetDir, Path pluginTemp) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            boolean pluginStart = false;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                /* Skip macOS fluff if present in ZIP */
                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                /* Plugin payload */
                if (name.startsWith("plugin/")) {
                    if (!pluginStart) {
                        pluginStart = true;
                        log("");
                        log("Extracting Fiji plugin payload...");
                    }
                    Path out = pluginTemp.resolve(name.substring("plugin/".length()));
                    writeZipEntry(zis, buf, entry, out);
                    continue;
                }

                /* Respect user selections for EXEs */
                if (name.equals("Viewer.exe") && !cbViewer.isSelected()) {
                    continue;
                }
                if (name.equals("Generate Squares.exe") && !cbGenerate.isSelected()) {
                    continue;
                }
                if (name.equals("Get Omero.exe") && !cbOmero.isSelected()) {
                    continue;
                }
                if (name.equals("Create Experiment.exe") && !cbExperiment.isSelected()) {
                    continue;
                }

                if (name.toLowerCase().endsWith(".exe")) {
                    log("Installing: " + name);
                }

                Path out = targetDir.resolve(name);
                writeZipEntry(zis, buf, entry, out);
            }
        }
    }

    /** Writes a single ZIP entry to disk, creating parent directories as needed. */
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

    /** Recursively copies one directory tree into another (REPLACE_EXISTING). */
    private void copyDirectory(Path src, Path dst) throws IOException {
        try (java.util.stream.Stream<Path> walk = Files.walk(src)) {
            walk.forEach(source -> {
                Path target = dst.resolve(src.relativize(source));
                try {
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log("Copy failed: " + source + " -> " + target + ": " + e.getMessage());
                }
            });
        }
        // Let a whole-operation failure (e.g. Files.walk) propagate to the caller,
        // which reports "Installation failed" — matching the macOS installer.
        // (Previously swallowed, so a failed copy still logged "Installation complete".)
    }

    /** Appends a line to the UI log area on the EDT. */
    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

    /* ======================================================================
       USER PROMPT FOR FIJI (Warn then Choose)
       ====================================================================== */

    /**
     * Shows a warning dialog explaining Fiji wasn't found. If user presses OK,
     * opens a folder chooser for the user to select the Fiji directory. If the
     * user cancels either dialog, returns null.
     */
    private Path askUserForFijiFolder() {

        // 1) Warning first (modal)
        int choice = JOptionPane.showConfirmDialog(
                frame,
                "Fiji installation not found.\nPlease select your Fiji folder (contains Fiji.exe).",
                "Fiji Not Found",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        // 2. If user presses Cancel -> do not show folder picker
        if (choice != JOptionPane.OK_OPTION) {
            log("User cancelled Fiji selection.");
            return null;
        }

        final Path[] result = new Path[1];

        // 2) Only open chooser after the warning is dismissed
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Select Fiji folder (contains Fiji.exe)");
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);

                int r = chooser.showOpenDialog(frame);
                if (r == JFileChooser.APPROVE_OPTION) {
                    result[0] = chooser.getSelectedFile().toPath();
                } else {
                    log("User cancelled folder chooser.");
                }
            });
        } catch (Exception ignored) {}

        return result[0];
    }

    /* ======================================================================
       PLUGIN JAR DISCOVERY
       ====================================================================== */

    /**
     * Prefer paint-fiji-plugin-*.jar; fallback to any .jar under plugin payload.
     * If multiple candidates exist, choose the lexicographically last name.
     */
    private Optional<Path> findPluginJar(Path root) throws IOException {

        // Comparator selecting lexicographically LAST filename
        Comparator<Path> byNameDescending =
                (a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString());

        // Preferred: paint-fiji-plugin-*.jar
        try (java.util.stream.Stream<Path> s = Files.walk(root)) {
            Optional<Path> preferred =
                    s.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().endsWith(".jar"))
                     .filter(p -> p.getFileName().toString().startsWith("paint-fiji-plugin-"))
                     .min(byNameDescending);      // <-- replaces sorted().findFirst()

            if (preferred.isPresent()) {
                return preferred;
            }
        }

        // Fallback: any .jar
        try (java.util.stream.Stream<Path> s2 = Files.walk(root)) {
            return s2.filter(Files::isRegularFile)
                     .filter(p -> p.getFileName().toString().endsWith(".jar"))
                     .min(byNameDescending);        // <-- also using min()
        }
    }
}