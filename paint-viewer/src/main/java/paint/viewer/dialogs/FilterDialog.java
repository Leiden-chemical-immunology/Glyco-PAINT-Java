/******************************************************************************
 *  Class:        FilterDialog.java
 *  Package:      paint.viewer.dialogs
 *
 *  PURPOSE:
 *    Provides a modal dialog for filtering a list of {@link paint.viewer.utils.RecordingEntry}
 *    objects by metadata attributes such as Cell Type, Probe Name, Probe Type,
 *    Adjuvant, and Concentration.
 *
 *  DESCRIPTION:
 *    The dialog presents a column-based interface with multi-selectable lists
 *    for each attribute category. Each column allows users to apply or reset
 *    filters independently, while global “Apply”, “Reset All”, and “Cancel”
 *    controls manage overall filter state.
 *
 *    The dialog dynamically updates available values based on active filters
 *    and always reflects the distinct attribute values in the filtered set.
 *
 *  KEY FEATURES:
 *    • Filter {@code RecordingEntry} objects by any combination of attributes.
 *    • Multi-select filtering lists with per-column Filter/Reset buttons.
 *    • “Apply”, “Reset All”, and “Cancel” global controls.
 *    • Dynamic list updates based on current filtered results.
 *    • Modal operation blocking interaction with the parent frame.
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
 ******************************************************************************/

package paint.viewer.dialogs;

import paint.viewer.utils.RecordingEntry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A modal dialog for filtering a list of {@link RecordingEntry} objects
 * based on attributes such as cell type, probe name, probe type,
 * adjuvant, and concentration.
 *
 * <p>Each filter category is displayed in a column with its own JList,
 * “Filter” and “Reset” buttons. Users can apply individual filters,
 * reset them, reset all filters, or cancel the operation.</p>
 */
public class FilterDialog extends JDialog {

    private final List<RecordingEntry> originalRecordings;
    private List<RecordingEntry> filteredRecordings;

    private final JList<String> cellTypeList;
    private final JList<String> probeNameList;
    private final JList<String> probeTypeList;
    private final JList<String> adjuvantList;
    private final JList<String> concentrationList;

    private boolean cancelled = true;

    /**
     * Constructs a modal {@code FilterDialog} for filtering a list of
     * {@link RecordingEntry} objects. The dialog allows the user to select
     * and apply filters on key attributes of the recordings.
     *
     * @param owner      the parent frame that owns this dialog
     * @param recordings the list of {@code RecordingEntry} objects to be filtered
     */
    public FilterDialog(Frame owner, List<RecordingEntry> recordings) {
        super(owner, "Filter Recordings", true);
        this.originalRecordings = new ArrayList<>(recordings);
        this.filteredRecordings = new ArrayList<>(recordings);

        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // ─────────────────────────────────────────────────────────────────────
        // Collect distinct values for each attribute
        // ─────────────────────────────────────────────────────────────────────
        Set<String> cellTypes      = new TreeSet<>();
        Set<String> probeNames     = new TreeSet<>();
        Set<String> probeTypes     = new TreeSet<>();
        Set<String> adjuvants      = new TreeSet<>();
        Set<String> concentrations = new TreeSet<>();

        for (RecordingEntry entry : recordings) {
            cellTypes.add(entry.getCellType());
            probeNames.add(entry.getProbeName());
            probeTypes.add(entry.getProbeType());
            adjuvants.add(entry.getAdjuvant());
            concentrations.add(String.valueOf(entry.getConcentration()));
        }

        // @formatter:off
        cellTypeList      = createList(cellTypes);
        probeNameList     = createList(probeNames);
        probeTypeList     = createList(probeTypes);
        adjuvantList      = createList(adjuvants);
        concentrationList = createList(concentrations);
        // @formatter:on

        JPanel listPanel = new JPanel(new GridLayout(1, 6, 10, 0));
        listPanel.add(createListBoxWithButtons("Cell Type", cellTypeList));
        listPanel.add(createListBoxWithButtons("Probe Name", probeNameList));
        listPanel.add(createListBoxWithButtons("Probe Type", probeTypeList));
        listPanel.add(createListBoxWithButtons("Adjuvant", adjuvantList));
        listPanel.add(createListBoxWithButtons("Concentration", concentrationList));

        // ─────────────────────────────────────────────────────────────────────
        // Global Apply / Reset All / Cancel controls
        // ─────────────────────────────────────────────────────────────────────
        JPanel rightButtonPanel = new JPanel();
        rightButtonPanel.setLayout(new BoxLayout(rightButtonPanel, BoxLayout.Y_AXIS));

        JButton applyButton    = new JButton("Apply");
        JButton resetAllButton = new JButton("Reset All");
        JButton cancelButton   = new JButton("Cancel");

        Dimension btnSize = new Dimension(100, 30);
        applyButton.setMaximumSize(btnSize);
        resetAllButton.setMaximumSize(btnSize);
        cancelButton.setMaximumSize(btnSize);

        applyButton.addActionListener(e -> {
            cancelled = false;
            dispose();
        });
        resetAllButton.addActionListener(e -> resetAllFilters());
        cancelButton.addActionListener(e -> {
            cancelled = true;
            dispose();
        });

        rightButtonPanel.add(applyButton);
        rightButtonPanel.add(Box.createVerticalStrut(10));
        rightButtonPanel.add(resetAllButton);
        rightButtonPanel.add(Box.createVerticalStrut(10));
        rightButtonPanel.add(cancelButton);
        rightButtonPanel.add(Box.createVerticalGlue());

        listPanel.add(rightButtonPanel);

        add(listPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Creates a multi-select {@link JList} populated with a given set of values.
     *
     * @param values the distinct string values to populate the list
     * @return a configured {@code JList} instance
     */
    private JList<String> createList(Set<String> values) {
        JList<String> list = new JList<>(values.toArray(new String[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(5);
        return list;
    }

    /**
     * Creates a bordered panel containing a labeled {@link JList} and
     * two buttons (“Filter” and “Reset”) that act on that list.
     *
     * @param title the title for the panel border
     * @param list  the {@code JList} instance to be displayed
     * @return the assembled panel
     */
    private JPanel createListBoxWithButtons(String title, JList<String> list) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(120, 160));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel   = new JPanel(new GridLayout(2, 1, 0, 5));
        JButton filterBtn = new JButton("Filter");
        JButton resetBtn  = new JButton("Reset");

        filterBtn.addActionListener(e -> applySingleFilter(list));
        resetBtn.addActionListener(e -> resetSingleFilter(list));

        btnPanel.add(filterBtn);
        btnPanel.add(resetBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Applies filtering logic using the selected values from a specific list.
     *
     * @param list the {@code JList} containing selected filter criteria
     */
    private void applySingleFilter(JList<String> list) {
        List<String> selected = list.getSelectedValuesList();
        if (selected.isEmpty()) {
            return;
        }

        filteredRecordings = filteredRecordings.stream()
                .filter(entry -> matches(entry, list, selected))
                .collect(Collectors.toList());
        updateLists();
    }

    /**
     * Resets a single filter by restoring {@code filteredRecordings}
     * to its original state and updating UI lists.
     *
     * @param list the list whose filter is being reset
     */
    private void resetSingleFilter(JList<String> list) {
        filteredRecordings = new ArrayList<>(originalRecordings);
        updateLists();
    }

    /** Resets all filters and restores the full unfiltered dataset. */
    private void resetAllFilters() {
        filteredRecordings = new ArrayList<>(originalRecordings);
        updateLists();
    }

    /**
     * Determines if a {@link RecordingEntry} matches the selected filter criteria.
     *
     * @param entry     the recording being tested
     * @param list      the {@code JList} defining the filter dimension
     * @param selected  selected filter values
     * @return {@code true} if the recording matches the criteria
     */
    private boolean matches(RecordingEntry entry, JList<String> list, List<String> selected) {
        if (list == cellTypeList) {
            return selected.contains(entry.getCellType());
        } else if (list == probeNameList) {
            return selected.contains(entry.getProbeName());
        } else if (list == probeTypeList) {
            return selected.contains(entry.getProbeType());
        } else if (list == adjuvantList) {
            return selected.contains(entry.getAdjuvant());
        } else if (list == concentrationList) {
            return selected.contains(String.valueOf(entry.getConcentration()));
        }
        return true;
    }

    /** Updates each filter list based on the attributes of the filtered recordings. */
    private void updateLists() {
        updateList(cellTypeList,      filteredRecordings.stream().map(RecordingEntry::getCellType).collect(Collectors.toSet()));
        updateList(probeNameList,     filteredRecordings.stream().map(RecordingEntry::getProbeName).collect(Collectors.toSet()));
        updateList(probeTypeList,     filteredRecordings.stream().map(RecordingEntry::getProbeType).collect(Collectors.toSet()));
        updateList(adjuvantList,      filteredRecordings.stream().map(RecordingEntry::getAdjuvant).collect(Collectors.toSet()));
        updateList(concentrationList, filteredRecordings.stream().map(e -> String.valueOf(e.getConcentration())).collect(Collectors.toSet()));
    }

    /** Replaces the data model of a {@link JList} with a new set of values. */
    private void updateList(JList<String> list, Set<String> values) {
        list.setListData(values.toArray(new String[0]));
    }

    /** Returns the currently filtered list of recordings. */
    public List<RecordingEntry> getFilteredRecordings() {
        return filteredRecordings;
    }

    /** Returns {@code true} if the user canceled the dialog. */
    public boolean isCancelled() {
        return cancelled;
    }
}