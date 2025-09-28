package createexperiment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CreateExperimentUI {
    // === Preferences keys ===
    private static final String PREF_NODE = "paint/create-experiment";
    private static final String KEY_IMAGES = "lastImagesDir";
    private static final String KEY_PROJECT = "lastProjectDir";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CreateExperimentUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);

        JFrame frame = new JFrame("Create Experiment");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        Font smallFont = new JLabel().getFont().deriveFont(Font.PLAIN, 11f);

        // === Regex filter controls ===
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JComboBox<String> regexCombo = new JComboBox<>();
        regexCombo.setEditable(true);
        regexCombo.setFont(smallFont);
        regexCombo.addItem(""); // always empty regex

        // Load regex history
        String lastRegex = "";
        for (int i = 0; ; i++) {
            String rx = prefs.get("regex." + i, null);
            if (rx == null) break;
            if (!rx.trim().isEmpty()) {
                regexCombo.addItem(rx.trim());
                lastRegex = rx.trim(); // remember last
            }
        }
        // Select last used regex if any
        if (!lastRegex.isEmpty()) {
            regexCombo.setSelectedItem(lastRegex);
        }
        // Right-click delete regex
        JTextField regexEditor = (JTextField) regexCombo.getEditor().getEditorComponent();
        JPopupMenu regexMenu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete this regex");
        regexMenu.add(deleteItem);
        regexEditor.setComponentPopupMenu(regexMenu);
        deleteItem.addActionListener(ev -> {
            String selected = regexEditor.getText().trim();
            if (!selected.isEmpty()) {
                regexCombo.removeItem(selected);
                saveRegexHistory(regexCombo, prefs);
            }
        });

        JButton filterButton = new JButton("Filter");
        filterButton.setFont(smallFont);
        filterButton.setPreferredSize(new Dimension(70, 22));

        topPanel.add(new JLabel("Regex:"), BorderLayout.WEST);
        topPanel.add(regexCombo, BorderLayout.CENTER);
        topPanel.add(filterButton, BorderLayout.EAST);

        // === File list ===
        DefaultListModel<File> listModel = new DefaultListModel<>();
        JList<File> fileList = new JList<>(listModel);
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileList.setFont(smallFont);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof File) setText(((File) value).getName());
                setFont(smallFont);
                return c;
            }
        });
        JScrollPane scrollPane = new JScrollPane(fileList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // === Input/output controls ===
        JPanel ioPanel = new JPanel();
        ioPanel.setLayout(new BoxLayout(ioPanel, BoxLayout.Y_AXIS));
        ioPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Dimension buttonSize = new Dimension(90, 22);

        final File[] inputDir = {new File(prefs.get(KEY_IMAGES, System.getProperty("user.home")))};
        final File[] outputDir = {new File(prefs.get(KEY_PROJECT, System.getProperty("user.home")))};

        // Images row
        JPanel imagesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton imagesButton = new JButton("Images");
        imagesButton.setPreferredSize(buttonSize);
        imagesButton.setFont(smallFont);
        JLabel inputDirLabel = new JLabel(inputDir[0].getAbsolutePath());
        inputDirLabel.setFont(smallFont);
        imagesRow.add(imagesButton);
        imagesRow.add(inputDirLabel);

        // Project row
        JPanel projectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton projectButton = new JButton("Project");
        projectButton.setPreferredSize(buttonSize);
        projectButton.setFont(smallFont);
        JLabel outputDirLabel = new JLabel(outputDir[0].getAbsolutePath());
        outputDirLabel.setFont(smallFont);
        projectRow.add(projectButton);
        projectRow.add(outputDirLabel);

        ioPanel.add(imagesRow);
        ioPanel.add(projectRow);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton processButton = new JButton("Process");
        JButton closeButton = new JButton("Close");
        processButton.setPreferredSize(new Dimension(80, 22));
        processButton.setFont(smallFont);
        closeButton.setPreferredSize(new Dimension(80, 22));
        closeButton.setFont(smallFont);
        actionPanel.add(processButton);
        actionPanel.add(closeButton);

        // Bottom container
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(ioPanel, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        // Layout
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // Refresh helper
        Runnable refresh = () -> {
            String regex = ((String) regexCombo.getEditor().getItem()).trim();
            if (!regex.isEmpty() && regex.length() <= 100 &&
                    ((DefaultComboBoxModel<String>) regexCombo.getModel()).getIndexOf(regex) == -1) {
                regexCombo.addItem(regex);
                saveRegexHistory(regexCombo, prefs);
            }
            refreshList(listModel, inputDir[0], regex, frame);
        };

        // Images chooser
        imagesButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser(inputDir[0]);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setFileHidingEnabled(true);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setDialogTitle("Select Images Directory");
            chooser.setPreferredSize(new Dimension(600, 350));
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                if (chosen != null && chosen.isDirectory() && !chosen.isHidden()) {
                    inputDir[0] = chosen;
                    inputDirLabel.setText(inputDir[0].getAbsolutePath());
                    prefs.put(KEY_IMAGES, inputDir[0].getAbsolutePath());
                    refresh.run();
                }
            }
        });

        // Project chooser
        projectButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser(outputDir[0]);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setFileHidingEnabled(true);
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setDialogTitle("Select Project Directory");
            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                if (chosen != null && chosen.isDirectory() && !chosen.isHidden()) {
                    outputDir[0] = chosen;
                    outputDirLabel.setText(outputDir[0].getAbsolutePath());
                    prefs.put(KEY_PROJECT, outputDir[0].getAbsolutePath());
                }
            }
        });

        // Regex combo + filter
        regexCombo.addActionListener(e -> refresh.run());
        filterButton.addActionListener(e -> refresh.run());

        // Process button
        processButton.addActionListener((ActionEvent e) -> {
            if (outputDir[0] == null) {
                JOptionPane.showMessageDialog(frame, "No project directory chosen.");
                return;
            }
            if (inputDir[0] == null) {
                JOptionPane.showMessageDialog(frame, "No images directory chosen.");
                return;
            }
            List<File> selectedFiles = fileList.getSelectedValuesList();
            if (selectedFiles.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No files selected to process.");
                return;
            }

            try {
                // 🔹 Use last component of Images dir to create experiment folder under Project dir
                String experimentName = inputDir[0].getName();
                File experimentDir = new File(outputDir[0], experimentName);
                if (!experimentDir.exists() && !experimentDir.mkdirs()) {
                    throw new IOException("Failed to create experiment directory: " + experimentDir);
                }

                // 🔹 Write ExperimentInfo.csv (with versioned name if exists)
                File createdFile = ExperimentInfoWriter.writeExperimentInfo(experimentDir, selectedFiles);

                fileList.clearSelection();
                JOptionPane.showMessageDialog(
                        frame,
                        "Experiment info written to:\n" + createdFile.getAbsolutePath(),
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // 🔹 Auto-open the CSV in the system default app
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(createdFile);
                }

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error writing ExperimentInfo.csv: " + ex.getMessage());
            }
        });

        closeButton.addActionListener(e -> frame.dispose());

        frame.setSize(400, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        if (inputDir[0] != null && inputDir[0].isDirectory()) {
            refresh.run();
        }
    }

    private static void saveRegexHistory(JComboBox<String> combo, Preferences prefs) {
        try {
            for (int i = 0; ; i++) {
                String key = "regex." + i;
                if (prefs.get(key, null) == null) break;
                prefs.remove(key);
            }
        } catch (Exception ignored) {}

        int idx = 0;
        for (int i = 0; i < combo.getItemCount(); i++) {
            String item = combo.getItemAt(i).trim();
            if (!item.isEmpty() && item.length() <= 100) {
                prefs.put("regex." + idx, item);
                idx++;
            }
        }
    }

    private static void refreshList(DefaultListModel<File> model, File dir, String regex, Component parent) {
        model.clear();
        if (dir == null) return;

        Pattern pattern = null;
        String rx = (regex == null) ? "" : regex.trim();
        if (!rx.isEmpty()) {
            try {
                pattern = Pattern.compile(rx);
            } catch (PatternSyntaxException ex) {
                JOptionPane.showMessageDialog(parent, "Invalid regex: " + ex.getDescription());
                return;
            }
        }

        File[] children = dir.listFiles(f -> !f.isHidden());
        if (children == null) return;

        for (File f : children) {
            if (f.isFile() && (pattern == null || pattern.matcher(f.getName()).matches())) {
                model.addElement(f);
            }
        }
    }
}