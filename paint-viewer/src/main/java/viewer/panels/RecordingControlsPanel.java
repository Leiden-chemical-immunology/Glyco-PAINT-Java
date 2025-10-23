package viewer.panels;

import viewer.shared.SquareControlParams;

import javax.swing.*;
import java.awt.*;

/**
 * The RecordingControlsPanel class represents a user interface panel that provides
 * various controls for managing and interacting with recorded data or grid-based operations.
 * It contains buttons, checkboxes, and radio buttons organized to allow users to
 * perform actions such as filtering recordings, selecting squares, assigning cells,
 * playing recordings, toggling display settings, and changing number display modes.
 *
 * Listeners can be registered to handle user interactions with these controls.
 */
public class RecordingControlsPanel {
    public interface Listener {
        void onFilterRequested();
        void onSelectSquaresRequested();
        void onAssignCellsRequested();
        void onPlayRecordingRequested();
        void onBordersToggled(boolean showBorders);
        void onShadingToggled(boolean showShading);
        void onNumberModeChanged(SquareGridPanel.NumberMode mode);
        void onApplySquareControl(String scope, SquareControlParams params);
    }

    private final JPanel root;

    /**
     * Constructs a new RecordingControlsPanel, providing an interface for controlling
     * the recording features such as filtering, selecting squares, assigning cells,
     * and toggling display options like borders, shading, and number modes.
     *
     * @param listener the event listener that handles the actions triggered by the controls
     */
    public RecordingControlsPanel(final Listener listener) {
        root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        root.setPreferredSize(new Dimension(240, 0));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JButton filterButton = new JButton("Filter recordings");
        JButton squareButton = new JButton("Select Squares");
        JButton cellButton = new JButton("Assign Cells");
        JButton playButton = new JButton("Play Recording");

        for (JButton b : new JButton[]{filterButton, squareButton, cellButton, playButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28)); // stretch horizontally
        }

        // === Controls panel ===
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        controls.setBorder(BorderFactory.createTitledBorder("Controls"));
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, controls.getPreferredSize().height));

        controls.add(filterButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(squareButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(cellButton);
        controls.add(Box.createVerticalStrut(10));
        controls.add(playButton);

        // === Borders and shading ===
        JCheckBox showBorders = new JCheckBox("Show borders", true);
        JCheckBox showShading = new JCheckBox("Show shading", true);
        JPanel borders = new JPanel();
        borders.setLayout(new BoxLayout(borders, BoxLayout.Y_AXIS));
        borders.setBorder(BorderFactory.createTitledBorder("Borders and Shading"));
        borders.setAlignmentX(Component.LEFT_ALIGNMENT);
        borders.setMaximumSize(new Dimension(Integer.MAX_VALUE, borders.getPreferredSize().height));
        borders.add(showBorders);
        borders.add(Box.createVerticalStrut(5));
        borders.add(showShading);

        // === Number display ===
        JRadioButton none = new JRadioButton("None", true);
        JRadioButton label = new JRadioButton("Label");
        JRadioButton square = new JRadioButton("Square");
        ButtonGroup g = new ButtonGroup();
        g.add(none);
        g.add(label);
        g.add(square);

        JPanel numbers = new JPanel();
        numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
        numbers.setBorder(BorderFactory.createTitledBorder("Numbers"));
        numbers.setAlignmentX(Component.LEFT_ALIGNMENT);
        numbers.setMaximumSize(new Dimension(Integer.MAX_VALUE, numbers.getPreferredSize().height));
        numbers.add(none);
        numbers.add(Box.createVerticalStrut(5));
        numbers.add(label);
        numbers.add(Box.createVerticalStrut(5));
        numbers.add(square);

        // === Action listeners ===
        filterButton.addActionListener(e -> listener.onFilterRequested());
        squareButton.addActionListener(e -> listener.onSelectSquaresRequested());
        cellButton.addActionListener(e -> listener.onAssignCellsRequested());
        playButton.addActionListener(e -> listener.onPlayRecordingRequested());
        showBorders.addActionListener(e -> listener.onBordersToggled(showBorders.isSelected()));
        showShading.addActionListener(e -> listener.onShadingToggled(showShading.isSelected()));
        none.addActionListener(e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.NONE));
        label.addActionListener(e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.LABEL));
        square.addActionListener(e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.SQUARE));

        // === Layout order ===
        content.add(controls);
        content.add(Box.createVerticalStrut(10));
        content.add(borders);
        content.add(Box.createVerticalStrut(10));
        content.add(numbers);
        content.add(Box.createVerticalGlue());

        root.add(content, BorderLayout.NORTH);
    }

    public JComponent getComponent() {
        return root;
    }
}