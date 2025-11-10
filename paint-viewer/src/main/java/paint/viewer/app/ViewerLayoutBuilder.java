package paint.viewer.app;

import paint.viewer.ui.panels.NavigationPanel;
import paint.viewer.ui.panels.RecordingAttributesPanel;
import paint.viewer.ui.panels.RecordingControlsPanel;
import paint.viewer.ui.panels.SquareGridPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_WIDTH;

/**
 * Builds the full layout for the ViewerFrame and returns all
 * top-level UI components that the frame must retain.
 */
public class ViewerLayoutBuilder {

    public static class LayoutComponents {
        public final JPanel rootPanel;
        public final JLabel rightImageLabel;
        public final JLabel experimentLabel;
        public final JLabel recordingLabel;
        public final SquareGridPanel leftGridPanel;
        public final RecordingAttributesPanel attributesPanel;
        public final NavigationPanel navigationPanel;
        public final RecordingControlsPanel controlsPanel;

        LayoutComponents(
                JPanel rootPanel,
                JLabel rightImageLabel,
                JLabel experimentLabel,
                JLabel recordingLabel,
                SquareGridPanel leftGridPanel,
                RecordingAttributesPanel attributesPanel,
                NavigationPanel navigationPanel,
                RecordingControlsPanel controlsPanel
        ) {
            this.rootPanel         = rootPanel;
            this.rightImageLabel   = rightImageLabel;
            this.experimentLabel   = experimentLabel;
            this.recordingLabel    = recordingLabel;
            this.leftGridPanel     = leftGridPanel;
            this.attributesPanel   = attributesPanel;
            this.navigationPanel   = navigationPanel;
            this.controlsPanel     = controlsPanel;
        }
    }

    /**
     * Builds the full viewer layout and returns all UI components
     * needed by ViewerFrame.
     */
    public LayoutComponents build(int gridDim,
            NavigationPanel.Listener navListener,
            RecordingControlsPanel.Listener controlsListener) {

        // --- Components created here so ViewerFrame does not ---
        JLabel rightImageLabel = new JLabel("", SwingConstants.CENTER);
        JLabel experimentLabel = new JLabel("", SwingConstants.CENTER);
        JLabel recordingLabel  = new JLabel("", SwingConstants.CENTER);

        SquareGridPanel leftGridPanel = new SquareGridPanel(gridDim, gridDim);
        RecordingAttributesPanel attributesPanel = new RecordingAttributesPanel();
        NavigationPanel navigationPanel           = new NavigationPanel(navListener);
        RecordingControlsPanel controlsPanel      = new RecordingControlsPanel(controlsListener);

        // --- Images panel ---
        JPanel imagesInner = new JPanel(new GridLayout(1, 2, 15, 0));
        imagesInner.add(createSquareImagePanel(leftGridPanel));
        imagesInner.add(createSquareImagePanel(rightImageLabel));

        // --- Labels panel ---
        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        experimentLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        recordingLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        labelsPanel.add(experimentLabel);
        labelsPanel.add(recordingLabel);

        // --- Image area + navigation ---
        JPanel imagesWithNav = new JPanel(new BorderLayout(15, 15));
        Border outer = BorderFactory.createLineBorder(Color.DARK_GRAY, 2);
        Border inner = BorderFactory.createEmptyBorder(15, 15, 15, 15);
        imagesWithNav.setBorder(BorderFactory.createCompoundBorder(outer, inner));

        imagesWithNav.add(navigationPanel.getComponent(), BorderLayout.NORTH);
        imagesWithNav.add(imagesInner,                        BorderLayout.CENTER);
        imagesWithNav.add(labelsPanel,                        BorderLayout.SOUTH);

        // --- Main layout ---
        JPanel root = new JPanel(new BorderLayout());
        root.add(attributesPanel.getComponent(), BorderLayout.WEST);
        root.add(imagesWithNav,                  BorderLayout.CENTER);
        root.add(controlsPanel.getComponent(),   BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton closeButton = new JButton("Close Viewer");
        closeButton.addActionListener(e -> SwingUtilities.getWindowAncestor(root).dispose());
        bottomPanel.add(closeButton);
        root.add(bottomPanel, BorderLayout.SOUTH);

        return new LayoutComponents(
                root,
                rightImageLabel,
                experimentLabel,
                recordingLabel,
                leftGridPanel,
                attributesPanel,
                navigationPanel,
                controlsPanel
        );
    }

    private JPanel createSquareImagePanel(JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout()) {
            public Dimension getPreferredSize() {
                return new Dimension(NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT);
            }
            public void setBounds(int x, int y, int w, int h) {
                int size = Math.min(w, h);
                super.setBounds(x, y, size, size);
            }
        };

        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }
}