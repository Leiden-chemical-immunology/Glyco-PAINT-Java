/*==============================================================================
 *  Class:        RecordingViewerFrame.java
 *  Package:      paint.viewer
 *
 *  PURPOSE:
 *    Provides the primary graphical interface for exploring and analyzing
 *    recordings within the PAINT Viewer application.
 *
 *  DESCRIPTION:
 *    This frame manages synchronized visualization of TrackMate and Brightfield
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
 *    • Displays paired TrackMate and Brightfield recordings with overlayed grids.
 *    • Integrates attribute and control panels for dynamic threshold adjustments.
 *    • Enables square filtering by density ratio, variability, and R².
 *    • Supports cell assignment and undo management through dedicated dialogs.
 *    • Provides full recording navigation across multiple experiments.
 *    • Supports preview recalculation of Tau, R², and density on slider movement.
 *    • Integrates TIFF/ND2 playback via {@link paint.viewer.io.TiffMoviePlayer}.
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
==============================================================================*/

package paint.viewer.ui.frames;

import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
import paint.viewer.app.Viewer;
import paint.viewer.io.FileHelper;
import paint.viewer.io.PanelExporter;
import paint.viewer.ui.RecordingDisplayUpdater;
import paint.viewer.ui.RecordingNavigator;
import paint.viewer.ui.RecordingPlaybackController;
import paint.viewer.ui.layout.ViewerLayoutBuilder;
import paint.viewer.ui.dialogs.CellAssignmentDialog;
import paint.viewer.ui.dialogs.RecordingFilterDialog;
import paint.viewer.ui.dialogs.SquareControlDialog;
import paint.viewer.control.CellAssignmentManager;
import paint.viewer.override.RecordingOverrideWriter;
import paint.viewer.control.SquareControlHandler;
import paint.viewer.override.SquareOverrideWriter;
import paint.viewer.ui.panels.NavigationPanel;
import paint.viewer.ui.panels.RecordingAttributesPanel;
import paint.viewer.ui.panels.RecordingControlsPanel;
import paint.viewer.ui.panels.SquareGridPanel;
import paint.viewer.model.SquareControlParams;
import paint.viewer.model.RecordingEntry;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static paint.shared.constants.PaintConstants.*;
import static paint.viewer.override.RecordingOverrideApplier.applyRecordingOverrides;
import static paint.viewer.override.SquareOverrideApplier.applySquareOverrides;

/**
 * The {@code RecordingViewerFrame} class defines the main window of the PAINT Viewer.
 * It combines left and right image panels, navigation controls, and metadata panels into
 * a cohesive interface for browsing, filtering, and analyzing experiment recordings.
 * <p>
 * Functionally, the class manages synchronization between user interactions and the
 * underlying model objects — specifically {@link RecordingEntry}
 * and {@link paint.shared.objects.Recording}. It enables users to:
 * <ul>
 *   <li>Navigate through multiple recordings within an experiment.</li>
 *   <li>Filter visible squares using configurable thresholds.</li>
 *   <li>Assign cell IDs interactively via selection dialogs.</li>
 *   <li>Play TIFF/ND2 recordings directly from disk.</li>
 *   <li>Preview live recalculations of Tau, R², and density metrics.</li>
 * </ul>
 * <p>
 * This class is instantiated by {@link Viewer} after successful
 * project initialization. All UI updates occur on the Swing event dispatch thread.
 */
public class ViewerFrame extends JFrame
        implements RecordingControlsPanel.Listener, NavigationPanel.Listener {

    // Remembers the last used filter criteria for the filter dialog
    private RecordingFilterDialog.FilterCriteria lastFilterCriteria = null;

    private final Project                      project;
    private final List<RecordingEntry>         allRecordingEntries;  // full unfiltered list
    private final List<RecordingEntry>         recordingEntries;     // currently visible (may be filtered)
    private       int                          currentIndex      = 0;

    private       SquareGridPanel              leftGridPanel;
    private       JLabel                       rightImageLabel;
    private       JLabel                       experimentLabel;
    private       JLabel                       recordingLabel;

    private       RecordingAttributesPanel     attributesPanel;
    private       NavigationPanel              navigationPanel;
    private       RecordingControlsPanel       controlsPanel;

    private final CellAssignmentManager        assignmentManager = new CellAssignmentManager();
    private final SquareControlHandler         controlHandler    = new SquareControlHandler();
    private       JDialog                      activeDialog      = null;

    private final RecordingOverrideWriter      recordingOverrideWriter;
    private final SquareOverrideWriter         squareOverrideWriter;

    private final RecordingPlaybackController  playbackController = new RecordingPlaybackController(this);
    private       RecordingDisplayUpdater      displayUpdater;
    private final RecordingNavigator           navigator;

    // New: checkbox in bottom-left for "Import Overrides"
    private       JCheckBox                    importOverridesCheckBox;

    /**
     * Constructs a {@code RecordingViewerFrame} that initializes and displays the complete
     * recording viewer environment. The frame sets up grid visualization, navigation,
     * control, and attribute panels while establishing event connections for user actions.
     *
     * @param project          the {@link Project} object providing experiment context and paths.
     * @param recordingEntries list of {@link RecordingEntry} objects representing loaded recordings.
     */
    public ViewerFrame(Project project, List<RecordingEntry> recordingEntries) {
        super("Recording Viewer - " + project.getProjectRootPath().getFileName());
        this.project                 = project;
        this.allRecordingEntries     = new java.util.ArrayList<>(recordingEntries);
        this.recordingEntries        = new java.util.ArrayList<>(recordingEntries);
        this.recordingOverrideWriter = new RecordingOverrideWriter(project.getProjectRootPath());
        this.squareOverrideWriter    = new SquareOverrideWriter(project.getProjectRootPath());

        this.navigator               = new RecordingNavigator(newIndex -> showRecordingEntry(newIndex));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        // Validate grid configuration
        int     numberOfSquaresInRecording = PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1);
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

        // --- Build UI layout using the layout builder ---
        ViewerLayoutBuilder builder = new ViewerLayoutBuilder();

        ViewerLayoutBuilder.LayoutComponents ui =
                builder.build(
                        numberOfSquareInOneDimension,
                        this,   // navigation listener
                        this    // controls listener
                );

        // Apply content pane
        setContentPane(ui.rootPanel);

        // Store references
        this.leftGridPanel           = ui.leftGridPanel;
        this.rightImageLabel         = ui.rightImageLabel;
        this.experimentLabel         = ui.experimentLabel;
        this.recordingLabel          = ui.recordingLabel;
        this.attributesPanel         = ui.attributesPanel;
        this.navigationPanel         = ui.navigationPanel;
        this.controlsPanel           = ui.controlsPanel;
        this.importOverridesCheckBox = ui.importOverridesCheckBox;

        this.displayUpdater = new RecordingDisplayUpdater(
                this.leftGridPanel,
                this.rightImageLabel,
                this.experimentLabel,
                this.recordingLabel,
                this.attributesPanel
        );

        // Wire behaviour for "Import Overrides" checkbox
        initImportOverridesBehaviour();

        setSize(1500, 700);
        setLocationRelativeTo(null);

        // Load first entry if available
        if (!recordingEntries.isEmpty()) {
            showRecordingEntry(0);
        }
    }

    /**
     * Initializes startup and runtime behaviour for the "Import Overrides" checkbox:
     * - If checked at startup → perform override import once.
     * - If user checks it later → perform override import.
     * - If user unchecks it   → show a message asking to restart the viewer.
     */
    private void initImportOverridesBehaviour() {
        if (importOverridesCheckBox == null) {
            return;
        }

        // Attach listener for runtime toggles
        importOverridesCheckBox.addActionListener(e -> {
            if (importOverridesCheckBox.isSelected()) {
                performImportOverrides();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Import Overrides has been turned off.\n" +
                                "Please restart the Viewer for this change to take full effect.",
                        "Restart Recommended",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        // Startup behaviour: if already selected, perform import once
        if (importOverridesCheckBox.isSelected()) {
            performImportOverrides();
        }
    }

    /**
     * Performs the actual "Import Overrides" action.
     * <p>
     * TODO: Wire this to your existing override-import logic
     * (e.g. reading override CSVs and applying them to the current project/recordings).
     */
    private void performImportOverrides() {
        PaintLogger.infof("Import Overrides requested (checkbox is checked).");

        // 1) Apply overrides to all RecordingEntry objects
        applyRecordingOverrides(allRecordingEntries, project.getProjectRootPath());
        applySquareOverrides(allRecordingEntries, project.getProjectRootPath());

        // 2) Re-apply filter for the currently visible recording
        if (recordingEntries.isEmpty()
                || currentIndex < 0
                || currentIndex >= recordingEntries.size()) {
            leftGridPanel.repaint();
            return;
        }

        RecordingEntry current = recordingEntries.get(currentIndex);

        // Build params from the now-updated recording
        SquareControlParams params = new SquareControlParams(
                current.getRecording().getMinRequiredDensityRatio(),
                current.getRecording().getMaxAllowableVariability(),
                current.getRecording().getMinRequiredRSquared(),
                current.getRecording().getNeighbourMode()
        );

        // 3) Re-apply control handler + filter to the grid
        controlHandler.apply(params, leftGridPanel);
        leftGridPanel.applyVisibilityFilter();

        // 4) Update the attributes panel preview to reflect the new thresholds
        int numSquares = PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1);
        attributesPanel.updatePreview(
                current,
                numSquares,
                params.minRequiredDensityRatio,
                params.maxAllowableVariability,
                params.minRequiredRSquared,
                params.neighbourMode
        );

        // 5) Redraw the left grid
        leftGridPanel.repaint();
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
        RecordingEntry entry = recordingEntries.get(index);
        displayUpdater.show(entry, index, recordingEntries.size());
        updateNavButtons();
    }

    // =========================================================================================
    // NAVIGATION LISTENER IMPLEMENTATION
    // =========================================================================================

    /**
     * Navigates to the first available recording entry in the list.
     */
    @Override
    public void onFirst() {
        navigator.first(recordingEntries);
    }

    @Override
    public void onPrev() {
        navigator.prev(recordingEntries, currentIndex);
    }

    @Override
    public void onNext() {
        navigator.next(recordingEntries, currentIndex);
    }

    @Override
    public void onLast() {
        navigator.last(recordingEntries);
    }

    @Override
    public void onExportLeftImageRequested() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("grid-export.png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path exportPath = chooser.getSelectedFile().toPath();

            try {
                PanelExporter.export(leftGridPanel, exportPath, 2.0);

                JOptionPane.showMessageDialog(
                        this,
                        "Exported successfully to:\n" + exportPath.toAbsolutePath(),
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE
                );

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error exporting image: " + ex.getMessage(),
                        "Export Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    @Override
    public void onShowSquaresRequested() {
        openSquaresForCurrentRecording();
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
        if (activeDialog != null && activeDialog.isShowing()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        setActionButtonsEnabled(false);
        leftGridPanel.hideSquareInfoIfVisible();
        activeDialog = new RecordingFilterDialog(this, recordingEntries, allRecordingEntries, lastFilterCriteria);
        setGridEnabled(false); // Disable grid interaction

        activeDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                setGridEnabled(true); // Re-enable grid
                setActionButtonsEnabled(true);
                activeDialog = null;
            }
        });

        activeDialog.setVisible(true);

        // Now handle user result after closing
        RecordingFilterDialog fd = (RecordingFilterDialog) activeDialog;
        if (!fd.isCancelled()) {
            List<RecordingEntry> filtered = fd.getFilteredRecordings();
            lastFilterCriteria = fd.getCurrentFilterCriteria();
            boolean isResetAll = (filtered.size() == allRecordingEntries.size());

            if (isResetAll) {
                recordingEntries.clear();
                recordingEntries.addAll(allRecordingEntries);
                currentIndex = 0;
                showRecordingEntry(currentIndex);
                PaintLogger.infof("Filter reset — showing all recordings (%d total).", recordingEntries.size());
                return;
            }

            if (!filtered.isEmpty()) {
                recordingEntries.clear();
                recordingEntries.addAll(filtered);
                currentIndex = 0;
                showRecordingEntry(currentIndex);
                PaintLogger.infof("Filter applied — showing %d of %d recordings.", filtered.size(), allRecordingEntries.size());
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "No recordings match the selected filter criteria.",
                        "No Results",
                        JOptionPane.INFORMATION_MESSAGE
                );
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
        if (activeDialog != null && activeDialog.isShowing()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        setActionButtonsEnabled(false);
        leftGridPanel.hideSquareInfoIfVisible();
        RecordingEntry current = recordingEntries.get(currentIndex);

        activeDialog = new SquareControlDialog(
                this,
                leftGridPanel,
                this,
                new SquareControlParams(
                        current.getRecording().getMinRequiredDensityRatio(),
                        current.getRecording().getMaxAllowableVariability(),
                        current.getRecording().getMinRequiredRSquared(),
                        "Free"
                )
        );
        setGridEnabled(false); // Disable grid

        activeDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                setGridEnabled(true); // Re-enable grid
                setActionButtonsEnabled(true);
                activeDialog = null;
            }
        });
        activeDialog.setVisible(true);
    }

    /**
     * Opens a dialog for assigning the currently selected squares to a specific cell ID.
     * Includes undo and cancel functions and disables selection when the dialog closes.
     */
    @Override
    public void onAssignCellsRequested() {
        if (activeDialog != null && activeDialog.isShowing()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        setActionButtonsEnabled(false);
        leftGridPanel.hideSquareInfoIfVisible();
        leftGridPanel.setSelectionEnabled(true);
        leftGridPanel.setInfoPopupsEnabled(false);

        final JFrame owner = this;
        activeDialog = new CellAssignmentDialog(owner, new CellAssignmentDialog.Listener() {
            public void onAssign(int cellId) {
                Map<Integer, Integer> userSelectedSquares =
                        assignmentManager.assignUserSelectedSquares(cellId, leftGridPanel);
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

        activeDialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                leftGridPanel.setSelectionEnabled(false);
                leftGridPanel.setInfoPopupsEnabled(true);
                setActionButtonsEnabled(true);
                activeDialog = null;
            }
        });

        activeDialog.setVisible(true);
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
            int numSquares = PaintConfig.getInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, -1);

            //  --- Reflect results in attribute preview panel ---
            attributesPanel.updatePreview(
                    currentRecordingEntry,
                    numSquares,
                    params.minRequiredDensityRatio,
                    params.maxAllowableVariability,
                    params.minRequiredRSquared,
                    params.neighbourMode
            );

            leftGridPanel.repaint();
            return;
        }

        // Full application: persist thresholds and repaint
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
        if (activeDialog != null && activeDialog.isShowing()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (playbackController.isPlaying()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (recordingEntries.isEmpty() || currentIndex < 0 || currentIndex >= recordingEntries.size()) {
            JOptionPane.showMessageDialog(this,
                                          "No recording selected to play.",
                                          "No Selection",
                                          JOptionPane.WARNING_MESSAGE);
            return;
        }
        RecordingEntry entry = recordingEntries.get(currentIndex);
        playbackController.playRecording(entry);
    }

    private void setGridEnabled(boolean enabled) {
        leftGridPanel.setInteractionEnabled(enabled);
    }

    private void setActionButtonsEnabled(boolean enabled) {
        if (controlsPanel != null) {
            controlsPanel.setButtonsEnabled(enabled);
        }
    }

    private void openSquaresForCurrentRecording() {
        try {
            String experimentName = allRecordingEntries.get(currentIndex).getExperimentName();
            String recordingName  = allRecordingEntries.get(currentIndex).getRecordingName();
            FileHelper.filterAndOpenSquaresCsv(project.getProjectRootPath(), experimentName, recordingName);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                                          "Error opening squares CSV:\n" + ex.getMessage(),
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    // Accessors used by the playback controller
    public SquareGridPanel getLeftGridPanel() {
        return leftGridPanel;
    }

    public Project getProject() {
        return project;
    }

    public void disableUI() {
        setActionButtonsEnabled(false);        // right control panel
        setGridEnabled(false);                 // left grid
        navigationPanel.setEnabledState(false, false); // nav panel
        attributesPanel.getComponent().setEnabled(false);    // optional: prevent editing attributes
    }

    public void enableUI() {
        setActionButtonsEnabled(true);
        setGridEnabled(true);
        updateNavButtons();
        attributesPanel.getComponent().setEnabled(true);
    }

    public void onNavigateTo(int newIndex) {
        showRecordingEntry(newIndex);
    }
}