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
 * Swing dialog for configuring experiments in a Paint project.
 * <p>
 * The dialog supports two modes of operation:
 * <ul>
 *   <li>{@link DialogMode#GENERATE_SQUARES} – configure parameters
 *       for square generation.</li>
 *   <li>{@link DialogMode#TRACKMATE} – configure paths for running TrackMate.</li>
 * </ul>
 * <p>
 * Additionally, the dialog allows selecting which experiment subfolders
 * to include, and optionally persists the configuration back into the
 * {@code Paint Configuration.json} file.
 * </p>
 */
public class ProjectSpecificationDialog {

    /**
     * Dialog modes:
     * <ul>
     *     <li>{@link #TRACKMATE} – configure TrackMate input paths.</li>
     *     <li>{@link #GENERATE_SQUARES} – configure square generation parameters.</li>
     * </ul>
     */
    public enum DialogMode {
        TRACKMATE,
        GENERATE_SQUARES
    }

    /**
     * Functional callback interface invoked when the user
     * presses OK and calculations should be executed.
     */
    @FunctionalInterface
    public interface CalculationCallback {
        /**
         * Runs calculations on the specified project.
         *
         * @param project the project specification built from the dialog
         */
        boolean run(Project project);
    }

    private CalculationCallback calculationCallback;

    /**
     * Sets the callback to execute after the user presses OK.
     *
     * @param callback the callback implementation
     */
    public void setCalculationCallback(CalculationCallback callback) {
        this.calculationCallback = callback;
    }

    private final JDialog dialog;
    private final Path projectPath;
    private final Path configPath;
    private final Project project;
    private final PaintConfig paintConfig;

    // Fields for GENERATE_SQUARES mode
    private final JTextField nrSquaresField;
    private final JTextField minTracksField;
    private final JTextField minRSquaredField;
    private final JTextField minDensityRatioField;
    private final JTextField maxVariabilityField;

    // Field for TRACKMATE mode
    private final JTextField imageDirectoryField;

    private final JCheckBox saveExperimentsCheckBox;
    private final JPanel checkboxPanel = new JPanel();
    private final java.util.List<JCheckBox> checkBoxes = new ArrayList<>();
    private boolean okPressed = false;

    private final DialogMode mode;

    // --- Constructor ---

    /**
     * Creates a new project specification dialog.
     *
     * @param owner       the parent window that owns this dialog
     * @param projectPath the path to the Paint project
     * @param mode        the dialog mode (TrackMate or Generate Squares)
     */
    public ProjectSpecificationDialog(Frame owner, Path projectPath, DialogMode mode) {
        this.projectPath = projectPath;
        this.configPath = projectPath.resolve(PAINT_CONFIGURATION_JSON);
        this.paintConfig = PaintConfig.instance();
        this.project = new Project(projectPath);
        this.mode = mode;

        // Apply a consistent font for UI elements
        Font baseFont = new Font("Dialog", Font.PLAIN, 12);
        UIManager.put("Label.font", baseFont);
        UIManager.put("CheckBox.font", baseFont);
        UIManager.put("TextField.font", baseFont);
        UIManager.put("Button.font", baseFont);

        // Set dialog title
        String dialogTitle = (mode == DialogMode.TRACKMATE)
                ? "Run TrackMate on project"
                : "Generate Squares for Project";

        this.dialog = new JDialog(owner, dialogTitle, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // --- Build input panel ---
        JPanel formPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        if (mode == DialogMode.GENERATE_SQUARES) {
            // Pre-fill fields from config
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

            // Add labels and fields
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

            imageDirectoryField = null;
        } else {
            // TrackMate: image directory input
            nrSquaresField = null;
            minTracksField = null;
            minRSquaredField = null;
            minDensityRatioField = null;
            maxVariabilityField = null;

            JLabel dirLabel = new JLabel("Images Root:");
            String defaultDir = paintConfig.getString("Paths", "Images Root", System.getProperty("user.home"));
            JTextField dirField = new JTextField(defaultDir, 30);
            JButton browseButton = new JButton("Browse...");

            browseButton.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (chooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                    dirField.setText(chooser.getSelectedFile().getAbsolutePath());
                }
            });

            // Layout with GridBag
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

        // --- Experiment selection checkboxes ---
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

        // --- Bottom buttons ---
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

            if (calculationCallback != null) {
                setInputsEnabled(false);
                new Thread(() -> {
                    boolean success = calculationCallback.run(getProject());
                    SwingUtilities.invokeLater(() -> {
                        setInputsEnabled(true);
                        JOptionPane.showMessageDialog(dialog,
                                success
                                        ? "Calculations finished successfully! You can select new experiments or close this dialog."
                                        : "Calculations finished with errors. Please check the log.");
                    });
                }).start();
            }
        });

        cancelButton.addActionListener(e -> {
            okPressed = false;
            dialog.dispose(); // Close on Cancel
        });

        rightPanel.add(okButton);
        rightPanel.add(cancelButton);
        buttonPanel.add(rightPanel, BorderLayout.EAST);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Final dialog setup
        dialog.pack();
        dialog.setSize(mode == DialogMode.GENERATE_SQUARES ? 800 : 400, 600);
        dialog.setLocationRelativeTo(owner);
    }

    /**
     * Populates the experiment selection panel with checkboxes.
     * <p>
     * Each immediate subdirectory of the project directory is represented
     * as a checkbox. The selection state of each is restored
     * from the provided {@link PaintConfig}.
     *
     * @param config the configuration used to restore experiment selections
     */
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

    /**
     * Saves the current dialog state into the {@link PaintConfig}.
     * <ul>
     *   <li>If in {@link DialogMode#GENERATE_SQUARES}, saves square generation parameters.</li>
     *   <li>If in {@link DialogMode#TRACKMATE}, saves image and project directory paths.</li>
     *   <li>If "Save Experiments" is selected, also saves experiment checkbox states.</li>
     * </ul>
     */
    private void saveConfig() {
        if (mode == DialogMode.GENERATE_SQUARES) {
            paintConfig.setInt("Generate Squares", "Nr of Squares in Row", Integer.parseInt(nrSquaresField.getText()));
            paintConfig.setInt("Generate Squares", "Min Tracks to Calculate Tau", Integer.parseInt(minTracksField.getText()));
            paintConfig.setDouble("Generate Squares", "Min Required R Squared", Double.parseDouble(minRSquaredField.getText()));
            paintConfig.setDouble("Generate Squares", "Min Required Density Ratio", Double.parseDouble(minDensityRatioField.getText()));
            paintConfig.setDouble("Generate Squares", "Max Allowable Variability", Double.parseDouble(maxVariabilityField.getText()));
        } else {
            paintConfig.setString("Paths", "Images Root", imageDirectoryField.getText());
            paintConfig.setString("Paths", "Project Root", this.projectPath.toString());
        }

        if (saveExperimentsCheckBox.isSelected()) {
            for (JCheckBox cb : checkBoxes) {
                paintConfig.setBoolean("Experiments", cb.getText(), cb.isSelected());
            }
        }
        paintConfig.save();
    }

    /**
     * Builds a {@link Project} object from the current dialog state.
     *
     * @return a {@link Project} containing configuration and experiment selections
     */
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

    /**
     * Displays the dialog and blocks until it is closed.
     *
     * @return the resulting {@link Project} based on user selections
     */
    public Project showDialog() {
        dialog.setVisible(true);
        return getProject();
    }

    /**
     * Enables or disables all inputs in the dialog.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    private void setInputsEnabled(boolean enabled) {
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
        if (imageDirectoryField != null) {
            imageDirectoryField.setEnabled(enabled);
        }
        for (JCheckBox cb : checkBoxes) {
            cb.setEnabled(enabled);
        }
        saveExperimentsCheckBox.setEnabled(enabled);
    }

    /**
     * Creates a compact text field with an optional document filter.
     *
     * @param value  initial value
     * @param filter optional input filter
     * @return configured text field
     */
    private JTextField createTightTextField(String value, DocumentFilter filter) {
        JTextField field = new JTextField(value, 6);
        if (filter != null) {
            ((AbstractDocument) field.getDocument()).setDocumentFilter(filter);
        }
        field.setFont(field.getFont().deriveFont(Font.PLAIN, 12f));
        return field;
    }

    /**
     * Creates a label with a consistent lightweight font.
     *
     * @param text label text
     * @return configured label
     */
    private JLabel createLightLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 12f));
        return lbl;
    }

    /**
     * Document filter that restricts input to integers.
     */
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

    /**
     * Document filter that restricts input to floating-point numbers.
     */
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