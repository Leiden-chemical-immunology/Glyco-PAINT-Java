package release;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

public class BuildSelector {

    private static final Path PROJECT_ROOT = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

    private JFrame frame;
    private JTextArea log;
    private JButton runBtn, closeBtn;

    // Map deliverable → Maven command + module directory
    private static class Task {
        final String label;
        final Path moduleDir;
        final List<String> mavenCmd;

        Task(String label, String module, String... cmd) {
            this.label = label;
            this.moduleDir = PROJECT_ROOT.resolve(module);
            this.mavenCmd = Arrays.asList(cmd);
        }
    }

    private final List<Task> tasks = Arrays.asList(
            // macOS apps
            new Task("Viewer.app", "paint-viewer", "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),
            new Task("Generate Squares.app", "paint-generate-squares", "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),
            new Task("Create Experiment.app", "paint-create-experiment", "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),
            new Task("Get Omero.app", "paint-get-omero", "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),

            // Windows apps
            new Task("Viewer.exe", "paint-viewer", "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),
            new Task("Generate Squares.exe", "paint-generate-squares", "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),
            new Task("Create Experiment.exe", "paint-create-experiment", "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),
            new Task("Get Omero.exe", "paint-get-omero", "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),

            // Plugin
            new Task("Fiji Plugin (shaded JAR)", "paint-fiji-plugin", "mvn", "-q", "-U", "clean", "package"),

            // Installers
            new Task("macOS Installer", "paint-installer/paint-installer-macos", "mvn", "-q", "-U", "clean", "package"),
            new Task("Windows Installer", "paint-installer/paint-installer-windows", "mvn", "-q", "-U", "clean", "package")
    );

    private final Map<Task, JCheckBox> boxes = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BuildSelector().show());
    }

    private void show() {
        frame = new JFrame("Glyco-PAINT – Deliverables Builder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 520);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel north = new JPanel();
        north.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

        addSection(north, "macOS Applications");
        addBoxes(north, 0, 4);

        addSection(north, "Windows Applications");
        addBoxes(north, 4, 8);

        addSection(north, "Plugin");
        addBoxes(north, 8, 9);

        addSection(north, "Installers");
        addBoxes(north, 9, 11);

        frame.add(north, BorderLayout.NORTH);

        // Center log
        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        frame.add(new JScrollPane(log), BorderLayout.CENTER);

        // Buttons bottom
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeBtn = new JButton("Close");
        runBtn = new JButton("Generate");

        closeBtn.addActionListener(e -> System.exit(0));
        runBtn.addActionListener(e -> runSelectedTasks());

        south.add(closeBtn);
        south.add(runBtn);

        frame.add(south, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void addSection(JPanel parent, String title) {
        JPanel spacer = new JPanel();
        spacer.setPreferredSize(new Dimension(1, 20));  // <<< extra space between groups
        spacer.setOpaque(false);
        parent.add(spacer);

        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        parent.add(lbl);
    }

    private void addBoxes(JPanel parent, int from, int to) {
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        boxPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 0)); // indent group items neatly

        for (int i = from; i < to; i++) {
            Task t = tasks.get(i);
            JCheckBox box = new JCheckBox(" " + t.label);
            boxes.put(t, box);
            boxPanel.add(box);
        }

        parent.add(boxPanel);
    }

    private void runSelectedTasks() {
        runBtn.setEnabled(false);

        new Thread(() -> {
            try {
                append("=== Running selected deliverable builds ===\n");

                for (Task t : boxes.keySet()) {
                    if (!boxes.get(t).isSelected()) continue;
                    runTask(t);
                }

                append("\n✅ Done.\n");

            } catch (Exception ex) {
                append("\n❌ " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> runBtn.setEnabled(true));
            }
        }).start();
    }

    private void runTask(Task t) throws IOException, InterruptedException {
        append("→ Building " + t.label);
        append("  Cmd: " + String.join(" ", t.mavenCmd));
        append("  Dir: " + t.moduleDir + "\n");

        ProcessBuilder pb = new ProcessBuilder(t.mavenCmd);
        pb.directory(t.moduleDir.toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.contains("sun.misc.Unsafe") || line.contains("HiddenClassDefiner")) continue;
                append("[" + t.moduleDir.getFileName() + "] " + line);
            }
        }

        int exit = p.waitFor();
        if (exit != 0) throw new IOException("Failed building " + t.label + " (exit " + exit + ")");
        append("✅ Built " + t.label + "\n");
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            log.append(s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }
}