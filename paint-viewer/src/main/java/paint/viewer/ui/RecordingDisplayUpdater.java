/*==============================================================================
 *  Class:        RecordingDisplayUpdater.java
 *  Package:      paint.viewer.ui
 *
 *  PURPOSE:
 *    Centralizes all UI updates required when the viewer navigates to a new
 *    {@link paint.viewer.model.RecordingEntry}. Ensures that every visible
 *    component in the ViewerFrame reflects the newly selected recording.
 *
 *  DESCRIPTION:
 *    This class updates the PAINT viewer UI whenever the user steps forward or
 *    backward through the list of loaded recording entries. It refreshes:
 *
 *      • The left grid panel (recording, square list, and background image)
 *      • The right image display (scaled image preview)
 *      • The experiment and recording labels
 *      • The attribute table showing recording metadata
 *
 *    All updates are performed in a coordinated manner to avoid inconsistent
 *    or partially refreshed UI states. This class is stateless; it operates
 *    exclusively on the components passed in via the constructor.
 *
 *  KEY FEATURES:
 *    • Fully refreshes the grid, images, labels, and attributes panel.
 *    • Performs deterministic high-quality scaling of the right-side image.
 *    • Reads the expected square count from {@link PaintConfig}.
 *    • Lightweight utility class with no retained business logic.
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
 ==============================================================================*/

package paint.viewer.ui;

import paint.shared.config.paintconfig.PaintConfig;
import paint.viewer.model.RecordingEntry;
import paint.viewer.ui.panels.SquareGridPanel;
import paint.viewer.ui.panels.RecordingAttributesPanel;

import javax.swing.*;
import java.awt.Color;

import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.NUMBER_OF_SQUARES_IN_RECORDING;

import static paint.shared.constants.PaintGeometry.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintGeometry.NUMBER_PIXELS_WIDTH;

/**
 * Utility class responsible for synchronizing the UI with a newly selected
 * {@link RecordingEntry}. Called whenever the user navigates between recordings
 * in the viewer.
 *
 * <p>This updater replaces the displayed grid, rescaled images, labels, and
 * attribute table to reflect the active recording. No state is stored inside
 * this class—its job is to apply a complete UI refresh against the components
 * provided to the constructor.</p>
 */
public final class RecordingDisplayUpdater {

    private final SquareGridPanel          leftGridPanel;
    private final JLabel                   rightImageLabel;
    private final JLabel                   experimentLabel;
    private final JLabel                   recordingLabel;
    private final RecordingAttributesPanel attributesPanel;
    private final Color                    defaultRecordingLabelColor;

    /**
     * Creates a new updater bound to the UI components it controls.
     *
     * @param leftGridPanel     the grid used for displaying square layout and overlays
     * @param rightImageLabel   the label used to show the right-side image
     * @param experimentLabel   the label displaying the experiment name
     * @param recordingLabel    the label displaying the recording name
     * @param attributesPanel   the table panel showing recording metadata
     */
    public RecordingDisplayUpdater(
            SquareGridPanel leftGridPanel,
            JLabel rightImageLabel,
            JLabel experimentLabel,
            JLabel recordingLabel,
            RecordingAttributesPanel attributesPanel
    ) {
        this.leftGridPanel              = leftGridPanel;
        this.rightImageLabel            = rightImageLabel;
        this.experimentLabel            = experimentLabel;
        this.recordingLabel             = recordingLabel;
        this.attributesPanel            = attributesPanel;
        this.defaultRecordingLabelColor = recordingLabel.getForeground();
    }

    /**
     * Updates all UI components to show the given recording entry.
     *
     * @param entry      the new {@link RecordingEntry} to display
     * @param index      the zero-based index of the entry in the full list
     * @param totalSize  the total number of loaded recording entries
     */
    public void show(RecordingEntry entry, int index, int totalSize) {

        // --- Left grid panel ---
        leftGridPanel.setBackgroundImage(entry.getLeftImage());
        leftGridPanel.setSquares(entry.getRecording().getSquaresOfRecording());

        // --- Right image ---
        ImageIcon scaled = new ImageIcon(
                entry.getRightImage().getImage().getScaledInstance(
                        NUMBER_PIXELS_WIDTH,
                        NUMBER_PIXELS_HEIGHT,
                        java.awt.Image.SCALE_SMOOTH
                )
        );
        rightImageLabel.setIcon(scaled);

        // --- Labels ---
        experimentLabel.setText(
                "Experiment: " + entry.getExperimentName()
                        + "   [" + (index + 1) + "/" + totalSize + "]"
        );

        applyExcludedUi(entry);

        // --- Attribute table ---
        int numberOfSquares = PaintConfig.getInt(
                GENERATE_SQUARES,
                NUMBER_OF_SQUARES_IN_RECORDING,
                -1
        );

        attributesPanel.updateFromEntry(entry, numberOfSquares);

        // --- Refresh grid ---
        leftGridPanel.repaint();
    }

    /**
     * Updates the recording label to reflect whether the current recording is excluded.
     * If excluded, appends "(Excluded)" and sets the label text to red.
     */
    public void applyExcludedUi(RecordingEntry entry) {
        final boolean excluded = isExcluded(entry);
        if (excluded) {
            recordingLabel.setText("Recording: " + entry.getRecordingName() + " (Excluded)");
            recordingLabel.setForeground(Color.RED);
        } else {
            recordingLabel.setText("Recording: " + entry.getRecordingName());
            recordingLabel.setForeground(defaultRecordingLabelColor);
        }
    }

    /**
     * Attempts to read an "excluded" flag from the RecordingEntry / Recording model.
     */
    private static boolean isExcluded(RecordingEntry recordingEntry) {
        if (recordingEntry == null) {
            return false;
        }

        return recordingEntry.getRecording().isExcluded();
    }
}