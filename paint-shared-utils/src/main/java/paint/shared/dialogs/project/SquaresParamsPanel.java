package paint.shared.dialogs.project;

import paint.shared.config.paintconfig.PaintConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

public class SquaresParamsPanel {

    private final JPanel            panel = new JPanel(new GridBagLayout());
    private final DialogMode        mode;

    private JCheckBox               runAfterTrackMate;
    private JComboBox<String>       gridSizeCombo;
    private JTextField              minTracksField;
    private JTextField              minRSqField;
    private JTextField              minDensityField;
    private JTextField              maxVariabilityField;

    private Runnable onChange = () -> {
    };

    public SquaresParamsPanel(DialogMode mode, PaintConfig cfg) {
        this.mode = mode;

        panel.setBorder(new TitledBorder("Generate Squares Parameters"));
        final GridBagConstraints pg = new GridBagConstraints();
        pg.insets  = new Insets(5,5,5,5);
        pg.anchor  = GridBagConstraints.WEST;
        pg.fill    = GridBagConstraints.NONE;

        int nrSquares   = PaintConfig.getInt(   "Generate Squares", "Number of Squares in Recording", 400);
        int minTracks   = PaintConfig.getInt(   "Generate Squares", "Min Tracks to Calculate Tau",    20);
        double minRSq   = PaintConfig.getDouble("Generate Squares", "Min Required R Squared",         0.1);
        double minDens  = PaintConfig.getDouble("Generate Squares", "Min Required Density Ratio",     2.0);
        double maxVar   = PaintConfig.getDouble("Generate Squares", "Max Allowable Variability",      10.0);

        int row = 0;
        if (mode == DialogMode.TRACKMATE) {
            runAfterTrackMate = new JCheckBox(
                    "Run Generate Squares after TrackMate",
                    PaintConfig.getBoolean("TrackMate", "Run Generate Squares After", true)
            );
            pg.gridx = 0; pg.gridy = row; pg.gridwidth = 2;
            panel.add(runAfterTrackMate, pg);
            row++;
            pg.gridwidth = 1;

            runAfterTrackMate.addActionListener(e -> {
                setSquaresEnabled(runAfterTrackMate.isSelected());
                onChange.run();
            });
        }

        final Dimension labelSize = new Dimension(220, 20);
        final Dimension fieldSize = new Dimension(80, 24);

        // grid
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Number of Squares in Recording", labelSize, pg);
        pg.gridx = 1;
        gridSizeCombo = new JComboBox<>(new String[]{"5x5", "10x10", "15x15", "20x20", "25x25", "30x30", "35x35", "40x40"});
        int n = (int) Math.sqrt(nrSquares);
        gridSizeCombo.setSelectedItem(n + "x" + n);
        panel.add(gridSizeCombo, pg);
        row++;

        // Min R²
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Min Required R²", labelSize, pg);
        pg.gridx = 1;
        minRSqField = text(String.valueOf(minRSq), fieldSize, true);
        panel.add(minRSqField, pg);
        row++;

        // Min Density Ratio
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Min Required Density Ratio", labelSize, pg);
        pg.gridx = 1;
        minDensityField = text(String.valueOf(minDens), fieldSize, true);
        panel.add(minDensityField, pg);
        row++;

        // Max Variability
        pg.gridx = 0;
        pg.gridy = row;
        label(panel, "Max Allowable Variability", labelSize, pg);
        pg.gridx = 1;
        maxVariabilityField = text(String.valueOf(maxVar), fieldSize, true);
        panel.add(maxVariabilityField, pg);

        gridSizeCombo.addActionListener(e -> onChange.run());
        minRSqField.getDocument().addDocumentListener((SimpleDocumentListener) e -> onChange.run());
        minDensityField.getDocument().addDocumentListener((SimpleDocumentListener) e -> onChange.run());
        maxVariabilityField.getDocument().addDocumentListener((SimpleDocumentListener) e -> onChange.run());

        if (mode == DialogMode.TRACKMATE) {
            setSquaresEnabled(runAfterTrackMate.isSelected());
        }
    }

    public JPanel component() {
        return panel;
    }

    public void onParamsChanged(Runnable r) {
        this.onChange = (r != null ? r : () -> {
        });
    }

    public void setEnabled(boolean enabled) {
        if (runAfterTrackMate != null) {
            runAfterTrackMate.setEnabled(enabled);
        }
        boolean squaresEnabled = enabled && (runAfterTrackMate == null || runAfterTrackMate.isSelected());
        gridSizeCombo.setEnabled(squaresEnabled);
        minRSqField.setEnabled(squaresEnabled);
        minDensityField.setEnabled(squaresEnabled);
        maxVariabilityField.setEnabled(squaresEnabled);
    }

    public void persistTo(PaintConfig cfg, DialogMode mode) {
        if (gridSizeCombo != null) {
            String sel = (String) gridSizeCombo.getSelectedItem();
            if (sel != null && sel.contains("x")) {
                int side = Integer.parseInt(sel.split("x")[0].trim());
                PaintConfig.setInt("Generate Squares", "Number of Squares in Recording", side * side);
            }
        }
        if (minRSqField != null) {
            PaintConfig.setDouble("Generate Squares", "Min Required R Squared",
                                  parseDouble(minRSqField.getText(), 0.1));
        }
        if (minDensityField != null) {
            PaintConfig.setDouble("Generate Squares", "Min Required Density Ratio",
                                  parseDouble(minDensityField.getText(), 2.0));
        }
        if (maxVariabilityField != null) {
            PaintConfig.setDouble("Generate Squares", "Max Allowable Variability",
                                  parseDouble(maxVariabilityField.getText(), 10.0));
        }
        if (mode == DialogMode.TRACKMATE && runAfterTrackMate != null) {
            PaintConfig.setBoolean("TrackMate", "Run Generate Squares After", runAfterTrackMate.isSelected());
        }
        PaintConfig.instance().save();
    }

    private static void label(JPanel p, String text, Dimension size, GridBagConstraints pg) {
        JLabel l = new JLabel(text);
        l.setPreferredSize(size);
        p.add(l, pg);
    }

    private static JTextField text(String v, Dimension size, boolean numeric) {
        JTextField t = new JTextField(v);
        t.setColumns(8);
        t.setPreferredSize(size);
        return t;
    }

    private void setSquaresEnabled(boolean enabled) {
        gridSizeCombo.setEnabled(enabled);
        minRSqField.setEnabled(enabled);
        minDensityField.setEnabled(enabled);
        maxVariabilityField.setEnabled(enabled);
    }

    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}