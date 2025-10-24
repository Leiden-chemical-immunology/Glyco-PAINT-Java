package paint.shared.dialogs;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;
import paint.shared.objects.Project;
import paint.shared.prefs.PaintPrefs;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintRuntime;

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

public class ProjectDialog {

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
    private final JDialog         dialog;
    private       Path            projectPath;
    private final PaintConfig     paintConfig;
    private       Project         project;

    private JTextField            projectRootField;
    private JTextField            imageDirectoryField;

    // Squares params (shown in TRACKMATE + GENERATE_SQUARES)
    private JPanel                paramsPanel;
    private JCheckBox             runSquaresAfterTrackMateCheck; // only visible in TRACKMATE
    private JComboBox<String>     gridSizeCombo;
    private JTextField            minTracksField;
    private JTextField            minRSquaredField;
    private JTextField            minDensityRatioField;
    private JTextField            maxVariabilityField;

    private List<JLabel>          squareParamLabels = new ArrayList<>();

    private final JCheckBox       saveExperimentsCheckBox;
    private final JCheckBox       verboseCheckBox;
    private final JCheckBox       sweepCheckBox;

    private final JPanel          checkboxPanel = new JPanel();
    private final List<JCheckBox> checkBoxes = new ArrayList<>();

    private JButton               selectAllButton;
    private JButton               clearAllButton;
    private JButton               projectBrowseButton;
    private JButton               imagesBrowseButton;

    private final JButton         okButton;
    private final JButton         cancelButton;

    private volatile boolean      cancelled = false;
    private          boolean      okPressed = false;

    private final DialogMode      mode;
    // @formatter:on

    public ProjectDialog(Frame owner, Path initialProjectPath, DialogMode mode) {
        this.projectPath = initialProjectPath;
        this.paintConfig = PaintConfig.instance();
        this.project     = new Project(initialProjectPath);
        this.mode        = mode;

        String projectName = projectPath != null && projectPath.getFileName() != null
                ? projectPath.getFileName().toString()
                : "(none)";

        String dialogTitle;
        switch (mode) {
            case TRACKMATE: dialogTitle = "Run TrackMate on Project - '" + projectName + "'"; break;
            case VIEWER:    dialogTitle = "View Recordings for Project - '" + projectName + "'"; break;
            default:        dialogTitle = "Generate Squares for Project - '" + projectName + "'";
        }

        this.dialog = new JDialog(owner, dialogTitle, false);
        this.dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // ======= TOP FORM =======
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Dimension labelSize = new Dimension(160, 20);
        int row = 0;

        // Project Root
        addLabeledFieldWithBrowse(formPanel, gbc, row++, "Project Root:", labelSize,
                                  (tf) -> projectRootField = tf,
                                  () -> (projectPath != null ? projectPath.toString() : System.getProperty("user.home")),
                                  this::onProjectRootChosen);

        // Images Root (always shown, all modes)
        addLabeledFieldWithBrowse(formPanel, gbc, row++, "Images Root:", labelSize,
                                  (tf) -> imageDirectoryField = tf,
                                  () -> {
                                      String def = PaintPrefs.getString("Images Root", System.getProperty("user.home"));
                                      return (def == null || def.isEmpty()) ? System.getProperty("user.home") : def;
                                  },
                                  this::onImagesRootChosen);

        // Disable and grey out Images Root in Generate Squares mode
        if (mode == DialogMode.GENERATE_SQUARES) {
            // Text field: read-only (can copy text but not edit)
            imageDirectoryField.setEditable(false);
            imageDirectoryField.setBackground(UIManager.getColor("TextField.inactiveBackground"));

            // Disable the Browse button
            if (imagesBrowseButton != null) {
                imagesBrowseButton.setEnabled(false);
            }

            // Grey out the label
            for (Component comp : formPanel.getComponents()) {
                if (comp instanceof JLabel && ((JLabel) comp).getText().startsWith("Images Root:")) {
                    comp.setForeground(Color.GRAY);
                    break;
                }
            }
        }


        // ======= PARAMS BLOCK (TRACKMATE + GENERATE_SQUARES) =======
        if (mode == DialogMode.TRACKMATE || mode == DialogMode.GENERATE_SQUARES) {
            paramsPanel = new JPanel(new GridBagLayout());
            paramsPanel.setBorder(BorderFactory.createTitledBorder("Generate Squares Parameters"));
            GridBagConstraints pg = new GridBagConstraints();
            pg.insets = new Insets(5,5,5,5);
            pg.anchor = GridBagConstraints.WEST;

            // @formatter:off
            int nrOfSquaresInRecording = PaintConfig.getInt(   "Generate Squares", "Number of Squares in Recording", 400);
            int minTracks              = PaintConfig.getInt(   "Generate Squares", "Min Tracks to Calculate Tau",    20);
            double minRSquared         = PaintConfig.getDouble("Generate Squares", "Min Required R Squared",         0.1);
            double minDensityRatio     = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio",     2.0);
            double maxVariability      = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability",      10.0);
            // @formatter:on

            Dimension narrowFieldSize = new Dimension(80, 24);

            int prow = 0;

            // TrackMate-only toggle
            if (mode == DialogMode.TRACKMATE) {
                pg.gridx = 0;
                pg.gridy = prow;
                pg.gridwidth = 2;

                runSquaresAfterTrackMateCheck = new JCheckBox(
                        "Run Generate Squares after TrackMate",
                        PaintConfig.getBoolean("TrackMate", "Run Generate Squares After", true)
                );

                runSquaresAfterTrackMateCheck.addActionListener(e -> {
                    setSquaresParamsEnabled(runSquaresAfterTrackMateCheck.isSelected());
                    updateOkButtonState(); // ✅ new line
                });

                paramsPanel.add(runSquaresAfterTrackMateCheck, pg);
                prow++;
                pg.gridwidth = 1;
            }

            // Prepare to collect labels for the later grey-out
            squareParamLabels = new ArrayList<>();

            // Number of squares (grid)
            pg.gridx = 0;
            pg.gridy = prow;
            JLabel lblNumSquares = label("Number of Squares in Recording:", labelSize);
            squareParamLabels.add(lblNumSquares);
            paramsPanel.add(lblNumSquares, pg);

            pg.gridx = 1;
            String[] gridOptions = {"5x5","10x10","15x15","20x20","25x25","30x30","35x35","40x40"};
            gridSizeCombo = new JComboBox<>(gridOptions);
            int n = (int)Math.sqrt(nrOfSquaresInRecording);
            gridSizeCombo.setSelectedItem(n + "x" + n);
            paramsPanel.add(gridSizeCombo, pg);
            prow++;

            // Min Tracks
            pg.gridx = 0;
            pg.gridy = prow;
            JLabel lblMinTracks = label("Min Tracks to Calculate Tau:", labelSize);
            squareParamLabels.add(lblMinTracks);
            paramsPanel.add(lblMinTracks, pg);

            pg.gridx = 1;
            minTracksField = createTightTextField(String.valueOf(minTracks), new IntegerDocumentFilter());
            minTracksField.setPreferredSize(narrowFieldSize);
            paramsPanel.add(minTracksField, pg);
            prow++;

            // Min R²
            pg.gridx = 0;
            pg.gridy = prow;
            JLabel lblRSq = label("Min Required R²:", labelSize);
            squareParamLabels.add(lblRSq);
            paramsPanel.add(lblRSq, pg);

            pg.gridx = 1;
            minRSquaredField = createTightTextField(String.valueOf(minRSquared), new FloatDocumentFilter());
            minRSquaredField.setPreferredSize(narrowFieldSize);
            paramsPanel.add(minRSquaredField, pg);
            prow++;

            // Min Density Ratio
            pg.gridx = 0;
            pg.gridy = prow;
            JLabel lblDensity = label("Min Required Density Ratio:", labelSize);
            squareParamLabels.add(lblDensity);
            paramsPanel.add(lblDensity, pg);

            pg.gridx = 1;
            minDensityRatioField = createTightTextField(String.valueOf(minDensityRatio), new FloatDocumentFilter());
            minDensityRatioField.setPreferredSize(narrowFieldSize);
            paramsPanel.add(minDensityRatioField, pg);
            prow++;

            // Max Variability
            pg.gridx = 0;
            pg.gridy = prow;
            JLabel lblVar = label("Max Allowed Variability:", labelSize);
            squareParamLabels.add(lblVar);
            paramsPanel.add(lblVar, pg);

            pg.gridx = 1;
            maxVariabilityField = createTightTextField(String.valueOf(maxVariability), new FloatDocumentFilter());
            maxVariabilityField.setPreferredSize(narrowFieldSize);
            paramsPanel.add(maxVariabilityField, pg);
            // initial enable state in TrackMate
            if (mode == DialogMode.TRACKMATE) {
                setSquaresParamsEnabled(runSquaresAfterTrackMateCheck.isSelected());
            }

            // write-through grid size to config immediately on change
            gridSizeCombo.addActionListener(e -> {
                String sel = (String) gridSizeCombo.getSelectedItem();
                if (sel != null && sel.contains("x")) {
                    int side = Integer.parseInt(sel.split("x")[0].trim());
                    PaintConfig.setInt("Generate Squares", "Number of Squares in Recording", side * side);
                }
            });

            // add the params panel to the form
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
            formPanel.add(paramsPanel, gbc);
            gbc.gridwidth = 1; // reset
        }

        dialog.add(formPanel, BorderLayout.NORTH);

        // ======= EXPERIMENTS =======
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        populateCheckboxes();

        JScrollPane scrollPane = new JScrollPane(checkboxPanel);
        scrollPane.setPreferredSize(new Dimension(680, 240));
        scrollPane.setBorder(BorderFactory.createEtchedBorder());

        selectAllButton = new JButton("Select All");
        clearAllButton  = new JButton("Clear All");
        selectAllButton.addActionListener(e -> { checkBoxes.forEach(cb -> cb.setSelected(true)); updateOkButtonState(); });
        clearAllButton.addActionListener(e -> { checkBoxes.forEach(cb -> cb.setSelected(false)); updateOkButtonState(); });

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.add(selectAllButton);
        controlPanel.add(clearAllButton);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(controlPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        dialog.add(centerPanel, BorderLayout.CENTER);

        // ======= BOTTOM =======
        saveExperimentsCheckBox = new JCheckBox("Save Experiments", false);
        verboseCheckBox         = new JCheckBox("Verbose", PaintRuntime.isVerbose());
        sweepCheckBox           = new JCheckBox("Sweep", PaintConfig.getBoolean("Sweep Settings", "Sweep", false));

        verboseCheckBox.addActionListener(e -> onVerboseToggled(verboseCheckBox.isSelected()));


        // Handle user toggling Sweep checkbox
        sweepCheckBox.addActionListener(e -> {
            boolean enabled = sweepCheckBox.isSelected();
            PaintConfig.setBoolean("Sweep Settings", "Sweep", enabled);
            PaintConfig.instance().save();
            PaintLogger.infof("Sweep mode %s.", enabled ? "enabled" : "disabled");
            updateOkButtonState();
        });

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.add(saveExperimentsCheckBox);
        leftPanel.add(verboseCheckBox);
        leftPanel.add(sweepCheckBox);

        okButton     = new JButton("OK");
        cancelButton = new JButton("Cancel");

        // listeners affecting OK state
        for (JCheckBox cb : checkBoxes) {
            cb.addActionListener(e -> updateOkButtonState());
        }
        projectRootField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateOkButtonState());
        imageDirectoryField.getDocument().addDocumentListener((SimpleDocumentListener) e -> updateOkButtonState());

        // Also, re-enable OK when toggling these checkboxes
        if (runSquaresAfterTrackMateCheck != null) {
            runSquaresAfterTrackMateCheck.addActionListener(e -> updateOkButtonState());
        }
        if (sweepCheckBox != null) {
            sweepCheckBox.addActionListener(e -> updateOkButtonState());
        }

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.add(okButton);
        rightPanel.add(cancelButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(leftPanel, BorderLayout.WEST);
        bottomPanel.add(rightPanel, BorderLayout.EAST);
        dialog.add(bottomPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> onOkPressed());
        cancelButton.addActionListener(e -> { cancelled = true; dialog.dispose(); });

        // size and show
        int width  = 820;
        int height = (mode == DialogMode.VIEWER) ? 600 : 680;
        dialog.setMinimumSize(new Dimension(width, height));
        dialog.setPreferredSize(new Dimension(width, height));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        updateOkButtonState(); // reflect current state
    }

    // ======= Actions =======

    private void onOkPressed() {
        okPressed = true;
        cancelled = false;
        saveConfig(); // persist roots + experiments (+ params if edited)

        if (calculationCallback != null) {
            setInputsEnabled(false);
            okButton.setText("Running...");
            okButton.setEnabled(false);

            new Thread(() -> {
                boolean success = false;
                Exception caught = null;

                try {
                    success = calculationCallback.run(getProject());
                } catch (Exception ex) {
                    caught = ex;
                    PaintLogger.errorf("Error in callback: %s", ex.getMessage());
                }

                final boolean callbackSuccess = success;
                final Exception callbackError = caught;

                SwingUtilities.invokeLater(() -> {
                    setInputsEnabled(true);

                    if (callbackSuccess) {
                        // ✅ Success: show "Completed", then disable the button
                        okButton.setText("Completed");
                        okButton.setEnabled(false);
                        PaintLogger.infof("Operation completed successfully.");

                        // Reset text to "OK" (still disabled) after a short delay
                        new javax.swing.Timer(1500, evt -> okButton.setText("OK")).start();

                    } else {
                        // ❌ Failure: enable immediately to allow retry
                        okButton.setText("OK");
                        okButton.setEnabled(true);
                        String msg = (callbackError != null)
                                ? "An error occurred: " + callbackError.getMessage()
                                : "Operation finished with errors. Check the log.";
                        JOptionPane.showMessageDialog(dialog, msg,
                                                      "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                    // Dialog intentionally stays open in all modes.
                });
            }, "ProjectDialog-OK").start();
        }
    }

    private void onProjectRootChosen(File chosen) {
        if (chosen != null && chosen.isDirectory()) {
            projectRootField.setText(chosen.getAbsolutePath());
            reloadConfigForNewProject(chosen.toPath());
        }
    }

    private void onImagesRootChosen(File chosen) {
        if (chosen != null && chosen.isDirectory()) {
            imageDirectoryField.setText(chosen.getAbsolutePath());
            updateOkButtonState();
        }
    }

    // ======= Helpers =======

    private void addLabeledFieldWithBrowse(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            String labelText,
            Dimension labelSize,
            java.util.function.Consumer<JTextField> fieldOut,
            java.util.function.Supplier<String> defaultValueSupplier,
            java.util.function.Consumer<File> onChosen
    ) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JLabel lbl = new JLabel(labelText);
        lbl.setPreferredSize(labelSize);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JTextField tf = new JTextField(defaultValueSupplier.get(), 32);
        panel.add(tf, gbc);
        fieldOut.accept(tf);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        JButton browse = new JButton("Browse...");

        // ✅ store reference for enabling/disabling later
        if (labelText.startsWith("Project Root:")) {
            projectBrowseButton = browse;
        } else if (labelText.startsWith("Images Root:")) {
            imagesBrowseButton = browse;
        }

        browse.addActionListener(e -> {
            File current = new File(tf.getText().trim());
            if (!current.exists() || !current.isDirectory()) {
                current = new File(System.getProperty("user.home"));
            }
            FileDialog dialogChooser = new FileDialog((Frame) null, labelText, FileDialog.LOAD);
            dialogChooser.setDirectory(current.getAbsolutePath());
            System.setProperty("apple.awt.fileDialogForDirectories", "true");
            dialogChooser.setVisible(true);
            System.clearProperty("apple.awt.fileDialogForDirectories");
            String dir = dialogChooser.getDirectory();
            String name = dialogChooser.getFile();
            if (dir != null && name != null) {
                onChosen.accept(new File(dir, name));
            }
        });

        panel.add(browse, gbc);
    }

    private JLabel label(String text, Dimension pref) {
        JLabel l = new JLabel(text);
        l.setPreferredSize(pref);
        return l;
    }

    private void setSquaresParamsEnabled(boolean enabled) {
        Color fg = enabled ? UIManager.getColor("Label.foreground") : Color.GRAY;

        for (JLabel lbl : squareParamLabels)
            lbl.setForeground(fg);

        if (gridSizeCombo != null) {
            gridSizeCombo.setEnabled(enabled);
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

        File proj = new File(projectRootField.getText().trim());
        File img  = new File(imageDirectoryField.getText().trim());

        boolean rootsValid = proj.isDirectory() && img.isDirectory();

        okButton.setEnabled(anySelected && rootsValid);
    }

    private void populateCheckboxes() {
        checkboxPanel.removeAll();
        checkBoxes.clear();

        File[] subs = (projectPath != null ? projectPath.toFile().listFiles() : null);
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
    }

    private void saveConfig() {
        // Roots
        PaintPrefs.putString("Project Root", projectRootField.getText().trim());
        PaintPrefs.putString("Images Root",  imageDirectoryField.getText().trim());

        // Squares params (write if the panel exists; values used by either mode)
        if (paramsPanel != null) {
            if (gridSizeCombo != null) {
                String selected = (String) gridSizeCombo.getSelectedItem();
                if (selected != null && selected.contains("x")) {
                    int side = Integer.parseInt(selected.split("x")[0].trim());
                    PaintConfig.setInt("Generate Squares", "Number of Squares in Recording", side * side);
                }
            }
            if (minTracksField != null) {
                PaintConfig.setInt(   "Generate Squares", "Min Tracks to Calculate Tau", parseIntSafe(minTracksField.getText(), 11));
            }
            if (minRSquaredField != null) {
                PaintConfig.setDouble("Generate Squares", "Min Required R Squared",      parseDoubleSafe(minRSquaredField.getText(), 0.1));
            }
            if (minDensityRatioField != null) {
                PaintConfig.setDouble("Generate Squares", "Min Required Density Ratio",  parseDoubleSafe(minDensityRatioField.getText(), 2.0));
            }
            if (maxVariabilityField != null) {
                PaintConfig.setDouble("Generate Squares", "Max Allowable Variability",   parseDoubleSafe(maxVariabilityField.getText(), 10.0));
            }
            if (mode == DialogMode.TRACKMATE && runSquaresAfterTrackMateCheck != null) {
                PaintConfig.setBoolean("TrackMate", "Run Generate Squares After", runSquaresAfterTrackMateCheck.isSelected());
            }
        }

        // Experiments
        if (saveExperimentsCheckBox.isSelected()) {
            PaintConfig.removeSection("Experiments");
            for (JCheckBox cb : checkBoxes)
                PaintConfig.setBoolean("Experiments", cb.getText(), cb.isSelected());
        }

        paintConfig.save();
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }
    private static double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    private Project getProject() {
        TrackMateConfig trackMateConfig     = TrackMateConfig.from(paintConfig);
        GenerateSquaresConfig squaresConfig = GenerateSquaresConfig.from(paintConfig);

        List<String> experimentNames = new ArrayList<>();
        for (JCheckBox cb : checkBoxes)
            if (cb.isSelected()) experimentNames.add(cb.getText());

        Path imagesPath = null;
        String imgText = imageDirectoryField.getText().trim();
        if (!imgText.isEmpty()) imagesPath = Paths.get(imgText);

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
        dialog.setVisible(true); // stays open until user closes it
        return getProject();
    }

    private void setInputsEnabled(boolean enabled) {
        if (projectRootField != null) {
            projectRootField.setEnabled(enabled);
        }

        if (imageDirectoryField != null) {
            if (mode == DialogMode.GENERATE_SQUARES) {
                // Keep read-only and inactive in Generate Squares mode
                imageDirectoryField.setEditable(false);
                imageDirectoryField.setEnabled(true); // allow selection/copy
                imageDirectoryField.setBackground(UIManager.getColor("TextField.inactiveBackground"));
            } else {
                imageDirectoryField.setEnabled(enabled);
            }
        }

        boolean enableSquares = enabled;
        if (mode == DialogMode.TRACKMATE && runSquaresAfterTrackMateCheck != null) {
            runSquaresAfterTrackMateCheck.setEnabled(enabled);
            enableSquares = enabled && runSquaresAfterTrackMateCheck.isSelected();
        }
        setSquaresParamsEnabled(enableSquares);

        for (JCheckBox cb : checkBoxes) {
            cb.setEnabled(enabled);
        }
        saveExperimentsCheckBox.setEnabled(enabled);
        saveExperimentsCheckBox.setEnabled(enabled);

        if (selectAllButton != null) {
            selectAllButton.setEnabled(enabled);
        }
        if (clearAllButton != null) {
            clearAllButton.setEnabled(enabled);
        }
        if (projectBrowseButton != null) {
            projectBrowseButton.setEnabled(enabled);
        }
        if (imagesBrowseButton != null) {
            imagesBrowseButton.setEnabled(enabled);
        }
        if (sweepCheckBox != null) {
            sweepCheckBox.setEnabled(enabled);
        }
        if (verboseCheckBox != null) {
            verboseCheckBox.setEnabled(enabled);
        }
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
    public JDialog getDialog()    { return dialog; }

    public void setOkEnabled(boolean enabled) {
        if (okButton != null) okButton.setEnabled(enabled);
    }

    private void onVerboseToggled(boolean enabled) {
        // Persist preference immediately
        PaintRuntime.setVerbose(enabled);
        PaintLogger.infof("Verbose mode %s.", enabled ? "enabled" : "disabled");
    }

    public boolean isSweepSelected() {
        return sweepCheckBox != null && sweepCheckBox.isSelected();
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