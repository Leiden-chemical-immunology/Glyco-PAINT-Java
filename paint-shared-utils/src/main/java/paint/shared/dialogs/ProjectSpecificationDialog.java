package paint.shared.dialogs;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import paint.shared.prefs.PaintPrefs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
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

    public void setCalculationCallback(CalculationCallback callback) {
        this.calculationCallback = callback;
    }

    // @formatter:off
    private final JDialog     dialog;
    private final Path        projectPath;
    private final Project     project;
    private final PaintConfig paintConfig;

    private JTextField        minTracksField;
    private JTextField        minRSquaredField;
    private JTextField        minDensityRatioField;
    private JTextField        maxVariabilityField;
    private JTextField        imageDirectoryField = null;
    private JComboBox<String> gridSizeCombo;

    private final JCheckBox       saveExperimentsCheckBox;
    private final JPanel          checkboxPanel = new JPanel();
    private final List<JCheckBox> checkBoxes = new ArrayList<>();
    private       boolean         okPressed = false;

    private final    JButton    okButton;
    private final    JButton    cancelButton;
    private volatile boolean    cancelled = false;

    private final    DialogMode mode;
    // @formatter:on

    public ProjectSpecificationDialog(Frame owner, Path projectPath, DialogMode mode) {
        // @formatter:off
        this.projectPath = projectPath;
        this.paintConfig = PaintConfig.instance();
        this.project     = new Project(projectPath);
        this.mode        = mode;
        // @formatter:on

        String projectName = projectPath.getFileName().toString();
        String dialogTitle = (mode == DialogMode.TRACKMATE)
                ? "Run TrackMate on Project - '" + projectName + "'"
                : "Generate Squares for Project - '" + projectName + "'";

        this.dialog = new JDialog(owner, dialogTitle, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dialog.dispose();
            }
        });

        JPanel formPanel = new JPanel();
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        formPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // All labels will share this width so they align with "Select All"
        Dimension labelSize = new Dimension(220, 20);

        if (mode == DialogMode.GENERATE_SQUARES) {

            // @formatter:off
            int nrOfSquaresInRecording = PaintConfig.getInt(   "Generate Squares", "Number of Squares in Recording", 400);
            int minTracks              = PaintConfig.getInt(   "Generate Squares", "Min Tracks to Calculate Tau", 11);
            double minRSquared         = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
            double minDensityRatio     = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
            double maxVariability      = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);

            int row   = 0;
            gbc.gridx = 0;
            gbc.gridy = row;
            // @formatter:on

            // === consistent sizing ===
            Dimension wideFieldSize   = new Dimension(100, 22);
            Dimension narrowFieldSize = new Dimension((int)(wideFieldSize.width * 0.7), wideFieldSize.height);

            JLabel lbl1 = new JLabel("Number of Squares in Recording:", SwingConstants.LEFT);
            lbl1.setPreferredSize(labelSize);
            formPanel.add(lbl1, gbc);

            gbc.gridx = 1;
            String[] gridOptions = { "5x5", "10x10", "15x15", "20x20", "25x25", "30x30", "35x35", "40x40" };
            gridSizeCombo = new JComboBox<>(gridOptions);
            int number = (int) Math.sqrt(nrOfSquaresInRecording);
            gridSizeCombo.setSelectedItem(number + "x" + number);
            gridSizeCombo.setPrototypeDisplayValue("000x000");

            JTextField sizeReference = new JTextField();
            sizeReference.setColumns(10);
            Dimension equalSize = sizeReference.getPreferredSize();

            gridSizeCombo.setPreferredSize(equalSize);
            gridSizeCombo.setMinimumSize(equalSize);
            gridSizeCombo.setMaximumSize(equalSize);

            JPanel comboWrapper = new JPanel(new BorderLayout());
            comboWrapper.add(gridSizeCombo, BorderLayout.CENTER);
            comboWrapper.setPreferredSize(equalSize);
            formPanel.add(comboWrapper, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            JLabel lbl2 = new JLabel("Min Number of Tracks to Calculate:", SwingConstants.LEFT);
            lbl2.setPreferredSize(labelSize);
            formPanel.add(lbl2, gbc);
            gbc.gridx = 1;
            minTracksField = createTightTextField(String.valueOf(minTracks), new IntegerDocumentFilter());
            minTracksField.setPreferredSize(narrowFieldSize);
            formPanel.add(minTracksField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            JLabel lbl3 = new JLabel("Min Required R²:", SwingConstants.LEFT);
            lbl3.setPreferredSize(labelSize);
            formPanel.add(lbl3, gbc);
            gbc.gridx = 1;
            minRSquaredField = createTightTextField(String.valueOf(minRSquared), new FloatDocumentFilter());
            minRSquaredField.setPreferredSize(narrowFieldSize);
            formPanel.add(minRSquaredField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            JLabel lbl4 = new JLabel("Min Required Density Ratio:", SwingConstants.LEFT);
            lbl4.setPreferredSize(labelSize);
            formPanel.add(lbl4, gbc);
            gbc.gridx = 1;
            minDensityRatioField = createTightTextField(String.valueOf(minDensityRatio), new FloatDocumentFilter());
            minDensityRatioField.setPreferredSize(narrowFieldSize);
            formPanel.add(minDensityRatioField, gbc);

            row++;
            gbc.gridx = 0;
            gbc.gridy = row;
            JLabel lbl5 = new JLabel("Max Allowed Variability:", SwingConstants.LEFT);
            lbl5.setPreferredSize(labelSize);
            formPanel.add(lbl5, gbc);
            gbc.gridx = 1;
            maxVariabilityField = createTightTextField(String.valueOf(maxVariability), new FloatDocumentFilter());
            maxVariabilityField.setPreferredSize(narrowFieldSize);
            formPanel.add(maxVariabilityField, gbc);

            gridSizeCombo.addActionListener(e -> {
                String selected = (String) gridSizeCombo.getSelectedItem();
                if (selected != null && selected.contains("x")) {
                    int n = Integer.parseInt(selected.split("x")[0].trim());
                    PaintConfig.setInt("Generate Squares", "Number of Squares in Recording", n * n);
                }
            });
        }

        if (mode == DialogMode.TRACKMATE) {
            JLabel dirLabel = new JLabel("Images Root:");
            dirLabel.setPreferredSize(labelSize);

            String defaultDir    = PaintPrefs.getString("Images Root", System.getProperty("user.home"));
            JTextField dirField  = new JTextField(defaultDir, 30);
            JButton browseButton = new JButton("Browse...");

            browseButton.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    dirField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(dirLabel, gbc);

            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            formPanel.add(dirField, gbc);

            gbc.gridx = 2;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            formPanel.add(browseButton, gbc);

            imageDirectoryField = dirField;
        }

        dialog.add(formPanel, BorderLayout.NORTH);

        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        populateCheckboxes();

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        scrollPane.setBorder(BorderFactory.createEtchedBorder());


        JPanel checkboxControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllButton = new JButton("Select All");
        JButton clearAllButton = new JButton("Clear All");
        selectAllButton.addActionListener(e -> {
            checkBoxes.forEach(cb -> cb.setSelected(true));
            updateOkButtonState();
        });
        clearAllButton.addActionListener(e -> {
            checkBoxes.forEach(cb -> cb.setSelected(false));
            updateOkButtonState();
        });

        checkboxControlPanel.add(selectAllButton);
        checkboxControlPanel.add(clearAllButton);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(checkboxControlPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new BorderLayout());

        saveExperimentsCheckBox = new JCheckBox("Save Experiments", false);
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(saveExperimentsCheckBox);
        buttonPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        okButton.setEnabled(checkBoxes.stream().anyMatch(JCheckBox::isSelected));
        for (JCheckBox cb : checkBoxes) {
            cb.addActionListener(e -> updateOkButtonState());
        }

        okButton.addActionListener(e -> {
            okPressed = true;
            cancelled = false;
            saveConfig();

            if (mode == DialogMode.VIEWER) {
                dialog.dispose();
                return;
            }

            if (calculationCallback != null) {
                setInputsEnabled(false);
                okButton.setEnabled(false);
                new Thread(() -> {
                    try {
                        Project project = getProject();
                        boolean success = calculationCallback.run(project);

                        SwingUtilities.invokeLater(() -> {
                            setInputsEnabled(true);
                            if (!cancelled) {
                                okButton.setEnabled(false);  // disable always, success or failure
                                if (success) {
                                    JOptionPane.showMessageDialog(dialog,
                                                                  "Calculations finished successfully!");
                                } else {
                                    JOptionPane.showMessageDialog(dialog,
                                                                  "Calculations finished with errors. Please check the log.");
                                }
                            }
                        });
                    } catch (Exception ex1) {
                        ex1.printStackTrace();
                    }
                }).start();
            } else {
                PaintLogger.warningf(">>> No calculationCallback set!");
            }
        });

        cancelButton.addActionListener(e -> {
            cancelled = true;
            dialog.dispose();
        });

        rightPanel.add(okButton);
        rightPanel.add(cancelButton);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();

        // ✅ Ensure the first selected experiment is visible after the dialog is shown
        dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!checkBoxes.isEmpty()) {
                        JCheckBox firstSelected = checkBoxes.stream()
                                .filter(JCheckBox::isSelected)
                                .findFirst()
                                .orElse(checkBoxes.get(0)); // fallback to first
                        Rectangle bounds = firstSelected.getBounds();
                        if (bounds != null) {
                            checkboxPanel.scrollRectToVisible(bounds);
                        }
                    }
                });
            }
        });

        dialog.setSize(mode == DialogMode.GENERATE_SQUARES ? 600 : 500, 600);
        dialog.setLocationRelativeTo(owner);
    }

    private void updateOkButtonState() {
        boolean anySelected = checkBoxes.stream().anyMatch(JCheckBox::isSelected);
        okButton.setEnabled(anySelected);
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
    }

    private void saveConfig() {
        if (mode == DialogMode.GENERATE_SQUARES) {
            String selected = (String) gridSizeCombo.getSelectedItem();
            if (selected != null && selected.contains("x")) {
                int n = Integer.parseInt(selected.split("x")[0].trim());
                PaintConfig.setInt("Generate Squares", "Number of Squares in Recording", n * n);
            }
            PaintConfig.setInt(   "Generate Squares", "Min Tracks to Calculate Tau", Integer.parseInt(minTracksField.getText()));
            PaintConfig.setDouble("Generate Squares", "Min Required R Squared",      Double.parseDouble(minRSquaredField.getText()));
            PaintConfig.setDouble("Generate Squares", "Min Required Density Ratio",  Double.parseDouble(minDensityRatioField.getText()));
            PaintConfig.setDouble("Generate Squares", "Max Allowable Variability",   Double.parseDouble(maxVariabilityField.getText()));
        } else if (mode == DialogMode.TRACKMATE) {
            PaintPrefs.putString("Images Root", imageDirectoryField.getText());
        }
        PaintPrefs.putString("Project Root", this.projectPath.toString());

        if (saveExperimentsCheckBox.isSelected()) {
            PaintConfig.removeSection("Experiments");
            for (JCheckBox cb : checkBoxes) {
                PaintConfig.setBoolean("Experiments", cb.getText(), cb.isSelected());
            }
        }
        paintConfig.save();
    }

    private Project getProject() {
        TrackMateConfig trackMateConfig = TrackMateConfig.from(paintConfig);
        GenerateSquaresConfig generateSquaresConfig = GenerateSquaresConfig.from(paintConfig);
        List<String> experimentNames = new ArrayList<>();
        for (JCheckBox cb : checkBoxes) {
            if (cb.isSelected()) experimentNames.add(cb.getText());
        }
        return new Project(okPressed, projectPath, null, experimentNames,
                           paintConfig, generateSquaresConfig, trackMateConfig, null);
    }

    public Project showDialog() {
        dialog.setVisible(true);
        return getProject();
    }

    private void setInputsEnabled(boolean enabled) {
        if (mode == DialogMode.GENERATE_SQUARES) {
            if (minTracksField != null) minTracksField.setEnabled(enabled);
            if (minRSquaredField != null) minRSquaredField.setEnabled(enabled);
            if (minDensityRatioField != null) minDensityRatioField.setEnabled(enabled);
            if (maxVariabilityField != null) maxVariabilityField.setEnabled(enabled);
        }
        if (mode == DialogMode.TRACKMATE && imageDirectoryField != null) imageDirectoryField.setEnabled(enabled);
        for (JCheckBox cb : checkBoxes) cb.setEnabled(enabled);
        saveExperimentsCheckBox.setEnabled(enabled);
    }

    private JTextField createTightTextField(String value, DocumentFilter filter) {
        JTextField field = new JTextField(value);
        field.setColumns(10);
        field.setPreferredSize(new Dimension(80, 22));
        if (filter != null) ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);
        return field;
    }

    static class IntegerDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string != null && string.matches("\\d*")) super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text != null && text.matches("\\d*")) super.replace(fb, offset, length, text, attrs);
        }
    }

    static class FloatDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
                throws BadLocationException {
            if (string != null && string.matches("\\d*(\\.\\d*)?")) super.insertString(fb, offset, string, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (text != null && text.matches("\\d*(\\.\\d*)?")) super.replace(fb, offset, length, text, attrs);
        }
    }

    public void setOkEnabled(boolean enabled) {
        if (okButton != null) okButton.setEnabled(enabled);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public JDialog getDialog() {
        return dialog;
    }
}