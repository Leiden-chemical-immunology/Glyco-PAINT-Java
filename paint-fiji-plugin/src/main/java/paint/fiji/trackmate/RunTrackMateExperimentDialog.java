package paint.fiji.trackmate;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.prefs.Preferences;

public class RunTrackMateExperimentDialog extends JDialog {

    private static final String PREF_NODE = "Glyco-PAINT.RunTrackMateDialog";
    private static final String KEY_IMAGES = "imagesDir";
    private static final String KEY_EXPERIMENT = "experimentDir";

    private final JTextField imagesField;
    private final JTextField experimentField;
    private JButton upImagesBtn;
    private JButton upExpBtn;
    private boolean confirmed = false;

    private static final Preferences prefs = Preferences.userRoot().node(PREF_NODE);

    public RunTrackMateExperimentDialog(Frame parent) {
        super(parent, "Run TrackMate on  Experiment", true); // modal dialog

        // Load last used dirs from preferences
        String defaultImages = System.getProperty("user.home");
        String defaultExperiment = System.getProperty("user.home");
        String lastImages = prefs.get(KEY_IMAGES, defaultImages);
        String lastExperiment = prefs.get(KEY_EXPERIMENT, defaultExperiment);

        imagesField = new JTextField(lastImages, 40);
        experimentField = new JTextField(lastExperiment, 40);

        JButton browseImagesBtn = new JButton("Browse…");
        JButton browseExperimentBtn = new JButton("Browse…");

        browseImagesBtn.addActionListener(e -> chooseDirectory(imagesField, upImagesBtn));
        browseExperimentBtn.addActionListener(e -> chooseDirectory(experimentField, upExpBtn));

        upImagesBtn = new JButton("Up");
        upImagesBtn.setEnabled(!isRootDirectory(new File(imagesField.getText().trim())));
        upImagesBtn.addActionListener(e -> goUp(imagesField, upImagesBtn));

        upExpBtn = new JButton("Up");
        upExpBtn.setEnabled(!isRootDirectory(new File(experimentField.getText().trim())));
        upExpBtn.addActionListener(e -> goUp(experimentField, upExpBtn));

        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");

        okBtn.addActionListener(e -> {
            File imagesDir = new File(imagesField.getText().trim());
            File experimentDir = new File(experimentField.getText().trim());

            if (!imagesDir.isDirectory()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Images directory does not exist:\n" + imagesDir,
                        "Invalid Directory",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            if (!experimentDir.isDirectory()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Experiment directory does not exist:\n" + experimentDir,
                        "Invalid Directory",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // Save choices for next time
            prefs.put(KEY_IMAGES, imagesDir.getAbsolutePath());
            prefs.put(KEY_EXPERIMENT, experimentDir.getAbsolutePath());

            confirmed = true;
            dispose();
        });

        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 12, 8, 12);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;

        // Row 1: Images
        g.gridx = 0; formPanel.add(new JLabel("Images Directory"), g);
        g.gridx = 1; formPanel.add(imagesField, g);
        JPanel imagesButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        imagesButtonPanel.add(browseImagesBtn);
        imagesButtonPanel.add(upImagesBtn);
        g.gridx = 2; formPanel.add(imagesButtonPanel, g);

        // Row 2: Experiment
        g.gridy++;
        g.gridx = 0; formPanel.add(new JLabel("Experiment Directory"), g);
        g.gridx = 1; formPanel.add(experimentField, g);
        JPanel expButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        expButtonPanel.add(browseExperimentBtn);
        expButtonPanel.add(upExpBtn);
        g.gridx = 2; formPanel.add(expButtonPanel, g);

        // Button row
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okBtn);
        buttonPanel.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(formPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(parent);
    }

    private void chooseDirectory(JTextField targetField, JButton upButton) {
        File current = new File(targetField.getText().trim());
        JFileChooser chooser = current.isDirectory()
                ? new JFileChooser(current)
                : new JFileChooser(new File(System.getProperty("user.home")));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File chosen = chooser.getSelectedFile();
            targetField.setText(chosen.getAbsolutePath());
            upButton.setEnabled(!isRootDirectory(chosen));
        }
    }

    private void goUp(JTextField targetField, JButton upButton) {
        File current = new File(targetField.getText().trim());
        File parent = current.getParentFile();
        if (parent != null && parent.isDirectory()) {
            targetField.setText(parent.getAbsolutePath());
            upButton.setEnabled(!isRootDirectory(parent));
        }
    }

    private boolean isRootDirectory(File dir) {
        return (dir == null || dir.getParentFile() == null);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getImagesDir() {
        return imagesField.getText().trim();
    }

    public String getExperimentDir() {
        return experimentField.getText().trim();
    }

    // Demo main
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            RunTrackMateExperimentDialog dlg = new RunTrackMateExperimentDialog(null);
            dlg.setVisible(true);

            if (dlg.isConfirmed()) {
                System.out.println("Images Dir: " + dlg.getImagesDir());
                System.out.println("Experiment Dir: " + dlg.getExperimentDir());

            } else {
                System.out.println("Cancelled");
            }
        });
    }
}