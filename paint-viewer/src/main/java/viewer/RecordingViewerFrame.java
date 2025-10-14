package viewer;

import paint.shared.config.PaintConfig;
import paint.shared.dialogs.RootSelectionDialog;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;

import viewer.dialogs.CellAssignmentDialog;
import viewer.dialogs.FilterDialog;
import viewer.dialogs.SquareControlDialog;
import viewer.logic.CellAssignmentManager;
import viewer.logic.SquareControlHandler;
import viewer.logic.ViewerOverrideWriter;
import viewer.panels.NavigationPanel;
import viewer.panels.RecordingAttributesPanel;
import viewer.panels.RecordingControlsPanel;
import viewer.panels.SquareGridPanel;
import viewer.shared.SquareControlParams;
import viewer.utils.RecordingEntry;
import viewer.utils.TiffMoviePlayer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static paint.shared.config.PaintConfig.getString;
import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_WIDTH;

public class RecordingViewerFrame extends JFrame
        implements RecordingControlsPanel.Listener, NavigationPanel.Listener {

    private final Project project;
    private final List<RecordingEntry> recordings;
    private int currentIndex = 0;

    private SquareGridPanel leftGridPanel;
    private final JLabel rightImageLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel experimentLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel recordingLabel = new JLabel("", SwingConstants.CENTER);

    private RecordingAttributesPanel attributesPanel;
    private RecordingControlsPanel controlsPanel;
    private NavigationPanel navigationPanel;

    private final CellAssignmentManager assignmentManager = new CellAssignmentManager();
    private final ViewerOverrideWriter overrideWriter;
    private final SquareControlHandler controlHandler = new SquareControlHandler();

    public RecordingViewerFrame(Project project, List<RecordingEntry> recordings) {
        super("Recording Viewer - " + project.getProjectRootPath().getFileName());
        this.project = project;
        this.recordings = recordings;
        this.overrideWriter = new ViewerOverrideWriter(
                new File(project.getProjectRootPath().toFile(), "Viewer Override.csv"));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        int numberOfSquaresInRecording = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", -1);
        int[] validLayouts = {25, 100, 225, 400, 900};
        boolean isValid = false;

        for (int valid : validLayouts) {
            if (numberOfSquaresInRecording == valid) {
                isValid = true;
                break;
            }
        }
        if (!isValid) {
            PaintLogger.errorf("Invalid square layout (d x d)");
            return;
        }
        int numberOfSquareInOneDimension = (int) Math.sqrt(numberOfSquaresInRecording);

        leftGridPanel = new SquareGridPanel(numberOfSquareInOneDimension, numberOfSquareInOneDimension);
        controlHandler.attach(leftGridPanel);

        attributesPanel = new RecordingAttributesPanel();
        controlsPanel = new RecordingControlsPanel(this);
        navigationPanel = new NavigationPanel(this);

        JPanel imagesInner = new JPanel(new GridLayout(1, 2, 15, 0));
        imagesInner.add(createSquareImagePanel(leftGridPanel));
        imagesInner.add(createSquareImagePanel(rightImageLabel));

        JPanel labelsPanel = new JPanel(new GridLayout(2, 1));
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        experimentLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        recordingLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelsPanel.add(experimentLabel);
        labelsPanel.add(recordingLabel);

        JPanel imagesWithNav = new JPanel(new BorderLayout(15, 15));
        imagesWithNav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)));
        imagesWithNav.add(navigationPanel.getComponent(), BorderLayout.NORTH);
        imagesWithNav.add(imagesInner, BorderLayout.CENTER);
        imagesWithNav.add(labelsPanel, BorderLayout.SOUTH);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(attributesPanel.getComponent(), BorderLayout.WEST);
        mainPanel.add(imagesWithNav, BorderLayout.CENTER);
        mainPanel.add(controlsPanel.getComponent(), BorderLayout.EAST);
        add(mainPanel, BorderLayout.CENTER);

        // --- Close button ---
        JButton closeButton = new JButton("Close Viewer");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to close the Recording Viewer?",
                    "Confirm Close",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                dispose(); // close this window
            }
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(1500, 700);
        setLocationRelativeTo(null);

        if (!recordings.isEmpty()) showEntry(0);
    }

    private JPanel createSquareImagePanel(JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout()) {
            public Dimension getPreferredSize() {
                return new Dimension(NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT);
            }

            public void setBounds(int x, int y, int w, int h) {
                int size = Math.min(w, h);
                super.setBounds(x, y, size, size);
            }
        };
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }

    private static ImageIcon scaleToFit(ImageIcon icon, int w, int h) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private void updateNavButtons() {
        navigationPanel.setEnabledState(currentIndex > 0, currentIndex < recordings.size() - 1);
    }

    private void showEntry(int index) {

        if (index < 0 || index >= recordings.size()) {
            return;
        }
        currentIndex = index;
        RecordingEntry entry = recordings.get(index);

        int numberOfSquaresInRecording = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", -1);

        leftGridPanel.setRecording(entry.getRecording());
        leftGridPanel.setBackgroundImage(entry.getLeftImage());
        leftGridPanel.setSquares(entry.getSquares(project, numberOfSquaresInRecording));

        rightImageLabel.setIcon(scaleToFit(entry.getRightImage(), NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT));

        experimentLabel.setText("Experiment: " + entry.getExperimentName() +
                                        "   [Overall: " + (currentIndex + 1) + "/" + recordings.size() + "]");
        recordingLabel.setText("Recording: " + entry.getRecordingName());

        attributesPanel.updateFromEntry(entry, numberOfSquaresInRecording);
        updateNavButtons();
        leftGridPanel.repaint();
    }

    @Override
    public void onFirst() {
        showEntry(0);
    }

    @Override
    public void onPrev() {
        showEntry(Math.max(0, currentIndex - 1));
    }

    @Override
    public void onNext() {
        showEntry(Math.min(recordings.size() - 1, currentIndex + 1));
    }

    @Override
    public void onLast() {
        showEntry(recordings.size() - 1);
    }

    @Override
    public void onFilterRequested() {
        FilterDialog dialog = new FilterDialog(this, recordings);
        dialog.setVisible(true);
        if (!dialog.isCancelled()) {
            List<RecordingEntry> filtered = dialog.getFilteredRecordings();
            if (!filtered.isEmpty()) {
                currentIndex = 0;
                showEntry(0);
            }
        }
    }

    @Override
    public void onSelectSquaresRequested() {
        RecordingEntry current = recordings.get(currentIndex);

        SquareControlDialog dialog = new SquareControlDialog(
                this,          // parent JFrame
                leftGridPanel,        // grid to control
                this,                 // listener (RecordingViewerFrame implements RecordingControlsPanel.Listener)
                new SquareControlParams(
                        current.getMinRequiredDensityRatio(),
                        current.getMaxAllowableVariability(),
                        current.getMinRequiredRSquared(),
                        "Free"
                )
        );

        dialog.setVisible(true);
    }

    @Override
    public void onAssignCellsRequested() {
        leftGridPanel.setSelectionEnabled(true);
        final JFrame owner = this;
        CellAssignmentDialog dialog = new CellAssignmentDialog(owner, new CellAssignmentDialog.Listener() {
            public void onAssign(int cellId) {
                assignmentManager.assignSelectedSquares(cellId, leftGridPanel);
            }

            public void onUndo() {
                assignmentManager.undo(leftGridPanel);
            }

            public void onCancelSelection() {
                leftGridPanel.clearSelection();
                leftGridPanel.repaint();
            }
        });
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                leftGridPanel.setSelectionEnabled(false);
            }
        });
        dialog.setVisible(true);
    }

    @Override
    public void onBordersToggled(boolean showBorders) {
        leftGridPanel.setShowBorders(showBorders);
        leftGridPanel.repaint();
    }

    @Override
    public void onShadingToggled(boolean showShading) {
        leftGridPanel.setShowShading(showShading);
        leftGridPanel.repaint();
    }

    @Override
    public void onNumberModeChanged(SquareGridPanel.NumberMode mode) {
        leftGridPanel.setNumberMode(mode);
        leftGridPanel.repaint();
    }

    @Override
    public void onApplySquareControl(String scope, SquareControlParams params) {
        controlHandler.apply(params, leftGridPanel);
        overrideWriter.applyAndWrite(scope, params, recordings, currentIndex, project);
        leftGridPanel.repaint();
    }

    @Override
    public void onPlayRecordingRequested() {
        if (recordings.isEmpty() || currentIndex < 0 || currentIndex >= recordings.size()) {
            PaintLogger.warningf("No recording selected to play.");
            JOptionPane.showMessageDialog(this,
                                          "No recording selected to play.",
                                          "No Selection",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        RecordingEntry entry = recordings.get(currentIndex);
        final String experimentName = entry.getExperimentName();
        final String recordingName = entry.getRecordingName();

        final String[] imagesRootPathRef = {
                getString("Paths", "Images Root", "/Volumes/Extreme Pro/Omero")
        };
        final Path[] imagePathRef = {
                Paths.get(imagesRootPathRef[0], experimentName, recordingName + ".nd2")
        };

        if (!Files.exists(imagePathRef[0])) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Recording file not found:\n" + imagePathRef[0] +
                            "\n\nSelect a new image root directory and try again?",
                    "File Missing",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                RootSelectionDialog dlg =
                        new RootSelectionDialog(this, RootSelectionDialog.Mode.IMAGES);
                Path newRoot = dlg.showDialog();

                if (dlg.isCancelled() || newRoot == null) {
                    JOptionPane.showMessageDialog(this,
                                                  "No directory selected. Playback cancelled.",
                                                  "Cancelled",
                                                  JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                imagesRootPathRef[0] = newRoot.toString();
                imagePathRef[0] = newRoot.resolve(experimentName).resolve(recordingName + ".nd2");

                if (!Files.exists(imagePathRef[0])) {
                    JOptionPane.showMessageDialog(this,
                                                  "Still could not find the recording file:\n" + imagePathRef[0],
                                                  "File Missing",
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }
                PaintConfig.setString("Paths", "Images Root", imagesRootPathRef[0]);
                PaintConfig.instance().save();
            } else {
                return;
            }
        }

        Thread movieThread = new Thread(() -> {
            try {
                TiffMoviePlayer player = new TiffMoviePlayer();
                player.playMovie(imagePathRef[0].toString());
            } catch (Exception ex) {
                PaintLogger.errorf("Error playing recording: %s", ex.getMessage());
                ex.printStackTrace();
            }
        }, "MoviePlayerThread");

        movieThread.setDaemon(true);
        movieThread.start();

        PaintLogger.infof("Playing recording: %s / %s", experimentName, recordingName);
    }
}