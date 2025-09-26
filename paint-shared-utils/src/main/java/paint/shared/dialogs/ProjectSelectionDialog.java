package paint.shared.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ProjectSelectionDialog extends JDialog {

    private static final String PREF_NODE = "Glyco-PAINT.GenerateSquares";
    private static final String KEY_PROJECT = "projectDir";

    private final JTextField directoryField;
    private final JButton okButton;
    private final JButton cancelButton;
    private boolean okPressed = false;
    private volatile boolean cancelled = false;

    public ProjectSelectionDialog(Frame owner) {
        super(owner, "Select Project Directory", true); // modal dialog
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // --- global font override for this dialog ---
        Font baseFont = new Font("Dialog", Font.PLAIN, 12);
        UIManager.put("Label.font", baseFont);
        UIManager.put("CheckBox.font", baseFont);
        UIManager.put("TextField.font", baseFont);
        UIManager.put("Button.font", baseFont);

        // Load last project dir from prefs or fallback to home
        final Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String lastProjectDirectory = prefs.get(KEY_PROJECT, System.getProperty("user.home"));

        directoryField = new JTextField(lastProjectDirectory, 40);

        JButton browseButton = new JButton("Browse...");
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        // === Browse button ===
        browseButton.addActionListener(e -> {
            File defaultDir = new File(directoryField.getText().trim());
            if (!defaultDir.exists()) {
                defaultDir = new File(System.getProperty("user.home"));
            }
            JFileChooser chooser = new JFileChooser(defaultDir);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // === OK button ===
        okButton.addActionListener(e -> {

            String projectDirectory = directoryField.getText().trim();
            if (projectDirectory.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "A directory needs to be specified",
                        "Specify a directory",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if directory exists
            Path path = Paths.get(projectDirectory);
            if (Files.isRegularFile(path)) {
                JOptionPane.showMessageDialog(this,
                        "Please specify a directory, not a file",
                        "Specify a directory",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }
            if (!Files.isDirectory(path)) {
                JOptionPane.showMessageDialog(this,
                        "The directory does no longer exist",
                        "Specify a directory",
                        JOptionPane.ERROR_MESSAGE);

                return;
            }
            okPressed = true;
            cancelled = false; // reset cancelled flag
            prefs.put(KEY_PROJECT, projectDirectory);

            dispose();
        });

        // === Cancel button ===
        cancelButton.addActionListener(e -> {
            okPressed = false;
            cancelled = true;
            dispose();
        });

        // Layout
        JPanel dirPanel = new JPanel(new BorderLayout(5, 5));
        dirPanel.add(browseButton, BorderLayout.WEST);
        dirPanel.add(directoryField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(dirPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * Shows the dialog and returns the chosen directory,
     * or null if cancelled.
     */
    public Path showDialog() {
        setVisible(true); // blocks until dispose()
        return okPressed ? Paths.get(directoryField.getText()) : null;
    }

    /** Enable/disable the OK button (useful during processing). */
    public void setOkEnabled(boolean enabled) {
        okButton.setEnabled(enabled);
    }

    /** Returns true if user cancelled the dialog or hit Cancel. */
    public boolean isCancelled() {
        return cancelled;
    }
}