package viewer;

import paint.shared.objects.Project;

import javax.swing.*;
import java.awt.*;

public class RecordingViewerFrame extends JFrame {

    private final Project project;

    public RecordingViewerFrame(Project project) {
        super("Recording Viewer - " + project.getProjectPath().getFileName());
        this.project = project;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Image pair area ---
        JPanel imagePanel = new JPanel(new GridLayout(1, 2, 5, 5));
        imagePanel.add(createImagePlaceholder("Left Image"));
        imagePanel.add(createImagePlaceholder("Right Image"));
        add(imagePanel, BorderLayout.CENTER);

        // --- South panel: attributes + navigation buttons ---
        JPanel southPanel = new JPanel(new BorderLayout());

        JPanel attributesPanel = new JPanel();
        attributesPanel.setBorder(BorderFactory.createTitledBorder("Image Attributes"));
        attributesPanel.add(new JLabel("Attributes will be displayed here..."));
        southPanel.add(attributesPanel, BorderLayout.CENTER);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        navPanel.add(new JButton("|<"));
        navPanel.add(new JButton("<"));
        navPanel.add(new JButton(">"));
        navPanel.add(new JButton(">|"));
        southPanel.add(navPanel, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.SOUTH);

        // --- Actions panel (right side) ---
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
        actionsPanel.add(new JButton("Action 1"));
        actionsPanel.add(Box.createVerticalStrut(10));
        actionsPanel.add(new JButton("Action 2"));
        actionsPanel.add(Box.createVerticalStrut(10));
        actionsPanel.add(new JButton("Action 3"));
        add(actionsPanel, BorderLayout.EAST);

        setSize(1200, 800);
        setLocationRelativeTo(null);
    }

    private JLabel createImagePlaceholder(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(512, 512));
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }
}