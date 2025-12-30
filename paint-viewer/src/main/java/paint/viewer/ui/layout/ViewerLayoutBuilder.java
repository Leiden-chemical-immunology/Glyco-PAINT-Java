/*==============================================================================
 *  Class:        ViewerLayoutBuilder.java
 *  Package:      paint.viewer.ui.layout
 *
 *  PURPOSE:
 *    Constructs the complete Swing user interface layout for the PAINT viewer.
 *    Centralises creation and assembly of all major UI components used by
 *    ViewerFrame, providing them in a convenient container object.
 *
 *  DESCRIPTION:
 *    This builder constructs:
 *      • The square grid panel (left image)
 *      • The processed / reference image panel (right image)
 *      • Experiment/recording labels
 *      • Navigation controls
 *      • Recording attributes panel
 *      • Recording controls panel
 *      • Import-overrides checkbox
 *      • Close button and bottom panel
 *
 *    The method {@code build(...)} returns a {@link LayoutComponents} object
 *    containing references to all created Swing components required for UI
 *    updates inside ViewerFrame.
 *
 *  KEY FEATURES:
 *    • Fully encapsulates all layout wiring for the viewer.
 *    • Ensures stable component references for ViewerFrame.
 *    • Provides custom layout containers with fixed aspect ratios for images.
 *    • Offers a clean data container (LayoutComponents) for external use.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-11-17
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

/*
 +-----------------------------------------------------------------------------------------------------------------------+
 |                                             NavigationPanel                                                           |
 |                           [ First ] [ Prev ]   (Experiment / Recording)   [ Next ] [ Last ]                           |
 +-----------------------------------------------------------------------------------------------------------------------+
 |                                                                                                                       |
 |  +-----------------------------+  +-----------------------------------------------------+   +-----------------------+ |
 |  | RecordingAttributesPanel    |  |                        IMAGE AREA                   |   | Recording             | |
 |  | (density / variability /    |  |                                                     |   | Controls              | |
 |  |  R² thresholds + metadata)  |  |   +--------------------+   +---------------------+  |   |  Panel                | |
 |  |                             |  |   |  SquareGridPanel   |   |   RightImageLabel   |  |   |                       | |
 |  |                             |  |   | (left image +      |   |     (Brightfield )  |  |   |  ┌──────────────────┐ | |
 |  |                             |  |   |  squares / shading)|   |                     |  |   |  │  Select Squares  │ | |
 |  +-----------------------------+  |   +--------------------+   +---------------------+  |   |  ├──────────────────┤ | |
 |                                   |                                                     |   |  │  Assign Cells    │ | |
 |                                   |                                                     |   |  ├──────────────────┤ | |
 |                                   |                                                     |   |  │  Filter Squares  │ | |
 |                                   |                                                     |   |  ├──────────────────┤ | |
 |                                   |                                                     |   |  │  Toggle Borders  │ | |
 |                                   |                                                     |   |  ├──────────────────┤ | |
 |                                   |                                                     |   |  │  Toggle Shading  │ | |
 |                                   +-----------------------------------------------------+   |  ├──────────────────┤ | |
 |                                                                                             |  │ Number Mode…     │ | |
 |                                                                                             |  ├──────────────────┤ | |
 |                                                                                             |  │  Play Recording  │ | |
 |                                                                                             |  ├──────────────────┤ | |
 |                                                                                             |  │  Export Panel    │ | |
 |                                                                                             |  └──────────────────┘ | |
 +-----------------------------------------------------------------------------------------------------------------------+
 |   [ ] Import Overrides                                                              [ Close Viewer ]                  |
 +-----------------------------------------------------------------------------------------------------------------------+
*/

package paint.viewer.ui.layout;

import paint.viewer.ui.panels.NavigationPanel;
import paint.viewer.ui.panels.RecordingAttributesPanel;
import paint.viewer.ui.panels.RecordingControlsPanel;
import paint.viewer.ui.panels.SquareGridPanel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import static paint.shared.constants.PaintGeometry.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintGeometry.NUMBER_PIXELS_WIDTH;

/**
 * Builds the full layout structure for the ViewerFrame. All components required
 * for UI updates (labels, grid panels, navigation, attributes, controls, etc.)
 * are created here and returned in a {@link LayoutComponents} container.
 */
public class ViewerLayoutBuilder {

    /**
     * Listener for the viewer close event.
     */
    public interface CloseListener {
        void onClose();
    }

    /**
     * Holds all top-level Swing components created by the layout builder.
     * ViewerFrame uses this object to update the UI during navigation.
     */
    public static class LayoutComponents {
        public final JPanel                   rootPanel;
        public final JLabel                   rightImageLabel;
        public final JLabel                   experimentLabel;
        public final JLabel                   recordingLabel;
        public final SquareGridPanel          leftGridPanel;
        public final RecordingAttributesPanel attributesPanel;
        public final NavigationPanel          navigationPanel;
        public final RecordingControlsPanel   controlsPanel;
        public final JCheckBox                importOverridesCheckBox;

        LayoutComponents(
                JPanel                   rootPanel,
                JLabel                   rightImageLabel,
                JLabel                   experimentLabel,
                JLabel                   recordingLabel,
                SquareGridPanel          leftGridPanel,
                RecordingAttributesPanel attributesPanel,
                NavigationPanel          navigationPanel,
                RecordingControlsPanel   controlsPanel,
                JCheckBox                importOverridesCheckBox
        ) {
            this.rootPanel               = rootPanel;
            this.rightImageLabel         = rightImageLabel;
            this.experimentLabel         = experimentLabel;
            this.recordingLabel          = recordingLabel;
            this.leftGridPanel           = leftGridPanel;
            this.attributesPanel         = attributesPanel;
            this.navigationPanel         = navigationPanel;
            this.controlsPanel           = controlsPanel;
            this.importOverridesCheckBox = importOverridesCheckBox;
        }
    }

    /**
     * Creates the full viewer layout, assembles all Swing components into a
     * top-level root panel, and returns the complete set of components that
     * ViewerFrame will retain and update during interaction.
     *
     * @param gridDim             dimension of the square grid (gridDim × gridDim)
     * @param navigationListener  listener for previous/next recording navigation
     * @param recordingsControlListener    listener for recording-specific control actions
     * @param closeListener       callback for closing the viewer
     * @return a {@link LayoutComponents} instance containing all UI components
     */
    public LayoutComponents build(
            int                                              gridDim,
            NavigationPanel.Listener                         navigationListener,
            RecordingControlsPanel.RecordingsControlListener recordingsControlListener,
            CloseListener                                    closeListener) {

        // --- Components created here so ViewerFrame never constructs them itself ---
        JLabel rightImageLabel = new JLabel("", SwingConstants.CENTER);
        JLabel experimentLabel = new JLabel("", SwingConstants.CENTER);
        JLabel recordingLabel  = new JLabel("", SwingConstants.CENTER);

        SquareGridPanel          leftGridPanel   = new SquareGridPanel(gridDim, gridDim);
        RecordingAttributesPanel attributesPanel = new RecordingAttributesPanel();
        NavigationPanel          navigationPanel = new NavigationPanel(navigationListener);
        RecordingControlsPanel   controlsPanel   = new RecordingControlsPanel(recordingsControlListener);

        // --- Image panels side-by-side (left grid + right image) ---
        JPanel imagesInner = new JPanel(new GridLayout(1, 2, 15, 0));
        imagesInner.add(createSquareImagePanel(leftGridPanel));
        imagesInner.add(createSquareImagePanel(rightImageLabel));

        // --- Labels panel (Experiment + Recording) ---
        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        experimentLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        recordingLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));

        labelsPanel.add(experimentLabel);
        labelsPanel.add(recordingLabel);

        // --- Main image area with navigation ---
        JPanel imagesWithNav = new JPanel(new BorderLayout(15, 15));
        Border outer         = BorderFactory.createLineBorder(Color.DARK_GRAY, 2);
        Border inner         = BorderFactory.createEmptyBorder(15, 15, 15, 15);
        imagesWithNav.setBorder(BorderFactory.createCompoundBorder(outer, inner));

        imagesWithNav.add(navigationPanel.getComponent(), BorderLayout.NORTH);
        imagesWithNav.add(imagesInner,                    BorderLayout.CENTER);
        imagesWithNav.add(labelsPanel,                    BorderLayout.SOUTH);

        // --- Root layout: left attributes, center images, right controls ---
        JPanel root = new JPanel(new BorderLayout());
        root.add(attributesPanel.getComponent(), BorderLayout.WEST);
        root.add(imagesWithNav,                  BorderLayout.CENTER);
        root.add(controlsPanel.getComponent(),   BorderLayout.EAST);

        // --- Bottom panel with import overrides checkbox + close button ---
        JPanel    bottomPanel             = new JPanel(new BorderLayout(10, 10));
        JCheckBox importOverridesCheckBox = new JCheckBox("Overrides");
        bottomPanel.add(importOverridesCheckBox, BorderLayout.WEST);
        JButton    closeButton            = new JButton("Close Viewer");
        closeButton.addActionListener(e -> closeListener.onClose());

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightBox.add(closeButton);
        bottomPanel.add(rightBox, BorderLayout.EAST);

        root.add(bottomPanel, BorderLayout.SOUTH);

        // --- Return all components (ViewerFrame consumes LayoutComponents) ---
        return new LayoutComponents(
                root,
                rightImageLabel,
                experimentLabel,
                recordingLabel,
                leftGridPanel,
                attributesPanel,
                navigationPanel,
                controlsPanel,
                importOverridesCheckBox
        );
    }

    /**
     * Creates a bordered panel that forces a fixed square aspect ratio based on
     * NUMBER_PIXELS_WIDTH and NUMBER_PIXELS_HEIGHT. Both the grid and right-image
     * panels use this container.
     */
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