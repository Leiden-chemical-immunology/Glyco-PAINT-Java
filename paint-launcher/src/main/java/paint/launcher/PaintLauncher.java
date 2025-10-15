package paint.launcher;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class PaintLauncher {

    // Map: Button label → jar filename
    private static final Map<String, String> APP_MAP = new LinkedHashMap<>();

    static {
        APP_MAP.put("Get Omero", "paint-get-omero.jar");
        APP_MAP.put("Create Experiment", "paint-create-experiment.jar");
        APP_MAP.put("Generate Squares", "paint-generate-squares.jar");
        APP_MAP.put("Viewer", "paint-viewer.jar");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PaintLauncher::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("PAINT Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 360);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        // Colors
        Color bg = new Color(35, 39, 42);
        Color fg = new Color(240, 240, 240);
        Color accent = new Color(0x4E9AF1);

        // Root panel
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel title = new JLabel("PAINT Launcher", SwingConstants.CENTER);
        title.setForeground(fg);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        root.add(title, BorderLayout.NORTH);

        // Buttons panel
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new GridLayout(APP_MAP.size(), 1, 12, 12));
        btnPanel.setOpaque(false);

        for (Map.Entry<String, String> entry : APP_MAP.entrySet()) {
            JButton btn = createStyledButton(entry.getKey(), accent, fg, bg);
            btn.addActionListener(e -> launchApp(entry.getValue(), frame));
            btnPanel.add(btn);
        }

        root.add(btnPanel, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("Herr Doctor • © 2025", SwingConstants.CENTER);
        footer.setForeground(new Color(160, 160, 160));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static JButton createStyledButton(String text, Color accent, Color fg, Color bg) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setForeground(fg);
        btn.setBackground(accent);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(accent.brighter());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(accent);
            }
        });

        return btn;
    }

    private static void launchApp(String jarName, Component parent) {
        File jarFile = new File(jarName);
        if (!jarFile.exists()) {
            JOptionPane.showMessageDialog(parent,
                    "JAR not found:\n" + jarFile.getAbsolutePath(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath())
                    .inheritIO()
                    .start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to launch:\n" + jarName + "\n\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}