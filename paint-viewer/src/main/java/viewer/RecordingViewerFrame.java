package viewer;

import paint.shared.objects.Project;
import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

public class RecordingViewerFrame extends JFrame {

    private final Project project;
    private final List<RecordingEntry> recordings;
    private int currentIndex = 0;

    // Grid panel
    private final SquareGridPanel leftGridPanel = new SquareGridPanel(20, 20);

    // Labels, tables, UI
    private final JLabel rightImageLabel = new JLabel("", SwingConstants.CENTER);
    private final DefaultTableModel attributesModel;
    private final JTable attributesTable;
    private final JLabel experimentLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel recordingLabel = new JLabel("", SwingConstants.CENTER);

    // Navigation buttons
    private final JButton firstBtn = new JButton("|<");
    private final JButton prevBtn = new JButton("<");
    private final JButton nextBtn = new JButton(">");
    private final JButton lastBtn = new JButton(">|");

    // Cell assignment state
    private final Map<Integer, Integer> squareAssignments = new HashMap<>();
    private final Deque<Map<Integer, Integer>> undoStack = new ArrayDeque<>();

    public RecordingViewerFrame(Project project, List<RecordingEntry> recordings) {
        super("Recording Viewer - " + project.getProjectRootPath().getFileName());
        this.project = project;
        this.recordings = recordings;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        int GAP = 15;

        // --- Images area ---
        JPanel imagesInner = new JPanel(new GridLayout(1, 2, GAP, 0));
        imagesInner.add(createSquareImagePanel(leftGridPanel));
        imagesInner.add(createSquareImagePanel(rightImageLabel));

        // --- Labels under images ---
        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        experimentLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        recordingLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelsPanel.add(experimentLabel);
        labelsPanel.add(recordingLabel);

        // --- Navigation ---
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        navPanel.add(firstBtn);
        navPanel.add(prevBtn);
        navPanel.add(nextBtn);
        navPanel.add(lastBtn);

        JPanel imagesWithNav = new JPanel(new BorderLayout(GAP, GAP));
        imagesWithNav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
        ));
        imagesWithNav.add(imagesInner, BorderLayout.CENTER);
        imagesWithNav.add(labelsPanel, BorderLayout.SOUTH);
        imagesWithNav.add(navPanel, BorderLayout.NORTH);

        // --- Attributes panel ---
        JPanel attrPanel = new JPanel(new BorderLayout());
        attrPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        attrPanel.setPreferredSize(new Dimension(240, 0));

        attributesModel = new DefaultTableModel(new Object[]{"Attr", "Val"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        attributesTable = new JTable(attributesModel);
        attributesTable.setRowHeight(22);
        attributesTable.setFocusable(false);
        attributesTable.setRowSelectionAllowed(false);
        attributesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        attributesTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        attributesTable.getColumnModel().getColumn(1).setPreferredWidth(70);

        JScrollPane scrollPane = new JScrollPane(attributesTable);
        scrollPane.setPreferredSize(new Dimension(220, attributesTable.getRowHeight() * 6));
        attrPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Actions panel ---
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        actionsPanel.setPreferredSize(new Dimension(240, 0));

        JPanel actionsContent = new JPanel();
        actionsContent.setLayout(new BoxLayout(actionsContent, BoxLayout.Y_AXIS));

        // --- Buttons ---
        JButton filterButton = new JButton("Filter recordings");
        JButton squareDialogButton = new JButton("Select Squares");
        JButton cellDialogButton = new JButton("Assign Cells");

        int maxWidth = Math.max(
                filterButton.getPreferredSize().width,
                Math.max(squareDialogButton.getPreferredSize().width,
                         cellDialogButton.getPreferredSize().width)
        );
        Dimension uniformSize = new Dimension(maxWidth, filterButton.getPreferredSize().height);
        for (JButton b : Arrays.asList(filterButton, squareDialogButton, cellDialogButton)) {
            b.setMaximumSize(uniformSize);
            b.setPreferredSize(uniformSize);
            b.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        // --- Controls frame ---
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        controlsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        controlsPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 180));

        // Inner container for buttons (vertical)
        JPanel controlsInner = new JPanel();
        controlsInner.setLayout(new BoxLayout(controlsInner, BoxLayout.Y_AXIS));
        controlsInner.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        controlsInner.add(filterButton);
        controlsInner.add(Box.createVerticalStrut(10));
        controlsInner.add(squareDialogButton);
        controlsInner.add(Box.createVerticalStrut(10));
        controlsInner.add(cellDialogButton);

        controlsPanel.add(controlsInner, BorderLayout.CENTER);

        actionsContent.add(controlsPanel);
        actionsContent.add(Box.createVerticalStrut(5));

        // --- Borders & Shading ---
        JCheckBox showBordersCheckBox = new JCheckBox("Show borders", true);
        JCheckBox showShadingCheckBox = new JCheckBox("Show shading", true);

        showBordersCheckBox.addActionListener(e -> {
            boolean show = showBordersCheckBox.isSelected();
            leftGridPanel.setShowBorders(show);
            leftGridPanel.repaint();
        });

        showShadingCheckBox.addActionListener(e -> {
            boolean show = showShadingCheckBox.isSelected();
            leftGridPanel.setShowShading(show);
            leftGridPanel.repaint();
        });

        JPanel bordersPanel = new JPanel();
        bordersPanel.setLayout(new BoxLayout(bordersPanel, BoxLayout.Y_AXIS));
        bordersPanel.setBorder(BorderFactory.createTitledBorder("Borders and Shading"));
        bordersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        bordersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, bordersPanel.getPreferredSize().height));
        bordersPanel.add(showBordersCheckBox);
        bordersPanel.add(Box.createVerticalStrut(5));
        bordersPanel.add(showShadingCheckBox);
        actionsContent.add(bordersPanel);
        actionsContent.add(Box.createVerticalStrut(15));

        // --- Numbers ---
        JRadioButton numberNoneRadio = new JRadioButton("None", true);
        JRadioButton numberLabelRadio = new JRadioButton("Label");
        JRadioButton numberSquareRadio = new JRadioButton("Square");

        ButtonGroup numbersGroup = new ButtonGroup();
        numbersGroup.add(numberNoneRadio);
        numbersGroup.add(numberLabelRadio);
        numbersGroup.add(numberSquareRadio);

        numberNoneRadio.addActionListener(e -> {
            leftGridPanel.setNumberMode(SquareGridPanel.NumberMode.NONE);
            leftGridPanel.repaint();
        });
        numberLabelRadio.addActionListener(e -> {
            leftGridPanel.setNumberMode(SquareGridPanel.NumberMode.LABEL);
            leftGridPanel.repaint();
        });
        numberSquareRadio.addActionListener(e -> {
            leftGridPanel.setNumberMode(SquareGridPanel.NumberMode.SQUARE);
            leftGridPanel.repaint();
        });

        JPanel numbersInner = new JPanel();
        numbersInner.setLayout(new BoxLayout(numbersInner, BoxLayout.Y_AXIS));
        numbersInner.add(numberNoneRadio);
        numbersInner.add(Box.createVerticalStrut(5));
        numbersInner.add(numberLabelRadio);
        numbersInner.add(Box.createVerticalStrut(5));
        numbersInner.add(numberSquareRadio);

        JPanel numbersPanel = new JPanel(new BorderLayout());
        numbersPanel.setBorder(BorderFactory.createTitledBorder("Numbers"));
        numbersPanel.add(numbersInner, BorderLayout.WEST);
        numbersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        numbersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, numbersPanel.getPreferredSize().height));
        actionsContent.add(numbersPanel);
        actionsContent.add(Box.createVerticalGlue());

        // --- Equalize widths ---
        int panelWidth = Math.max(
                Math.max(controlsPanel.getPreferredSize().width,
                         bordersPanel.getPreferredSize().width),
                numbersPanel.getPreferredSize().width
        );
        controlsPanel.setMaximumSize(new Dimension(panelWidth, controlsPanel.getMaximumSize().height));
        bordersPanel.setMaximumSize(new Dimension(panelWidth, bordersPanel.getMaximumSize().height));
        numbersPanel.setMaximumSize(new Dimension(panelWidth, numbersPanel.getMaximumSize().height));

        actionsPanel.add(actionsContent, BorderLayout.NORTH);

        // --- Assemble ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(attrPanel, BorderLayout.WEST);
        mainPanel.add(imagesWithNav, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);

        setSize(1500, 700);
        setLocationRelativeTo(null);

        // --- Button logic ---
        filterButton.addActionListener(e -> {
            FilterDialog dialog = new FilterDialog(this, recordings);
            dialog.setVisible(true);
            if (!dialog.isCancelled()) {
                List<RecordingEntry> filtered = dialog.getFilteredRecordings();
                if (!filtered.isEmpty()) {
                    currentIndex = 0;
                    showEntry(0);
                }
            }
        });

        squareDialogButton.addActionListener(e -> {
            RecordingEntry current = recordings.get(currentIndex);
            SquareControlDialog dialog = new SquareControlDialog(
                    this,
                    leftGridPanel,
                    this,
                    new SquareControlParams(
                            current.getMinRequiredDensityRatio(),
                            current.getMaxAllowableVariability(),
                            current.getMinRequiredRSquared(),
                            "Free"
                    )
            );
            dialog.setVisible(true);
        });

        cellDialogButton.addActionListener(e -> {
            leftGridPanel.setSelectionEnabled(true);
            CellAssignmentDialog dialog = new CellAssignmentDialog(this, new CellAssignmentDialog.Listener() {
                public void onAssign(int cellId) { assignSelectedSquares(cellId); }
                public void onUndo() { undoLastAssignment(); }
                public void onCancelSelection() { clearSelection(); }
            });
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    leftGridPanel.setSelectionEnabled(false);
                }
            });
            dialog.setVisible(true);
        });

        // --- Navigation ---
        firstBtn.addActionListener(e -> showEntry(0));
        prevBtn.addActionListener(e -> showEntry(Math.max(0, currentIndex - 1)));
        nextBtn.addActionListener(e -> showEntry(Math.min(recordings.size() - 1, currentIndex + 1)));
        lastBtn.addActionListener(e -> showEntry(recordings.size() - 1));

        if (!recordings.isEmpty()) showEntry(0);
    }

    private JPanel createSquareImagePanel(JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout()) {
            public Dimension getPreferredSize() { return new Dimension(512, 512); }
            public void setBounds(int x, int y, int w, int h) {
                int size = Math.min(w, h);
                super.setBounds(x, y, size, size);
            }
        };
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private ImageIcon scaleToFit(ImageIcon icon, int w, int h) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) return null;
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void showEntry(int index) {
        if (index < 0 || index >= recordings.size()) return;
        currentIndex = index;
        RecordingEntry entry = recordings.get(index);
        int expectNumberOfSquares = 400;

        leftGridPanel.setBackgroundImage(entry.getLeftImage());
        rightImageLabel.setIcon(scaleToFit(entry.getRightImage(), 512, 512));
        leftGridPanel.setSquares(entry.getSquares(project, expectNumberOfSquares));

        experimentLabel.setText("Experiment: " + entry.getExperimentName() +
                                        "   [Overall: " + (currentIndex + 1) + "/" + recordings.size() + "]");
        recordingLabel.setText("Recording: " + entry.getRecordingName());

        attributesModel.setRowCount(0);
        attributesModel.addRow(new Object[]{"Probe", entry.getProbeName()});
        attributesModel.addRow(new Object[]{"Probe Type", entry.getProbeType()});
        attributesModel.addRow(new Object[]{"Adjuvant", entry.getAdjuvant()});
        attributesModel.addRow(new Object[]{"Cell Type", entry.getCellType()});
        attributesModel.addRow(new Object[]{"Concentration", entry.getConcentration()});
        attributesModel.addRow(new Object[]{"Number of Spots", entry.getNumberOfSpots()});
        attributesModel.addRow(new Object[]{"Number of Tracks", entry.getNumberOfTracks()});
        attributesModel.addRow(new Object[]{"Threshold", entry.getThreshold()});
        attributesModel.addRow(new Object[]{"Tau", formatWithPrecision(entry.getTau(), 1)});
        attributesModel.addRow(new Object[]{"Density", entry.getDensity()});
        attributesModel.addRow(new Object[]{"Min Density Ratio", entry.getMinRequiredDensityRatio()});
        attributesModel.addRow(new Object[]{"Max Variability", entry.getMaxAllowableVariability()});
        attributesModel.addRow(new Object[]{"Min R²", entry.getMinRequiredRSquared()});
        attributesModel.addRow(new Object[]{"Neighbour Mode", entry.getNeighbourMode()});
        updateNavButtons();
    }

    private void updateNavButtons() {
        firstBtn.setEnabled(currentIndex > 0);
        prevBtn.setEnabled(currentIndex > 0);
        nextBtn.setEnabled(currentIndex < recordings.size() - 1);
        lastBtn.setEnabled(currentIndex < recordings.size() - 1);
    }

    // === Cell assignment methods ===
    public void assignSelectedSquares(int cellId) {
        Set<Integer> selectedNow = leftGridPanel.getSelectedSquares();
        if (selectedNow.isEmpty()) return;

        undoStack.push(new HashMap<>(squareAssignments));
        for (Square sq : leftGridPanel.getSquares()) {
            if (selectedNow.contains(sq.getSquareNumber())) {
                sq.setCellId(cellId);
                squareAssignments.put(sq.getSquareNumber(), cellId);
            }
        }
        leftGridPanel.clearMouseSelection();
        leftGridPanel.repaint();
    }

    public void undoLastAssignment() {
        if (!undoStack.isEmpty()) {
            squareAssignments.clear();
            squareAssignments.putAll(undoStack.pop());
            for (Square sq : leftGridPanel.getSquares()) {
                int cellId = squareAssignments.getOrDefault(sq.getSquareNumber(), 0);
                sq.setCellId(cellId);
            }
            leftGridPanel.repaint();
        }
    }

    public void clearSelection() {
        leftGridPanel.clearSelection();
        leftGridPanel.repaint();
    }

    // === SquareControl ===

    public void updateSquareControlParameters(double densityRatio,
                                              double variability,
                                              double rSquared,
                                              String neighbourMode) {
        leftGridPanel.setControlParameters(
                densityRatio,
                variability,
                rSquared,
                neighbourMode
        );

        leftGridPanel.applyVisibilityFilter();  // recompute visible squares
        leftGridPanel.repaint();                // refresh display
    }

    public void updateSquareNumberMode(SquareGridPanel.NumberMode mode) {
        leftGridPanel.setNumberMode(mode);
        leftGridPanel.repaint();
    }

    public void applySquareControlParameters(String scope, SquareControlParams params) {
        String timestamp = LocalDateTime.now().toString();
        File csvFile = new File(project.getProjectRootPath().toFile(), "Viewer Override.csv");

        switch (scope) {
            case "Recording":
                RecordingEntry current = recordings.get(currentIndex);
                writeOverrideRecord(current.getRecordingName(), params, timestamp, csvFile);
                break;
            case "Experiment":
                RecordingEntry cur = recordings.get(currentIndex);
                for (RecordingEntry r : recordings) {
                    if (r.getExperimentName().equals(cur.getExperimentName())) {
                        writeOverrideRecord(r.getRecordingName(), params, timestamp, csvFile);
                    }
                }
                break;
            case "Project":
                for (RecordingEntry r : recordings) {
                    writeOverrideRecord(r.getRecordingName(), params, timestamp, csvFile);
                }
                break;
        }
    }

    private void writeOverrideRecord(String recordingName, SquareControlParams params,
                                     String timestamp, File csvFile) {
        PaintLogger.infof(
                "Override for '%s': MinDensityRatio=%.0f, MaxVariability=%.0f, MinRSquared=%.2f, NeighbourMode=%s",
                recordingName,
                params.densityRatio,
                params.variability,
                params.rSquared,
                params.neighbourMode
        );
        try {
            List<String> lines = new ArrayList<>();
            if (csvFile.exists()) {
                lines = Files.readAllLines(csvFile.toPath());
            }

            if (lines.isEmpty() || !lines.get(0).startsWith("recordingName,")) {
                lines.clear();
                lines.add("recordingName,timestamp,densityRatio,variability,rSquared,minDuration,maxDuration,neighbourMode");
            }

            String prefix = recordingName + ",";
            String newLine = recordingName + "," + timestamp + "," +
                    params.densityRatio + "," + params.variability + "," + params.rSquared + "," +
                    params.neighbourMode;

            boolean replaced = false;
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).startsWith(prefix)) {
                    lines.set(i, newLine);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lines.add(newLine);
            }

            File tmp = new File(csvFile.getParentFile(), csvFile.getName() + ".tmp");
            Files.write(tmp.toPath(), lines);
            Files.move(tmp.toPath(), csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String formatWithPrecision(double value, int precision) {
        if (Double.isNaN(value)) return "NaN";
        if (Double.isInfinite(value)) return value > 0 ? "∞" : "-∞";
        return String.format("%." + precision + "f", value);
    }
}