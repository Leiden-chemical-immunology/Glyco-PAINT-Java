package paint.getomero;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

/**
 * GetOmeroUI represents a graphical user interface (GUI) application designed to manage
 * directory-based operations related to Omero file processes. It allows the user to select
 * a directory, initiate a processing operation on "Fileset" directories within the selected
 * directory, and display the results of the operation.
 *
 * This class extends JFrame, providing a Swing-based GUI framework for interactions.
 *
 * Features:
 * - Directory selection via a file chooser.
 * - Initiating processing for directories containing "Fileset" data.
 * - Moving valid files to the root folder and deleting empty "Fileset" directories upon processing.
 * - Displaying remaining files within the root directory in a scrollable dialog.
 * - Allowing users to close the application via a dedicated button.
 *
 * Components:
 * - JLabel: Displays a label for the directory selection field.
 * - JTextField: Displays the path of the currently selected directory.
 * - JButton: Used for browsing directories, initiating processing, and exiting the application.
 * - JPanel: Organizes layout for the directory selection area and buttons.
 * - JScrollPane and JTextArea: Display the list of files after the processing operation.
 *
 * Processing Workflow:
 * - The user selects a root directory from the application.
 * - Valid files from "Fileset" directories within the root directory are moved to the root.
 * - Empty "Fileset" directories are deleted after processing.
 * - A summary of visible files now in the root directory is displayed.
 *
 * Exceptions:
 * - Catches and handles I/O errors that may occur during file processing.
 *
 * Navigation:
 * - JFrame window headline defaults to "Get Omero".
 * - The application provides interactive navigation options for directory viewing and processing.
 *
 * Restrictions:
 * - The directory field is non-editable, and must be selected via the browse button.
 * - Prevents processing unless the chosen directory is valid.
 */
public class GetOmeroUI extends JFrame {

    private final JTextField directoryField;

    /**
     * Constructs an instance of the GetOmeroUI class, a GUI for processing directory-based data
     * in the context of Omero file operations. The UI allows users to select a directory,
     * initiate processing of "Fileset" directories within the selected directory, and view the
     * processing results.
     *
     * Key features:
     * - Displays a graphical user interface for directory selection and interaction.
     * - Contains a "Browse" button for selecting directories via a file chooser.
     * - Provides a "Process" button to initiate processing of selected directories.
     * - Shows a processing summary, including the list of files remaining in the processed directory.
     * - Includes a "Close" button to exit the application.
     *
     * Layout includes:
     * - A text field showing the currently selected directory.
     * - Buttons for browsing directories, processing data, and closing the application.
     * - Proper spacing and formatting for usability.
     *
     * Upon processing:
     * - Moves valid files from "Fileset" directories to the root directory.
     * - Deletes empty "Fileset" directories post-processing.
     * - Displays the results in a scrollable dialog.
     */
    public GetOmeroUI() {
        super("Get Omero");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Base font (plain, small)
        Font baseFont = new JLabel().getFont().deriveFont(Font.PLAIN, 12f);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Directory row
        JPanel dirPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridy = 0;

        JLabel dirLabel = new JLabel("Directory:");
        dirLabel.setFont(baseFont);

        directoryField = new JTextField(System.getProperty("user.home"));
        directoryField.setEditable(false);
        directoryField.setFont(baseFont);
        directoryField.setPreferredSize(new Dimension(320, 24));

        JButton browseButton = new JButton("Browse…");
        browseButton.setFont(baseFont);
        browseButton.setMargin(new Insets(2, 8, 2, 8));
        browseButton.addActionListener(this::onBrowse);

        gbc.gridx = 0; gbc.anchor = GridBagConstraints.WEST;
        dirPanel.add(dirLabel, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        dirPanel.add(directoryField, gbc);

        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
        dirPanel.add(browseButton, gbc);

        // Buttons row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton processButton = new JButton("Process");
        JButton closeButton = new JButton("Close");
        processButton.setFont(baseFont);
        closeButton.setFont(baseFont);
        buttonPanel.add(processButton);
        buttonPanel.add(closeButton);

        processButton.addActionListener(this::onProcess);
        closeButton.addActionListener(e -> dispose());

        // Layout
        content.add(dirPanel, BorderLayout.CENTER);
        content.add(buttonPanel, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
        pack();

        // Fix width = 500, keep auto height
        Dimension size = getSize();
        setSize(new Dimension(500, size.height));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Handles the action event triggered when the user clicks the "Browse" button.
     * Opens a file chooser dialog to select a directory and updates the
     * directory field with the selected directory path.
     *
     * @param e the action event triggered by the "Browse" button
     */
    private void onBrowse(ActionEvent e) {
        JFileChooser chooser = new JFileChooser(directoryField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            directoryField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * Handles the action event triggered when the "Process" button is clicked.
     * Validates the directory selected by the user, processes the files within
     * "Fileset" directories in the selected directory, and displays the list of
     * remaining files in a scrollable dialog. Displays error messages for invalid
     * inputs or processing failures.
     *
     * @param e the action event triggered by the "Process" button
     */
    private void onProcess(ActionEvent e) {
        String path = directoryField.getText();
        if (path == null || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a directory first.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File rootDir = new File(path);
        if (!rootDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "Invalid directory.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            ProcessOmeroFiles.process(rootDir);

            // ✅ Collect visible files now in the directory (skip dotfiles)
            File[] remaining = rootDir.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
            StringBuilder sb = new StringBuilder();
            if (remaining != null && remaining.length > 0) {
                for (File f : remaining) {
                    sb.append(f.getName()).append("\n");
                }
            } else {
                sb.append("(No files found)");
            }

            // ✅ Show them in a scrollable dialog
            JTextArea area = new JTextArea(sb.toString(), 15, 50);
            area.setEditable(false);
            area.setFont(new JLabel().getFont().deriveFont(Font.PLAIN, 12f));
            JScrollPane scroll = new JScrollPane(area);

            JOptionPane.showMessageDialog(
                    this,
                    scroll,
                    "Processing completed. Files now in directory:",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error during processing:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GetOmeroUI::new);
    }
}