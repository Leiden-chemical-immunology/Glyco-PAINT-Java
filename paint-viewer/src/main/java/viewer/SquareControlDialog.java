package viewer;

import javax.swing.*;
import java.awt.*;

public class SquareControlDialog extends JDialog {

    private final JCheckBox showBordersCheckBox;
    private final JRadioButton numberNoneRadio;
    private final JRadioButton numberLabelRadio;
    private final JRadioButton numberSquareRadio;

    // Remember last selected number mode when borders are ON
    private SquareGridPanel.NumberMode lastNumberMode = SquareGridPanel.NumberMode.NONE;

    public SquareControlDialog(JFrame owner, SquareGridPanel gridPanel) {
        super(owner, "Square Controls", false);

        setLayout(new BorderLayout(10, 10));
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // --- Radio buttons ---
        numberNoneRadio = new JRadioButton("Show no number", true);
        numberLabelRadio = new JRadioButton("Show label number");
        numberSquareRadio = new JRadioButton("Show square number");

        ButtonGroup group = new ButtonGroup();
        group.add(numberNoneRadio);
        group.add(numberLabelRadio);
        group.add(numberSquareRadio);

        numberNoneRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);
            lastNumberMode = SquareGridPanel.NumberMode.NONE;
        });
        numberLabelRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL);
            lastNumberMode = SquareGridPanel.NumberMode.LABEL;
        });
        numberSquareRadio.addActionListener(e -> {
            gridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE);
            lastNumberMode = SquareGridPanel.NumberMode.SQUARE;
        });

        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.Y_AXIS));
        radioPanel.setBorder(BorderFactory.createTitledBorder("Numbers"));
        radioPanel.add(numberNoneRadio);
        radioPanel.add(numberLabelRadio);
        radioPanel.add(numberSquareRadio);

        // --- Show borders checkbox ---
        showBordersCheckBox = new JCheckBox("Show borders of selected squares", true);
        showBordersCheckBox.addActionListener(e -> {
            boolean show = showBordersCheckBox.isSelected();
            gridPanel.setShowBorders(show);

            if (!show) {
                // Remember current mode before forcing to NONE
                if (numberLabelRadio.isSelected()) lastNumberMode = SquareGridPanel.NumberMode.LABEL;
                else if (numberSquareRadio.isSelected()) lastNumberMode = SquareGridPanel.NumberMode.SQUARE;
                else lastNumberMode = SquareGridPanel.NumberMode.NONE;

                // Force to "Show no number"
                numberNoneRadio.setSelected(true);
                gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);

                // Disable radios
                numberNoneRadio.setEnabled(false);
                numberLabelRadio.setEnabled(false);
                numberSquareRadio.setEnabled(false);

            } else {
                // Re-enable radios
                numberNoneRadio.setEnabled(true);
                numberLabelRadio.setEnabled(true);
                numberSquareRadio.setEnabled(true);

                // Restore last remembered mode
                switch (lastNumberMode) {
                    case LABEL:
                        numberLabelRadio.setSelected(true);
                        gridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL);
                        break;
                    case SQUARE:
                        numberSquareRadio.setSelected(true);
                        gridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE);
                        break;
                    default:
                        numberNoneRadio.setSelected(true);
                        gridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);
                        break;
                }
            }
        });

        content.add(showBordersCheckBox);
        content.add(Box.createVerticalStrut(10));
        content.add(radioPanel);

        add(content, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}