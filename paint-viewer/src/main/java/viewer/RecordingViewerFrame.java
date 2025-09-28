package viewer;

import paint.shared.objects.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RecordingViewerFrame extends JFrame {

    private final Project project;
    private final List<RecordingEntry> originalRecordings;
    private List<RecordingEntry> recordings;
    private int currentIndex = 0;

    private final JLabel leftImageLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel rightImageLabel = new JLabel("", SwingConstants.CENTER);
    private final DefaultTableModel attributesModel;
    private final JTable attributesTable;

    private final JLabel experimentLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel recordingLabel = new JLabel("", SwingConstants.CENTER);

    private final JButton firstBtn = new JButton("|<");
    private final JButton prevBtn = new JButton("<");
    private final JButton nextBtn = new JButton(">");
    private final JButton lastBtn = new JButton(">|");

    public RecordingViewerFrame(Project project, List<RecordingEntry> recordings) {
        super("Recording Viewer - " + project.getProjectPath().getFileName());
        this.project = project;
        this.originalRecordings = recordings;
        this.recordings = recordings;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        int GAP = 15;

        // --- Images area ---
        JPanel imagesInner = new JPanel(new GridLayout(1, 2, GAP, 0));
        imagesInner.add(createSquareImagePanel(leftImageLabel));
        imagesInner.add(createSquareImagePanel(rightImageLabel));

        // --- Labels under images ---
        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
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

        JPanel southWrapper = new JPanel(new BorderLayout());
        southWrapper.add(labelsPanel, BorderLayout.NORTH);
        southWrapper.add(navPanel, BorderLayout.SOUTH);

        JPanel imagesWithNav = new JPanel(new BorderLayout(GAP, GAP));
        imagesWithNav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
        ));
        imagesWithNav.add(imagesInner, BorderLayout.CENTER);
        imagesWithNav.add(southWrapper, BorderLayout.SOUTH);

        // --- Attributes block ---
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
        attributesTable.setFont(new Font("SansSerif", Font.PLAIN, 12));
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

        // --- Actions block ---
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        actionsPanel.setPreferredSize(new Dimension(240, 0));

        JPanel actionsContent = new JPanel();
        actionsContent.setLayout(new BoxLayout(actionsContent, BoxLayout.Y_AXIS));

        JButton action1Button = new JButton("Filter…");
        action1Button.addActionListener(e -> openFilterDialog());

        actionsContent.add(action1Button);
        actionsContent.add(Box.createVerticalStrut(15));
        actionsContent.add(new JButton("Action 2"));
        actionsContent.add(Box.createVerticalStrut(15));
        actionsContent.add(new JButton("Action 3"));
        actionsContent.add(Box.createVerticalGlue());

        actionsPanel.add(actionsContent, BorderLayout.NORTH);

        // --- Assemble ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(attrPanel, BorderLayout.WEST);
        mainPanel.add(imagesWithNav, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        setSize(1500, 600);
        setLocationRelativeTo(null);

        // Nav actions
        firstBtn.addActionListener(e -> showEntry(0));
        prevBtn.addActionListener(e -> showEntry(currentIndex - 1));
        nextBtn.addActionListener(e -> showEntry(currentIndex + 1));
        lastBtn.addActionListener(e -> showEntry(recordings.size() - 1));

        if (!recordings.isEmpty()) {
            showEntry(0);
        }
    }

    private JPanel createSquareImagePanel(JLabel label) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(512, 512);
            }
        };
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private ImageIcon scaleToFit(ImageIcon icon, int width, int height) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) return null;
        Image img = icon.getImage();
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void showEntry(int index) {
        if (recordings.isEmpty()) {
            experimentLabel.setText("No recordings");
            recordingLabel.setText("");
            leftImageLabel.setIcon(null);
            rightImageLabel.setIcon(null);
            updateNavButtons();
            return;
        }

        if (index < 0 || index >= recordings.size()) return;
        currentIndex = index;
        RecordingEntry entry = recordings.get(index);

        int size = 512;
        leftImageLabel.setIcon(scaleToFit(entry.getLeftImage(), size, size));
        rightImageLabel.setIcon(scaleToFit(entry.getRightImage(), size, size));
        leftImageLabel.setText(null);
        rightImageLabel.setText(null);

        experimentLabel.setText("Experiment: " + entry.getExperimentName());
        recordingLabel.setText("Recording: " + entry.getRecordingName() +
                " (" + (index + 1) + " / " + recordings.size() + ")");

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

    private void updateNavButtons() {
        firstBtn.setEnabled(currentIndex > 0);
        prevBtn.setEnabled(currentIndex > 0);
        nextBtn.setEnabled(currentIndex < recordings.size() - 1);
        lastBtn.setEnabled(currentIndex < recordings.size() - 1);
    }

    private void openFilterDialog() {
        FilterDialog dialog = new FilterDialog(this, originalRecordings);
        dialog.setVisible(true);

        if (!dialog.isCancelled()) {
            recordings = dialog.getFilteredRecordings();
            if (!recordings.isEmpty()) {
                showEntry(0);
            } else {
                showEntry(-1);
            }
        }
    }
}