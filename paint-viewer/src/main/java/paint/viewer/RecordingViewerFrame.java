/******************************************************************************
 *  Class:        RecordingViewerFrame.java
 *  Package:      paint.viewer
 *
 *  PURPOSE:
 *    Provides the primary graphical interface for exploring and analyzing
 *    recordings within the PAINT Viewer application.
 *
 *  DESCRIPTION:
 *    This frame manages synchronized visualization of TrackMate and BrightField
 *    images alongside grid-based square data derived from quantitative analyses.
 *    It coordinates recording navigation, filtering, and interactive manipulation
 *    of square attributes, enabling users to refine visibility and assignment logic.
 *
 *    The viewer integrates multiple UI components — including attribute panels,
 *    navigation controls, and square grid visualizations — into a unified workspace.
 *    It supports square selection, filtering by statistical thresholds, live Tau
 *    and density calculations, and playback of ND2/TIFF recordings.
 *
 *  KEY FEATURES:
 *    • Displays paired TrackMate and BrightField recordings with overlayed grids.
 *    • Integrates attribute and control panels for dynamic threshold adjustments.
 *    • Enables square filtering by density ratio, variability, and R².
 *    • Supports cell assignment and undo management through dedicated dialogs.
 *    • Provides full recording navigation across multiple experiments.
 *    • Supports preview recalculation of Tau, R², and density on slider movement.
 *    • Integrates TIFF/ND2 playback via {@link paint.viewer.utils.TiffMoviePlayer}.
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

package paint.viewer;

import paint.shared.config.PaintConfig;
import paint.shared.objects.Project;
import paint.shared.objects.Track;
import paint.shared.utils.CalculateTau;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;
import paint.viewer.dialogs.CellAssignmentDialog;
import paint.viewer.dialogs.FilterDialog;
import paint.viewer.dialogs.SquareControlDialog;
import paint.viewer.logic.CellAssignmentManager;
import paint.viewer.logic.RecordingOverrideWriter;
import paint.viewer.logic.SquareControlHandler;
import paint.viewer.logic.SquareOverrideWriter;
import paint.viewer.panels.NavigationPanel;
import paint.viewer.panels.RecordingAttributesPanel;
import paint.viewer.panels.RecordingControlsPanel;
import paint.viewer.panels.SquareGridPanel;
import paint.viewer.shared.SquareControlParams;
import paint.viewer.utils.RecordingEntry;
import paint.viewer.utils.TiffMoviePlayer;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.lang.Float.NaN;
import static paint.shared.constants.PaintConstants.*;
import static paint.shared.objects.Square.calculateSquareArea;
import static paint.shared.utils.CalculateTau.CalculateTauResult.Status.TAU_SUCCESS;
import static paint.shared.utils.CalculateTau.calculateTau;
import static paint.shared.utils.SharedSquareUtils.*;

/**
 * The {@code RecordingViewerFrame} class defines the main window of the PAINT Viewer.
 * It combines left and right image panels, navigation controls, and metadata panels into
 * a cohesive interface for browsing, filtering, and analyzing experiment recordings.
 * <p>
 * Functionally, the class manages synchronization between user interactions and the
 * underlying model objects — specifically {@link paint.viewer.utils.RecordingEntry}
 * and {@link paint.shared.objects.Recording}. It enables users to:
 * <ul>
 *   <li>Navigate through multiple recordings within an experiment.</li>
 *   <li>Filter visible squares using configurable thresholds.</li>
 *   <li>Assign cell IDs interactively via selection dialogs.</li>
 *   <li>Play TIFF/ND2 recordings directly from disk.</li>
 *   <li>Preview live recalculations of Tau, R², and density metrics.</li>
 * </ul>
 * <p>
 * This class is instantiated by {@link paint.viewer.RecordingViewer} after successful
 * project initialization. All UI updates occur on the Swing event dispatch thread.
 */
public class RecordingViewerFrame extends JFrame
        implements RecordingControlsPanel.Listener, NavigationPanel.Listener {
    private final Project                  project;
    private final List<RecordingEntry>     recordingEntries;   // The main data structure containing all recordings
    private       int                      currentIndex      = 0;

    private       SquareGridPanel          leftGridPanel;
    private final JLabel                   rightImageLabel   = new JLabel("", SwingConstants.CENTER);
    private final JLabel                   experimentLabel   = new JLabel("", SwingConstants.CENTER);
    private final JLabel                   recordingLabel    = new JLabel("", SwingConstants.CENTER);

    private       RecordingAttributesPanel attributesPanel;
    private       NavigationPanel          navigationPanel;

    private final CellAssignmentManager    assignmentManager = new CellAssignmentManager();
    private final RecordingOverrideWriter  recordingOverrideWriter;
    private final SquareOverrideWriter     squareOverrideWriter;
    private final SquareControlHandler     controlHandler    = new SquareControlHandler();

    /**
     * Constructs a {@code RecordingViewerFrame} that initializes and displays the complete
     * recording viewer environment. The frame sets up grid visualization, navigation,
     * control, and attribute panels while establishing event connections for user actions.
     *
     * @param project          the {@link Project} object providing experiment context and paths.
     * @param recordingEntries list of {@link RecordingEntry} objects representing loaded recordings.
     */
    public RecordingViewerFrame(Project project, List<RecordingEntry> recordingEntries) {
        super("Recording Viewer - " + project.getProjectRootPath().getFileName());
        this.project                 = project;
        this.recordingEntries        = recordingEntries;  // All the information is maintained here
        this.recordingOverrideWriter = new RecordingOverrideWriter(project.getProjectRootPath());
        this.squareOverrideWriter    = new SquareOverrideWriter(project.getProjectRootPath());


        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        // Validate grid configuration
        int     numberOfSquaresInRecording = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", -1);
        int[]   validSquareLayouts         = {25, 100, 225, 400, 900};
        boolean isValidSquareLayout        = false;

        for (int valid : validSquareLayouts) {
            if (numberOfSquaresInRecording == valid) {
                isValidSquareLayout = true;
                break;
            }
        }
        if (!isValidSquareLayout) {
            PaintLogger.errorf("Invalid square layout (d x d)");
            return;
        }
        int numberOfSquareInOneDimension = (int) Math.sqrt(numberOfSquaresInRecording);

        // --- Initialize panels and handlers ---
        leftGridPanel = new SquareGridPanel(numberOfSquareInOneDimension, numberOfSquareInOneDimension);
        controlHandler.attach(leftGridPanel);

        attributesPanel = new RecordingAttributesPanel();
        navigationPanel = new NavigationPanel(this);
        RecordingControlsPanel controlsPanel = new RecordingControlsPanel(this);

        // --- Build the main layout ---
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

        // --- Close button setup ---
        JButton closeButton = new JButton("Close Viewer");
        closeButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        closeButton.addActionListener(e -> dispose());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(1500, 700);
        setLocationRelativeTo(null);

        // Load first entry if available
        if (!recordingEntries.isEmpty()) {
            showRecordingEntry(0);
        }
    }

    /**
     * Creates a square container panel that holds the specified component (typically an image).
     * Ensures consistent square proportions regardless of frame resizing.
     *
     * @param comp the child component to display inside the square panel.
     * @return a configured {@link JPanel} maintaining a square aspect ratio.
     */
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

    /**
     * Resizes an {@link ImageIcon} proportionally to the specified dimensions.
     *
     * @param icon image icon to scale.
     * @param w    target width.
     * @param h    target height.
     * @return a new scaled {@link ImageIcon}, or {@code null} if the source is invalid.
     */
    private static ImageIcon scaleToFit(ImageIcon icon, int w, int h) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Updates the navigation buttons based on the current index position.
     * Enables "previous" or "next" navigation only when appropriate.
     */
    private void updateNavButtons() {
        navigationPanel.setEnabledState(currentIndex > 0, currentIndex < recordingEntries.size() - 1);
    }

    /**
     * Displays a specific recording entry by index, updating both left and right
     * image panels and all related attribute components.
     *
     * @param index the target recording index to display.
     */
    private void showRecordingEntry(int index) {
        if (index < 0 || index >= recordingEntries.size()) {
            return;
        }
        currentIndex = index;

        RecordingEntry recordingEntry = recordingEntries.get(index);
        leftGridPanel.setRecording(recordingEntry.getRecording());
        leftGridPanel.setBackgroundImage(recordingEntry.getLeftImage());

        int numberOfSquaresInRecording = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", -1);
        leftGridPanel.setSquares(recordingEntry.getRecording().getSquaresOfRecording());

        rightImageLabel.setIcon(scaleToFit(recordingEntry.getRightImage(), NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT));
        experimentLabel.setText("Experiment: " + recordingEntry.getExperimentName() + "   [Overall: " + (currentIndex + 1) + "/" + recordingEntries.size() + "]");
        recordingLabel.setText("Recording: " + recordingEntry.getRecordingName());

        attributesPanel.updateFromEntry(recordingEntry, numberOfSquaresInRecording);
        updateNavButtons();
        leftGridPanel.repaint();
    }

    // =========================================================================================
    // NAVIGATION LISTENER IMPLEMENTATION
    // =========================================================================================

    /**
     * Navigates to the first available recording entry in the list.
     */
    @Override
    public void onFirst() {
        showRecordingEntry(0);
    }

    /**
     * Navigates to the previous recording entry, if available.
     */
    @Override
    public void onPrev() {
        showRecordingEntry(Math.max(0, currentIndex - 1));
    }

    /**
     * Navigates to the next recording entry, if available.
     */
    @Override
    public void onNext() {
        showRecordingEntry(Math.min(recordingEntries.size() - 1, currentIndex + 1));
    }

    /**
     * Navigates directly to the final recording entry in the project.
     */
    @Override
    public void onLast() {
        showRecordingEntry(recordingEntries.size() - 1);
    }

    // =========================================================================================
    // FILTER AND CONTROL REQUEST HANDLERS
    // =========================================================================================

    /**
     * Invoked when the user opens the filter dialog.
     * The dialog allows narrowing the visible recording list by user-defined criteria.
     * Once the user confirms, the filtered result is applied immediately to the viewer.
     */
    @Override
    public void onFilterRequested() {
        leftGridPanel.hideSquareInfoIfVisible();  // Close any popup first
        FilterDialog dialog = new FilterDialog(this, recordingEntries);
        dialog.setVisible(true);

        if (!dialog.isCancelled()) {
            List<RecordingEntry> filtered = dialog.getFilteredRecordings();
            if (!filtered.isEmpty()) {
                currentIndex = 0;
                showRecordingEntry(0);
            }
        }
    }

    /**
     * Opens the Square Control dialog for adjusting visibility thresholds on the grid.
     * The dialog enables configuration of minimum density ratio, maximum variability,
     * minimum R², and neighbour mode. Changes can be previewed live or fully applied.
     */
    @Override
    public void onSelectSquaresRequested() {
        leftGridPanel.hideSquareInfoIfVisible();
        RecordingEntry current = recordingEntries.get(currentIndex);

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
    }

    /**
     * Opens a dialog for assigning the currently selected squares to a specific cell ID.
     * Includes undo and cancel functions and disables selection when the dialog closes.
     */
    @Override
    public void onAssignCellsRequested() {
        leftGridPanel.hideSquareInfoIfVisible();
        leftGridPanel.setSelectionEnabled(true);

        final JFrame owner = this;
        CellAssignmentDialog dialog = new CellAssignmentDialog(owner, new CellAssignmentDialog.Listener() {

            public void onAssign(int cellId) {
                Map<Integer, Integer> userSelectedSquares = assignmentManager.assignUserSelectedSquares(cellId, leftGridPanel);   // This has filled CellAssignmentManager.squareAssignments

                // --- Persist cell assignments to Square Override.csv ---
                RecordingEntry currentRecording = recordingEntries.get(currentIndex);

                if (!userSelectedSquares.isEmpty()) {
                    squareOverrideWriter.writeSquareOverrides(currentRecording, userSelectedSquares);
                }
            }

            public void onUndo() {
                assignmentManager.undo(leftGridPanel);
            }

            public void onCancelSelection() {
                leftGridPanel.clearSelection();
                leftGridPanel.applyVisibilityFilter();
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

    // =========================================================================================
    // VISUAL SETTINGS TOGGLES
    // =========================================================================================

    /**
     * Toggles the visibility of borders around grid squares.
     *
     * @param showBorders true to show borders, false to hide them.
     */
    @Override
    public void onBordersToggled(boolean showBorders) {
        leftGridPanel.setShowBorders(showBorders);
        leftGridPanel.repaint();
    }

    /**
     * Toggles whether shading overlays are shown over grid squares.
     *
     * @param showShading true to show shading overlays, false to hide them.
     */
    @Override
    public void onShadingToggled(boolean showShading) {
        leftGridPanel.setShowShading(showShading);
        leftGridPanel.repaint();
    }

    /**
     * Updates how numbers are displayed on selected squares (none, labels, or square IDs).
     *
     * @param mode number display mode for the grid.
     */
    @Override
    public void onNumberModeChanged(SquareGridPanel.NumberMode mode) {
        leftGridPanel.setNumberMode(mode);
        leftGridPanel.repaint();
    }

    // =========================================================================================
    // APPLYING SQUARE CONTROL PARAMETERS
    // =========================================================================================

    /**
     * Applies square control parameters from the dialog to the current grid or project scope.
     * <p>
     * When invoked in "Preview" mode, recalculates Tau, R², and density values dynamically
     * without committing them to disk. For full application, thresholds are persisted via
     * {@link RecordingOverrideWriter}.
     *
     * @param scope  the operational scope ("Preview" or "Apply").
     * @param params parameter bundle defining the visibility thresholds and neighbour mode.
     */
    @Override
    public void onApplySquareControl(String scope, SquareControlParams params) {
        if ("Preview".equals(scope)) {
            controlHandler.apply(params, leftGridPanel);
            leftGridPanel.applyVisibilityFilter();

            RecordingEntry currentRecordingEntry = recordingEntries.get(currentIndex);
            int numSquares = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", -1);

            // --- Compute Tau and R² for preview ---
            List<Track> tracksFromSelectedSquares = getTracksFromSelectedSquares(currentRecordingEntry.getRecording().getSquaresOfRecording());
            CalculateTau.CalculateTauResult results = calculateTau(tracksFromSelectedSquares, params.minRequiredRSquared);
            if (results.getStatus() == TAU_SUCCESS) {
                currentRecordingEntry.getRecording().setTau(results.getTau());
                currentRecordingEntry.getRecording().setRSquared(results.getRSquared());
            } else {
                currentRecordingEntry.getRecording().setTau(NaN);
                currentRecordingEntry.getRecording().setRSquared(NaN);
                // No successful Tau calculation; retain existing state
            }

            // --- Compute density for current selection ---
            double density = calculateDensity(
                    tracksFromSelectedSquares.size(),
                    calculateSquareArea(getNumberOfSelectedSquares(currentRecordingEntry.getRecording())),
                    RECORDING_DURATION,
                    currentRecordingEntry.getRecording().getConcentration()
            );

            // --- Reflect results in attribute preview panel ---
            attributesPanel.updatePreview(
                    currentRecordingEntry,
                    numSquares,
                    results.getTau(),
                    density,
                    params.minRequiredDensityRatio,
                    params.maxAllowableVariability,
                    params.minRequiredRSquared,
                    params.neighbourMode
            );

            leftGridPanel.repaint();
            return;
        }

        // --- Full application: persist thresholds and repaint ---
        controlHandler.apply(params, leftGridPanel);
        recordingOverrideWriter.applyAndWrite(scope, params, recordingEntries, currentIndex);
        leftGridPanel.repaint();
    }

    // =========================================================================================
    // RECORDING PLAYBACK HANDLER
    // =========================================================================================

    /**
     * Initiates playback of the ND2 or TIFF file corresponding to the current recording.
     * Performs validation of user selection and file presence before launching playback.
     * Errors and missing files are reported via dialog boxes.
     */
    @Override
    public void onPlayRecordingRequested() {
        leftGridPanel.hideSquareInfoIfVisible();

        if (recordingEntries.isEmpty() || currentIndex < 0 || currentIndex >= recordingEntries.size()) {
            PaintLogger.warnf("No recording selected to play.");
            JOptionPane.showMessageDialog(this,
                                          "No recording selected to play.",
                                          "No Selection",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }

        RecordingEntry entry          = recordingEntries.get(currentIndex);
        String         experimentName = entry.getExperimentName();
        String         recordingName  = entry.getRecordingName();

        // Determine the path for image playback
        Path imagesRoot = project.getImagesRootPath();
        if (imagesRoot == null) {
            String stored = PaintPrefs.getString("Path", "Images Root", "");
            if (stored != null && !stored.isEmpty()) {
                imagesRoot = Paths.get(stored);
            } else {
                JOptionPane.showMessageDialog(this,
                                              "No Images Root is defined.\nPlease set it in the Project Specification dialog.",
                                              "Configuration Error",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Path imagePath = imagesRoot.resolve(experimentName).resolve(recordingName + ".nd2");
        if (!Files.exists(imagePath)) {
            JOptionPane.showMessageDialog(this,
                                          "Recording file not found:\n" + imagePath,
                                          "File Missing",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Launch movie playback on a background thread
        Thread movieThread = new Thread(() -> {
            try {
                TiffMoviePlayer player = new TiffMoviePlayer();
                player.playMovie(imagePath.toString());
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