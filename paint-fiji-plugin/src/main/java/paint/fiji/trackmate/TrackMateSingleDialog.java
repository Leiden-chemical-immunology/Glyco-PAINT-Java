/*=============================================================================
 *  Class:        TrackMateSingleDialog.java
 *  Package:      paint.fiji.trackmate
 *
 *  PURPOSE:
 *    Modal dialog for TRACKMATE_SINGLE mode: choose one Recording Name (radio
 *    buttons), choose a Threshold (slider 1..50), and choose an action:
 *    Calculate / Save / Cancel.
 *
 *  NOTES:
 *    - Closing via red window button behaves like Cancel.
 *    - Java 8 compatible.
 *============================================================================*/

package paint.fiji.trackmate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackMateSingleDialog extends JDialog {

    public enum Action {
        CALCULATE,
        SAVE,
        CANCEL
    }

    private Action action = Action.CANCEL;

    private final ButtonGroup recordingGroup = new ButtonGroup();
    private final List<JRadioButton> recordingButtons = new ArrayList<>();

    private final JSlider thresholdSlider;
    private final JLabel  thresholdValueLabel;

    private String selectedRecordingName;
    private int    selectedThreshold;

    /**
     * @param owner                 parent frame (can be null)
     * @param recordingNames        list of recording names to show as radio buttons
     * @param initialRecordingName  preferred initially selected recording (may be null/empty)
     * @param initialThreshold      initial threshold (clamped to 1..50)
     */
    public TrackMateSingleDialog(
            Window       owner,
            List<String> recordingNames,
            String       initialRecordingName,
            int          initialThreshold
    ) {
        super(owner, "TrackMate Single", ModalityType.APPLICATION_MODAL);

        List<String> names = (recordingNames == null) ? Collections.<String>emptyList() : new ArrayList<>(recordingNames);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelAndClose();
            }
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        // ---------------------------------------------------------------------
        // Left: recordings (radio buttons, scrollable)
        // ---------------------------------------------------------------------
        JPanel recordingsPanel = new JPanel();
        recordingsPanel.setLayout(new BoxLayout(recordingsPanel, BoxLayout.Y_AXIS));
        recordingsPanel.setBorder(BorderFactory.createTitledBorder("Recording"));

        boolean anySelected = false;
        String preferred = safeTrim(initialRecordingName);

        for (String name : names) {
            if (name == null) continue;
            final String rec = name;

            JRadioButton rb = new JRadioButton(rec);
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Select preferred if matches
            if (!preferred.isEmpty() && preferred.equals(rec)) {
                rb.setSelected(true);
                anySelected = true;
                selectedRecordingName = rec;
            }

            rb.addActionListener(e -> selectedRecordingName = rec);

            recordingGroup.add(rb);
            recordingButtons.add(rb);
            recordingsPanel.add(rb);
        }

        // Fallback: select first if nothing selected
        if (!anySelected && !recordingButtons.isEmpty()) {
            JRadioButton first = recordingButtons.get(0);
            first.setSelected(true);
            selectedRecordingName = first.getText();
        }

        JScrollPane recordingsScroll = new JScrollPane(recordingsPanel,
                                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        recordingsScroll.setPreferredSize(new Dimension(420, 320));

        // ---------------------------------------------------------------------
        // Right: threshold slider
        // ---------------------------------------------------------------------
        JPanel thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new BoxLayout(thresholdPanel, BoxLayout.Y_AXIS));
        thresholdPanel.setBorder(BorderFactory.createTitledBorder("Threshold"));

        int initT = clamp(initialThreshold, 1, 50);

        thresholdValueLabel = new JLabel(String.valueOf(initT));
        thresholdValueLabel.setFont(thresholdValueLabel.getFont().deriveFont(Font.BOLD, 18f));
        thresholdValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        thresholdSlider = new JSlider(1, 50, initT);
        thresholdSlider.setMajorTickSpacing(10);
        thresholdSlider.setMinorTickSpacing(1);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        thresholdSlider.addChangeListener(e -> {
            int v = thresholdSlider.getValue();
            thresholdValueLabel.setText(String.valueOf(v));
        });

        thresholdPanel.add(new JLabel("Value:"));
        thresholdPanel.add(Box.createVerticalStrut(6));
        thresholdPanel.add(thresholdValueLabel);
        thresholdPanel.add(Box.createVerticalStrut(12));
        thresholdPanel.add(thresholdSlider);
        thresholdPanel.add(Box.createVerticalGlue());

        // ---------------------------------------------------------------------
        // Center layout
        // ---------------------------------------------------------------------
        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(recordingsScroll, BorderLayout.CENTER);
        center.add(thresholdPanel, BorderLayout.EAST);

        root.add(center, BorderLayout.CENTER);

        // ---------------------------------------------------------------------
        // Bottom buttons: Calculate / Save / Cancel
        // ---------------------------------------------------------------------
        JButton calculateBtn = new JButton("Calculate");
        JButton saveBtn      = new JButton("Save");
        JButton cancelBtn    = new JButton("Cancel");

        Dimension btnSize = new Dimension(110, 30);
        calculateBtn.setPreferredSize(btnSize);
        saveBtn.setPreferredSize(btnSize);
        cancelBtn.setPreferredSize(btnSize);

        calculateBtn.addActionListener(e -> {
            if (!captureSelections()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            action = Action.CALCULATE;
            dispose();
        });

        saveBtn.addActionListener(e -> {
            if (!captureSelections()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            action = Action.SAVE;
            dispose();
        });

        cancelBtn.addActionListener(e -> cancelAndClose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(calculateBtn);
        buttons.add(saveBtn);
        buttons.add(cancelBtn);

        root.add(buttons, BorderLayout.SOUTH);

        // Initial capture
        selectedThreshold = thresholdSlider.getValue();

        pack();
        setLocationRelativeTo(owner);
    }

    // -------------------------------------------------------------------------
    // Public getters
    // -------------------------------------------------------------------------

    public Action getAction() {
        return action;
    }

    public boolean isCancelled() {
        return action == Action.CANCEL;
    }

    public String getSelectedRecordingName() {
        return selectedRecordingName;
    }

    public int getSelectedThreshold() {
        return selectedThreshold;
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private void cancelAndClose() {
        action = Action.CANCEL;
        dispose();
    }

    private boolean captureSelections() {
        // Ensure a recording is selected
        String rec = selectedRecordingName;
        if (rec == null || rec.trim().isEmpty()) {
            // Try to recover from the selected button
            ButtonModel bm = recordingGroup.getSelection();
            if (bm != null) {
                for (JRadioButton rb : recordingButtons) {
                    if (rb.isSelected()) {
                        rec = rb.getText();
                        break;
                    }
                }
            }
        }

        if (rec == null || rec.trim().isEmpty()) {
            return false;
        }

        selectedRecordingName = rec;
        selectedThreshold     = thresholdSlider.getValue();
        return true;
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}