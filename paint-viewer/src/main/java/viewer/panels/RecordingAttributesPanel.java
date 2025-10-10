package viewer.panels;

import viewer.utils.RecordingEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class RecordingAttributesPanel {
    private final JPanel root;
    private final DefaultTableModel model;
    private final JTable table;

    public RecordingAttributesPanel() {
        root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        root.setPreferredSize(new Dimension(240, 0));

        model = new DefaultTableModel(new Object[]{"Attr", "Val"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(22);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scroll = new JScrollPane(table);
        root.add(scroll, BorderLayout.CENTER);
    }

    public JComponent getComponent() { return root; }

    public void updateFromEntry(RecordingEntry e, int numSquares) {
        model.setRowCount(0);
        model.addRow(new Object[]{"Probe", e.getProbeName()});
        model.addRow(new Object[]{"Probe Type", e.getProbeType()});
        model.addRow(new Object[]{"Adjuvant", e.getAdjuvant()});
        model.addRow(new Object[]{"Cell Type", e.getCellType()});
        model.addRow(new Object[]{"Concentration", e.getConcentration()});
        model.addRow(new Object[]{"Number of Spots", e.getNumberOfSpots()});
        model.addRow(new Object[]{"Number of Tracks", e.getNumberOfTracks()});
        model.addRow(new Object[]{"Number of Squares", numSquares});
        model.addRow(new Object[]{"Threshold", e.getThreshold()});
        model.addRow(new Object[]{"Tau", format(e.getTau(), 1)});
        model.addRow(new Object[]{"Density", e.getDensity()});
        model.addRow(new Object[]{"Min Density Ratio", e.getMinRequiredDensityRatio()});
        model.addRow(new Object[]{"Max Variability", e.getMaxAllowableVariability()});
        model.addRow(new Object[]{"Min R²", e.getMinRequiredRSquared()});
        model.addRow(new Object[]{"Neighbour Mode", e.getNeighbourMode()});
    }

    private static String format(double v, int p) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        return String.format("%." + p + "f", v);
    }
}