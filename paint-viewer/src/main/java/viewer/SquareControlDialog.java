package viewer;

import javax.swing.*;
import java.awt.*;

public class SquareControlDialog extends JDialog {

    private JCheckBox showBordersCheckBox;
    private JRadioButton noNumberRadio;
    private JRadioButton labelNumberRadio;
    private JRadioButton squareNumberRadio;

    private SquareGridPanel gridPanel;

    public SquareControlDialog(Frame owner, SquareGridPanel gridPanel) {
        super(owner, "Square Controls", false); // non-modal
        this.gridPanel = gridPanel;

        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // === Show Borders ===
        showBordersCheckBox = new JCheckBox("Show borders of selected squares", true);
        showBordersCheckBox.addActionListener(e ->
                gridPanel.setShowBorders(showBordersCheckBox.isSelected())
        );

        // === Number display radio buttons ===
        noNumberRadio = new JRadioButton("Show no number", true);
        labelNumberRadio = new JRadioButton("Show label number");
        squareNumberRadio = new JRadioButton("Show square number");

        ButtonGroup group = new ButtonGroup();
        group.add(noNumberRadio);
        group.add(labelNumberRadio);
        group.add(squareNumberRadio);

        noNumberRadio.addActionListener(e -> gridPanel.setNumberMode(0));
        labelNumberRadio.addActionListener(e -> gridPanel.setNumberMode(1));
        squareNumberRadio.addActionListener(e -> gridPanel.setNumberMode(2));

        JPanel radiosPanel = new JPanel();
        radiosPanel.setLayout(new BoxLayout(radiosPanel, BoxLayout.Y_AXIS));
        radiosPanel.add(noNumberRadio);
        radiosPanel.add(labelNumberRadio);
        radiosPanel.add(squareNumberRadio);

        // === Main layout ===
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(showBordersCheckBox);
        center.add(Box.createVerticalStrut(10));
        center.add(new JLabel("Number display:"));
        center.add(radiosPanel);

        add(center, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}