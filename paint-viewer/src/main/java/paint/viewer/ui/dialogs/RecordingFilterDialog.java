/*==============================================================================
 *  Class:        RecordingFilterDialog.java
 *  Package:      paint.viewer.dialogs
 *
 *  PURPOSE:
 *    Modal dialog to filter a list of RecordingEntry objects by metadata
 *    (Cell Type, Probe Name, Probe Type, Adjuvant, Concentration).
 *
 *  NOTES:
 *    - Highlights list boxes that have an active filter (blue border).
 *    - Per-column "Reset" only clears that column and reapplies the remaining filters.
 *    - "Reset All" clears everything and restores the full dataset.
 *
 *  UPDATED:
 *    2025-11-01
 ==============================================================================*/

package paint.viewer.ui.dialogs;
import static paint.shared.constants.PaintColumnNames.*;


import paint.viewer.model.RecordingEntry;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RecordingFilterDialog extends JDialog {

    // ----- Data sets -----
    private final List<RecordingEntry> originalRecordings; // full dataset (for Reset All)
    private       List<RecordingEntry> filteredRecordings; // current filtered result

    // ----- UI lists -----
    private final JList<String> cellTypeList;
    private final JList<String> probeNameList;
    private final JList<String> probeTypeList;
    private final JList<String> adjuvantList;
    private final JList<String> concentrationList;

    // ----- Book-keeping for highlighting and per-column behavior -----
    private final Map<JList<String>, JPanel> listPanels                                = new HashMap<>();
    private final Map<JList<String>, JButton> resetButtons                             = new HashMap<>();
    private final Map<JList<String>, String> titles                                    = new HashMap<>();
    private final Map<JList<String>, Function<RecordingEntry, String>> valueExtractors = new HashMap<>();
    private final Map<JList<String>, List<String>> selections                          = new HashMap<>();
    private final Set<JList<String>> activeFilters                                     = new HashSet<>();

    private       boolean            cancelled                                         = true;

    // ===== Constructor =====
    public RecordingFilterDialog(Frame owner,
                                 List<RecordingEntry> currentVisibleRecordings,
                                 List<RecordingEntry> allRecordings,
                                 FilterCriteria initialCriteria) {
        super(owner, "Filter Recordings", true);

        final FilterCriteria criteria = (initialCriteria == null) ? FilterCriteria.empty() : initialCriteria;

        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Store full dataset
        this.originalRecordings = new ArrayList<>(allRecordings);

        // Compute the base list (start subset) from the current visible subset and incoming criteria
        // visible subset when dialog opened
        List<RecordingEntry> baseList;
        if (criteria.isNotEmpty()) {
            baseList = currentVisibleRecordings.stream()
                                               .filter(e -> criteria.cellTypes.isEmpty()      || criteria.cellTypes.contains(e.getCellType()))
                                               .filter(e -> criteria.probeNames.isEmpty()     || criteria.probeNames.contains(e.getProbeName()))
                                               .filter(e -> criteria.probeTypes.isEmpty()     || criteria.probeTypes.contains(e.getProbeType()))
                                               .filter(e -> criteria.adjuvants.isEmpty()      || criteria.adjuvants.contains(e.getAdjuvant()))
                                               .filter(e -> criteria.concentrations.isEmpty() || criteria.concentrations.contains(String.valueOf(e.getConcentration())))
                                               .collect(Collectors.toList());
        } else {
            baseList = new ArrayList<>(currentVisibleRecordings);
        }

        this.filteredRecordings = new ArrayList<>(baseList);

        // Build initial list box contents from baseList (not the full dataset)
        Set<String> cellTypes      = baseList.stream()
                                             .map(RecordingEntry::getCellType)
                                             .collect(Collectors.toCollection(TreeSet::new));
        Set<String> probeNames     = baseList.stream()
                                             .map(RecordingEntry::getProbeName)
                                             .collect(Collectors.toCollection(TreeSet::new));
        Set<String> probeTypes     = baseList.stream()
                                             .map(RecordingEntry::getProbeType)
                                             .collect(Collectors.toCollection(TreeSet::new));
        Set<String> adjuvants      = baseList.stream()
                                             .map(RecordingEntry::getAdjuvant)
                                             .collect(Collectors.toCollection(TreeSet::new));
        Set<String> concentrations = baseList.stream()
                                             .map(e -> String.valueOf(e.getConcentration()))
                                             .collect(Collectors.toCollection(TreeSet::new));

        cellTypeList      = createList(cellTypes);
        probeNameList     = createList(probeNames);
        probeTypeList     = createList(probeTypes);
        adjuvantList      = createList(adjuvants);
        concentrationList = createList(concentrations);

        // Register metadata for each list box
        titles.put(cellTypeList,      CELL_TYPE);
        titles.put(probeNameList,     PROBE_NAME);
        titles.put(probeTypeList,     PROBE_TYPE);
        titles.put(adjuvantList,      ADJUVANT);
        titles.put(concentrationList, CONCENTRATION);

        valueExtractors.put(cellTypeList,      RecordingEntry::getCellType);
        valueExtractors.put(probeNameList,     RecordingEntry::getProbeName);
        valueExtractors.put(probeTypeList,     RecordingEntry::getProbeType);
        valueExtractors.put(adjuvantList,      RecordingEntry::getAdjuvant);
        valueExtractors.put(concentrationList, e -> String.valueOf(e.getConcentration()));

        // main lists row
        JPanel listPanel = new JPanel(new GridLayout(1, 6, 10, 0));
        listPanel.add(createListBoxWithButtons(cellTypeList));
        listPanel.add(createListBoxWithButtons(probeNameList));
        listPanel.add(createListBoxWithButtons(probeTypeList));
        listPanel.add(createListBoxWithButtons(adjuvantList));
        listPanel.add(createListBoxWithButtons(concentrationList));

        // Right controls
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

        resetAllButton.addActionListener(e -> {
            // Reset everything to the full dataset, clear all selections and highlights
            filteredRecordings = new ArrayList<>(originalRecordings);
            updateAllListsFrom(filteredRecordings);

            // Clear remembered selections and active filter flags
            for (JList<String> l : selections.keySet()) {
                selections.put(l, Collections.emptyList());
                l.clearSelection();
            }
            activeFilters.clear();
            updateResetButtonStates();
        });

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

        // Initialize the selection map with empties
        for (JList<String> l : Arrays.asList(cellTypeList, probeNameList, probeTypeList, adjuvantList, concentrationList)) {
            selections.put(l, Collections.emptyList());
        }

        // Restore incoming criteria -> selections/activeFilters, apply, and highlight
        if (criteria.isNotEmpty()) {
            restoreSelectionsFromCriteria(criteria);
            filteredRecordings = applyAllFilters(); // apply all selected lists
            updateAllListsFrom(filteredRecordings);
        }
        updateResetButtonStates();
    }

    // ===== UI helpers =====

    private JList<String> createList(Set<String> values) {
        JList<String> list = new JList<>(values.toArray(new String[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(5);
        return list;
    }

    private JPanel createListBoxWithButtons(JList<String> list) {
        String title = titles.get(list);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        listPanels.put(list, panel);

        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(140, 170));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel   = new JPanel(new GridLayout(2, 1, 0, 5));
        JButton filterBtn = new JButton("Filter");
        JButton resetBtn  = new JButton("Reset");
        resetBtn.setEnabled(false); // initially disabled
        resetButtons.put(list, resetBtn);

        filterBtn.addActionListener(e -> onFilterClicked(list));
        resetBtn.addActionListener(e -> onResetClicked(list));

        btnPanel.add(filterBtn);
        btnPanel.add(resetBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        return panel;
    }

    // ===== Button handlers =====

    private void onFilterClicked(JList<String> list) {
        List<String> selected = list.getSelectedValuesList();
        if (selected == null || selected.isEmpty()) {
            return;
        }

        // Remember this column’s selection and mark it as active
        selections.put(list, new ArrayList<>(selected));
        activeFilters.add(list);

        // Recompute from the full dataset (so filters stack properly)
        filteredRecordings = applyAllFilters();

        // Update visible lists to reflect the new filtered set
        updateAllListsFrom(filteredRecordings);

        // Restore previous selections for *all* active lists
        for (Map.Entry<JList<String>, List<String>> entry : selections.entrySet()) {
            reselect(entry.getKey(), entry.getValue());
        }

        // Update borders (highlight active lists)
        updateResetButtonStates();
    }

    private void onResetClicked(JList<String> list) {
        // Clear only THIS column’s selection and flag
        selections.put(list, Collections.emptyList());
        activeFilters.remove(list);

        // Re-apply the remaining active filters to the base list
        filteredRecordings = applyAllFilters();

        // Update UI lists from the recomputed working set
        updateAllListsFrom(filteredRecordings);

        // Clear selection on this list; preserve selections on others
        list.clearSelection();
        for (Map.Entry<JList<String>, List<String>> e : selections.entrySet()) {
            if (!e.getKey().equals(list) && !e.getValue().isEmpty()) {
                reselect(e.getKey(), e.getValue());
            }
        }
        updateResetButtonStates();
    }

    // ===== Core filtering =====

    /**
     * Apply all currently active filters (as recorded in 'selections') to the baseList.
     */
    private List<RecordingEntry> applyAllFilters() {
        // Always reapply filters to the full dataset, not the baseList.
        List<RecordingEntry> current = new ArrayList<>(originalRecordings);
        for (Map.Entry<JList<String>, List<String>> entry : selections.entrySet()) {
            JList<String> l = entry.getKey();
            List<String> sel = entry.getValue();
            if (sel == null || sel.isEmpty()) {
                continue;
            }

            Function<RecordingEntry, String> extractor = valueExtractors.get(l);
            current = current.stream()
                    .filter(r -> sel.contains(extractor.apply(r)))
                    .collect(Collectors.toList());
        }
        return current;
    }

    // ===== List content updates =====

    private void updateAllListsFrom(List<RecordingEntry> source) {
        updateList(cellTypeList,      collectDistinct(source, valueExtractors.get(cellTypeList)));
        updateList(probeNameList,     collectDistinct(source, valueExtractors.get(probeNameList)));
        updateList(probeTypeList,     collectDistinct(source, valueExtractors.get(probeTypeList)));
        updateList(adjuvantList,      collectDistinct(source, valueExtractors.get(adjuvantList)));
        updateList(concentrationList, collectDistinct(source, valueExtractors.get(concentrationList)));
    }

    private Set<String> collectDistinct(List<RecordingEntry> src, Function<RecordingEntry, String> fn) {
        return src.stream().map(fn).collect(Collectors.toCollection(TreeSet::new));
    }

    private void updateList(JList<String> list, Set<String> values) {
        list.setListData(values.toArray(new String[0]));
    }

    private void reselect(JList<String> list, List<String> values) {
        if (values == null || values.isEmpty()) {
            list.clearSelection();
            return;
        }
        ListModel<String> model = list.getModel();
        java.util.List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            String v = model.getElementAt(i);
            if (values.contains(v)) {
                indices.add(i);
            }
        }
        int[] idxArr = indices.stream().mapToInt(Integer::intValue).toArray();
        list.setSelectedIndices(idxArr);
    }

    // ===== Visual highlighting =====

    private void updateResetButtonStates() {
        for (Map.Entry<JList<String>, JButton> e : resetButtons.entrySet()) {
            JList<String> list = e.getKey();
            JButton button = e.getValue();
            button.setEnabled(activeFilters.contains(list));
        }
    }

    // ===== Criteria restore =====

    private void restoreSelectionsFromCriteria(FilterCriteria filterCriteria) {
        // Copy criteria into the selection map and mark active filters
        setSelectionFrom(cellTypeList,      filterCriteria.cellTypes);
        setSelectionFrom(probeNameList,     filterCriteria.probeNames);
        setSelectionFrom(probeTypeList,     filterCriteria.probeTypes);
        setSelectionFrom(adjuvantList,      filterCriteria.adjuvants);
        setSelectionFrom(concentrationList, filterCriteria.concentrations);

        // Reflect those selections in the JLists
        reselect(cellTypeList,      selections.get(cellTypeList));
        reselect(probeNameList,     selections.get(probeNameList));
        reselect(probeTypeList,     selections.get(probeTypeList));
        reselect(adjuvantList,      selections.get(adjuvantList));
        reselect(concentrationList, selections.get(concentrationList));
    }

    private void setSelectionFrom(JList<String> list, List<String> values) {
        if (values != null && !values.isEmpty()) {
            selections.put(list, new ArrayList<>(values));
            activeFilters.add(list);
        } else {
            selections.put(list, Collections.emptyList());
            activeFilters.remove(list);
        }
    }

    // ===== Getters =====

    public List<RecordingEntry> getFilteredRecordings() {
        return filteredRecordings;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    // ===== Criteria I/O =====

    public static class FilterCriteria {
        public final List<String> cellTypes;
        public final List<String> probeNames;
        public final List<String> probeTypes;
        public final List<String> adjuvants;
        public final List<String> concentrations;

        public FilterCriteria(List<String> cellTypes,
                              List<String> probeNames,
                              List<String> probeTypes,
                              List<String> adjuvants,
                              List<String> concentrations) {
            this.cellTypes      = cellTypes;
            this.probeNames     = probeNames;
            this.probeTypes     = probeTypes;
            this.adjuvants      = adjuvants;
            this.concentrations = concentrations;
        }

        public static FilterCriteria empty() {
            return new FilterCriteria(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        public boolean isEmpty() {
            return cellTypes.isEmpty()
                    && probeNames.isEmpty()
                    && probeTypes.isEmpty()
                    && adjuvants.isEmpty()
                    && concentrations.isEmpty();
        }

        public boolean isNotEmpty() {
            return !isEmpty();
        }
    }

    public FilterCriteria getCurrentFilterCriteria() {
        return new FilterCriteria(
                selections.get(cellTypeList),
                selections.get(probeNameList),
                selections.get(probeTypeList),
                selections.get(adjuvantList),
                selections.get(concentrationList)
        );
    }
}