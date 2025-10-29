package paint.viewer.panels;

import paint.viewer.utils.RecordingEntry;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * A panel that displays the attributes of a `RecordingEntry` in a tabular format.
 *
 * This class provides a graphical user interface component to visualize key details
 * of a given `RecordingEntry`, such as probe properties, experiment parameters, and
 * statistical information. The panel is updated dynamically based on the data provided
 * through the `updateFromEntry` method.
 */
public class RecordingAttributesPanel {
    private final JPanel root;
    private final DefaultTableModel model;
    private final JTable table;

    /**
     * Constructs a RecordingAttributesPanel for displaying attributes of a recording in a tabular format.
     *
     * This constructor initializes the graphical components, including a table to display the attributes,
     * and sets their layout and styles. The table is non-editable and displays attributes in two columns:
     * "Attr" for attribute names and "Val" for their corresponding values. The visualization is designed
     * to be embedded into a parent component, such that it is scrollable and visually organized.
     *
     * The root panel uses a BorderLayout and applies a compound border for better visual distinction.
     * Its preferred width is configured while allowing flexible height adjustments.
     *
     * Key features include:
     * - A scrollable table for displaying attributes.
     * - A non-editable table model to ensure data integrity.
     * - Styled table headers for better readability.
     */
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

    public JComponent getComponent() {
        return root;
    }

    /**
     * Updates the table model with attributes from the provided {@link RecordingEntry} instance and the given number of squares.
     * The method resets the table data and populates it with key attributes related to the recording.
     *
     * @param e           the {@link RecordingEntry} object containing details about a recording
     * @param numSquares  the number of squares associated with the recording
     */
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

    /**
     * Temporarily updates the attribute table to preview parameter changes
     * without modifying the underlying {@link RecordingEntry}.
     *
     * @param recordingEntry                the current recording entry (for static fields like Probe, Adjuvant, etc.)
     * @param numSquares       number of squares in this recording
     * @param tau              Tau value to preview
     * @param density          Density value to preview
     * @param minDensityRatio  minimum density ratio threshold
     * @param maxVariability   maximum variability threshold
     * @param minRSquared      minimum R² threshold
     * @param neighbourMode    neighbour mode string ("Free", "Relaxed", "Strict")
     */
    public void updatePreview(RecordingEntry recordingEntry,
                              int            numSquares,
                              double         tau,
                              double         density,
                              double         minDensityRatio,
                              double         maxVariability,
                              double         minRSquared,
                              String         neighbourMode) {

        // rebuild the model with the new transient values
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
     * Formats the given double value to a string with a specified number of decimal places.
     * If the value is NaN (Not a Number), it returns "NaN".
     * If the value is infinite, it returns "∞" for positive infinity or "-∞" for negative infinity.
     *
     * @param v the double value to format
     * @param p the number of decimal places to include in the formatted string
     * @return a string representation of the given double value formatted to the specified precision,
     *         or "NaN"/"∞"/"-∞" for special cases
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