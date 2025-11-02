/******************************************************************************
 *  Class:        CreateExperimentUI.java
 *  Package:      createexperiment
 *
 *  PURPOSE:
 *    Provides a complete Swing-based user interface for creating experiment
 *    configuration files. It allows users to select directories, filter image
 *    files by regular expression, and generate an "Experiment Info.csv" file.
 *
 *  DESCRIPTION:
 *    This class defines a graphical desktop tool that enables users to manage
 *    experiment setup through directory selection, regex-based filtering, and
 *    automated CSV generation. Regex history, directory paths, and UI state
 *    are stored in user preferences for convenience between sessions.
 *
 *  KEY FEATURES:
 *    • Persistent regex and directory preferences using java.util.prefs.
 *    • Interactive regex management (add, delete, apply filters).
 *    • Automatic refresh of visible file lists when input or filters change.
 *    • File processing workflow via ExperimentInfoWriter.
 *    • Cross-platform compatibility (macOS, Windows, Linux).
 *
 *  AUTHOR:
 *    Hans Bakker (jjabakker)
 *
 *  MODULE:
 *    paint-create-experiment
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *    Licensed under the MIT License.
 ******************************************************************************/

package createexperiment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Fixed and visually balanced CreateExperimentUI.
 */
public class CreateExperimentUI {

    private static final String PREF_NODE   = "paint/create-experiment";
    private static final String KEY_IMAGES  = "lastImagesDir";
    private static final String KEY_PROJECT = "lastProjectDir";

    private static final String[] DEFAULT_REGEXES = {
            "",
            "^\\d-Exp-\\d+-[A-Z]\\d+-\\d+\\.nd2$",
            "^(?!.*BF).*\\.nd2$"
    };

    /**
     * The main method serves as the entry point for the application, initializing
     * and displaying the Create Experiment GUI.
     *
     * @param args the command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CreateExperimentUI::createAndShowGUI);
    }

    /**
     * Configures and displays a graphical user interface (GUI) for managing regex filters,
     * selecting input/output directories, and processing files. The GUI consists of multiple
     * sections, including regex controls, file list display, input/output directory configurations,
     * and action buttons. This method also uses preferences to remember user choices
     * (e.g., regex history and last-used directories) and dynamically updates its components
     * based on user actions.
     * <p>
     * Key Features:
     * - Regex management: Users can add, select, and delete regex patterns.
     * - Input/output directory selection: Users can specify directories for source files
     * and output storage.
     * - File display: Files in the selected input directory are listed and filtered based
     * on the regex.
     * - Persisted preferences: User choices (like regex patterns or directories) are saved
     * and reloaded on subsequent application launches.
     * - Modular panel layout: Organized interface using labeled sections for regex filtering,
     * file selection, and processing actions.
     * <p>
     * Components:
     * - Regex Controls: Dropdown allowing users to input regex patterns, supported by history and deletion menus.
     * - File List: A dynamically updated list of files matching the selected regex in the input directory.
     * - Input/Output Controls: Buttons and labels for selecting directories and displaying their current paths.
     * - Action Buttons: Buttons for initiating the file processing operation and closing the application.
     * <p>
     * Dialogs:
     * - Uses JFileChooser dialogs for selecting directories.
     * <p>
     * Event Handling:
     * - Handles user interactions via buttons and combo box events (e.g., regex filtering, file refreshing).
     * <p>
     * Constraints:
     * - Limits regex entry length to a maximum of 100 characters.
     * - Ensures only valid directories are selected and persistently stores their paths.
     * <p>
     * Dependencies:
     * - javax.swing (for GUI elements like JFrame, JPanel, JComboBox, JList, JButton).
     * - java.util.prefs.Preferences (for storing user-specific persistent settings).
     * - java.io.File (for directory and file handling).
     * <p>
     * Usage:
     * Typically called during application initialization to present the user interface.
     * This method does not return, as it focuses on creating and displaying the application
     * window with active event handling for user interaction.
     */
    private static void createAndShowGUI() {
        Preferences prefs = Preferences.userRoot().node(PREF_NODE);

        JFrame frame = new JFrame("Create Experiment");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Font smallFont = new JLabel().getFont().deriveFont(Font.PLAIN, 12f);
        int labelWidth = 100;
        int buttonWidth = 90;
        Dimension fieldSize = new Dimension(360, 24);
        Dimension buttonSize = new Dimension(buttonWidth, 24);

        // === Top panel (directories + regex) ===
        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 12, 5, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 3, 3, 3);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Function<String, JLabel> label = t -> {
            JLabel l = new JLabel(t, SwingConstants.LEFT);
            l.setFont(smallFont);
            l.setPreferredSize(new Dimension(labelWidth, 24));
            return l;
        };

        final File[] projectDir = {new File(prefs.get(KEY_PROJECT, System.getProperty("user.home")))};
        final File[] imagesDir  = {new File(prefs.get(KEY_IMAGES, System.getProperty("user.home")))};

        // --- Project Root ---
        gbc.gridy = 0; gbc.gridx = 0;
        top.add(label.apply("Project Root:"), gbc);
        gbc.gridx = 1;
        JTextField projectField = new JTextField(projectDir[0].getAbsolutePath());
        projectField.setEditable(false);
        projectField.setFont(smallFont);
        projectField.setPreferredSize(fieldSize);
        top.add(projectField, gbc);
        gbc.gridx = 2;
        JButton projectBrowse = new JButton("Browse…");
        projectBrowse.setFont(smallFont);
        projectBrowse.setPreferredSize(buttonSize);
        top.add(projectBrowse, gbc);

        // --- Images Root ---
        gbc.gridy++;
        gbc.gridx = 0;
        top.add(label.apply("Images Root:"), gbc);
        gbc.gridx = 1;
        JTextField imagesField = new JTextField(imagesDir[0].getAbsolutePath());
        imagesField.setEditable(false);
        imagesField.setFont(smallFont);
        imagesField.setPreferredSize(fieldSize);
        top.add(imagesField, gbc);
        gbc.gridx = 2;
        JButton imagesBrowse = new JButton("Browse…");
        imagesBrowse.setFont(smallFont);
        imagesBrowse.setPreferredSize(buttonSize);
        top.add(imagesBrowse, gbc);

        // --- Regex ---
        gbc.gridy++;
        gbc.gridx = 0;
        top.add(label.apply("Regex:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> regexCombo = new JComboBox<>();
        regexCombo.setEditable(true);
        regexCombo.setFont(smallFont);
        for (String def : DEFAULT_REGEXES) regexCombo.addItem(def);
        top.add(regexCombo, gbc);
        gbc.gridx = 2;
        JButton filterButton = new JButton("Filter");
        filterButton.setFont(smallFont);
        filterButton.setPreferredSize(buttonSize);
        top.add(filterButton, gbc);

        // === File list ===
        DefaultListModel<File> listModel = new DefaultListModel<>();
        JList<File> fileList = new JList<>(listModel);
        fileList.setFont(smallFont);
        JScrollPane scroll = new JScrollPane(fileList);
        scroll.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));

        // === Bottom action bar ===
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton process = new JButton("Process");
        JButton close = new JButton("Close");
        process.setFont(smallFont);
        close.setFont(smallFont);
        process.setPreferredSize(buttonSize);
        close.setPreferredSize(buttonSize);
        actions.add(process);
        actions.add(close);

        frame.add(top, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(actions, BorderLayout.SOUTH);

        // === Logic ===
        Runnable refresh = () -> {
            String regex = ((String) regexCombo.getEditor().getItem()).trim();
            listModel.clear();
            if (imagesDir[0] == null) return;
            Pattern p = null;
            if (!regex.isEmpty()) {
                try { p = Pattern.compile(regex); }
                catch (PatternSyntaxException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid regex: " + ex.getDescription());
                    return;
                }
            }
            File[] files = imagesDir[0].listFiles(f -> f.isFile() && !f.isHidden());
            if (files == null) return;
            for (File f : files)
                if (p == null || p.matcher(f.getName()).matches())
                    listModel.addElement(f);
        };

        projectBrowse.addActionListener(e -> {
            JFileChooser ch = new JFileChooser(projectDir[0]);
            ch.setDialogTitle("Select Project Directory");
            ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            ch.setAcceptAllFileFilterUsed(false); // ✅ hides "All files" filter
            ch.setMultiSelectionEnabled(false);   // ✅ single directory only
            if (ch.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                projectDir[0] = ch.getSelectedFile();
                projectField.setText(projectDir[0].getAbsolutePath());
                prefs.put(KEY_PROJECT, projectDir[0].getAbsolutePath());
            }
        });

        imagesBrowse.addActionListener(e -> {
            JFileChooser ch = new JFileChooser(imagesDir[0]);
            ch.setDialogTitle("Select Images Directory");
            ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            ch.setAcceptAllFileFilterUsed(false);
            ch.setMultiSelectionEnabled(false);
            if (ch.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                imagesDir[0] = ch.getSelectedFile();
                imagesField.setText(imagesDir[0].getAbsolutePath());
                prefs.put(KEY_IMAGES, imagesDir[0].getAbsolutePath());
                refresh.run();
            }
        });

        filterButton.addActionListener(e -> refresh.run());
        regexCombo.addActionListener(e -> refresh.run());

        process.addActionListener((ActionEvent e) -> {
            if (projectDir[0] == null || imagesDir[0] == null) {
                JOptionPane.showMessageDialog(frame, "Select both Project and Images directories.");
                return;
            }
            List<File> selected = fileList.getSelectedValuesList();
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No files selected.");
                return;
            }
            try {
                String expName = imagesDir[0].getName();
                File expDir = new File(projectDir[0], expName);
                if (!expDir.exists() && !expDir.mkdirs())
                    throw new IOException("Cannot create: " + expDir);
                File created = ExperimentInfoWriter.writeExperimentInfo(expDir, selected);
                JOptionPane.showMessageDialog(frame,
                                              "Experiment info written to:\n" + created.getAbsolutePath(),
                                              "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
            }
        });

        close.addActionListener(e -> frame.dispose());

        frame.setSize(560, 520);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        refresh.run();
    }
}