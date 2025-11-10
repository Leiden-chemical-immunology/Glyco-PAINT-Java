package paint.viewer.app;

import paint.shared.config.paintconfig.PaintConfig;
import paint.viewer.ui.panels.SquareGridPanel;
import paint.viewer.ui.panels.RecordingAttributesPanel;
import paint.viewer.model.RecordingEntry;

import javax.swing.*;

/**
 * Handles updating the viewer UI when the user navigates
 * to a different recording entry.
 */
public final class RecordingDisplayUpdater {

    private final SquareGridPanel leftGridPanel;
    private final JLabel rightImageLabel;
    private final JLabel experimentLabel;
    private final JLabel recordingLabel;
    private final RecordingAttributesPanel attributesPanel;

    public RecordingDisplayUpdater(
            SquareGridPanel leftGridPanel,
            JLabel rightImageLabel,
            JLabel experimentLabel,
            JLabel recordingLabel,
            RecordingAttributesPanel attributesPanel
    ) {
        this.leftGridPanel = leftGridPanel;
        this.rightImageLabel = rightImageLabel;
        this.experimentLabel = experimentLabel;
        this.recordingLabel = recordingLabel;
        this.attributesPanel = attributesPanel;
    }

    /**
     * Updates all UI components to show the given recording.
     */
    public void show(RecordingEntry entry, int index, int totalSize) {

        // Update left grid panel
        leftGridPanel.setRecording(entry.getRecording());
        leftGridPanel.setBackgroundImage(entry.getLeftImage());
        leftGridPanel.setSquares(entry.getRecording().getSquaresOfRecording());

        // Update right panel image
        ImageIcon scaled = new ImageIcon(
                entry.getRightImage().getImage().getScaledInstance(
                        paint.shared.constants.PaintConstants.NUMBER_PIXELS_WIDTH,
                        paint.shared.constants.PaintConstants.NUMBER_PIXELS_HEIGHT,
                        java.awt.Image.SCALE_SMOOTH
                )
        );
        rightImageLabel.setIcon(scaled);

        // Update labels
        experimentLabel.setText(
                "Experiment: " + entry.getExperimentName()
                        + "   [" + (index + 1) + "/" + totalSize + "]"
        );

        recordingLabel.setText(
                "Recording: " + entry.getRecordingName()
        );

        // Update attribute panel
        int numberOfSquares = PaintConfig.getInt(
                "Generate Squares",
                "Number of Squares in Recording",
                -1
        );

        attributesPanel.updateFromEntry(entry, numberOfSquares);

        leftGridPanel.repaint();
    }
}