/*==============================================================================
 *  Class:        ViewerFrame.java
 *  Package:      paint.viewer.ui.frames
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
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.ui.frames;

import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.objects.Project;
import paint.shared.utils.PaintLogger;
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
import paint.viewer.control.SquareControlHandler;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.NUMBER_OF_SQUARES_IN_RECORDING;
import static paint.viewer.override.ExportOverridesFromViewer.exportOverrides;

import static paint.viewer.override.recording_exclude.ImportRecordingExclude.importRecordingExcludes;
import static paint.viewer.override.recording_exclude.WriteRecordingExclude.updateExcludeRecordingsCsv;
import static paint.viewer.override.recording_override.ImportRecordingOverride.importRecordingOverrides;
import static paint.viewer.override.square_override.ImportSquareOverride.importSquareOverrides;
import paint.viewer.override.recording_override.WriteRecordingOverride;
import paint.viewer.override.square_override.WriteSquareOverride;

/**
 * The {@code ViewerFrame} class defines the main window of the PAINT Viewer.
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
 * This class is instantiated by {@link paint.viewer.app.Viewer} after successful
 * project initialization. All UI updates occur on the Swing event dispatch thread.
 */
public class ViewerFrame extends JFrame implements
        RecordingControlsPanel.RecordingsControlListener,
        SquareControlDialog.SquareControlListener,
        NavigationPanel.Listener,
        ViewerLayoutBuilder.CloseListener {

    // Remembers the last used filter criteria for the filter dialog
    private RecordingFilterDialog.FilterCriteria lastFilterCriteria = null;

    private final Project                      project;
    private final List<RecordingEntry>         allRecordingEntries;  // full unfiltered list
    private final List<RecordingEntry>         recordingEntries;     // currently visible (can be filtered)
    private       int                          currentIndex      = 0;

    private       SquareGridPanel              leftGridPanel;

    private       RecordingAttributesPanel     attributesPanel;
    private       NavigationPanel              navigationPanel;
    private       RecordingControlsPanel       controlsPanel;

    private final CellAssignmentManager        assignmentManager = new CellAssignmentManager();
    private final SquareControlHandler         controlHandler    = new SquareControlHandler();
    private       JDialog                      activeDialog      = null;

    private final WriteRecordingOverride       writeRecordingOverride;
    private final WriteSquareOverride          writeSquareOverride;

    private final RecordingPlaybackController  playbackController = new RecordingPlaybackController(this);
    private       RecordingDisplayUpdater      displayUpdater;
    private final RecordingNavigator           navigator;

    private       JCheckBox                    importOverridesCheckBox;

    private static final String OVERRIDE_TEXT_OFF = "Overrides";
    private static final String OVERRIDE_TEXT_ON  = "Export Overrides";

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
        this.allRecordingEntries     = new ArrayList<>(recordingEntries);                          // This is the unfiltered set of recordings
        this.recordingEntries        = new ArrayList<>(recordingEntries);                          // This is the filtered set of recordings
        this.writeRecordingOverride  = new WriteRecordingOverride(project.getProjectRootPath());
        this.writeSquareOverride     = new WriteSquareOverride(project.getProjectRootPath());
        this.navigator               = new RecordingNavigator(this::showRecordingEntry);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        // Validate grid configuration: can only be a limited set
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
                        this,
                        this,
                        this
                );
        // Apply content pane
        setContentPane(ui.rootPanel);

        // Store references
        this.leftGridPanel           = ui.leftGridPanel;
        JLabel rightImageLabel       = ui.rightImageLabel;
        JLabel experimentLabel       = ui.experimentLabel;
        JLabel recordingLabel        = ui.recordingLabel;
        this.attributesPanel         = ui.attributesPanel;
        this.navigationPanel         = ui.navigationPanel;
        this.controlsPanel           = ui.controlsPanel;
        this.importOverridesCheckBox = ui.importOverridesCheckBox;

        this.displayUpdater = new RecordingDisplayUpdater(
                this.leftGridPanel,
                rightImageLabel,
                experimentLabel,
                recordingLabel,
                this.attributesPanel
        );

        // Wire behaviour for "Overrides" checkbox
        initImportOverridesBehaviour();

        setSize(1500, 700);
        setLocationRelativeTo(null);

        // Load the first entry if available
        if (!recordingEntries.isEmpty()) {
            showRecordingEntry(0);
        }
    }

    /**
     * Initializes startup and runtime behaviour for the "Import Overrides" checkbox:
     * - If checked at startup → perform override import once.
     * - If user checks it later → perform override import.
     * - If user unchecks it → show a message asking to restart the viewer.
     */
    private void initImportOverridesBehaviour() {
        if (importOverridesCheckBox == null) {
            return;
        }

        // Set initial label based on initial state
        importOverridesCheckBox.setText(
                importOverridesCheckBox.isSelected() ? OVERRIDE_TEXT_ON : OVERRIDE_TEXT_OFF
        );

        importOverridesCheckBox.addItemListener(e -> {
            boolean selected = importOverridesCheckBox.isSelected();

            // Update title immediately
            importOverridesCheckBox.setText(selected ? OVERRIDE_TEXT_ON : OVERRIDE_TEXT_OFF);

            // Your existing behavior
            if (selected) {
                performImportOverrides();
            } else {
                // Optional: tell user they must restart to fully “unimport”
                // JOptionPane.showMessageDialog(this, "To remove imported overrides, restart the viewer.", ...);
            }
        });

        // Startup behavior: if already selected, import once
        if (importOverridesCheckBox.isSelected()) {
            performImportOverrides();
        }

        importOverridesCheckBox.setToolTipText(
                "When enabled, reads override CSV files and applies them to the current session."
        );
    }

    /**
     * Performs the actual "Import Overrides" action by reading override CSV files
     * and applying them to the current project's recording and square metadata.
     */
    private void performImportOverrides() {
        PaintLogger.debugf("Overrides requested (checkbox is checked).");

        // 1) Apply overrides to all RecordingEntry objects
        importRecordingOverrides (allRecordingEntries, project.getProjectRootPath());
        importSquareOverrides    (allRecordingEntries, project.getProjectRootPath());
        importRecordingExcludes  (allRecordingEntries, project.getProjectRootPath());

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

        // Ensure UI reflects new exclude state after import
        if (displayUpdater != null) {
            displayUpdater.applyExcludedUi(current);
        }
        if (controlsPanel != null) {
            controlsPanel.setExcludeButtonText(current.getRecording().isExcluded());
        }
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
        // Reset grid interaction deterministically
        leftGridPanel.setInteractionEnabled(true);
        leftGridPanel.setSelectionEnabled(false);
        leftGridPanel.setInfoPopupsEnabled(true);

        if (index < 0 || index >= recordingEntries.size()) {
            return;
        }

        assignmentManager.clear();

        // clear grid selection
        leftGridPanel.clearSelection();
        leftGridPanel.applyVisibilityFilter();

        currentIndex = index;
        RecordingEntry entry = recordingEntries.get(index);
        displayUpdater.show(entry, index, recordingEntries.size());
        updateNavButtons();

        // --- Ensure Exclude/Include UI always matches the current recording ---
        if (controlsPanel != null) {
            controlsPanel.setExcludeButtonText(entry.getRecording().isExcluded());
        }
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

    /**
     * Navigates to the previous recording entry.
     */
    @Override
    public void onPrev() {
        navigator.prev(recordingEntries, currentIndex);
    }

    /**
     * Navigates to the next recording entry.
     */
    @Override
    public void onNext() {
        navigator.next(recordingEntries, currentIndex);
    }

    /**
     * Navigates to the last recording entry in the list.
     */
    @Override
    public void onLast() {
        navigator.last(recordingEntries);
    }

    /**
     * Opens a file chooser to export the current left grid view as a high-resolution image.
     */
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

    /**
     * Opens the squares CSV file for the currently selected recording.
     */
    @Override
    public void onShowSquaresRequested() {
        openSquaresForCurrentRecording();
    }

    /**
     * Toggles the excluded state of the current recording and updates the
     * project-level exclude CSV and experiment-level recordings CSV.
     */
    @Override
    public void onExcludeToggleRequested() {
        if (recordingEntries.isEmpty()
                || currentIndex < 0
                || currentIndex >= recordingEntries.size()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        RecordingEntry entry = recordingEntries.get(currentIndex);

        // 1) Toggle model state
        boolean newExcluded = !entry.getRecording().isExcluded();   // adapt to your actual getter/setter
        entry.getRecording().setExcluded(newExcluded);

        // 2) Persist to "All Recordings" (your existing writer or a new small helper)

        // Update the RecordingEntry
        entry.getRecording().setExcluded(newExcluded);

        // Write a record in the Exclude file
        String recordingName  = entry.getRecording().getRecordingName();
        updateExcludeRecordingsCsv(project.getProjectRootPath(), recordingName, newExcluded);

        // 3) Update UI (button text + labels)
        controlsPanel.setExcludeButtonText(newExcluded);
        if (displayUpdater != null) {
            displayUpdater.applyExcludedUi(entry);
        }

    }

    // =========================================================================================
    // MODAL DIALOG LIFECYCLE
    // =========================================================================================

    /**
     * Opens a modal dialog exclusively, and guarantees the UI is restored when it closes.
     * <p>
     * Every modal dialog in this frame needs the same ceremony: refuse to open if one is
     * already up, disable the action buttons, put the grid into some temporary state, and then
     * undo all of that when the dialog closes. That was previously copy-pasted at each call
     * site, and it is exactly the kind of duplication you do not want: if one copy forgets to
     * re-enable the buttons, or forgets to clear {@link #activeDialog}, the viewer locks up
     * for good. Doing it once, here, makes that impossible to get wrong.
     * <p>
     * The created dialog is returned so a caller can read its result from its own reference.
     * Do not read {@link #activeDialog} after the dialog closes — the close listener clears it.
     *
     * @param factory  creates the dialog (called only once it is certain we can open one)
     * @param prepare  temporary UI state to apply while the dialog is up
     * @param restore  undoes {@code prepare}; always run when the dialog closes
     * @return the dialog that was shown, or {@code null} if one was already open
     */
    private <D extends JDialog> D showExclusiveDialog(java.util.function.Supplier<D> factory,
                                                      Runnable prepare,
                                                      Runnable restore) {
        if (activeDialog != null && activeDialog.isShowing()) {
            Toolkit.getDefaultToolkit().beep();
            return null;
        }

        setActionButtonsEnabled(false);
        leftGridPanel.hideSquareInfoIfVisible();
        prepare.run();

        D dialog = factory.get();
        activeDialog = dialog;

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                restore.run();
                setActionButtonsEnabled(true);
                activeDialog = null;
            }
        });

        dialog.setVisible(true);   // modal: blocks until closed
        return dialog;
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

        RecordingFilterDialog fd = showExclusiveDialog(
                () -> new RecordingFilterDialog(this, recordingEntries, allRecordingEntries, lastFilterCriteria),
                () -> setGridEnabled(false),
                () -> setGridEnabled(true));

        if (fd == null) {
            return;   // another dialog was already open
        }

        // Read the result from our own reference: activeDialog has been cleared by the close
        // listener by now, and reading it here used to be a latent NPE.
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

        RecordingEntry current = recordingEntries.get(currentIndex);

        showExclusiveDialog(
                () -> new SquareControlDialog(
                        this,
                        leftGridPanel,
                        this,
                        new SquareControlParams(
                                current.getRecording().getMinRequiredDensityRatio(),
                                current.getRecording().getMaxAllowableVariability(),
                                current.getRecording().getMinRequiredRSquared(),
                                current.getRecording().getNeighbourMode()
                        )
                ),
                () -> setGridEnabled(false),
                () -> setGridEnabled(true));
    }

    /**
     * Opens a dialog for assigning a cell ID to the user-selected squares.
     * If previous assignments exist for the current recording, the user is
     * asked whether to merge the new assignments with the old ones or to
     * replace all existing assignments entirely.
     * A CANCEL option aborts the operation without modifying any data.
     * Includes undo and cancel functions and temporarily disables regular grid
     * selection until the dialog is closed.
     */
    @Override
    public void onAssignCellsRequested() {

        final JFrame owner = this;

        showExclusiveDialog(
                () -> new CellAssignmentDialog(owner, new CellAssignmentDialog.Listener() {
                    public void onAssign(int cellId) {
                        handleCellAssignment(cellId);
                    }

                    public void onUndo() {
                        assignmentManager.undo(leftGridPanel);
                    }

                    public void onCancelSelection() {
                        leftGridPanel.clearSelection();
                        leftGridPanel.applyVisibilityFilter();
                        leftGridPanel.repaint();
                    }
                }),
                // While assigning, the grid becomes a selection surface rather than an
                // info surface.
                () -> {
                    leftGridPanel.setSelectionEnabled(true);
                    leftGridPanel.setInfoPopupsEnabled(false);
                },
                () -> {
                    leftGridPanel.setSelectionEnabled(false);
                    leftGridPanel.setInfoPopupsEnabled(true);
                });
    }

    private void handleCellAssignment(int cellId) {
        Map<Integer, Integer> userSelectedSquares = assignmentManager.assignUserSelectedSquares(cellId, leftGridPanel);

        if (userSelectedSquares.isEmpty()) {
            return;
        }

        RecordingEntry current                = recordingEntries.get(currentIndex);
        boolean        hasExistingOverrides   = writeSquareOverride.hasOverridesFor(current);
        boolean        existingOverridesShown = (importOverridesCheckBox != null && importOverridesCheckBox.isSelected());
        boolean        keepOld                = true;

        // If there are existing overrides and if thgey are not shown ask
        if (hasExistingOverrides && !existingOverridesShown && assignmentManager.isFirstAssignmentForRecording()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "There are existing cell assignments for this recording.\n\n" +
                            "Do you want to keep those and only update the selected squares?\n\n" +
                            "Choose 'Yes' to merge assignments.\n" +
                            "Choose 'No' to replace all existing assignments.",
                    "Existing Assignments Found",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }

            keepOld = (choice == JOptionPane.YES_OPTION);
        }
        assignmentManager.setFirstAssignmentForRecording(false);

        if (keepOld) {
            writeSquareOverride.mergeSquareOverrides(current, userSelectedSquares);
        } else {
            writeSquareOverride.replaceSquareOverrides(current, userSelectedSquares);
        }
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
     * {@link WriteRecordingOverride}.
     *
     * @param scope  the operational scope ("Preview" or "Apply").
     * @param params parameter bundle defining the visibility thresholds and neighbour mode.
     */

    @Override
    public void onApplySquareControl(SquareControlDialog.SquareControlListener.Scope scope,
            SquareControlParams params) {

        if (scope == SquareControlDialog.SquareControlListener.Scope.PREVIEW) {
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
        writeRecordingOverride.writeRecordingOverridesToFile(scope.label(), params, recordingEntries, currentIndex);
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
    /**
     * @return the {@link SquareGridPanel} displaying the current recording's squares.
     */
    public SquareGridPanel getLeftGridPanel() {
        return leftGridPanel;
    }

    /**
     * @return the {@link Project} associated with this viewer.
     */
    public Project getProject() {
        return project;
    }

    /**
     * Disables the user interface components.
     * Used during long-running background tasks.
     */
    public void disableUI() {
        setActionButtonsEnabled(false);        // right control panel
        setGridEnabled(false);                 // left grid
        navigationPanel.setEnabledState(false, false); // nav panel
        attributesPanel.getComponent().setEnabled(false);    // optional: prevent editing attributes
    }

    /**
     * Enables the user interface components.
     * Restores interactive state after background tasks.
     */
    public void enableUI() {
        setActionButtonsEnabled(true);
        setGridEnabled(true);
        updateNavButtons();
        attributesPanel.getComponent().setEnabled(true);
    }

    @Override
    public void onClose() {
        if (importOverridesCheckBox != null && importOverridesCheckBox.isSelected()) {

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "You are about to save override data into '-override' files.\n"
                            + "This may take a while.\n\n"
                            + "Do you want to save overrides now?",
                    "Process Overrides?",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.CANCEL_OPTION) {
                // User aborted closing
                return;
            }

            if (choice == JOptionPane.YES_OPTION) {
                try {
                    exportOverrides(project.getProjectRootPath(), "-override");
                } catch (Exception ex) {
                    PaintLogger.error("Failed to export overrides", ex);
                    JOptionPane.showMessageDialog(
                            this,
                            "Error while processing overrides:\n" + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            // NO_OPTION → skip override processing and continue closing
        }

        // Always close the window afterwards
        this.dispose();
    }


}