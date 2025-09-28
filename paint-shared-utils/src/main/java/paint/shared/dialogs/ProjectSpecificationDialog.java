package paint.shared.dialogs;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.Project;
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

    private final JDialog dialog;
    private final Path projectPath;
    private final Project project;
    private final PaintConfig paintConfig;

    private final JTextField nrSquaresField;
    private final JTextField minTracksField;
    private final JTextField minRSquaredField;
    private final JTextField minDensityRatioField;
    private final JTextField maxVariabilityField;
    private JTextField imageDirectoryField = null;

    private final JCheckBox saveExperimentsCheckBox;
    private final JPanel checkboxPanel = new JPanel();
    private final java.util.List<JCheckBox> checkBoxes = new ArrayList<>();
    private boolean okPressed = false;

    private JButton okButton;
    private JButton cancelButton;
    private volatile boolean cancelled = false;
    private int cancelCount = 0;

    private final DialogMode mode;

    public ProjectSpecificationDialog(Frame owner, Path projectPath, DialogMode mode) {
        this.projectPath = projectPath;
        this.paintConfig = PaintConfig.instance();
        this.project = new Project(projectPath);
        this.mode = mode;

        Font baseFont = new Font("Dialog", Font.PLAIN, 12);
        UIManager.put("Label.font", baseFont);
        UIManager.put("CheckBox.font", baseFont);
        UIManager.put("TextField.font", baseFont);
        UIManager.put("Button.font", baseFont);

        String projectName = projectPath.getFileName().toString();
        String dialogTitle = (mode == DialogMode.TRACKMATE)
                ? "Run TrackMate on Project - '" + projectName + "'"
                : "Generate Squares for Project - '" + projectName + "'";

        // 🔑 Modal so setVisible() blocks until closed
        this.dialog = new JDialog(owner, dialogTitle, false);

        this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                dialog.dispose();
            }
        });

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (mode == DialogMode.GENERATE_SQUARES) {

            // Display the fields to enter parameters
            int nrSquares = PaintConfig.getInt("Generate Squares", "Nr of Squares in Row", 5);
            int minTracks = PaintConfig.getInt("Generate Squares", "Min Tracks to Calculate Tau", 11);
            double minRSquared = PaintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
            double minDensityRatio = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
            double maxVariability = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);

            nrSquaresField = createTightTextField(String.valueOf(nrSquares), new IntegerDocumentFilter());
            minTracksField = createTightTextField(String.valueOf(minTracks), new IntegerDocumentFilter());
            minRSquaredField = createTightTextField(String.valueOf(minRSquared), new FloatDocumentFilter());
            minDensityRatioField = createTightTextField(String.valueOf(minDensityRatio), new FloatDocumentFilter());
            maxVariabilityField = createTightTextField(String.valueOf(maxVariability), new FloatDocumentFilter());

            formPanel.add(createLightLabel("Number of Squares in Row (and Column):"));
            formPanel.add(nrSquaresField);
            formPanel.add(createLightLabel("Min Number of Tracks to Calculate:"));
            formPanel.add(minTracksField);
            formPanel.add(createLightLabel("Min Required R²:"));
            formPanel.add(minRSquaredField);
            formPanel.add(createLightLabel("Min Required Density Ratio:"));
            formPanel.add(minDensityRatioField);
            formPanel.add(createLightLabel("Max Allowed Variability:"));
            formPanel.add(maxVariabilityField);
        } else {
            nrSquaresField = null;
            minTracksField = null;
            minRSquaredField = null;
            minDensityRatioField = null;
            maxVariabilityField = null;
        }

        if (mode == DialogMode.TRACKMATE) {
            // Display the images root option
            JLabel dirLabel = new JLabel("Images Root:");
            String defaultDir = PaintConfig.getString("Paths", "Images Root", System.getProperty("user.home"));
            JTextField dirField = new JTextField(defaultDir, 30);
            JButton browseButton = new JButton("Browse...");

            browseButton.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    dirField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            // Set the lay-out of the Directory label, textfield and Browsw button
            formPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

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

        // And the experiment selection we do in all cases
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        populateCheckboxes();

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        scrollPane.setBorder(BorderFactory.createEtchedBorder());

        JPanel checkboxControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAllButton = new JButton("Select All");
        JButton clearAllButton = new JButton("Clear All");
        selectAllButton.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(true)));
        clearAllButton.addActionListener(e -> checkBoxes.forEach(cb -> cb.setSelected(false)));

        checkboxControlPanel.add(selectAllButton);
        checkboxControlPanel.add(clearAllButton);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(checkboxControlPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        // --- bottom panel ---
        JPanel buttonPanel = new JPanel(new BorderLayout());

        saveExperimentsCheckBox = new JCheckBox("Save Experiments", false);
        saveExperimentsCheckBox.setFont(saveExperimentsCheckBox.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(saveExperimentsCheckBox);
        buttonPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            okPressed = true;
            cancelled = false;
            cancelCount = 0;
            saveConfig();

            // For the VIEWER case - Just close, RecordingViewer will open the viewer window
            if (mode == DialogMode.VIEWER) {
                dialog.dispose();
                return;
            }

            // For the other cases we use callbacks because lengthy calculations are triggered
            if (calculationCallback != null) {
                setInputsEnabled(false);
                okButton.setEnabled(false);
                new Thread(() -> {
                    try {
                        Project p = getProject();
                        boolean success = calculationCallback.run(p);

                        SwingUtilities.invokeLater(() -> {
                            setInputsEnabled(true);
                            okButton.setEnabled(true);
                            if (!cancelled) {
                                JOptionPane.showMessageDialog(dialog,
                                        success
                                                ? "Calculations finished successfully!"
                                                : "Calculations finished with errors. Please check the log.");
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
            cancelCount++;
            if (cancelCount == 1) {
                cancelled = true;
                JOptionPane.showMessageDialog(dialog,
                        "Processing will stop after the current recording finishes.",
                        "Cancellation Requested",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                dialog.dispose();
            }
        });

        rightPanel.add(okButton);
        rightPanel.add(cancelButton);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setSize(mode == DialogMode.GENERATE_SQUARES ? 600 : 500, 600);
        dialog.setLocationRelativeTo(owner);
    }

    private void populateCheckboxes() {
        checkboxPanel.removeAll();
        checkBoxes.clear();
        File[] subs = project.getProjectPath().toFile().listFiles();
        if (subs != null) {
            Arrays.sort(subs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File sub : subs) {
                if (sub.isDirectory()) {
                    File file = new File(sub, EXPERIMENT_INFO_CSV);
                    if (!file.isFile()) {
                        continue;
                    }
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
            PaintConfig.setInt("Generate Squares", "Nr of Squares in Row", Integer.parseInt(nrSquaresField.getText()));
            PaintConfig.setInt("Generate Squares", "Min Tracks to Calculate Tau", Integer.parseInt(minTracksField.getText()));
            PaintConfig.setDouble("Generate Squares", "Min Required R Squared", Double.parseDouble(minRSquaredField.getText()));
            PaintConfig.setDouble("Generate Squares", "Min Required Density Ratio", Double.parseDouble(minDensityRatioField.getText()));
            PaintConfig.setDouble("Generate Squares", "Max Allowable Variability", Double.parseDouble(maxVariabilityField.getText()));
        } else if (mode == DialogMode.TRACKMATE) {
            PaintConfig.setString("Paths", "Images Root", imageDirectoryField.getText());
            PaintConfig.setString("Paths", "Project Root", this.projectPath.toString());
        } else if (mode == DialogMode.VIEWER) {
            PaintConfig.setString("Paths", "Project Root", this.projectPath.toString());
        }

        if (saveExperimentsCheckBox.isSelected()) {
            // Clean out the Experiments section first
            PaintConfig.removeSection("Experiments");

            // And then write fresh experiments
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
            if (cb.isSelected()) {
                experimentNames.add(cb.getText());
            }
        }
        return new Project(okPressed, projectPath, null, experimentNames, paintConfig, generateSquaresConfig, trackMateConfig, null);
    }

    public Project showDialog() {
        dialog.setVisible(true);
        return getProject();
    }

    private void setInputsEnabled(boolean enabled) {
        if (mode == DialogMode.GENERATE_SQUARES) {
            if (nrSquaresField != null) {
                nrSquaresField.setEnabled(enabled);
            }
            if (minTracksField != null) {
                minTracksField.setEnabled(enabled);
            }
            if (minRSquaredField != null) {
                minRSquaredField.setEnabled(enabled);
            }
            if (minDensityRatioField != null) {
                minDensityRatioField.setEnabled(enabled);
            }
            if (maxVariabilityField != null) {
                maxVariabilityField.setEnabled(enabled);
            }
        }
        if (mode == DialogMode.TRACKMATE) {
            if (imageDirectoryField != null) {
                imageDirectoryField.setEnabled(enabled);
            }
        }
        for (JCheckBox cb : checkBoxes) {
            cb.setEnabled(enabled);
        }
        saveExperimentsCheckBox.setEnabled(enabled);
    }

    private JTextField createTightTextField(String value, DocumentFilter filter) {
        JTextField field = new JTextField(value, 6);
        if (filter != null) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);
        }
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f));
        return field;
    }

    private JLabel createLightLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        return lbl;
    }

    static class IntegerDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d*")) {
                super.insertString(fb, offset, string, attr);
            }
        }
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text != null && text.matches("\\d*")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    static class FloatDocumentFilter extends DocumentFilter {
        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string != null && string.matches("\\d*(\\.\\d*)?")) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text != null && text.matches("\\d*(\\.\\d*)?")) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }

    public void setOkEnabled(boolean enabled) {
        if (okButton != null) {
            okButton.setEnabled(enabled);
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public JDialog getDialog() {
        return dialog;
    }
}