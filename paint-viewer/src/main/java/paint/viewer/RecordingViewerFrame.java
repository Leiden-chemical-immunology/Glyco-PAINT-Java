package paint.viewer;

import paint.shared.config.PaintConfig;
import paint.shared.objects.Project;
import paint.shared.utils.PaintPrefs;
import paint.shared.utils.PaintLogger;

import paint.viewer.dialogs.CellAssignmentDialog;
import paint.viewer.dialogs.FilterDialog;
import paint.viewer.dialogs.SquareControlDialog;
import paint.viewer.logic.CellAssignmentManager;
import paint.viewer.logic.SquareControlHandler;
import paint.viewer.logic.ViewerOverrideWriter;
import paint.viewer.panels.NavigationPanel;
import paint.viewer.panels.RecordingAttributesPanel;
import paint.viewer.panels.RecordingControlsPanel;
import paint.viewer.panels.SquareGridPanel;
import paint.viewer.shared.SquareControlParams;
import paint.viewer.utils.RecordingEntry;
import paint.viewer.utils.TiffMoviePlayer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_WIDTH;

/**
 * The RecordingViewerFrame class manages the GUI frame for visualizing and interacting with
 * recordings and their attributes. It provides functionalities to navigate recordings, display
 * recording-related images, and manage square-based grid interactions.
 *
 * The frame includes a left grid panel showing squares associated with recordings, labels
 * displaying experiment and recording details, and panels for recording attributes, controls,
 * and navigation. Additionally, the interface handles user actions such as navigation, filtering,
 * and square interaction.
 *
 * This class implements the listener interfaces for RecordingControlsPanel and NavigationPanel
 * to handle user-triggered events and updates accordingly.
 */
public class RecordingViewerFrame extends JFrame
        implements RecordingControlsPanel.Listener, NavigationPanel.Listener {

    //  @formatter:off

    private final Project                  project;
    private final List<RecordingEntry>     recordingEntries;   // This is the main datastructure for the viewer
    private       int                      currentIndex      = 0;

    private       SquareGridPanel          leftGridPanel;
    private final JLabel                   rightImageLabel   = new JLabel("", SwingConstants.CENTER);
    private final JLabel                   experimentLabel   = new JLabel("", SwingConstants.CENTER);
    private final JLabel                   recordingLabel    = new JLabel("", SwingConstants.CENTER);

    private       RecordingAttributesPanel attributesPanel;
    private       RecordingControlsPanel   controlsPanel;
    private       NavigationPanel          navigationPanel;

    private final CellAssignmentManager    assignmentManager = new CellAssignmentManager();
    private final ViewerOverrideWriter     overrideWriter;
    private final SquareControlHandler     controlHandler    = new SquareControlHandler();

    //  @formatter:on

    /**
     * Constructs a {@code RecordingViewerFrame} that initializes and displays the recording viewer UI.
     * The frame includes panels for grid visualization, controls, recording attributes,
     * navigation, and an option to close the viewer.
     *
     * @param project    the {@code Project} instance associated with this viewer, providing the
     *                   project context and directory structure needed for initialization.
     * @param recordingEntries a {@code List} of {@code RecordingEntry} objects representing the recordings
     *                   to be displayed and navigated within the viewer.
     */
    public RecordingViewerFrame(Project project, List<RecordingEntry> recordings) {
        super("Recording Viewer - " + project.getProjectRootPath().getFileName());
        this.project          = project;
        this.recordingEntries = recordingEntries;  // All the information is maintained here
        this.overrideWriter   = new ViewerOverrideWriter(new File(new File(project.getProjectRootPath().toFile(), "Out"), "Viewer Override.csv"));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

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

        // Create the panel in which the grid will be displayed
        leftGridPanel = new SquareGridPanel(numberOfSquareInOneDimension, numberOfSquareInOneDimension);
        controlHandler.attach(leftGridPanel);

        attributesPanel = new RecordingAttributesPanel();
        controlsPanel   = new RecordingControlsPanel(this);
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
            dispose(); // close this window
        });

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(1500, 700);
        setLocationRelativeTo(null);

        // Assuming there are recordingEntries to be shown, show, the first one
        if (!recordingEntries.isEmpty()) {
            showRecordingEntry(0);
        }
    }

    /**
     * Creates a square JPanel that contains the provided JComponent. The panel ensures
     * that its width and height remain equal by adjusting its dimensions, making it ideal
     * for displaying square content such as images.
     *
     * @param comp the JComponent to be embedded within the square JPanel. This component
     *             will be positioned at the center of the panel.
     * @return a JPanel with a square layout containing the given component.
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
     * Scales the given {@code ImageIcon} to fit within the specified width and height using smooth scaling.
     * Returns a new {@code ImageIcon} with the resized dimensions or {@code null} if the input icon is invalid.
     *
     * @param icon the {@code ImageIcon} to be scaled; must not be null and should have valid dimensions.
     * @param w the target width to scale the icon to.
     * @param h the target height to scale the icon to.
     * @return a new scaled {@code ImageIcon} or {@code null} if the input icon is invalid.
     */
    private static ImageIcon scaleToFit(ImageIcon icon, int w, int h) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) {
            return null;
        }
        Image scaled = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    /**
     * Updates the enabled state of navigation buttons based on the current index
     * relative to the total number of recordings. Enables or disables the
     * "previous" and "next" navigation buttons depending on whether there are
     * preceding or subsequent recordings to navigate to.
     *
     * The method utilizes the {@code setEnabledState} method of the
     * {@code navigationPanel} to configure the state of navigation controls:
     * - The "previous" buttons are enabled only if the current index is greater than 0.
     * - The "next" buttons are enabled only if the current index is less than the
     *   index of the last recording.
     */
    private void updateNavButtons() {
        navigationPanel.setEnabledState(currentIndex > 0, currentIndex < recordings.size() - 1);
    }

    /**
     * Displays the recording entry at the specified index, updates various UI components
     * with the entry's details, and manages navigation within the recording viewer.
     * If the given index is out of bounds, this method exits without making changes.
     *
     * @param index the index of the recording entry to be displayed; must be
     *              within the bounds of the recordings list.
     */
    private void showRecordingEntry(int index) {

        if (index < 0 || index >= recordingEntries.size()) {
            return;
        }

        // Store the index so that is available for other methods
        currentIndex = index;

        // Retrieve the current recordingEntry from recordingEntries
        RecordingEntry recordingEntry = recordingEntries.get(index);


        // Load the leftGridPanel with the information from recordingEntries indicated by the index

        int numberOfSquaresInRecording = PaintConfig.getInt("Generate Squares", "Number of Squares in Recording", -1);

        // Display the right image
        rightImageLabel.setIcon(scaleToFit(recordingEntry.getRightImage(), NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT));

        // Update the Experiment and Recording information (undetr the left and right image)
        experimentLabel.setText("Experiment: " + recordingEntry.getExperimentName() + "   [Overall: " + (currentIndex + 1) + "/" + recordingEntries.size() + "]");
        recordingLabel.setText("Recording: " + recordingEntry.getRecordingName());

        experimentLabel.setText("Experiment: " + entry.getExperimentName() +
                                        "   [Overall: " + (currentIndex + 1) + "/" + recordings.size() + "]");
        recordingLabel.setText("Recording: " + entry.getRecordingName());

        // Make sure that buttons are disabled of we are at the first or last image
        updateNavButtons();

        //  Display the leftGridpanel
        leftGridPanel.repaint();
    }

    @Override
    public void onFirst() {
        showRecordingEntry(0);
    }

    @Override
    public void onPrev() {
        showRecordingEntry(Math.max(0, currentIndex - 1));
    }

    @Override
    public void onNext() {
        showRecordingEntry(Math.min(recordingEntries.size() - 1, currentIndex + 1));
    }

    @Override
    public void onLast() {
        showRecordingEntry(recordingEntries.size() - 1);
    }

    /**
     * Handles the action of requesting a filter on the list of recordings.
     * Opens a {@code FilterDialog} to allow the user to filter the recordings
     * based on specific criteria. If the dialog is not cancelled and a filtered
     * list is returned, it updates the view to display the first entry of the
     * filtered results.
     *
     * This method interacts with the existing list of recordings and updates
     * the display accordingly. If the filtered list is validated and not empty,
     * the current view is reset to show the first recording entry in the new
     * filtered set.
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
     * Invoked when the user requests to select and control specific squares on the grid.
     *
     * This method retrieves the current recording entry based on the viewer's state
     * and initializes a {@code SquareControlDialog} to provide square control options.
     *
     * The dialog allows the user to configure square constraints using parameters
     * such as minimum required density ratio, maximum allowable variability, and minimum
     * required R-squared value. The dialog is created with the current grid, relevant parameters,
     * and a listener to handle subsequent user interactions.
     *
     * The dialog is displayed as a modal window, pausing further interaction with the
     * main application until the dialog is closed.
     */
    @Override
    public void onSelectSquaresRequested() {
        leftGridPanel.hideSquareInfoIfVisible();  // Close any popup first
        RecordingEntry current = recordingEntries.get(currentIndex);

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

    /**
     * Handles the action of assigning selected squares to a specific cell.
     *
     * This method enables selection interactions on the left grid panel and opens a
     * {@code CellAssignmentDialog} to allow the user to assign selected squares to a specified cell.
     * The dialog provides three key functions:
     * - Assign a cell ID to the selected squares.
     * - Undo the last assignment action.
     * - Cancel the current selection and clear the highlights.
     *
     * A listener is attached to the dialog to handle these user actions. Additionally, a
     * {@code WindowAdapter} is added to the dialog to disable selection interactions on the
     * left grid panel once the dialog is closed.
     *
     * The dialog is displayed as a modal window, suspending further interaction with the main
     * application until the dialog is closed.
     */
    @Override
    public void onAssignCellsRequested() {

        leftGridPanel.hideSquareInfoIfVisible();  // Close any popup first
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

    /**
     * Toggles the visibility of borders on the left grid panel and repaints the panel to reflect the change.
     *
     * @param showBorders a boolean indicating whether borders should be shown (true) or hidden (false).
     */
    @Override
    public void onBordersToggled(boolean showBorders) {
        leftGridPanel.setShowBorders(showBorders);
        leftGridPanel.repaint();
    }

    /**
     * Toggles the shading visibility on the left grid panel and repaints the panel to
     * reflect the updated shading state.
     *
     * @param showShading a boolean indicating whether shading should be enabled (true)
     *                    or disabled (false).
     */
    @Override
    public void onShadingToggled(boolean showShading) {
        leftGridPanel.setShowShading(showShading);
        leftGridPanel.repaint();
    }

    /**
     * Handles changes in the number mode for the left grid panel. This method updates
     * the number mode of the left grid panel and repaints it to reflect the updated state.
     *
     * @param mode the {@code SquareGridPanel.NumberMode} representing the new number mode
     *             to be applied to the left grid panel.
     */
    @Override
    public void onNumberModeChanged(SquareGridPanel.NumberMode mode) {
        leftGridPanel.setNumberMode(mode);
        leftGridPanel.repaint();
    }

    /**
     * Applies the specified square control parameters to the grid panel and updates
     * the relevant components. This method processes the parameters using the control handler,
     * writes the updated settings using the override writer, and repaints the left grid panel
     * to reflect the applied changes.
     *
     * @param scope  the scope of the square control application, which defines the context or
     *               boundaries for applying the control (e.g., current recording, project-wide).
     * @param params an instance of {@code SquareControlParams} containing the parameters
     *               for square control, such as size, constraints, or other configuration settings.
     */
    @Override
    public void onApplySquareControl(String scope, SquareControlParams params) {
        // --- Live preview (while moving sliders) ---
        if ("Preview".equals(scope)) {
            controlHandler.apply(params, leftGridPanel);
            leftGridPanel.applyVisibilityFilter();  // <── recompute which squares are visible
            leftGridPanel.repaint();
            return;
        }

        // --- Apply buttons: full apply + write overrides ---
        controlHandler.apply(params, leftGridPanel);
        overrideWriter.applyAndWrite(scope, params, recordings, currentIndex, project);
        leftGridPanel.repaint();
    }

    /**
     * Handles the request to play a selected recording.
     *
     * This method checks whether a valid recording is selected within a valid range.
     * If no selection is made, or the selection is invalid, a warning message is
     * displayed to the user. The method also verifies the existence of the images
     * root path and the specific recording file to be played. If any of these
     * conditions are not met, appropriate error or warning messages are provided
     * to the user via dialog boxes.
     *
     * If all prerequisites are satisfied, a separate thread is created to play
     * the recording file using a Tiff movie player. A log message is generated
     * to indicate the playback process, and any errors encountered during playback
     * are logged and printed to the error stream.
     *
     * Preconditions:
     * - A recording must be selected and exist within the valid index range.
     * - The images root path must be correctly configured and accessible.
     * - The file for the selected recording must exist in the specified path.
     *
     * Postconditions:
     * - The recording is played using a separate thread if all conditions are satisfied.
     *
     * Error Handling:
     * - If the recording is not selected, an appropriate warning message is shown.
     * - If the images root path is not configured, an error dialog prompts the user to configure it.
     * - If the recording file is missing, an error dialog notifies the user.
     * - Any playback errors encountered are logged and printed to the console.
     */
    @Override
    public void onPlayRecordingRequested() {
        leftGridPanel.hideSquareInfoIfVisible();  // Close any popup first

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