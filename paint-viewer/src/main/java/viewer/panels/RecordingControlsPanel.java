package viewer.panels;

import viewer.shared.SquareControlParams;

import javax.swing.*;
import java.awt.*;

public class RecordingControlsPanel {
    public interface Listener {
        void onFilterRequested();
        void onSelectSquaresRequested();
        void onAssignCellsRequested();
        void onBordersToggled(boolean showBorders);
        void onShadingToggled(boolean showShading);
        void onNumberModeChanged(SquareGridPanel.NumberMode mode);
        void onApplySquareControl(String scope, SquareControlParams params);
    }

    private final JPanel root;

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

        for (JButton b : new JButton[]{filterButton, squareButton, cellButton}) {
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        JPanel controls = new JPanel(new BorderLayout());
        controls.setBorder(BorderFactory.createTitledBorder("Controls"));
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.add(filterButton);
        inner.add(Box.createVerticalStrut(10));
        inner.add(squareButton);
        inner.add(Box.createVerticalStrut(10));
        inner.add(cellButton);
        controls.add(inner, BorderLayout.CENTER);

        JCheckBox showBorders = new JCheckBox("Show borders", true);
        JCheckBox showShading = new JCheckBox("Show shading", true);
        JPanel borders = new JPanel();
        borders.setLayout(new BoxLayout(borders, BoxLayout.Y_AXIS));
        borders.setBorder(BorderFactory.createTitledBorder("Borders and Shading"));
        borders.add(showBorders);
        borders.add(Box.createVerticalStrut(5));
        borders.add(showShading);

        JRadioButton none = new JRadioButton("None", true);
        JRadioButton label = new JRadioButton("Label");
        JRadioButton square = new JRadioButton("Square");
        ButtonGroup g = new ButtonGroup();
        g.add(none); g.add(label); g.add(square);
        JPanel numbers = new JPanel();
        numbers.setLayout(new BoxLayout(numbers, BoxLayout.Y_AXIS));
        numbers.setBorder(BorderFactory.createTitledBorder("Numbers"));
        numbers.add(none);
        numbers.add(Box.createVerticalStrut(5));
        numbers.add(label);
        numbers.add(Box.createVerticalStrut(5));
        numbers.add(square);

        filterButton.addActionListener(e -> listener.onFilterRequested());
        squareButton.addActionListener(e -> listener.onSelectSquaresRequested());
        cellButton.addActionListener(e -> listener.onAssignCellsRequested());
        showBorders.addActionListener(e -> listener.onBordersToggled(showBorders.isSelected()));
        showShading.addActionListener(e -> listener.onShadingToggled(showShading.isSelected()));
        none.addActionListener(e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.NONE));
        label.addActionListener(e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.LABEL));
        square.addActionListener(e -> listener.onNumberModeChanged(SquareGridPanel.NumberMode.SQUARE));

        content.add(controls);
        content.add(Box.createVerticalStrut(5));
        content.add(borders);
        content.add(Box.createVerticalStrut(15));
        content.add(numbers);
        content.add(Box.createVerticalGlue());
        root.add(content, BorderLayout.NORTH);
    }

    public JComponent getComponent() { return root; }
}