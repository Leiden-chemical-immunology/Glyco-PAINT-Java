/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.ui.dialogs.project;

import paint.shared.config.paintconfig.PaintConfig;
import paint.ui.dialogs.ProjectDialog;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static paint.shared.constants.PaintFileNames.EXPERIMENT_INFO_CSV;

public class ExperimentsPanel {

    private final ProjectDialog.DialogMode dialogMode;
    private final JPanel                   panel = new JPanel(new BorderLayout());
    private final JPanel                   list  = new JPanel();
    private final List<AbstractButton>     boxes = new ArrayList<>();
    private ButtonGroup                    group; // only used in TRACKMATE_SINGLE

    private Runnable onChanged = () -> {};

    /**
     * Constructs an {@code ExperimentsPanel} and immediately loads the list of
     * experiments from the given project root.
     *
     * @param projectRoot the root directory of the project
     */
    public ExperimentsPanel(Path projectRoot, ProjectDialog.DialogMode dialogMode) {

        this.dialogMode = dialogMode;
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        // Scrollable container for experiment checkboxes
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(680, 240));
        scroll.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        // Top control bar (Select All / Clear All) for checkbox modes
        if (dialogMode != ProjectDialog.DialogMode.TRACKMATE_SINGLE) {
            JPanel  controls  = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton selectAll = new JButton("Select All");
            JButton clearAll  = new JButton("Clear All");
            controls.add(selectAll);
            controls.add(clearAll);

            selectAll.addActionListener(this::selectAllExperiments);
            clearAll.addActionListener(this::clearAllExperiments);

            panel.add(controls, BorderLayout.NORTH);
        }

        panel.add(scroll, BorderLayout.CENTER);

        // Initial load
        reload(projectRoot);
    }

    /**
     * Returns the main Swing component for this panel.
     *
     * @return the panel UI component
     */
    public JPanel component() {
        return panel;
    }

    /**
     * Registers a callback that is invoked whenever selection changes
     * If the caller provides a non-null Runnable → use it.
     * If the caller passes null → fall back to an empty no-op runnable, so your code never has to deal with a null callback.
     *
     * @param runnable a Runnable to invoke on selection change
     */
    public void onSelectionChanged(Runnable runnable) {
        this.onChanged = (runnable != null ? runnable : () -> {});
    }

    /**
     * Checks whether at least one experiment is selected.
     * @return true if any checkbox is selected, false otherwise
     */
    public boolean anySelected() {
        for (AbstractButton abstractButton : boxes) {
            if (abstractButton.isSelected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the names of all selected experiments.
     *
     * @return list of experiment names
     */
    public List<String> selectedExperimentNames() {

        List<String> experimentNames = new ArrayList<>();
        for (AbstractButton abstractButton : boxes) {
            if (abstractButton.isSelected()) {
                experimentNames.add(abstractButton.getText());
            }
        }
        PaintLogger.debugf("ExperimentsPanel.selectedExperimentNames: %s", experimentNames);
        return experimentNames;
    }

    /**
     * Enables or disables all experiment checkboxes.
     *
     * @param enabled whether the checkboxes should be enabled
     */
    public void setEnabled(boolean enabled) {
        for (AbstractButton abstractButton : boxes) {
            abstractButton.setEnabled(enabled);
        }
    }

    /**
     * Reloads and reconstructs the list of experiments from the given project
     * root directory. Only subdirectories containing {@code ExperimentInfo.csv}
     * (and not named "Sweep") are included.
     *
     * @param projectRoot the project root path
     */
    public void reload(Path projectRoot) {
        list.removeAll();
        boxes.clear();

        // Create checkbox/radiobutton for the experiment
        if (dialogMode == ProjectDialog.DialogMode.TRACKMATE_SINGLE) {
            group = new ButtonGroup();
        } else {
            group = null;
        }

        String  selectedExperiment       = null;
        boolean singleExperimentSelected = false;

        if (dialogMode == ProjectDialog.DialogMode.TRACKMATE_SINGLE) {
            selectedExperiment = PaintConfig.getString("Single TrackMate Mode", "SelectedExperiment", "");
        }
        // Scan subdirectories of the project root
        File[] experimentDirectories = (projectRoot != null ? projectRoot.toFile().listFiles() : null);
        if (experimentDirectories != null) {
            Arrays.sort(experimentDirectories, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File experimentDirectory : experimentDirectories) {
                if (!experimentDirectory.isDirectory()) {
                    continue;
                }

                // Experiment must contain ExperimentInfo.csv
                File experimentInfofile = new File(experimentDirectory, EXPERIMENT_INFO_CSV);
                if (!experimentInfofile.isFile()) {
                    continue;
                }

                if ("Sweep".equals(experimentDirectory.getName())) {
                    continue;
                }

                final String experimentName = experimentDirectory.getName();

                AbstractButton abstractButton;
                if (dialogMode == ProjectDialog.DialogMode.TRACKMATE_SINGLE) {
                    abstractButton = new JRadioButton(experimentName);
                    group.add(abstractButton);
                } else {
                    abstractButton = new JCheckBox(experimentName);
                }

                abstractButton.addActionListener(e -> {
                    if (dialogMode == ProjectDialog.DialogMode.TRACKMATE_SINGLE && abstractButton.isSelected()) {
                        PaintConfig.setString(
                                "Single TrackMate Mode",
                                "SelectedExperiment",
                                experimentName
                        );
                    }
                    onChanged.run();
                });

                if (dialogMode == ProjectDialog.DialogMode.TRACKMATE_SINGLE) {
                    if (experimentName.equals(selectedExperiment)) {
                        abstractButton.setSelected(true);
                        singleExperimentSelected = true;
                    }
                } else {
                    boolean saved = PaintConfig.instance().getBooleanValueNoWarning("Experiments", experimentName, false);
                    abstractButton.setSelected(saved);
                }
                boxes.add(abstractButton);
                list.add(abstractButton);
            }

            if (dialogMode == ProjectDialog.DialogMode.TRACKMATE_SINGLE) {
                if (!singleExperimentSelected && !boxes.isEmpty()) {
                    boxes.get(0).setSelected(true);
                }
            }
        }

        list.revalidate();
        list.repaint();
        onChanged.run();
    }

    @SuppressWarnings("unused")
    private void selectAllExperiments(ActionEvent e) {
        for (AbstractButton abstractButton : boxes) {
            abstractButton.setSelected(true);
        }
        onChanged.run();
    }

    @SuppressWarnings("unused")
    private void clearAllExperiments(ActionEvent e) {
        for (AbstractButton abstractButton : boxes) {
            abstractButton.setSelected(false);
        }
        onChanged.run();
    }
}