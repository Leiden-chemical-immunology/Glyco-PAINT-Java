/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.getomero.app;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * A graphical user interface for processing Omero-exported "Fileset" directories.
 * <p>
 * This Swing-based tool enables users to:
 * <ul>
 *   <li>Select a root directory containing "Fileset" subdirectories.</li>
 *   <li>Automatically move valid files to the root directory.</li>
 *   <li>Delete empty "Fileset" folders after processing.</li>
 *   <li>Display a scrollable list of remaining files.</li>
 * </ul>
 * The interface provides buttons for browsing, processing, and closing, with clear
 * error handling for invalid directories or I/O failures.
 */
public class GetOmeroUI extends JFrame {

    private final JTextField directoryField;

    /**
     * Constructs a new instance of the {@code GetOmeroUI} class and initializes
     * the application window with controls for directory selection, processing,
     * and viewing results.
     */
    public GetOmeroUI() {
        super("Get Omero");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Base font (plain, small)
        Font baseFont = new JLabel().getFont().deriveFont(Font.PLAIN, 12f);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- Directory selection panel ---
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
        browseButton.addActionListener(e -> onBrowse());

        gbc.gridx  = 0;
        gbc.anchor = GridBagConstraints.WEST;
        dirPanel.add(dirLabel, gbc);

        gbc.gridx   = 1;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        dirPanel.add(directoryField, gbc);

        gbc.gridx   = 2;
        gbc.fill    = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        dirPanel.add(browseButton, gbc);

        // --- Action buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton processButton = new JButton("Process");
        JButton closeButton = new JButton("Close");
        processButton.setFont(baseFont);
        closeButton.setFont(baseFont);
        buttonPanel.add(processButton);
        buttonPanel.add(closeButton);

        processButton.addActionListener(e -> onProcess());
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
     * Opens a directory chooser dialog and updates the directory path display.
     */
    private void onBrowse() {
        JFileChooser chooser = new JFileChooser(directoryField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            directoryField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * Executes when the "Process" button is clicked. Processes all "Fileset" subdirectories
     * of the selected folder by invoking {@code ProcessOmeroFiles.process()}, then displays
     * a summary of remaining files in a scrollable dialog.
     */
    private void onProcess() {
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

            // Collect visible files now in the directory (skip dotfiles)
            File[] remaining = rootDir.listFiles(f -> f.isFile() && !f.getName().startsWith("."));
            StringBuilder sb = new StringBuilder();
            if (remaining != null && remaining.length > 0) {
                for (File f : remaining) {
                    sb.append(f.getName()).append("\n");
                }
            } else {
                sb.append("(No files found)");
            }

            // Display them in a scrollable dialog
            JTextArea area = new JTextArea(sb.toString(), 15, 50);
            area.setEditable(false);
            area.setFont(new JLabel().getFont().deriveFont(Font.PLAIN, 12f));
            JScrollPane scroll = new JScrollPane(area);

            String dialogTitle = "Processing completed — " + rootDir.getAbsolutePath();JOptionPane.showMessageDialog(
                    this,
                    scroll,
                    dialogTitle,
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                                          "Error during processing:\n" + ex.getMessage(),
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Launches the Get Omero GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GetOmeroUI::new);
    }
}