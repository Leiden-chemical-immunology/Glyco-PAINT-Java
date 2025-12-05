/*
 * ============================================================================
 *  PURPOSE
 *      Panel for displaying and selecting experiments within a project.
 *
 *  DESCRIPTION
 *      This class builds a UI component consisting of a scrollable list of
 *      experiment directories represented as checkboxes. Experiments are
 *      discovered by scanning the project root for subdirectories containing
 *      the ExperimentInfo.csv file. It also provides Select All / Clear All
 *      controls and exposes selection change notifications.
 *
 *  KEY FEATURES
 *      - Dynamically loads experiment list from project root.
 *      - Persists checkbox states using PaintConfig.
 *      - Provides callbacks when selection changes.
 *      - Allows external components to query selected experiments.
 *
 *  AUTHOR
 *      PAINT Automatic Header Generator
 *
 *  MODULE
 *      paint.shared.dialogs.project
 *
 *  UPDATED
 *      2025-11-24
 *
 *  COPYRIGHT
 *      © PAINT Project. All rights reserved.
 * ============================================================================
 */

package paint.shared.dialogs.project;

import paint.shared.config.paintconfig.PaintConfig;
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

/**
 * A Swing panel that displays a selectable list of experiment directories.
 * <p>
 * Each experiment is represented as a checkbox. Experiments are detected by
 * scanning the project root directory for subdirectories containing the
 * {@code ExperimentInfo.csv} file.
 * <p>
 * The panel also provides "Select All" and "Clear All" controls and exposes
 * helper methods to query selected experiments.
 */
public class ExperimentsPanel {


    private final JPanel          panel = new JPanel(new BorderLayout());   // The root container panel (BorderLayout).
    private final JPanel          list  = new JPanel();                     // The vertical list panel holding all experiment checkboxes.
    private final List<JCheckBox> boxes = new ArrayList<>();                // Stores the dynamically generated experiment checkboxes.

    /** Callback invoked whenever the checkbox selection changes. */
    private Runnable onChanged = () -> {
    };

    /**
     * Constructs an {@code ExperimentsPanel} and immediately loads the list of
     * experiments from the given project root.
     *
     * @param projectRoot the root directory of the project
     */
    public ExperimentsPanel(Path projectRoot) {
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        // Scrollable container for experiment checkboxes
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(680, 240));
        scroll.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        // Top control bar (Select All / Clear All)
        JPanel  controls  = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAll = new JButton("Select All");
        JButton clearAll  = new JButton("Clear All");
        controls.add(selectAll);
        controls.add(clearAll);

        // Select and clear all experiments
        selectAll.addActionListener(this::selectAllExperiments);
        clearAll.addActionListener(this::clearAllExperiments);

        panel.add(controls, BorderLayout.NORTH);
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
        this.onChanged = (runnable != null ? runnable : () -> {
        });
    }

    /**
     * Checks whether at least one experiment is selected.
     * @return true if any checkbox is selected, false otherwise
     */
    public boolean anySelected() {
        for (JCheckBox cb : boxes) {
            if (cb.isSelected()) {
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
        for (JCheckBox cb : boxes) {
            if (cb.isSelected()) {
                experimentNames.add(cb.getText());
            }
        }
        PaintLogger.debugf( "ExperimentsPanel.selectedExperimentNames: %s", experimentNames);
        return experimentNames;
    }

    /**
     * Enables or disables all experiment checkboxes.
     *
     * @param enabled whether the checkboxes should be enabled
     */
    public void setEnabled(boolean enabled) {
        for (JCheckBox checkBox : boxes) {
            checkBox.setEnabled(enabled);
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

                // Skip Sweep directory
                if ("Sweep".equals(experimentDirectory.getName())) {
                    continue;
                }

                // Create checkbox for the experiment
                JCheckBox cb = new JCheckBox(experimentDirectory.getName());
                boolean saved = PaintConfig.getBoolean("Experiments", experimentDirectory.getName(), false);
                cb.setSelected(saved);
                cb.addActionListener(e -> onChanged.run());
                boxes.add(cb);
                list.add(cb);
            }
        }

        list.revalidate();
        list.repaint();
        onChanged.run();  // Notify listeners of new state
    }

    private void selectAllExperiments(ActionEvent e) {
        for (JCheckBox cb : boxes) {
            cb.setSelected(true);
        }
        onChanged.run();
    }

    private void clearAllExperiments(ActionEvent e) {
        for (JCheckBox cb : boxes) {
            cb.setSelected(false);
        }
        onChanged.run();
    }
}