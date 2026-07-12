/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import paint.shared.utils.PaintLogger;

/**
 * Provides a modeless dialog for running TrackMate on a single recording with interactive
 * threshold adjustment.
 * <p>
 * The dialog allows users to:
 * </p>
 * <ul>
 *   <li>Select a recording from the current project/experiment.</li>
 *   <li>Adjust the spot detection threshold via a slider.</li>
 *   <li>Trigger TrackMate calculation in a background thread.</li>
 *   <li>Save the resulting track and spot data to CSV.</li>
 * </ul>
 * <p>
 * It provides visual feedback during long-running TrackMate operations and prevents concurrent
 * execution through UI-state management.
 * </p>
 * <ul>
 *   <li>Interactive recording selection and threshold tuning.</li>
 *   <li>Background thread execution for responsive UI.</li>
 *   <li>Status feedback with animated dots for active processes.</li>
 *   <li>Java 8 compatible.</li>
 * </ul>
 */
public class TrackMateSingleDialog extends JDialog {

    public interface CalculationHandler {
        void run(String recordingName, int threshold) throws Exception;
    }

    public interface SaveHandler {
        void run(String recordingName, int threshold) throws Exception;
    }

    private final ButtonGroup        recordingGroup   = new ButtonGroup();
    private final List<JRadioButton> recordingButtons = new ArrayList<>();

    private final JSlider thresholdSlider;
    private final JLabel  thresholdValueLabel;
    private final JLabel  statusLabel;

    private volatile Thread  runningThread;
    private volatile Thread  dotThread;
    private volatile boolean dotRunning;

    private String selectedRecordingName;
    private int    selectedThreshold;

    public interface ThresholdProvider {
        int getThreshold(String recordingName);
    }

    private ThresholdProvider thresholdProvider;

    public void setThresholdProvider(ThresholdProvider p) {
        this.thresholdProvider = p;
        applyThresholdFromProvider(); // set slider for current selection immediately
    }

    public TrackMateSingleDialog(
            Window             owner,
            List<String>       recordingNames,
            String             initialRecordingName,
            int                initialThreshold,
            CalculationHandler onCalculate,
            SaveHandler        onSave
    ) {
        super(owner, "TrackMate Single", ModalityType.MODELESS);

        List<String> names = (recordingNames == null)
                ? Collections.<String>emptyList()
                : new ArrayList<>(recordingNames);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCancel(); // same behavior as Cancel button
            }
        });

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(root);

        // ---------------------------------------------------------------------
        // Left: recordings
        // ---------------------------------------------------------------------
        JPanel recordingsPanel = new JPanel();
        recordingsPanel.setLayout(new BoxLayout(recordingsPanel, BoxLayout.Y_AXIS));
        recordingsPanel.setBorder(BorderFactory.createTitledBorder("Recording"));

        boolean anySelected = false;
        String preferred = safeTrim(initialRecordingName);

        for (String name : names) {
            if (name == null) continue;
            final String rec = name.trim();
            if (rec.isEmpty()) {
                continue;
            }

            JRadioButton rb = new JRadioButton(rec);
            rb.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Select preferred if matches
            if (!preferred.isEmpty() && preferred.equals(rec)) {
                rb.setSelected(true);
                anySelected = true;
                selectedRecordingName = rec;
            }

            rb.addActionListener(e -> {
                selectedRecordingName = rec;
                applyThresholdFromProvider();
            });

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

        JScrollPane recordingsScroll = new JScrollPane(
                recordingsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        recordingsScroll.setPreferredSize(new Dimension(420, 320));
        recordingsScroll.setBorder(BorderFactory.createEmptyBorder());

        // ---------------------------------------------------------------------
        // Right: threshold slider
        // ---------------------------------------------------------------------
        JPanel thresholdPanel = new JPanel();
        thresholdPanel.setLayout(new BoxLayout(thresholdPanel, BoxLayout.Y_AXIS));
        thresholdPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Threshold"),
                        new EmptyBorder(8, 12, 8, 8) // top, left, bottom, right
                )
        );
        // thresholdPanel.add(Box.createVerticalStrut(8));

        int initT = clamp(initialThreshold, 1, 50);

        thresholdValueLabel = new JLabel(String.valueOf(initT));
        thresholdValueLabel.setFont(thresholdValueLabel.getFont().deriveFont(Font.BOLD, 18f));
        thresholdValueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        thresholdSlider = new JSlider(0, 50, Math.max(1, initT));
        thresholdSlider.setMajorTickSpacing(10);
        thresholdSlider.setMinorTickSpacing(0);     // no fine ticks
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        thresholdSlider.setSnapToTicks(false);

        thresholdSlider.setAlignmentX(Component.LEFT_ALIGNMENT);

        thresholdSlider.addChangeListener(e -> {
            int v = thresholdSlider.getValue();

            // Prevent selecting 0
            if (v == 0) {
                thresholdSlider.setValue(1);
                v = 1;
            }

            thresholdValueLabel.setText(String.valueOf(v));
        });

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
        statusLabel = new JLabel(" ");
        statusLabel.setBorder(new EmptyBorder(0, 0, 6, 0));

        JButton calculateBtn = new JButton("Calculate");
        JButton saveBtn      = new JButton("Save");
        JButton cancelBtn    = new JButton("Cancel");

        calculateBtn.addActionListener(e -> {
            if (!captureSelections()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            if (runningThread != null) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            setUiRunning(true, "Running…");

            runningThread = new Thread(() -> {
                startDots();
                try {
                    if (onCalculate != null) {
                        onCalculate.run(selectedRecordingName, selectedThreshold);
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        SwingUtilities.invokeLater(() -> setUiRunning(false, "Cancelled"));
                        return;
                    }

                    SwingUtilities.invokeLater(() -> setUiRunning(false, "Completed"));
                } catch (Exception ex) {
                    if (Thread.currentThread().isInterrupted()) {
                        SwingUtilities.invokeLater(() -> setUiRunning(false, "Cancelled"));
                    } else {
                        SwingUtilities.invokeLater(() -> setUiRunning(false, "Failed: " + ex.getMessage()));
                    }
                } finally {
                    stopDots();
                    runningThread = null;
                }
            }, "TrackMateSingle-Worker");

            runningThread.start();
        });

        saveBtn.addActionListener(e -> {
            if (!captureSelections()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            if (runningThread != null) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            try {
                if (onSave != null) {
                    onSave.run(selectedRecordingName, selectedThreshold);
                }
                statusLabel.setText("Saved");
            } catch (Exception ex) {
                statusLabel.setText("Save failed: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> handleCancel());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(calculateBtn);
        buttons.add(saveBtn);
        buttons.add(cancelBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(buttons, BorderLayout.EAST);

        root.add(bottom, BorderLayout.SOUTH);

        // store initial
        selectedThreshold = thresholdSlider.getValue();

        pack();
        setLocationRelativeTo(owner);

        // local helpers that need button refs
        this.calculateBtn = calculateBtn;
        this.saveBtn = saveBtn;
        this.cancelBtn = cancelBtn;
    }

    // keep refs for enabling/disabling
    private final JButton calculateBtn;
    private final JButton saveBtn;
    private final JButton cancelBtn;

    private void handleCancel() {
        Thread t = runningThread;
        if (t != null) {
            // first Cancel interrupts running job (dialog stays open)
            statusLabel.setText("Stopping…");
            stopDots();
            t.interrupt();
            return;
        }
        dispose();
    }

    private void setUiRunning(boolean running, String status) {
        statusLabel.setText(status);
        calculateBtn.setEnabled(!running);
        saveBtn.setEnabled(!running);
        cancelBtn.setEnabled(true);
        thresholdSlider.setEnabled(!running);
        for (JRadioButton rb : recordingButtons) rb.setEnabled(!running);
    }

    private boolean captureSelections() {
        String rec = selectedRecordingName;
        if (rec == null || rec.trim().isEmpty()) {
            for (JRadioButton rb : recordingButtons) {
                if (rb.isSelected()) {
                    rec = rb.getText();
                    break;
                }
            }
        }
        if (rec == null || rec.trim().isEmpty()) {
            return false;
        }

        selectedRecordingName = rec.trim();
        selectedThreshold     = thresholdSlider.getValue();
        return true;
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private void applyThresholdFromProvider() {
        if (thresholdProvider == null) {
            return;
        }
        if (selectedRecordingName == null || selectedRecordingName.trim().isEmpty()) {
            return;
        }

        int v = thresholdProvider.getThreshold(selectedRecordingName.trim());
        if (v < 1) {
            v = 1;
        }
        if (v > 50) {
            v = 50;
        }

        thresholdSlider.setValue(v); // your changeListener will also update label & enforce 0->1
    }

    private void startDots() {
        if (dotThread != null && dotThread.isAlive()) {
            return;
        }
        if (dotRunning) {
            return;
        }
        dotRunning = true;
        dotThread = new Thread(() -> {
            int dots = 0;
            while (dotRunning && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!dotRunning) break;

                PaintLogger.raw(".");
                dots++;

                if (dots >= 80) {
                    PaintLogger.raw("\n                                                    ");
                    dots = 0;
                }
            }
        }, "TrackMateSingle-Dots");

        dotThread.setDaemon(true);
        dotThread.start();
    }

    private void stopDots() {
        dotRunning = false;
        Thread t = dotThread;
        if (t != null) {
            t.interrupt();
        }
        dotThread = null;
    }

    @Override
    public void dispose() {
        stopDots();
        super.dispose();
    }
}