package paint.shared.dialogs;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.Project;
import paint.shared.prefs.PaintPrefs;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;

public class ProjectSpecificationDialog {

    public enum DialogMode {
        TRACKMATE,
        GENERATE_SQUARES,
        VIEWER
    }

    @FunctionalInterface
    public interface CalculationCallback {
        boolean run(Project project);
    }

    private CalculationCallback calculationCallback;
    public void setCalculationCallback(CalculationCallback callback) { this.calculationCallback = callback; }

    // @formatter:off
    private final JDialog     dialog;
    private       Path        projectPath;
    private final PaintConfig paintConfig;
    private       Project     project;

    private JTextField        projectRootField;
    private JTextField        imageDirectoryField;
    private JTextField        minTracksField;
    private JTextField        minRSquaredField;
    private JTextField        minDensityRatioField;
    private JTextField        maxVariabilityField;
    private JComboBox<String> gridSizeCombo;

    private final JCheckBox       saveExperimentsCheckBox;
    private final JPanel          checkboxPanel = new JPanel();
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private       boolean         okPressed = false;

    private final JButton         okButton;
    private final JButton         cancelButton;
    private volatile boolean      cancelled = false;

    private final DialogMode      mode;
    // @formatter:on

    public ProjectSpecificationDialog(Frame owner, Path initialProjectPath, DialogMode mode) {
        this.projectPath = initialProjectPath;
        this.paintConfig = PaintConfig.instance();
        this.project     = new Project(initialProjectPath);
        this.mode        = mode;

        String projectName = projectPath.getFileName() != null
                ? projectPath.getFileName().toString()
                : "(none)";
        String dialogTitle;
        switch (mode) {
            case TRACKMATE:
                dialogTitle = "Run TrackMate on Project - '" + projectName + "'";
                break;
            case VIEWER:
                dialogTitle = "View Recordings for Project - '" + projectName + "'";
                break;
            default:
                dialogTitle = "Generate Squares for Project - '" + projectName + "'";
        }

        this.dialog = new JDialog(owner, dialogTitle, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        Dimension labelSize = new Dimension(220, 20);

        // === PROJECT ROOT SELECTION ===
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel rootLabel = new JLabel("Project Root:");
        rootLabel.setPreferredSize(labelSize);
        formPanel.add(rootLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        projectRootField = new JTextField(projectPath.toString(), 30);
        formPanel.add(projectRootField, gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton browseRootButton = new JButton("Browse...");

        browseRootButton.addActionListener(e -> {
            File current = new File(projectRootField.getText().trim());
            if (!current.exists() || !current.isDirectory()) {
                current = new File(System.getProperty("user.home"));
            }
            FileDialog dialogChooser = new FileDialog((Frame) null, "Select Project Root", FileDialog.LOAD);
            dialogChooser.setDirectory(current.getAbsolutePath());
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            dialogChooser.setVisible(true);
            System.clearProperty("apple.awt.fileDialogForDirectories");

            String selectedDir = dialogChooser.getDirectory();
            if (selectedDir != null) {
                String selectedFile = dialogChooser.getFile();
                if (selectedFile != null) {
                    File selected = new File(selectedDir, selectedFile);
                    projectRootField.setText(selected.getAbsolutePath());
                    reloadConfigForNewProject(selected.toPath());
                }
            }
        });
        formPanel.add(browseRootButton, gbc);

        // === IMAGES ROOT SELECTION (TRACKMATE + VIEWER MODES) ===
        int row = 1;
        if (mode == DialogMode.TRACKMATE || mode == DialogMode.VIEWER) {
            gbc.gridx = 0;
            gbc.gridy = row;
            JLabel imageRootLabel = new JLabel("Images Root:");
            imageRootLabel.setPreferredSize(labelSize);
            formPanel.add(imageRootLabel, gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            String imagesRootPref = PaintPrefs.getString("Images Root", System.getProperty("user.home"));
            imageDirectoryField = new JTextField(imagesRootPref, 30);
            formPanel.add(imageDirectoryField, gbc);

            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            JButton browseImageButton = new JButton("Browse...");
            browseImageButton.addActionListener(e -> {
                File current = new File(imageDirectoryField.getText().trim());
                if (!current.exists() || !current.isDirectory()) {
                    current = new File(System.getProperty("user.home"));
                }
                FileDialog dialogChooser = new FileDialog((Frame) null, "Select Images Root", FileDialog.LOAD);
                dialogChooser.setDirectory(current.getAbsolutePath());
                System.setProperty("apple.awt.fileDialogForDirectories", "true");
                dialogChooser.setVisible(true);
                System.clearProperty("apple.awt.fileDialogForDirectories");

                String selectedDir = dialogChooser.getDirectory();
                if (selectedDir != null) {
                    String selectedFile = dialogChooser.getFile();
                    if (selectedFile != null) {
                        File selected = new File(selectedDir, selectedFile);
                        imageDirectoryField.setText(selected.getAbsolutePath());
                    }
                }
            });
            formPanel.add(browseImageButton, gbc);
            row++;
        }

        // === GENERATE SQUARES PARAMETERS ===
        if (mode == DialogMode.GENERATE_SQUARES) {
            int nrOfSquaresInRecording = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", 400);
            int minTracks              = PaintConfig.getInt("Generate Squares", "Min Tracks to Calculate Tau", 11);
            double minRSquared         = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
            double minDensityRatio     = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
            double maxVariability      = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);

            Dimension narrowFieldSize = new Dimension(70, 22);

            JLabel lbl1 = new JLabel("Number of Squares in Recording:");
            lbl1.setPreferredSize(labelSize);
            gbc.gridx = 0; gbc.gridy = row;
            formPanel.add(lbl1, gbc);
            gbc.gridx = 1;
            String[] gridOptions = {"5x5", "10x10", "15x15", "20x20", "25x25", "30x30", "35x35", "40x40"};
            gridSizeCombo = new JComboBox<>(gridOptions);
            int number = (int) Math.sqrt(nrOfSquaresInRecording);
            gridSizeCombo.setSelectedItem(number + "x" + number);
            formPanel.add(gridSizeCombo, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            JLabel lbl2 = new JLabel("Min Tracks to Calculate Tau:");
            lbl2.setPreferredSize(labelSize);
            formPanel.add(lbl2, gbc);
            gbc.gridx = 1;
            minTracksField = createTightTextField(String.valueOf(minTracks), new IntegerDocumentFilter());
            minTracksField.setPreferredSize(narrowFieldSize);
            formPanel.add(minTracksField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            JLabel lbl3 = new JLabel("Min Required R²:");
            lbl3.setPreferredSize(labelSize);
            formPanel.add(lbl3, gbc);
            gbc.gridx = 1;
            minRSquaredField = createTightTextField(String.valueOf(minRSquared), new FloatDocumentFilter());
            minRSquaredField.setPreferredSize(narrowFieldSize);
            formPanel.add(minRSquaredField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            JLabel lbl4 = new JLabel("Min Required Density Ratio:");
            lbl4.setPreferredSize(labelSize);
            formPanel.add(lbl4, gbc);
            gbc.gridx = 1;
            minDensityRatioField = createTightTextField(String.valueOf(minDensityRatio), new FloatDocumentFilter());
            minDensityRatioField.setPreferredSize(narrowFieldSize);
            formPanel.add(minDensityRatioField, gbc);
            row++;

            gbc.gridx = 0; gbc.gridy = row;
            JLabel lbl5 = new JLabel("Max Allowed Variability:");
            lbl5.setPreferredSize(labelSize);
            formPanel.add(lbl5, gbc);
            gbc.gridx = 1;
            maxVariabilityField = createTightTextField(String.valueOf(maxVariability), new FloatDocumentFilter());
            maxVariabilityField.setPreferredSize(narrowFieldSize);
            formPanel.add(maxVariabilityField, gbc);
        }

        dialog.add(formPanel, BorderLayout.NORTH);

        // === EXPERIMENT SELECTION ===
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        populateCheckboxes();

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        scrollPane.setBorder(BorderFactory.createEtchedBorder());

        JButton selectAll = new JButton("Select All");
        JButton clearAll  = new JButton("Clear All");
        selectAll.addActionListener(e -> { checkBoxes.forEach(cb -> cb.setSelected(true)); updateOkButtonState(); });
        clearAll.addActionListener(e -> { checkBoxes.forEach(cb -> cb.setSelected(false)); updateOkButtonState(); });

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(selectAll);
        controlPanel.add(clearAll);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(controlPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        // === BOTTOM BUTTONS ===
        saveExperimentsCheckBox = new JCheckBox("Save Experiments", false);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(saveExperimentsCheckBox);

        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        okButton.setEnabled(false); // will update dynamically
        for (JCheckBox cb : checkBoxes) {
            cb.addActionListener(e -> updateOkButtonState());
        }
        projectRootField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateOkButtonState());
        if (imageDirectoryField != null)
            imageDirectoryField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateOkButtonState());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(okButton);
        rightPanel.add(cancelButton);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(leftPanel, BorderLayout.WEST);
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            okPressed = true;
            cancelled = false;
            saveConfig();

            if (calculationCallback != null) {
                setInputsEnabled(false);
                okButton.setEnabled(false);

                new Thread(() -> {
                    try {
                        boolean success = calculationCallback.run(getProject());
                        SwingUtilities.invokeLater(() -> {
                            setInputsEnabled(true);
                            okButton.setEnabled(true);

                            // ✅ Dialog stays open for all modes
                            if (!success) {
                                JOptionPane.showMessageDialog(dialog,
                                                              "Operation failed. Please check the log.",
                                                              "Warning",
                                                              JOptionPane.WARNING_MESSAGE);
                            } else {
                                // Optional success notice for Viewer mode
                                if (mode == DialogMode.VIEWER) {
                                    PaintLogger.infof("Viewer launched successfully.");
                                }
                            }
                        });
                    } catch (Exception ex1) {
                        PaintLogger.errorf("Error in callback: %s", ex1.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            setInputsEnabled(true);
                            okButton.setEnabled(true);
                            JOptionPane.showMessageDialog(dialog,
                                                          "Unexpected error: " + ex1.getMessage(),
                                                          "Error",
                                                          JOptionPane.ERROR_MESSAGE);
                        });
                    }
                }).start();
            }
        });

        cancelButton.addActionListener(e -> {
            cancelled = true;
            dialog.dispose();
        });

        // === Ensure adequate dialog size ===
        int width  = (mode == DialogMode.GENERATE_SQUARES) ? 800 : 700;
        int height = 600;
        dialog.setMinimumSize(new Dimension(width, height));
        dialog.setPreferredSize(new Dimension(width, height));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        updateOkButtonState();
    }

    private void reloadConfigForNewProject(Path newRoot) {
        this.projectPath = newRoot;
        this.project = new Project(newRoot);
        PaintLogger.infof("Switched project root to: %s", newRoot);
        PaintConfig.initialise(newRoot);
        populateCheckboxes();
        checkboxPanel.revalidate();
        checkboxPanel.repaint();
        updateOkButtonState();
    }

    private void updateOkButtonState() {
        if (okButton == null) return;

        boolean anySelected = checkBoxes.stream().anyMatch(JCheckBox::isSelected);
        boolean rootsValid = true;

        if (mode == DialogMode.GENERATE_SQUARES || mode == DialogMode.TRACKMATE || mode == DialogMode.VIEWER) {
            File proj = new File(projectRootField.getText().trim());
            File img  = (imageDirectoryField != null) ? new File(imageDirectoryField.getText().trim()) : null;
            rootsValid = proj.isDirectory() && (img == null || img.isDirectory());
        }

        okButton.setEnabled(anySelected && rootsValid);
    }

    private void populateCheckboxes() {
        checkboxPanel.removeAll();
        checkBoxes.clear();

        File[] subs = project.getProjectRootPath().toFile().listFiles();
        if (subs != null) {
            Arrays.sort(subs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File sub : subs) {
                if (sub.isDirectory()) {
                    File file = new File(sub, EXPERIMENT_INFO_CSV);
                    if (!file.isFile()) continue;

                    JCheckBox cb = new JCheckBox(sub.getName());
                    boolean savedState = PaintConfig.getBoolean("Experiments", sub.getName(), false);
                    cb.setSelected(savedState);
                    checkboxPanel.add(cb);
                    checkBoxes.add(cb);
                }
            }
        }

        for (JCheckBox cb : checkBoxes) cb.addActionListener(e -> updateOkButtonState());
        updateOkButtonState();
        checkboxPanel.revalidate();
        checkboxPanel.repaint();
    }

    private void saveConfig() {
        PaintPrefs.putString("Project Root", projectRootField.getText().trim());
        if (imageDirectoryField != null)
            PaintPrefs.putString("Images Root", imageDirectoryField.getText().trim());
        if (saveExperimentsCheckBox.isSelected()) {
            PaintConfig.removeSection("Experiments");
            for (JCheckBox cb : checkBoxes)
                PaintConfig.setBoolean("Experiments", cb.getText(), cb.isSelected());
        }
        paintConfig.save();
    }

    private Project getProject() {
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);
        GenerateSquaresConfig squaresConfig = GenerateSquaresConfig.from(paintConfig);
        List<String> experimentNames = new ArrayList<>();
        for (JCheckBox cb : checkBoxes)
            if (cb.isSelected()) experimentNames.add(cb.getText());

        Path imagesPath = null;
        if (imageDirectoryField != null) {
            String imgText = imageDirectoryField.getText().trim();
            if (!imgText.isEmpty()) imagesPath = Paths.get(imgText);
        }

        return new Project(
                okPressed,
                projectPath,
                imagesPath,
                experimentNames,
                paintConfig,
                squaresConfig,
                trackMateConfig,
                null
        );
    }

    public Project showDialog() {
        dialog.setVisible(true);
        return getProject();
    }

    private void setInputsEnabled(boolean enabled) {
        if (projectRootField != null) projectRootField.setEnabled(enabled);
        if (imageDirectoryField != null) imageDirectoryField.setEnabled(enabled);
        if (minTracksField != null) minTracksField.setEnabled(enabled);
        if (minRSquaredField != null) minRSquaredField.setEnabled(enabled);
        if (minDensityRatioField != null) minDensityRatioField.setEnabled(enabled);
        if (maxVariabilityField != null) maxVariabilityField.setEnabled(enabled);
        checkBoxes.forEach(cb -> cb.setEnabled(enabled));
        saveExperimentsCheckBox.setEnabled(enabled);
    }

    private JTextField createTightTextField(String value, DocumentFilter filter) {
        JTextField field = new JTextField(value);
        field.setColumns(8);
        if (filter != null) ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);
        return field;
    }

    static class IntegerDocumentFilter extends DocumentFilter {
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException { if (string.matches("\\d*")) super.insertString(fb, offset, string, attr); }
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException { if (text.matches("\\d*")) super.replace(fb, offset, length, text, attrs); }
    }

    static class FloatDocumentFilter extends DocumentFilter {
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException { if (string.matches("\\d*(\\.\\d*)?")) super.insertString(fb, offset, string, attr); }
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException { if (text.matches("\\d*(\\.\\d*)?")) super.replace(fb, offset,length, text, attrs); }
    }

    public boolean isCancelled() { return cancelled; }
    public JDialog getDialog() { return dialog; }

    public void setOkEnabled(boolean enabled) {
        if (okButton != null) okButton.setEnabled(enabled);
    }

    // Simple lambda-friendly document listener
    @FunctionalInterface
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}