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

    // Common Fiji locations
    private static final String[] FIJI_PATHS = {
            envPath("ProgramFiles") + "\\Fiji.app",
            envPath("ProgramFiles(x86)") + "\\Fiji.app",
            envPath("LOCALAPPDATA") + "\\Fiji.app"
    };

    private static String envPath(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v;
    }

    private static final String PREF_NODE        = "InstallerWindows";
    private static final String KEY_PARENT_DIR   = "InstallDirParent";
    private static final String KEY_FIJI_DIR     = "Fiji Dir";

    private final JFrame frame;
    private final JProgressBar progress;
    private final JTextArea log;
    private final JButton closeButton;

    private JCheckBox cbViewer;
    private JCheckBox cbGenerate;
    private JCheckBox cbOmero;
    private JCheckBox cbExperiment;
    private JCheckBox cbPlugin;

    private Path installRoot;
    private String version = "unknown";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GlycoPaintInstallerWindows().show());
    }

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

    private void detectVersion() {
        try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME);
             ZipInputStream zis = new ZipInputStream(in)) {
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

    private void show() {
        String defaultParent = PaintPrefs.getString(
                PREF_NODE,
                KEY_PARENT_DIR,
                FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath()
        );

        Path parent = Paths.get(defaultParent);
        try { Files.createDirectories(parent); } catch (IOException ignored) {}

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

        // Component selection
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

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton installButton = new JButton("Install");
        JButton cancelButton  = new JButton("Cancel");
        buttons.add(cancelButton);
        buttons.add(installButton);
        top.add(buttons, BorderLayout.SOUTH);

        frame.add(top, BorderLayout.NORTH);

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

    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(() -> progress.setVisible(true));
            log("Installing " + PRODUCT_NAME + " " + version + " into " + installRoot);
            log("");

            Path tmpZip    = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTmp = Files.createTempDirectory("glyco-paint-plugin");

            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null) throw new IOException("Missing embedded payload.zip");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            extractZip(tmpZip, installRoot, pluginTmp);

            boolean pluginInstalled = installFijiPlugin(pluginTmp);

            Files.deleteIfExists(tmpZip);

            if (pluginInstalled) {
                Files.walk(pluginTmp)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> p.toFile().delete());
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

    /** Only install plugin if the checkbox is selected */
    private boolean installFijiPlugin(Path pluginSourceRoot) throws IOException {
        if (!cbPlugin.isSelected()) {
            log("Fiji plugin skipped by user.");
            return false;
        }

        Optional<Path> pluginJar = Files.walk(pluginSourceRoot)
                .filter(p -> p.getFileName().toString().startsWith("paint-fiji-plugin-"))
                .filter(p -> p.toString().endsWith(".jar"))
                .findFirst();

        if (!pluginJar.isPresent()) {
            log("No Fiji plugin found.");
            return false;
        }

        Path jar = pluginJar.get();
        String saved = PaintPrefs.getString(PREF_NODE, KEY_FIJI_DIR, null);

        if (saved != null) {
            Path pluginsDir = Paths.get(saved, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString(PREF_NODE, KEY_FIJI_DIR, saved);
                return true;
            }
        }

        for (String base : FIJI_PATHS) {
            if (base == null || base.isEmpty()) continue;
            Path pluginsDir = Paths.get(base, "plugins");
            if (Files.isDirectory(pluginsDir)) {
                installJarIntoFijiDir(jar, pluginsDir);
                PaintPrefs.putString(PREF_NODE, KEY_FIJI_DIR, base);
                return true;
            }
        }

        log("Fiji not detected.");
        return false;
    }

    private void installJarIntoFijiDir(Path jar, Path pluginsDir) throws IOException {
        Files.list(pluginsDir)
                .filter(p -> p.getFileName().toString().startsWith("paint-") && p.toString().endsWith(".jar"))
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });

        Files.copy(jar, pluginsDir.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        log("Installed plugin to: " + pluginsDir);
    }

    /** Filtering only selected EXEs */
    private void extractZip(Path zipFile, Path targetDir, Path pluginTemp) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            boolean pluginStart = false;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store")) continue;

                // Plugin
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

                // Skip EXEs not selected
                if (name.equals("Viewer.exe") && !cbViewer.isSelected()) continue;
                if (name.equals("Generate Squares.exe") && !cbGenerate.isSelected()) continue;
                if (name.equals("Get Omero.exe") && !cbOmero.isSelected()) continue;
                if (name.equals("Create Experiment.exe") && !cbExperiment.isSelected()) continue;

                // Log EXEs
                if (name.toLowerCase().endsWith(".exe")) {
                    log("Installing: " + name);
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
                while ((n = zis.read(buf)) > 0) os.write(buf, 0, n);
            }
        }
    }

    private void copyDirectory(Path src, Path dst) {
        try {
            Files.walk(src).forEach(source -> {
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
        } catch (IOException ignored) {}
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}