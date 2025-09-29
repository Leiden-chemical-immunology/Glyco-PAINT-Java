package viewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SquareControlDialog extends JDialog {

    private final JCheckBox showBordersCheck;
    private final JRadioButton noNumberRadio;
    private final JRadioButton labelNumberRadio;
    private final JRadioButton squareNumberRadio;

    public SquareControlDialog(Frame owner, SquareGridPanel gridPanel) {
        super(owner, "Square Controls", false);

        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // --- Border controls ---
        showBordersCheck = new JCheckBox("Show borders of selected squares", true);
        showBordersCheck.setFont(new Font("SansSerif", Font.PLAIN, 12));
        showBordersCheck.addActionListener(e -> gridPanel.setShowBorders(showBordersCheck.isSelected()));

        JPanel borderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        borderPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        borderPanel.add(showBordersCheck);

        // --- Number controls ---
        noNumberRadio = new JRadioButton("Show no number", true);
        labelNumberRadio = new JRadioButton("Show label number");
        squareNumberRadio = new JRadioButton("Show square number");

        for (JRadioButton btn : new JRadioButton[]{noNumberRadio, labelNumberRadio, squareNumberRadio}) {
            btn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        }

        ButtonGroup group = new ButtonGroup();
        group.add(noNumberRadio);
        group.add(labelNumberRadio);
        group.add(squareNumberRadio);

        noNumberRadio.addActionListener(e -> gridPanel.setNumberMode(0));
        labelNumberRadio.addActionListener(e -> gridPanel.setNumberMode(1));
        squareNumberRadio.addActionListener(e -> gridPanel.setNumberMode(2));

        JPanel numbersPanel = new JPanel();
        numbersPanel.setLayout(new BoxLayout(numbersPanel, BoxLayout.Y_AXIS));
        numbersPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        numbersPanel.add(noNumberRadio);
        numbersPanel.add(Box.createVerticalStrut(5));
        numbersPanel.add(labelNumberRadio);
        numbersPanel.add(Box.createVerticalStrut(5));
        numbersPanel.add(squareNumberRadio);

        // --- Assemble ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(borderPanel, BorderLayout.NORTH);
        mainPanel.add(numbersPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }
}