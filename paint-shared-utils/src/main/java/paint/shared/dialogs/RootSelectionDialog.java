package paint.shared.dialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class RootSelectionDialog extends JDialog {

    public enum Mode { PROJECT, IMAGES }

    private static final String PREF_NODE = "Glyco-PAINT.RootSelection";
    private static final String KEY_PROJECT = "projectDir";
    private static final String KEY_IMAGES = "imagesDir";

    // @formatter:off
    private final    JTextField directoryField;
    private final    JButton    okButton;
    private final    JButton    cancelButton;
    private          boolean    okPressed = false;
    private volatile boolean    cancelled = false;
    // @formatter:on

    private final Mode mode;

    public RootSelectionDialog(Frame owner, Mode mode) {
        super(owner,
              mode == Mode.PROJECT ? "Select Project Directory"
                      : "Select Image Root Directory",
              true); // modal dialog
        this.mode = mode;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // --- Font setup ---
        Font baseFont = new Font("Dialog", Font.PLAIN, 12);
        UIManager.put("Label.font", baseFont);
        UIManager.put("CheckBox.font", baseFont);
        UIManager.put("TextField.font", baseFont);
        UIManager.put("Button.font", baseFont);

        // --- Load last used path ---
        final Preferences prefs = Preferences.userRoot().node(PREF_NODE);
        String key = (mode == Mode.PROJECT) ? KEY_PROJECT : KEY_IMAGES;
        String lastDir = prefs.get(key, System.getProperty("user.home"));

        directoryField = new JTextField(lastDir, 40);

        JButton browseButton = new JButton("Browse...");
        okButton = new JButton("OK");
        cancelButton = new JButton("Cancel");

        // === Browse button ===
        browseButton.addActionListener(e -> {
            File defaultDir = new File(directoryField.getText().trim());
            if (!defaultDir.exists()) defaultDir = new File(System.getProperty("user.home"));

            JFileChooser chooser = new JFileChooser(defaultDir);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                directoryField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // === OK button ===
        okButton.addActionListener(e -> {
            String dir = directoryField.getText().trim();
            if (dir.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                                              "A directory needs to be specified",
                                              "Specify a directory",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }

            Path path = Paths.get(dir);
            if (Files.isRegularFile(path)) {
                JOptionPane.showMessageDialog(this,
                                              "Please specify a directory, not a file",
                                              "Specify a directory",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!Files.isDirectory(path)) {
                JOptionPane.showMessageDialog(this,
                                              "The directory does not exist",
                                              "Specify a directory",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }

            okPressed = true;
            cancelled = false;
            prefs.put(key, dir); // ✅ remember last selection
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

    /**
     * Enable/disable the OK button (useful during processing).
     */
    public void setOkEnabled(boolean enabled) {
        okButton.setEnabled(enabled);
    }

    /**
     * Returns true if user cancelled the dialog or hit Cancel.
     */
    public boolean isCancelled() {
        return cancelled;
    }
}