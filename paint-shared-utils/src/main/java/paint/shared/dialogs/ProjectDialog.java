package paint.shared.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class ProjectDialog extends JDialog {

    private static final String PREF_NODE = "Glyco-PAINT.GenerateSquares";
    private static final String KEY_PROJECT = "projectDir";

    private final JTextField directoryField;
    private boolean okPressed = false;

    public ProjectDialog(Frame owner) {
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
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

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
            okPressed = true;
            prefs.put(KEY_PROJECT, directoryField.getText());
            dispose();
        });

        // === Cancel button ===
        cancelButton.addActionListener(e -> {
            okPressed = false;
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
}