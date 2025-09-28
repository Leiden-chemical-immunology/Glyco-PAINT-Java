package createexperiment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class CreateExperimentUI {
    // === Preferences setup ===
    private static final String PREF_NODE = "paint/create-experiment";
    private static final String KEY_IMAGES = "lastImagesDir";
    private static final String KEY_PROJECT = "lastProjectDir";
    private static final String KEY_REGEX_HISTORY = "regexHistory";

    public static void main(String[] args) {
        // Global UI tweaks for lighter look
        Font lightFont = new Font("SansSerif", Font.PLAIN, 11);
        UIManager.put("FileChooser.font", lightFont);
        UIManager.put("FileChooser.listFont", lightFont);
        UIManager.put("OptionPane.messageFont", lightFont);
        UIManager.put("OptionPane.buttonFont", lightFont);
        UIManager.put("Label.font", lightFont);
        UIManager.put("Button.font", lightFont);
        UIManager.put("TextField.font", lightFont);
        UIManager.put("List.font", lightFont);

        SwingUtilities.invokeLater(CreateExperimentUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);

        JFrame frame = new JFrame("Create Experiment");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        Font smallFont = new JLabel().getFont().deriveFont(Font.PLAIN, 11f);

        // === Regex filter controls (combo with history) ===
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JComboBox<String> regexCombo = new JComboBox<>();
        regexCombo.setEditable(true);
        regexCombo.setFont(smallFont);

        // Always ensure empty regex is first entry
        regexCombo.addItem("");

        // Load regex history from prefs
        String history = prefs.get(KEY_REGEX_HISTORY, "");
        if (!history.isEmpty()) {
            for (String rx : history.split(",")) {
                if (!rx.trim().isEmpty()) regexCombo.addItem(rx.trim());
            }
        }

        JButton filterButton = new JButton("Filter");
        filterButton.setFont(smallFont);
        filterButton.setPreferredSize(new Dimension(70, 22));

        topPanel.add(new JLabel("Regex:"), BorderLayout.WEST);
        topPanel.add(regexCombo, BorderLayout.CENTER);
        topPanel.add(filterButton, BorderLayout.EAST);

        // Right-click menu to delete regex patterns
        JPopupMenu popup = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("Delete Selected");
        popup.add(deleteItem);
        regexCombo.setComponentPopupMenu(popup);

        deleteItem.addActionListener(ev -> {
            String selected = (String) regexCombo.getSelectedItem();
            if (selected != null && !selected.isEmpty()) { // 🚫 never delete empty regex
                regexCombo.removeItem(selected);
                saveRegexHistory(regexCombo, prefs);
            }
        });

        // === File list (File-backed) ===
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

        // Last dirs from prefs
        final File[] inputDir = {new File(prefs.get(KEY_IMAGES, System.getProperty("user.home")))};
        final File[] outputDir = {new File(prefs.get(KEY_PROJECT, System.getProperty("user.home")))};

        // --- Images row ---
        JPanel imagesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        JButton imagesButton = new JButton("Images");
        imagesButton.setPreferredSize(buttonSize);
        imagesButton.setFont(smallFont);
        JLabel inputDirLabel = new JLabel(inputDir[0].getAbsolutePath());
        inputDirLabel.setFont(smallFont);
        imagesRow.add(imagesButton);
        imagesRow.add(inputDirLabel);

        // --- Project row ---
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

        // === Action buttons ===
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton processButton = new JButton("Process");
        JButton closeButton = new JButton("Close");
        processButton.setPreferredSize(new Dimension(80, 22));
        processButton.setFont(smallFont);
        closeButton.setPreferredSize(new Dimension(80, 22));
        closeButton.setFont(smallFont);
        actionPanel.add(processButton);
        actionPanel.add(closeButton);

        // === Bottom container ===
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 8));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(ioPanel, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        // Layout
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        // === Helper: refresh file list ===
        Runnable refresh = () -> {
            String regex = (String) regexCombo.getEditor().getItem();
            refreshList(listModel, inputDir[0], regex, frame);
        };

        // --- Images directory chooser ---
        imagesButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser(inputDir[0]);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Images Directory");
            chooser.setPreferredSize(new Dimension(600, 350));

            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                if (chosen != null && chosen.isDirectory()) {
                    inputDir[0] = chosen;
                    inputDirLabel.setText(inputDir[0].getAbsolutePath());
                    prefs.put(KEY_IMAGES, inputDir[0].getAbsolutePath());
                    refresh.run();
                    fileList.requestFocusInWindow();
                }
            }
        });

        // --- Project directory chooser ---
        projectButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser(outputDir[0]);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Select Project Directory");
            chooser.setPreferredSize(new Dimension(600, 350));

            int result = chooser.showOpenDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File chosen = chooser.getSelectedFile();
                if (chosen != null && chosen.isDirectory()) {
                    outputDir[0] = chosen;
                    outputDirLabel.setText(outputDir[0].getAbsolutePath());
                    prefs.put(KEY_PROJECT, outputDir[0].getAbsolutePath());
                }
            }
        });

        // --- Regex combo immediate filtering ---
        regexCombo.addActionListener(e -> {
            String regex = ((String) regexCombo.getEditor().getItem()).trim();
            if (!regex.isEmpty()) {
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) regexCombo.getModel();
                if (model.getIndexOf(regex) == -1) {
                    regexCombo.addItem(regex);
                }
                saveRegexHistory(regexCombo, prefs);
            }
            refresh.run();
        });

        // --- Filter button logic (still available) ---
        filterButton.addActionListener((ActionEvent e) -> {
            String regex = ((String) regexCombo.getEditor().getItem()).trim();
            if (!regex.isEmpty()) {
                DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) regexCombo.getModel();
                if (model.getIndexOf(regex) == -1) {
                    regexCombo.addItem(regex);
                }
                saveRegexHistory(regexCombo, prefs);
            }
            refresh.run();
        });

        // --- Attach popup also to combo dropdown list ---
        SwingUtilities.invokeLater(() -> {
            Object child = regexCombo.getUI().getAccessibleChild(regexCombo, 0);
            if (child instanceof JPopupMenu) {
                JPopupMenu comboPopup = (JPopupMenu) child;
                if (comboPopup.getComponent(0) instanceof JScrollPane) {
                    JScrollPane scroll = (JScrollPane) comboPopup.getComponent(0);
                    JList<?> list = (JList<?>) scroll.getViewport().getView();
                    list.setComponentPopupMenu(popup);
                }
            }
        });

        // --- Process button logic ---
        processButton.addActionListener((ActionEvent e) -> {
            if (outputDir[0] == null) {
                JOptionPane.showMessageDialog(frame, "No project directory chosen.",
                        "Error", JOptionPane.PLAIN_MESSAGE);
                return;
            }
            java.util.List<File> selectedFiles = fileList.getSelectedValuesList();
            if (selectedFiles.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No files selected to process.",
                        "Error", JOptionPane.PLAIN_MESSAGE);
                return;
            }
            for (File f : selectedFiles) {
                // TODO: Replace with your real processing logic
                System.out.println("Processing " + f.getAbsolutePath() + " -> " + outputDir[0].getAbsolutePath());
            }
            JOptionPane.showMessageDialog(frame, "Processing complete.",
                    "Info", JOptionPane.PLAIN_MESSAGE);
        });

        closeButton.addActionListener(e -> frame.dispose());

        frame.setSize(700, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Initial population if input dir was loaded from prefs
        if (inputDir[0] != null && inputDir[0].isDirectory()) {
            refresh.run();
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
                JOptionPane.showMessageDialog(parent, "Invalid regex: " + ex.getDescription(),
                        "Regex Error", JOptionPane.PLAIN_MESSAGE);
                return;
            }
        }

        File[] children = dir.listFiles(f -> !f.isHidden()); // 🚫 skip hidden files
        if (children == null) return;

        for (File f : children) {
            if (pattern == null || pattern.matcher(f.getName()).matches()) {
                model.addElement(f);
            }
        }

        if (model.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No matches.",
                    "Info", JOptionPane.PLAIN_MESSAGE);
        }
    }

    private static void saveRegexHistory(JComboBox<String> regexCombo, Preferences prefs) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) regexCombo.getModel();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < model.getSize(); i++) {
            String entry = model.getElementAt(i);
            if (entry != null && !entry.isEmpty()) { // skip empty placeholder
                if (sb.length() > 0) sb.append(",");
                sb.append(entry);
            }
        }
        prefs.put(KEY_REGEX_HISTORY, sb.toString());
    }
}