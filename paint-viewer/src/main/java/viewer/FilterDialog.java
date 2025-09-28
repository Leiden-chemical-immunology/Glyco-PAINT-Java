package viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A dialog for filtering recordings by metadata fields.
 * Provides independent multi-selection lists for Cell Type,
 * Probe Name, Probe Type, and Adjuvant.
 */
public class FilterDialog extends JDialog {

    private final JList<String> cellTypeList;
    private final JList<String> probeNameList;
    private final JList<String> probeTypeList;
    private final JList<String> adjuvantList;

    private boolean applied = false;

    public FilterDialog(Frame owner,
                        Set<String> cellTypes,
                        Set<String> probeNames,
                        Set<String> probeTypes,
                        Set<String> adjuvants) {
        super(owner, "Filter Recordings", true);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        JPanel listsPanel = new JPanel(new GridLayout(1, 4, 10, 0));

        cellTypeList = createList(cellTypes, "Cell Type");
        probeNameList = createList(probeNames, "Probe Name");
        probeTypeList = createList(probeTypes, "Probe Type");
        adjuvantList = createList(adjuvants, "Adjuvant");

        listsPanel.add(wrapWithTitledBorder(cellTypeList, "Cell Type"));
        listsPanel.add(wrapWithTitledBorder(probeNameList, "Probe Name"));
        listsPanel.add(wrapWithTitledBorder(probeTypeList, "Probe Type"));
        listsPanel.add(wrapWithTitledBorder(adjuvantList, "Adjuvant"));

        add(listsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");
        JButton cancelButton = new JButton("Cancel");

        applyButton.addActionListener(this::applyAction);
        clearButton.addActionListener(e -> clearSelections());
        cancelButton.addActionListener(e -> {
            applied = false;
            setVisible(false);
        });

        buttonPanel.add(clearButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);

        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private JList<String> createList(Set<String> values, String name) {
        DefaultListModel<String> model = new DefaultListModel<>();
        values.stream().sorted().forEach(model::addElement);
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(10);
        list.setLayoutOrientation(JList.VERTICAL);
        return list;
    }

    private JScrollPane wrapWithTitledBorder(JList<String> list, String title) {
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private void applyAction(ActionEvent e) {
        applied = true;
        setVisible(false);
    }

    private void clearSelections() {
        cellTypeList.clearSelection();
        probeNameList.clearSelection();
        probeTypeList.clearSelection();
        adjuvantList.clearSelection();
    }

    public boolean isApplied() {
        return applied;
    }

    public List<String> getSelectedCellTypes() {
        return cellTypeList.getSelectedValuesList();
    }

    public List<String> getSelectedProbeNames() {
        return probeNameList.getSelectedValuesList();
    }

    public List<String> getSelectedProbeTypes() {
        return probeTypeList.getSelectedValuesList();
    }

    public List<String> getSelectedAdjuvants() {
        return adjuvantList.getSelectedValuesList();
    }
}