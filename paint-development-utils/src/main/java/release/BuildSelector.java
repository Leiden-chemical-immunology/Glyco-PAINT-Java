/*=============================================================================
 *  Class:        BuildSelector.java
 *  Package:      release
 *
 *  PURPOSE:
 *    Interactive CLI utility to select modules and platforms for the build
 *    process.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-development-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package release;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuildSelector {

    private static final Path PROJECT_ROOT =
            Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    private final List<Task> tasks = Arrays.asList(
            // macOS apps
            new Task("Viewer.app", "paint-viewer",
                     "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),

            new Task("Generate Squares.app", "paint-generate-squares",
                     "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),

            new Task("Create Experiment.app", "paint-create-experiment",
                     "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),

            new Task("Get Omero.app", "paint-get-omero",
                     "mvn", "-q", "-U", "clean", "package", "-P", "macos-appbundle"),

            // Windows apps
            new Task("Viewer.exe", "paint-viewer",
                     "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),

            new Task("Generate Squares.exe", "paint-generate-squares",
                     "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),

            new Task("Create Experiment.exe", "paint-create-experiment",
                     "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),

            new Task("Get Omero.exe", "paint-get-omero",
                     "mvn", "-q", "-U", "clean", "package", "-P", "windows-exe"),

            // Fiji plugin
            new Task("Fiji Plugin (shaded JAR)", "paint-fiji-plugin",
                     "mvn", "-q", "-U", "clean", "install")
    );
    private final Map<Task, JCheckBox> boxes = new LinkedHashMap<>();
    private JTextArea log;
    private JButton   runBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BuildSelector().show());
    }

    private void show() {
        JFrame frame = new JFrame("Glyco-PAINT – Deliverables Builder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 800);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel north = new JPanel();
        north.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));

        addSection(north, "macOS Applications");
        addBoxes(north, 0, 4);

        addSection(north, "Windows Applications");
        addBoxes(north, 4, 8);

        addSection(north, "Plugin");
        addBoxes(north, 8, 9);

        frame.add(north, BorderLayout.NORTH);

        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        frame.add(new JScrollPane(log), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeBtn = new JButton("Close");
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
        spacer.setPreferredSize(new Dimension(1, 20));
        spacer.setOpaque(false);
        parent.add(spacer);

        JLabel lbl = new JLabel(title);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        parent.add(lbl);
    }

    private void addBoxes(JPanel parent, int from, int to) {
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        boxPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 0));

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
                append("=== Installing shared utils first ===");
                runSharedUtils();

                append("\n=== Running selected builds ===\n");

                for (Task t : boxes.keySet()) {
                    if (!boxes.get(t).isSelected()) {
                        continue;
                    }
                    runTask(t);
                }

                append("\n✅ Finished all selected builds.\n");

            } catch (Exception ex) {
                append("\n❌ " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> runBtn.setEnabled(true));
            }
        }).start();
    }

    private void runSharedUtils() throws IOException, InterruptedException {
        Path utilsDir = PROJECT_ROOT.resolve("paint-shared-utils");
        append("→ Building paint-shared-utils (installing into local repo)");
        append("  Dir: " + utilsDir);

        List<String> cmd = Arrays.asList(
                "mvn", "-q", "-U", "clean", "install", "-DskipTests"
        );

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(utilsDir.toFile());
        pb.redirectErrorStream(true);

        Process p = pb.start();
        try (BufferedReader r =
                     new BufferedReader(new InputStreamReader(p.getInputStream()))) {

            String line;
            while ((line = r.readLine()) != null) {
                append("[paint-shared-utils] " + line);
            }
        }

        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Failed building paint-shared-utils (exit " + exit + ")");
        }

        append("✅ Installed paint-shared-utils\n");
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
                if (line.contains("sun.misc.Unsafe") || line.contains("HiddenClassDefiner")) {
                    continue;
                }
                append("[" + t.moduleDir.getFileName() + "] " + line);
            }
        }

        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("Failed building " + t.label + " (exit " + exit + ")");
        }

        append("✅ Built " + t.label + "\n");
    }

    private void append(String s) {
        SwingUtilities.invokeLater(() -> {
            log.append(s + "\n");
            log.setCaretPosition(log.getDocument().getLength());
        });
    }

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
}