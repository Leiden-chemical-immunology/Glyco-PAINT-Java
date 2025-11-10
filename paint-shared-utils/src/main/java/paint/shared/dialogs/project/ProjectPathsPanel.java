package paint.shared.dialogs.project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

import static paint.shared.dialogs.ProjectDialog.DialogMode;

public class ProjectPathsPanel {

    private final JPanel     panel = new JPanel(new GridBagLayout());
    private final JTextField projectRootField;
    private final JTextField imagesRootField;
    private final JButton    browseProjectBtn;
    private final JButton    browseImagesBtn;

    private Runnable       rootsChanged    = () -> {
    };
    private Consumer<File> onProjectChosen = f -> {
    };
    private Consumer<File> onImagesChosen  = f -> {
    };

    public ProjectPathsPanel(DialogMode mode, Path initialProject) {
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets   = new Insets(5,5,5,5);
        gbc.anchor   = GridBagConstraints.WEST;
        gbc.fill     = GridBagConstraints.HORIZONTAL;
        gbc.weightx  = 1.0;

        final Dimension labelSize = new Dimension(200, 20);
        int row = 0;

        // Project Root
        JLabel lblProject = new JLabel("Project Root");
        lblProject.setPreferredSize(labelSize);

        gbc.gridx   = 0;
        gbc.gridy   = row;
        gbc.weightx = 0;
        gbc.fill    = GridBagConstraints.NONE;
        panel.add(lblProject, gbc);

        projectRootField = new JTextField(
                initialProject != null ? initialProject.toString() : System.getProperty("user.home"), 32);
        gbc.gridx   = 1;
        gbc.weightx = 1.0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        panel.add(projectRootField, gbc);

        browseProjectBtn = new JButton("Browse...");
        gbc.gridx   = 2;
        gbc.weightx = 0;
        gbc.fill    = GridBagConstraints.NONE;
        panel.add(browseProjectBtn, gbc);

        row++;

        // Images Root
        JLabel lblImages = new JLabel("Images Root");
        lblImages.setPreferredSize(labelSize);

        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(lblImages, gbc);

        String savedImagesRoot = paint.shared.utils.PaintPrefs.getString(
                "Path",
                "Images Root",
                System.getProperty("user.home")
        );
        imagesRootField = new JTextField(savedImagesRoot, 32);        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(imagesRootField, gbc);

        browseImagesBtn = new JButton("Browse...");
        gbc.gridx   = 2;
        gbc.weightx = 0;
        gbc.fill    = GridBagConstraints.NONE;
        panel.add(browseImagesBtn, gbc);

        // wire
        browseProjectBtn.addActionListener(e -> {
            File dir = FileDialogs.chooseDirectory(panel, "Project Root", valueOrHome(projectRootField.getText()));
            if (dir != null && dir.isDirectory()) {
                projectRootField.setText(dir.getAbsolutePath());
                onProjectChosen.accept(dir);
                // rootsChanged.run();
            }
        });

        browseImagesBtn.addActionListener(e -> {
            File dir = FileDialogs.chooseDirectory(panel, "Images Root", valueOrHome(imagesRootField.getText()));
            if (dir != null && dir.isDirectory()) {
                imagesRootField.setText(dir.getAbsolutePath());
                onImagesChosen.accept(dir);
                // rootsChanged.run();
            }
        });

        projectRootField.getDocument().addDocumentListener((SimpleDocumentListener) this::onTextChanged);
        imagesRootField.getDocument().addDocumentListener((SimpleDocumentListener) this::onTextChanged);

        // mode-specific: in GENERATE_SQUARES, keep images field visually disabled/read-only
        if (mode == DialogMode.GENERATE_SQUARES) {
            imagesRootField.setEnabled(false);
            imagesRootField.setBackground(UIManager.getColor("Panel.background"));
            imagesRootField.setForeground(Color.GRAY);
            imagesRootField.setFocusable(false);
            browseImagesBtn.setEnabled(false);
        }
    }

    public JPanel component() {
        return panel;
    }

    public String projectRootText() {
        return projectRootField.getText().trim();
    }

    public String imagesRootText() {
        return imagesRootField.getText().trim();
    }

    public boolean isProjectRootValid() {
        return new File(projectRootText()).isDirectory();
    }

    public void setEnabled(boolean enabled, DialogMode mode) {
        projectRootField.setEnabled(enabled);
        browseProjectBtn.setEnabled(enabled);
        if (mode == DialogMode.GENERATE_SQUARES) {
            imagesRootField.setEditable(false);
            imagesRootField.setEnabled(true); // selectable
        } else {
            imagesRootField.setEnabled(enabled);
        }
        browseImagesBtn.setEnabled(enabled && mode != DialogMode.GENERATE_SQUARES);
    }

    public void onProjectRootChanged(Path newRoot) {
        if (newRoot != null) {
            projectRootField.setText(newRoot.toString());
        }
    }

    public void onRootsChanged(Runnable r) {
        this.rootsChanged = (r != null ? r : () -> {
        });
    }

    public void onBrowseProject(Consumer<File> c) {
        this.onProjectChosen = (c != null ? c : f -> {
        });
    }

    public void onBrowseImages(Consumer<File> c) {
        this.onImagesChosen = (c != null ? c : f -> {
        });
    }

    private void onTextChanged(DocumentEvent e) {
        rootsChanged.run();
    }

    private static String valueOrHome(String s) {
        if (s == null || s.trim().isEmpty()) {
            return System.getProperty("user.home");
        }
        File f = new File(s.trim());
        return f.isDirectory() ? f.getAbsolutePath() : System.getProperty("user.home");
    }
}