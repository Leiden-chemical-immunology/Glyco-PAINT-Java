/******************************************************************************
 *  Class:        RecordingAttributesPanel.java
 *  Package:      paint.viewer.panels
 *
 *  PURPOSE:
 *    Displays the attributes of a {@link paint.viewer.utils.RecordingEntry}
 *    in a structured tabular format within the PAINT viewer interface.
 *
 *  DESCRIPTION:
 *    Provides a graphical panel for visualizing the key metadata of a
 *    recording entry, including probe characteristics, experiment parameters,
 *    and derived statistics such as Tau, Density, and R² thresholds.
 *
 *    The panel uses a non-editable {@link JTable} backed by a
 *    {@link javax.swing.table.DefaultTableModel}, allowing data to be
 *    refreshed dynamically from a given {@link RecordingEntry}.
 *    It supports both permanent updates and transient previews of
 *    parameter changes.
 *
 *  KEY FEATURES:
 *    • Displays probe, cell type, and recording statistics in a table view.
 *    • Supports real-time preview updates without altering data objects.
 *    • Formats numeric fields with configurable decimal precision.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.viewer.panels;

import paint.viewer.utils.RecordingEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * A Swing panel that presents attributes of a {@link RecordingEntry} in tabular form.
 * <p>
 * The panel includes a scrollable, non-editable table showing probe, experiment,
 * and computed parameters for the active recording. It supports both static
 * updates (via {@link #updateFromEntry}) and temporary previews
 * (via {@link #updatePreview}).
 */
public class RecordingAttributesPanel {

    private final JPanel            root;
    private final DefaultTableModel model;
    private final JTable            table;

    /**
     * Constructs a new {@code RecordingAttributesPanel}.
     * <p>
     * Initializes a styled, non-editable table for displaying recording attributes.
     * The layout uses a {@link BorderLayout} with compound borders and a scroll pane
     * for readability and integration within parent components.
     */
    public RecordingAttributesPanel() {
        root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        root.setPreferredSize(new Dimension(240, 0));

        model = new DefaultTableModel(new Object[]{"Attr", "Val"}, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(model);
        table.setRowHeight(22);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scroll = new JScrollPane(table);
        root.add(scroll, BorderLayout.CENTER);
    }

    /**
     * Returns the root Swing component representing this panel.
     *
     * @return the root {@link JComponent}
     */
    public JComponent getComponent() {
        return root;
    }

    /**
     * Updates the displayed attributes using data from the specified recording entry.
     *
     * @param recordingEntry the {@link RecordingEntry} containing the data
     * @param numSquares     the number of squares associated with this recording
     */
    public void updateFromEntry(RecordingEntry recordingEntry, int numSquares) {
        model.setRowCount(0);
        model.addRow(new Object[]{"Probe",             recordingEntry.getProbeName()});
        model.addRow(new Object[]{"Probe Type",        recordingEntry.getProbeType()});
        model.addRow(new Object[]{"Adjuvant",          recordingEntry.getAdjuvant()});
        model.addRow(new Object[]{"Cell Type",         recordingEntry.getCellType()});
        model.addRow(new Object[]{"Concentration",     recordingEntry.getConcentration()});
        model.addRow(new Object[]{"Number of Spots",   recordingEntry.getNumberOfSpots()});
        model.addRow(new Object[]{"Number of Tracks",  recordingEntry.getNumberOfTracks()});
        model.addRow(new Object[]{"Number of Squares", numSquares});
        model.addRow(new Object[]{"Threshold",         recordingEntry.getThreshold()});
        model.addRow(new Object[]{"Tau",               format(recordingEntry.getTau(), 1)});
        model.addRow(new Object[]{"Density",           recordingEntry.getDensity()});
        model.addRow(new Object[]{"Min Density Ratio", recordingEntry.getMinRequiredDensityRatio()});
        model.addRow(new Object[]{"Max Variability",   recordingEntry.getMaxAllowableVariability()});
        model.addRow(new Object[]{"Min R²",            recordingEntry.getMinRequiredRSquared()});
        model.addRow(new Object[]{"Neighbour Mode",    recordingEntry.getNeighbourMode()});
    }

    /**
     * Temporarily updates the displayed attributes to preview parameter changes.
     * <p>
     * This does not modify the underlying {@link RecordingEntry}.
     *
     * @param recordingEntry  the current recording entry (for static attributes)
     * @param numSquares      number of squares for the recording
     * @param tau             Tau value to preview
     * @param density         Density value to preview
     * @param minDensityRatio minimum density ratio threshold
     * @param maxVariability  maximum variability threshold
     * @param minRSquared     minimum R² threshold
     * @param neighbourMode   neighbour mode label (“Free”, “Relaxed”, “Strict”)
     */
    public void updatePreview(
            RecordingEntry recordingEntry,
            int            numSquares,
            double         tau,
            double         density,
            double         minDensityRatio,
            double         maxVariability,
            double         minRSquared,
            String         neighbourMode) {

        model.setRowCount(0);
        model.addRow(new Object[]{"Probe",               recordingEntry.getProbeName()});
        model.addRow(new Object[]{"Probe Type",          recordingEntry.getProbeType()});
        model.addRow(new Object[]{"Adjuvant",            recordingEntry.getAdjuvant()});
        model.addRow(new Object[]{"Cell Type",           recordingEntry.getCellType()});
        model.addRow(new Object[]{"Concentration",       recordingEntry.getConcentration()});
        model.addRow(new Object[]{"Number of Spots",     recordingEntry.getNumberOfSpots()});
        model.addRow(new Object[]{"Number of Tracks",    recordingEntry.getNumberOfTracks()});
        model.addRow(new Object[]{"Number of Squares",   numSquares});
        model.addRow(new Object[]{"Threshold",           recordingEntry.getThreshold()});
        model.addRow(new Object[]{"Tau",                 format(tau, 1)});
        model.addRow(new Object[]{"Density",             format(density, 2)});
        model.addRow(new Object[]{"Min Density Ratio",   format(minDensityRatio, 1)});
        model.addRow(new Object[]{"Max Variability",     format(maxVariability, 1)});
        model.addRow(new Object[]{"Min R²",              format(minRSquared, 2)});
        model.addRow(new Object[]{"Neighbour Mode",      neighbourMode});
    }

    /**
     * Formats a numeric value to the specified precision, handling NaN and infinity cases.
     *
     * @param v the value to format
     * @param p number of decimal places
     * @return formatted string representation of the value
     */
    private static String format(double v, int p) {
        if (Double.isNaN(v)) {
            return "NaN";
        }
        if (Double.isInfinite(v)) {
            return v > 0 ? "∞" : "-∞";
        }
        return String.format("%." + p + "f", v);
    }
}