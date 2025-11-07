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

    public GlycoPaintInstallerWindows() {
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

    private void detectVersion() {
        try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry entry;
            Pattern versionPattern = Pattern.compile(
                    "paint-[a-zA-Z-]+-([0-9]+\\.[0-9]+(\\.[0-9]+)?)"
            );

            while ((entry = zis.getNextEntry()) != null) {
                Matcher m = versionPattern.matcher(entry.getName());
                if (m.find()) {
                    version = m.group(1);
                    break;
                }
            }
        } catch (IOException ignored) {}
    }

    private void show() {

        // Default parent directory: C:\Users\<name>\Applications
        Path parentStart = Paths.get(
                System.getProperty("user.home"), "Applications"
        );
        try { Files.createDirectories(parentStart); } catch (IOException ignored) {}

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose installation parent folder for " + PRODUCT_NAME);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setApproveButtonText("Install here");
        chooser.setCurrentDirectory(parentStart.toFile());
        chooser.setSelectedFile(parentStart.toFile());

        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.exit(0);
            return;
        }

        File selected = chooser.getSelectedFile();
        if (selected == null || !selected.isDirectory()) {
            selected = chooser.getCurrentDirectory();
        }

        Path parent = selected.toPath();
        installRoot = parent.resolve(PRODUCT_NAME);

        log("Selected install root: " + installRoot);

        try {
            Files.createDirectories(installRoot);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Cannot create installation folder:\n" + installRoot + "\n" + e,
                    "Permission error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit(1);
        }

        frame.setVisible(true);
        Executors.newSingleThreadExecutor().submit(this::runInstaller);
    }

    private void runInstaller() {
        try {
            SwingUtilities.invokeLater(() -> progress.setVisible(true));
            log("Installing " + PRODUCT_NAME + " " + version + " into: " + installRoot);

            Path tmpZip = Files.createTempFile("glyco-paint", ".zip");
            Path pluginTemp = Files.createTempDirectory("glyco-paint-plugin");

            try (InputStream in = getClass().getResourceAsStream(PAYLOAD_NAME)) {
                if (in == null)
                    throw new IOException("Embedded " + PAYLOAD_NAME + " not found.");
                Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
            }

            extractZip(tmpZip, installRoot, pluginTemp);

            boolean pluginInstalled = installFijiPlugin(pluginTemp);

            Files.deleteIfExists(tmpZip);

            if (pluginInstalled) {
                Files.walk(pluginTemp)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> p.toFile().delete());
            } else {
                Path manual = installRoot.resolve("plugin");
                log("Fiji.app not found — copying plugin folder: " + manual);
                copyDirectory(pluginTemp, manual);
            }

            SwingUtilities.invokeLater(() -> {
                progress.setVisible(false);
                closeButton.setEnabled(true);
            });

            log("\nInstallation complete.");

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
            log("No Fiji plugin found.");
            return false;
        }

        Path jar = pluginJar.get();

        for (String base : FIJI_PATHS) {
            if (base == null) continue;

            Path pluginsDir = Paths.get(base, "plugins");
            if (!Files.isDirectory(pluginsDir)) continue;

            log("Found Fiji.app at " + base);

            Files.list(pluginsDir)
                    .filter(p -> p.getFileName().toString().startsWith("paint-")
                            && p.toString().endsWith(".jar"))
                    .forEach(p -> {
                        try { Files.delete(p); } catch (IOException ignored) {}
                    });

            Files.copy(jar, pluginsDir.resolve(jar.getFileName()),
                       StandardCopyOption.REPLACE_EXISTING);

            log("Installed plugin to " + pluginsDir);
            return true;
        }

        return false;
    }

    private void extractZip(Path zipFile, Path targetDir, Path pluginTemp) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.startsWith("__MACOSX/") || name.endsWith(".DS_Store"))
                    continue;

                if (name.startsWith("plugin/")) {
                    Path out = pluginTemp.resolve(name.substring("plugin/".length()));
                    writeZipEntry(zis, buf, entry, out);
                    continue;
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
            log.append(msg + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}