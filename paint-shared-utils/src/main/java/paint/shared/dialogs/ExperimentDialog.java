package paint.shared.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.Project;

import static paint.shared.constants.PaintConstants.PAINT_CONFIGURATION_JSON;

/**
 * Dialog for configuring experiments (Generate Squares or TrackMate).
 */
public class ExperimentDialog {

    public enum DialogMode {
        TRACKMATE,
        GENERATE_SQUARES
    }

    @FunctionalInterface
    public interface CalculationCallback {
        void run(Project project);
    }

    private CalculationCallback calculationCallback;

    public void setCalculationCallback(CalculationCallback callback) {
        this.calculationCallback = callback;
    }

    private final JDialog dialog;
    private final Path projectPath;
    private final Path configPath;
    private final Project project;
    private final PaintConfig paintConfig;

    // Fields for GENERATE_SQUARES
    private final JTextField nrSquaresField;
    private final JTextField minTracksField;
    private final JTextField minRSquaredField;
    private final JTextField minDensityRatioField;
    private final JTextField maxVariabilityField;

    // Field for TRACKMATE
    private final JTextField imageDirectoryField;

    private final JCheckBox saveExperimentsCheckBox;

    private final JPanel checkboxPanel = new JPanel();
    private final java.util.List<JCheckBox> checkBoxes = new ArrayList<>();
    private boolean okPressed = false;

    private final DialogMode mode;

    // --- integrated constructor ---
    public ExperimentDialog(Frame owner, Path projectPath, DialogMode mode) {

        // Store key context
        this.projectPath = projectPath;
        this.configPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        this.paintConfig = PaintConfig.from(configPath);
        this.project = new Project(projectPath);
        this.mode = mode;

        // --- global font override for this dialog ---
        Font baseFont = new Font("Dialog", Font.PLAIN, 12);
        UIManager.put("Label.font", baseFont);
        UIManager.put("CheckBox.font", baseFont);
        UIManager.put("TextField.font", baseFont);
        UIManager.put("Button.font", baseFont);

        // 🚨 non-modal (false) so it doesn’t disappear automatically
        String dialogTitle;
        if (mode == DialogMode.TRACKMATE) {
            dialogTitle = "Run TrackMate on project";
        }
        else {
            dialogTitle = "Generate Squares for Project";
        }
        this.dialog = new JDialog(owner, dialogTitle, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel formPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (mode == DialogMode.GENERATE_SQUARES) {
            int nrSquares = paintConfig.getInt("Generate Squares", "Nr of Squares in Row", 5);
            int minTracks = paintConfig.getInt("Generate Squares", "Min Tracks to Calculate Tau", 11);
            double minRSquared = paintConfig.getDouble("Generate Squares", "Min Required R Squared", 0.1);
            double minDensityRatio = paintConfig.getDouble("Generate Squares", "Min Required Density Ratio", 2.0);
            double maxVariability = paintConfig.getDouble("Generate Squares", "Max Allowable Variability", 10.0);

            nrSquaresField = createTightTextField(String.valueOf(nrSquares), new IntegerDocumentFilter());
            minTracksField = createTightTextField(String.valueOf(minTracks), new IntegerDocumentFilter());
            minRSquaredField = createTightTextField(String.valueOf(minRSquared), new FloatDocumentFilter());
            minDensityRatioField = createTightTextField(String.valueOf(minDensityRatio), new FloatDocumentFilter());
            maxVariabilityField = createTightTextField(String.valueOf(maxVariability), new FloatDocumentFilter());

            formPanel.add(createLightLabel("Number of Squares in Row (and Column):"));
            formPanel.add(nrSquaresField);
            formPanel.add(createLightLabel("Min Number of Tracks to Calculate :"));
            formPanel.add(minTracksField);
            formPanel.add(createLightLabel("Min Required R²:"));
            formPanel.add(minRSquaredField);
            formPanel.add(createLightLabel("Min Required Density Ratio:"));
            formPanel.add(minDensityRatioField);
            formPanel.add(createLightLabel("Max Allowed Variability:"));
            formPanel.add(maxVariabilityField);

            imageDirectoryField = null;
        } else {
            nrSquaresField = null;
            minTracksField = null;
            minRSquaredField = null;
            minDensityRatioField = null;
            maxVariabilityField = null;

            JLabel dirLabel = new JLabel("Image Directory:");
            String defaultDir = paintConfig.getString("Paths", "Image Directory", System.getProperty("user.home"));
            JTextField dirField = new JTextField(defaultDir, 30);
            JButton browseButton = new JButton("Browse...");

            browseButton.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    dirField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            formPanel.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
            formPanel.add(dirLabel, gbc);

            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            formPanel.add(dirField, gbc);

            gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
            formPanel.add(browseButton, gbc);

            this.imageDirectoryField = dirField;
        }

        dialog.add(formPanel, BorderLayout.NORTH);

        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        populateCheckboxes(paintConfig);

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(600, 200));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

        JPanel buttonPanel = new JPanel(new BorderLayout());

        saveExperimentsCheckBox = new JCheckBox("Save Experiments", false);
        saveExperimentsCheckBox.setFont(saveExperimentsCheckBox.getFont().deriveFont(Font.PLAIN, 12f));
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(saveExperimentsCheckBox);
        buttonPanel.add(leftPanel, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            okPressed = true;
            saveConfig();

            if (calculationCallback != null && okPressed) {
                setInputsEnabled(false);
                new Thread(() -> {
                    calculationCallback.run(getProject());
                    SwingUtilities.invokeLater(() -> {
                        setInputsEnabled(true);
                        JOptionPane.showMessageDialog(dialog,
                                "Calculations finished! You can now close this dialog.");
                    });
                }).start();
            }
        });

        cancelButton.addActionListener(e -> {
            okPressed = false;
            dialog.dispose(); // close only on Cancel
        });

        rightPanel.add(okButton);
        rightPanel.add(cancelButton);
        buttonPanel.add(rightPanel, BorderLayout.EAST);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        if (mode == DialogMode.GENERATE_SQUARES) {
            dialog.setSize(800, 600);
        } else {
            dialog.setSize(800, 400);
        }
        dialog.setLocationRelativeTo(owner);
    }

    private void populateCheckboxes(PaintConfig config) {
        checkboxPanel.removeAll();
        checkBoxes.clear();

        if (project != null) {
            File[] subs = project.getProjectPath().toFile().listFiles();
            if (subs != null) {
                Arrays.sort(subs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
                for (File sub : subs) {
                    if (sub.isDirectory()) {
                        JCheckBox cb = new JCheckBox(sub.getName());
                        boolean savedState = config.getBoolean("Experiments", sub.getName(), false);
                        cb.setSelected(savedState);
                        checkboxPanel.add(cb);
                        checkBoxes.add(cb);
                    }
                }
            }
        }
        checkboxPanel.revalidate();
        checkboxPanel.repaint();
    }

    private void saveConfig() {
        if (mode == DialogMode.GENERATE_SQUARES) {
            paintConfig.setInt("Generate Squares", "Nr of Squares in Row", Integer.parseInt(nrSquaresField.getText()));
            paintConfig.setInt("Generate Squares", "Min Tracks to Calculate Tau", Integer.parseInt(minTracksField.getText()));
            paintConfig.setDouble("Generate Squares", "Min Required R Squared", Double.parseDouble(minRSquaredField.getText()));
            paintConfig.setDouble("Generate Squares", "Min Required Density Ratio", Double.parseDouble(minDensityRatioField.getText()));
            paintConfig.setDouble("Generate Squares", "Max Allowable Variability", Double.parseDouble(maxVariabilityField.getText()));
        }
        else {
            paintConfig.setString("Paths", "Image Directory", imageDirectoryField.getText());
            paintConfig.setString("Paths", "Project Directory", this.projectPath.toString());
        }

        if (saveExperimentsCheckBox.isSelected()) {
            for (JCheckBox cb : checkBoxes) {
                paintConfig.setBoolean("Experiments", cb.getText(), cb.isSelected());
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
        return new Project(
                okPressed,
                projectPath,
                null,
                experimentNames,
                paintConfig,
                generateSquaresConfig,
                trackMateConfig,
                null);
    }



    public Project showDialog() {
        dialog.setVisible(true);
        return getProject();
    }

    private void setInputsEnabled(boolean enabled) {
        if (nrSquaresField != null) nrSquaresField.setEnabled(enabled);
        if (minTracksField != null) minTracksField.setEnabled(enabled);
        if (minRSquaredField != null) minRSquaredField.setEnabled(enabled);
        if (minDensityRatioField != null) minDensityRatioField.setEnabled(enabled);
        if (maxVariabilityField != null) maxVariabilityField.setEnabled(enabled);

        if (imageDirectoryField != null) imageDirectoryField.setEnabled(enabled);

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
}