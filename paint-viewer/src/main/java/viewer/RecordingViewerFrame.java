package viewer;

import paint.shared.objects.Project;

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
    private final SquareGridPanel leftGridPanel = new SquareGridPanel(20, 20, 512, 512);

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

    // --- Square filter parameters ---
    private double minDensityRatio = 0.0;
    private double maxVariability = 0.0;
    private double minRSquared = 0.5;
    private int minDuration = 100;
    private int maxDuration = 500;
    private String neighbourMode = "Free";

    // --- Cell assignment state ---
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

        // --- Attributes panel (left) ---
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
        attributesTable.setColumnSelectionAllowed(false);
        attributesTable.setCellSelectionEnabled(false);
        attributesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(attributesTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(220, attributesTable.getRowHeight() * 6));
        attrPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Actions panel (right) ---
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        actionsPanel.setPreferredSize(new Dimension(240, 0));

        JPanel actionsContent = new JPanel();
        actionsContent.setLayout(new BoxLayout(actionsContent, BoxLayout.Y_AXIS));

        JButton filterButton = new JButton("Filter");
        JButton squareDialogButton = new JButton("Square Dialog");
        JButton cellDialogButton = new JButton("Cell Assignment");

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
            SquareControlDialog dialog = new SquareControlDialog(
                    this,
                    leftGridPanel,
                    this,
                    new SquareControlParams(
                            minDensityRatio,
                            maxVariability,
                            minRSquared,
                            minDuration,
                            maxDuration,
                            neighbourMode
                    )
            );
            dialog.setVisible(true);
        });

        cellDialogButton.addActionListener(e -> {
            CellAssignmentDialog dialog = new CellAssignmentDialog(this, new CellAssignmentDialog.Listener() {
                @Override
                public void onAssign(int cellId) {
                    assignSelectedSquares(cellId);
                }
                @Override
                public void onUndo() {
                    undoLastAssignment();
                }
                @Override
                public void onCancelSelection() {
                    clearSelection();
                }
            });
            dialog.setVisible(true);
        });

        actionsContent.add(filterButton);
        actionsContent.add(Box.createVerticalStrut(15));
        actionsContent.add(squareDialogButton);
        actionsContent.add(Box.createVerticalStrut(15));
        actionsContent.add(cellDialogButton);
        actionsContent.add(Box.createVerticalGlue());

        actionsPanel.add(actionsContent, BorderLayout.NORTH);

        // --- Main assembly ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(attrPanel, BorderLayout.WEST);
        mainPanel.add(imagesWithNav, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        setSize(1500, 700);
        setLocationRelativeTo(null);

        // Navigation actions
        firstBtn.addActionListener(e -> showEntry(0));
        prevBtn.addActionListener(e -> showEntry(Math.max(0, currentIndex - 1)));
        nextBtn.addActionListener(e -> showEntry(Math.min(recordings.size() - 1, currentIndex + 1)));
        lastBtn.addActionListener(e -> showEntry(recordings.size() - 1));

        if (!recordings.isEmpty()) {
            showEntry(0);
        }
    }

    private JPanel createSquareImagePanel(JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(512, 512);
            }
            @Override
            public void setBounds(int x, int y, int width, int height) {
                int size = Math.min(width, height);
                super.setBounds(x, y, size, size);
            }
        };
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private ImageIcon scaleToFit(ImageIcon icon, int width, int height) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) return null;
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void showEntry(int index) {
        if (index < 0 || index >= recordings.size()) {
            return;
        }

        int expectNumberOfSquares = 0; // TODO

        currentIndex = index;
        RecordingEntry entry = recordings.get(index);

        int size = 512;
        leftGridPanel.setBackgroundImage(entry.getLeftImage());
        rightImageLabel.setIcon(scaleToFit(entry.getRightImage(), size, size));

        leftGridPanel.setSquares(entry.getSquaresForViewer(project, expectNumberOfSquares));

        // Re-sync assignment map from the squares we just loaded
        resyncAssignmentsFromSquares();
        leftGridPanel.clearSelection();

        // Count experiment index
        int totalInExperiment = 0;
        int indexInExperiment = 0;
        for (RecordingEntry r : recordings) {
            if (r.getExperimentName().equals(entry.getExperimentName())) {
                totalInExperiment++;
                if (r == entry) {
                    indexInExperiment = totalInExperiment;
                }
            }
        }

        experimentLabel.setText("Experiment: " + entry.getExperimentName() +
                " (" + indexInExperiment + "/" + totalInExperiment + ")" +
                "   [Overall: " + (currentIndex + 1) + "/" + recordings.size() + "]");

        recordingLabel.setText("Recording: " + entry.getRecordingName());

        boolean densityOk = entry.getDensity() >= entry.getMinRequiredDensityRatio();
        boolean tauOk = entry.getTau() <= entry.getMaxAllowableVariability();
        boolean r2Ok = entry.getObservedRSquared() >= entry.getMinRequiredRSquared();

        attributesModel.setRowCount(0);
        attributesModel.addRow(new Object[]{"Probe", entry.getProbeName()});
        attributesModel.addRow(new Object[]{"Probe Type", entry.getProbeType()});
        attributesModel.addRow(new Object[]{"Adjuvant", entry.getAdjuvant()});
        attributesModel.addRow(new Object[]{"Cell Type", entry.getCellType()});
        attributesModel.addRow(new Object[]{"Concentration", entry.getConcentration()});
        attributesModel.addRow(new Object[]{"Spots", entry.getNumberOfSpots()});
        attributesModel.addRow(new Object[]{"Tracks", entry.getNumberOfTracks()});
        attributesModel.addRow(new Object[]{"Threshold", entry.getThreshold()});
        attributesModel.addRow(new Object[]{
                "Tau", entry.getTau() + " " + (tauOk ? "✅ ≤ " : "❌ ≤ ") + entry.getMaxAllowableVariability()
        });
        attributesModel.addRow(new Object[]{
                "Density", entry.getDensity() + " " + (densityOk ? "✅ ≥ " : "❌ ≥ ") + entry.getMinRequiredDensityRatio()
        });
        attributesModel.addRow(new Object[]{
                "R²", entry.getObservedRSquared() + " " + (r2Ok ? "✅ ≥ " : "❌ ≥ ") + entry.getMinRequiredRSquared()
        });

        updateNavButtons();
    }

    private void resyncAssignmentsFromSquares() {
        squareAssignments.clear();
        for (SquareForDisplay sq : leftGridPanel.getSquares()) {
            if (sq.cellId > 0) {
                squareAssignments.put(sq.squareNumber, sq.cellId);
            }
        }
    }

    private void updateNavButtons() {
        firstBtn.setEnabled(currentIndex > 0);
        prevBtn.setEnabled(currentIndex > 0);
        nextBtn.setEnabled(currentIndex < recordings.size() - 1);
        lastBtn.setEnabled(currentIndex < recordings.size() - 1);
    }

    // === Cell assignment methods ===
    public void assignSelectedSquares(int cellId) {
        Set<Integer> selected = leftGridPanel.getSelectedSquares();
        if (selected.isEmpty()) return;

        long timestamp = System.currentTimeMillis();

        // save current state for undo
        undoStack.push(new HashMap<>(squareAssignments));

        for (Integer sqId : selected) {
            squareAssignments.put(sqId, cellId);
            for (SquareForDisplay sq : leftGridPanel.getSquares()) {
                if (sq.squareNumber == sqId) {
                    sq.cellId = cellId;
                }
            }
        }

        RecordingEntry entry = recordings.get(currentIndex);
        String recordingName = entry.getRecordingName();

        // Log only assigned (cellId != 0) with one shared timestamp for this batch
        for (Integer sqId : selected) {
            int assignedCell = squareAssignments.getOrDefault(sqId, 0);
            if (assignedCell != 0) {
                System.out.printf("%s,%d,%d,%d%n",
                        recordingName, sqId, assignedCell, timestamp);
            }
        }

        leftGridPanel.repaint();
    }

    public void undoLastAssignment() {
        if (!undoStack.isEmpty()) {
            squareAssignments.clear();
            squareAssignments.putAll(undoStack.pop());

            // sync with panel
            for (SquareForDisplay sq : leftGridPanel.getSquares()) {
                sq.cellId = squareAssignments.getOrDefault(sq.squareNumber, 0);
            }

            leftGridPanel.repaint();
            System.out.println("Undo performed");
        }
    }

    public void clearSelection() {
        leftGridPanel.clearSelection();
        leftGridPanel.repaint();
        System.out.println("Selection cleared");
    }

    // === Existing square control ===
    public void updateSquareControlParameters(
            double densityRatio,
            double variability,
            double rSquared,
            int minDuration,
            int maxDuration,
            String neighbourMode
    ) {
        this.minDensityRatio = densityRatio;
        this.maxVariability = variability;
        this.minRSquared = rSquared;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.neighbourMode = neighbourMode;

        System.out.println("Updated square controls:");
        System.out.println(" DensityRatio = " + minDensityRatio);
        System.out.println(" Variability   = " + maxVariability);
        System.out.println(" R²            = " + minRSquared);
        System.out.println(" MinDuration   = " + minDuration);
        System.out.println(" MaxDuration   = " + maxDuration);
        System.out.println(" NeighbourMode = " + neighbourMode);

        repaint();
    }

    // Called when “Apply” is pressed
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
                    params.minDuration + "," + params.maxDuration + "," + params.neighbourMode;

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

            System.out.println("Wrote override record for " + recordingName + " into " + csvFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}