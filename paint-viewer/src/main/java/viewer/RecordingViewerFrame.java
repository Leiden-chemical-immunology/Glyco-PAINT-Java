package viewer;

import paint.shared.objects.Project;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RecordingViewerFrame extends JFrame {

    private final Project project;
    private final List<RecordingEntry> recordings;
    private int currentIndex = 0;

    private final JLabel leftImageLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel rightImageLabel = new JLabel("", SwingConstants.CENTER);
    private final DefaultTableModel attributesModel;
    private final JTable attributesTable;

    private final JLabel experimentLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel recordingLabel = new JLabel("", SwingConstants.CENTER);

    // Navigation buttons as fields
    private final JButton firstBtn = new JButton("|<");
    private final JButton prevBtn = new JButton("<");
    private final JButton nextBtn = new JButton(">");
    private final JButton lastBtn = new JButton(">|");

    // Position indicator
    private final JLabel positionLabel = new JLabel("", SwingConstants.CENTER);

    public RecordingViewerFrame(Project project, List<RecordingEntry> recordings) {
        super("Recording Viewer - " + project.getProjectPath().getFileName());
        this.project = project;
        this.recordings = recordings;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false); // fixed size

        int GAP = 15;

        // --- Images area ---
        JPanel imagesInner = new JPanel(new GridLayout(1, 2, GAP, 0));
        imagesInner.add(createSquareImagePanel(leftImageLabel));
        imagesInner.add(createSquareImagePanel(rightImageLabel));

        // --- Labels under images (experiment + recording) ---
        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        experimentLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        recordingLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelsPanel.add(experimentLabel);
        labelsPanel.add(recordingLabel);

        // --- Navigation centered below labels ---
        JPanel navPanel = new JPanel(new BorderLayout());
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonsPanel.add(firstBtn);
        buttonsPanel.add(prevBtn);
        buttonsPanel.add(nextBtn);
        buttonsPanel.add(lastBtn);

        navPanel.add(buttonsPanel, BorderLayout.CENTER);
        navPanel.add(positionLabel, BorderLayout.SOUTH);

        // Wrap labels + nav together
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

        // --- Attributes block (left) ---
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
        attributesTable.setFocusable(false); // prevent focus
        attributesTable.setRowSelectionAllowed(false); // no row selection
        attributesTable.setColumnSelectionAllowed(false); // no column selection
        attributesTable.setCellSelectionEnabled(false); // no cell selection
        attributesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(attributesTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(220, attributesTable.getRowHeight() * 6));
        attrPanel.add(scrollPane, BorderLayout.CENTER);

        // --- Actions block (right) ---
        JPanel actionsPanel = new JPanel(new BorderLayout());
        actionsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        actionsPanel.setPreferredSize(new Dimension(240, 0));

        JPanel actionsContent = new JPanel();
        actionsContent.setLayout(new BoxLayout(actionsContent, BoxLayout.Y_AXIS));

        actionsContent.add(new JButton("Action 1"));
        actionsContent.add(Box.createVerticalStrut(15));
        actionsContent.add(new JButton("Action 2"));
        actionsContent.add(Box.createVerticalStrut(15));
        actionsContent.add(new JButton("Action 3"));
        actionsContent.add(Box.createVerticalGlue());

        actionsPanel.add(actionsContent, BorderLayout.NORTH);

        // --- Assemble main layout ---
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(attrPanel, BorderLayout.WEST);
        mainPanel.add(imagesWithNav, BorderLayout.CENTER);
        mainPanel.add(actionsPanel, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        // --- Window size ---
        setSize(1500, 600);
        setLocationRelativeTo(null);

        // --- Nav actions ---
        firstBtn.addActionListener(e -> showEntry(0));
        prevBtn.addActionListener(e -> showEntry(Math.max(0, currentIndex - 1)));
        nextBtn.addActionListener(e -> showEntry(Math.min(recordings.size() - 1, currentIndex + 1)));
        lastBtn.addActionListener(e -> showEntry(recordings.size() - 1));

        // --- Keyboard navigation ---
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "prevRecording");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "nextRecording");
        inputMap.put(KeyStroke.getKeyStroke("HOME"), "firstRecording");
        inputMap.put(KeyStroke.getKeyStroke("END"), "lastRecording");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "pageUpRecording");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "pageDownRecording");

        actionMap.put("prevRecording", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showEntry(Math.max(0, currentIndex - 1));
            }
        });
        actionMap.put("nextRecording", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showEntry(Math.min(recordings.size() - 1, currentIndex + 1));
            }
        });
        actionMap.put("firstRecording", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showEntry(0);
            }
        });
        actionMap.put("lastRecording", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showEntry(recordings.size() - 1);
            }
        });
        actionMap.put("pageUpRecording", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showEntry(Math.max(0, currentIndex - 5));
            }
        });
        actionMap.put("pageDownRecording", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                showEntry(Math.min(recordings.size() - 1, currentIndex + 5));
            }
        });

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

            @Override
            public void setBounds(int x, int y, int width, int height) {
                int size = Math.min(width, height);
                super.setBounds(x, y, size, size);
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
        if (index < 0 || index >= recordings.size()) return;
        currentIndex = index;
        RecordingEntry entry = recordings.get(index);

        int size = 512;
        leftImageLabel.setIcon(scaleToFit(entry.getLeftImage(), size, size));
        rightImageLabel.setIcon(scaleToFit(entry.getRightImage(), size, size));
        leftImageLabel.setText(null);
        rightImageLabel.setText(null);

        experimentLabel.setText("Experiment: " + entry.getExperimentName());
        recordingLabel.setText("Recording: " + entry.getLeftImagePath().getFileName().toString());

        boolean densityOk = entry.getDensity() >= entry.getMinRequiredDensityRatio();
        boolean tauOk = entry.getTau() <= entry.getMaxAllowableVariability();
        boolean r2Ok = entry.getObservedRSquared() >= entry.getMinRequiredRSquared();

        attributesModel.setRowCount(0);
        attributesModel.addRow(new Object[]{"Probe", entry.getProbeName()});
        attributesModel.addRow(new Object[]{"Adjuvant", entry.getAdjuvant()});
        attributesModel.addRow(new Object[]{"Cell Type", entry.getCellType()});
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

        // --- position indicator: global and per-experiment ---
        String exp = entry.getExperimentName();

        int expTotal = 0;
        int expIndexWithin = 0;
        int seen = 0;
        for (int i = 0; i < recordings.size(); i++) {
            if (exp.equals(recordings.get(i).getExperimentName())) {
                if (i == currentIndex) expIndexWithin = seen;
                seen++;
            }
        }
        expTotal = seen;

        positionLabel.setText(
                "Global: " + (currentIndex + 1) + " / " + recordings.size() +
                        "  —  Experiment " + exp + ": " + (expIndexWithin + 1) + " / " + expTotal
        );

        // --- update nav buttons ---
        firstBtn.setEnabled(currentIndex > 0);
        prevBtn.setEnabled(currentIndex > 0);
        nextBtn.setEnabled(currentIndex < recordings.size() - 1);
        lastBtn.setEnabled(currentIndex < recordings.size() - 1);
    }
}