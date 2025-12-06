/*
 * ============================================================================
 *  PURPOSE
 *      Swing panel for selecting and managing the Project Root and Images Root paths.
 *
 *  DESCRIPTION
 *      This class provides UI components for entering or browsing directories used by
 *      the application. It supports callbacks for detecting root changes, browsing actions,
 *      and mode-dependent behaviors (such as disabling the Images Root field in
 *      GENERATE_SQUARES mode).
 *
 *  KEY FEATURES
 *      - Project Root and Images Root text fields
 *      - Browse buttons with directory chooser dialogs
 *      - Callbacks via Runnable and Consumer interfaces
 *      - Mode-dependent UI restrictions
 *      - Automatic notifications on text edits
 *
 *  AUTHOR
 *      PAINT Automatic Header Generator
 *
 *  MODULE
 *      paint.shared.dialogs.project
 *
 *  UPDATED
 *      2025-11-24
 *
 *  COPYRIGHT
 *      © PAINT Project. All rights reserved.
 * ============================================================================
 */

package paint.shared.dialogs.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

/**
 * Panel allowing users to select the Project Root and Images Root directories.
 * <p>
 * The panel exposes browse buttons, text fields, and callback hooks. It notifies
 * listeners when the text changes or when browse actions complete.
 */
public class ProjectPathsPanel {

    private final JPanel     panel = new JPanel(new GridBagLayout());  // The root UI panel containing the layout.
    private final JTextField projectRootField;                         // Text field for the project root directory.
    private final JTextField imagesRootField;                          // Text field for the images root directory
    private final JButton    browseProjectBtn;                         // Browse button for project root.
    private final JButton    browseImagesBtn;                          // Browse button for images root.

    /**
     * Runnable invoked when either root changes.
     */
    private Runnable rootsChanged = () -> {
    };

    /**
     * Consumer invoked when a project directory is chosen via the browse dialog.
     */
    private Consumer<File> onProjectChosen = f -> {
    };

    /**
     * Consumer invoked when an images directory is chosen via the browse dialog.
     */
    private Consumer<File> onImagesChosen = f -> {
    };

    /**
     * Constructs the UI panel, initializes fields, and wires callbacks.
     *
     * @param mode           dialog mode (affects Images Root field behavior)
     * @param initialProject initial project directory to display
     */
    public ProjectPathsPanel(DialogMode mode, Path initialProject) {
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(5, 5, 5, 5);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        final Dimension labelSize = new Dimension(200, 20);
        int row = 0;

        // Project Root label
        JLabel lblProject = new JLabel("Project Root");
        lblProject.setPreferredSize(labelSize);

        gbc.gridx   = 0;
        gbc.gridy   = row;
        gbc.weightx = 0;
        gbc.fill    = GridBagConstraints.NONE;
        panel.add(lblProject, gbc);

        // Text field for project root
        projectRootField = new JTextField(initialProject != null ? initialProject.toString() : System.getProperty("user.home"), 32);
        gbc.gridx        = 1;
        gbc.weightx      = 1.0;
        gbc.fill         = GridBagConstraints.HORIZONTAL;
        panel.add(projectRootField, gbc);

        // Browse project button
        browseProjectBtn = new JButton("Browse...");
        gbc.gridx        = 2;
        gbc.weightx      = 0;
        gbc.fill         = GridBagConstraints.NONE;
        panel.add(browseProjectBtn, gbc);

        row++;

        // Images Root label
        JLabel lblImages = new JLabel("Images Root");
        lblImages.setPreferredSize(labelSize);

        gbc.gridx   = 0;
        gbc.gridy   = row;
        gbc.weightx = 0;
        gbc.fill    = GridBagConstraints.NONE;
        panel.add(lblImages, gbc);

        // Load saved Images Root
        String savedImagesRoot = paint.shared.utils.PaintPrefs.getString(
                "Path",
                "Images Root",
                System.getProperty("user.home")
        );

        // Text field for images root
        imagesRootField = new JTextField(savedImagesRoot, 32);
        gbc.gridx       = 1;
        gbc.weightx     = 1.0;
        gbc.fill        = GridBagConstraints.HORIZONTAL;
        panel.add(imagesRootField, gbc);

        // Browse images button
        browseImagesBtn = new JButton("Browse...");
        gbc.gridx       = 2;
        gbc.weightx     = 0;
        gbc.fill        = GridBagConstraints.NONE;
        panel.add(browseImagesBtn, gbc);

        // Browse action for project root
        browseProjectBtn.addActionListener(this::browseProjectRoot);

        // Browse action for images root
        browseImagesBtn.addActionListener(this::browseImagesRoot);

        // Notify when text fields change
        projectRootField.getDocument().addDocumentListener((SimpleDocumentListener) this::onTextChanged);
        imagesRootField.getDocument().addDocumentListener((SimpleDocumentListener) this::onTextChanged);

        // GENERATE_SQUARES mode: disable Images Root controls
        if (mode == DialogMode.GENERATE_SQUARES) {
            imagesRootField.setEnabled(false);
            imagesRootField.setBackground(UIManager.getColor("Panel.background"));
            imagesRootField.setForeground(Color.GRAY);
            imagesRootField.setFocusable(false);
            browseImagesBtn.setEnabled(false);
        }
    }

    /**
     * @return the root Swing component
     */
    public JPanel component() {
        return panel;
    }

    /**
     * @return the project root path as text
     */
    public String projectRootText() {
        return projectRootField.getText().trim();
    }

    /**
     * @return the images root path as text
     */
    public String imagesRootText() {
        return imagesRootField.getText().trim();
    }

    /**
     * @return true if the project root path points to a directory
     */
    public boolean isProjectRootValid() {
        return new File(projectRootText()).isDirectory();
    }

    /**
     * Enables or disables UI fields depending on dialog mode.
     */
    public void setEnabled(boolean enabled, DialogMode mode) {
        // Project root always follows enabled state
        projectRootField.setEnabled(enabled);
        browseProjectBtn.setEnabled(enabled);

        if (mode == DialogMode.GENERATE_SQUARES) {
            // Only this mode disables images root
            imagesRootField.setEnabled(false);
            browseImagesBtn.setEnabled(false);
        } else {
            // TRACKMATE and VIEWER
            imagesRootField.setEnabled(enabled);
            browseImagesBtn.setEnabled(enabled);
        }
    }

    /**
     * Update the project root text field when changed externally.
     */
    public void onProjectRootChanged(Path newRoot) {
        if (newRoot != null) {
            projectRootField.setText(newRoot.toString());
        }
    }

    /**
     * Assign callback for general root changes.
     */
    public void onRootsChanged(Runnable r) {
        this.rootsChanged = (r != null ? r : () -> {
        });
    }

    /**
     * Assign callback specifically for project directory browsing.
     */
    public void onBrowseProject(Consumer<File> c) {
        this.onProjectChosen = (c != null ? c : f -> {
        });
    }

    /**
     * Assign callback specifically for images directory browsing.
     */
    public void onBrowseImages(Consumer<File> c) {
        this.onImagesChosen = (c != null ? c : f -> {
        });
    }

    /**
     * Invoked when text changes in either root field; triggers the rootsChanged callback.
     */
    @SuppressWarnings("unused")
    private void onTextChanged(DocumentEvent e) {
        rootsChanged.run();
    }

    /**
     * Returns the value if it is a valid directory; otherwise returns the user's home directory.
     */
    private static String valueOrHome(String s) {
        if (s == null || s.trim().isEmpty()) {
            return System.getProperty("user.home");
        }
        File f = new File(s.trim());
        return f.isDirectory() ? f.getAbsolutePath() : System.getProperty("user.home");
    }

    /** Handles the Browse action for the Project Root. */
    @SuppressWarnings("unused")
    private void browseProjectRoot(ActionEvent e) {
        File dir = DirectoryChooser.chooseDirectory(panel, "Project Root", valueOrHome(projectRootField.getText()));
        if (dir != null && dir.isDirectory()) {
            projectRootField.setText(dir.getAbsolutePath());
            onProjectChosen.accept(dir);
        }
    }

    /** Handles the Browse action for the Images Root. */
    @SuppressWarnings("unused")
    private void browseImagesRoot(ActionEvent e) {
        File dir = DirectoryChooser.chooseDirectory(panel, "Images Root", valueOrHome(imagesRootField.getText()));
        if (dir != null && dir.isDirectory()) {
            imagesRootField.setText(dir.getAbsolutePath());
            onImagesChosen.accept(dir);
        }
    }


}
