package viewer.dialogs;

import viewer.utils.RecordingEntry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FilterDialog extends JDialog {

    private final List<RecordingEntry> originalRecordings;
    private List<RecordingEntry> filteredRecordings;

    private final JList<String> cellTypeList;
    private final JList<String> probeNameList;
    private final JList<String> probeTypeList;
    private final JList<String> adjuvantList;
    private final JList<String> concentrationList;

    private boolean cancelled = true;

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

    private JList<String> createList(Set<String> values) {
        JList<String> list = new JList<>(values.toArray(new String[0]));
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(5);
        return list;
    }

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

    private void resetSingleFilter(JList<String> list) {
        filteredRecordings = new ArrayList<>(originalRecordings);
        updateLists();
    }

    private void resetAllFilters() {
        filteredRecordings = new ArrayList<>(originalRecordings);
        updateLists();
    }

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

    private void updateLists() {
        updateList(cellTypeList, filteredRecordings.stream().map(RecordingEntry::getCellType).collect(Collectors.toSet()));
        updateList(probeNameList, filteredRecordings.stream().map(RecordingEntry::getProbeName).collect(Collectors.toSet()));
        updateList(probeTypeList, filteredRecordings.stream().map(RecordingEntry::getProbeType).collect(Collectors.toSet()));
        updateList(adjuvantList, filteredRecordings.stream().map(RecordingEntry::getAdjuvant).collect(Collectors.toSet()));
        updateList(concentrationList, filteredRecordings.stream().map(e -> String.valueOf(e.getConcentration())).collect(Collectors.toSet()));
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