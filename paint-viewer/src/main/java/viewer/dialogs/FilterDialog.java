package viewer.dialogs;

import viewer.utils.RecordingEntry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The FilterDialog class provides a modal dialog for filtering a list of
 * RecordingEntry objects based on various attributes such as cell type,
 * probe name, probe type, adjuvant, and concentration.
 *
 * This class extends JDialog and uses JList components for user input,
 * allowing multiple filter criteria to be selected. Users can apply filters,
 * reset individual filters, reset all filters, or cancel the operation.
 *
 * Key Features:
 * 1. Displays various filtering options as lists, each grouped in separate
 *    panels with filter and reset buttons.
 * 2. Allows multi-selection in each filter list for flexible filtering.
 * 3. Updates the displayed options dynamically based on the filtered results.
 * 4. Provides options to apply filters, reset all filters, or cancel the actions.
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
     * Constructs a FilterDialog for filtering a list of RecordingEntry objects. The dialog
     * provides a user interface for selecting and applying various filtering options
     * based on attributes such as cell type, probe name, probe type, adjuvants, and concentrations.
     *
     * @param owner the parent frame that owns this dialog
     * @param recordings the list of RecordingEntry objects to be filtered
     */
    public FilterDialog(Frame owner, List<RecordingEntry> recordings) {
        super(owner, "Filter Recordings", true);
        this.originalRecordings = new ArrayList<>(recordings);
        this.filteredRecordings = new ArrayList<>(recordings);

        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Collect distinct values for each attribute
        // @formatter:off
        Set<String> cellTypes      = new TreeSet<>();
        Set<String> probeNames     = new TreeSet<>();
        Set<String> probeTypes     = new TreeSet<>();
        Set<String> adjuvants      = new TreeSet<>();
        Set<String> concentrations = new TreeSet<>();
        // @formatter:on

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

        // Global Apply / Reset All / Cancel buttons (right side, aligned top)
        JPanel rightButtonPanel = new JPanel();
        rightButtonPanel.setLayout(new BoxLayout(rightButtonPanel, BoxLayout.Y_AXIS));

        JButton applyButton = new JButton("Apply");
        JButton resetAllButton = new JButton("Reset All");
        JButton cancelButton = new JButton("Cancel");

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
     * Creates a JList from the provided set of string values.
     * The JList is configured to support multiple interval selection
     * and displays a maximum of five visible rows.
     *
     * @param values the set of string values to populate the JList
     * @return a JList containing the given values
     */
    private JList<String> createList(Set<String> values) {
        JList<String> list = new JList<>(values.toArray(new String[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(5);
        return list;
    }

    /**
     * Creates a JPanel containing a titled border, a scrollable JList,
     * and two buttons ("Filter" and "Reset") with attached functionality.
     * The "Filter" button applies filters based on selected items in the JList,
     * and the "Reset" button resets the filters.
     *
     * @param title the title to be displayed on the JPanel border
     * @param list  the JList component to be added to the panel; used for filtering and resetting functionality
     * @return a JPanel that integrates the JList and associated buttons in a structured layout
     */
    private JPanel createListBoxWithButtons(String title, JList<String> list) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(120, 160));
        panel.add(scrollPane, BorderLayout.CENTER);

        // @formatter:off
        JPanel btnPanel   = new JPanel(new GridLayout(2, 1, 0, 5));
        JButton filterBtn = new JButton("Filter");
        JButton resetBtn  = new JButton("Reset");
        // @formatter:on

        filterBtn.addActionListener(e -> applySingleFilter(list));
        resetBtn.addActionListener(e -> resetSingleFilter(list));

        btnPanel.add(filterBtn);
        btnPanel.add(resetBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Applies a filter to the list of recordings based on the selected items
     * in the provided JList. The method modifies the filteredRecordings field by
     * retaining only the entries that match the selected criteria in the JList.
     * Updates the UI lists after applying the filter.
     *
     * @param list the JList component containing selectable filter criteria
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
     * Resets the filtered recordings list to its original unfiltered state and updates the UI
     * components based on the full set of original recordings. This action corresponds
     * to resetting a single filter associated with the provided JList.
     *
     * @param list the JList representing the filter criteria to be reset
     */
    private void resetSingleFilter(JList<String> list) {
        filteredRecordings = new ArrayList<>(originalRecordings);
        updateLists();
    }

    /**
     * Resets all applied filters and restores the filtered recordings to the original unfiltered state.
     * This method resets the filteredRecordings list to match the originalRecordings list, ensuring
     * that no filtering constraints are applied. After resetting the filters, it updates the associated
     * UI lists to reflect the full set of available recordings and their attributes.
     */
    private void resetAllFilters() {
        filteredRecordings = new ArrayList<>(originalRecordings);
        updateLists();
    }

    /**
     * Checks if a given RecordingEntry matches the selected filter criteria in the specified JList.
     * The method evaluates the relevant attribute of the RecordingEntry based on the type of JList
     * provided (e.g., cell type, probe name, etc.) and determines whether it is included in the selected criteria.
     *
     * @param entry the RecordingEntry to be evaluated against the filter criteria
     * @param list the JList representing the filter criteria (e.g., cell type list, probe name list)
     * @param selected the list of selected filter criteria corresponding to the JList
     * @return true if the RecordingEntry matches the selected criteria; false otherwise
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

    /**
     * Updates the contents of several UI lists (cellTypeList, probeNameList, probeTypeList, adjuvantList, and concentrationList)
     * based on the unique attribute values present in the filteredRecordings list.
     *
     * This method utilizes the updateList helper method to populate each JList with the corresponding
     * set of distinct values derived from the attributes of the filtered recordings.
     *
     * Specific attributes of RecordingEntry objects are extracted, including cell type, probe name, probe type,
     * adjuvant, and concentration. The extracted values are converted to sets to ensure uniqueness, and
     * the UI lists are updated accordingly.
     */
    private void updateLists() {

        //@formatter:off
        updateList(cellTypeList,      filteredRecordings.stream().map(RecordingEntry::getCellType).collect(Collectors.toSet()));
        updateList(probeNameList,     filteredRecordings.stream().map(RecordingEntry::getProbeName).collect(Collectors.toSet()));
        updateList(probeTypeList,     filteredRecordings.stream().map(RecordingEntry::getProbeType).collect(Collectors.toSet()));
        updateList(adjuvantList,      filteredRecordings.stream().map(RecordingEntry::getAdjuvant).collect(Collectors.toSet()));
        updateList(concentrationList, filteredRecordings.stream().map(e -> String.valueOf(e.getConcentration())).collect(Collectors.toSet()));
        //@formatter:off

    }

    private void updateList(JList<String> list, Set<String> values) {
        list.setListData(values.toArray(new String[0]));
    }

    public List<RecordingEntry> getFilteredRecordings() {
        return filteredRecordings;
    }

    public boolean isCancelled() {
        return cancelled;
    }
}