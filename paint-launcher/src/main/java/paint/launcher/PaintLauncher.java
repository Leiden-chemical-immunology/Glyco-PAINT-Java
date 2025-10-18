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

    private static final Map<String, String> APP_MAP = new LinkedHashMap<>();
    private static final String JAR_DIR = "jars";

    static {
        APP_MAP.put("Get Omero", "paint-get-omero");
        APP_MAP.put("Create Experiment", "paint-create-experiment");
        APP_MAP.put("Run TrackMate", null); // visual only
        APP_MAP.put("Generate Squares", "paint-generate-squares");
        APP_MAP.put("Viewer", "paint-viewer");
        APP_MAP.put("Analyse Results", null); // visual only
    }


    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Glyco-PAINT Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        Color bg = new Color(35, 39, 42);
        Color fg = new Color(240, 240, 240);
        Color accent = new Color(0x4E9AF1);
        Color runColor = new Color(0xBFD7F8);      // "Run TrackMate"
        Color analyseColor = new Color(0xD8E7FB);  // "Analyse Results"
        Color arrowMain = new Color(210, 210, 210);
        Color arrowShadow = new Color(80, 80, 80);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Glyco-PAINT Launcher", SwingConstants.CENTER);
        title.setForeground(fg);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setBorder(new EmptyBorder(0, 0, 20, 0));
        root.add(title, BorderLayout.NORTH);

        // Main workflow panel
        JPanel flowPanel = new JPanel();
        flowPanel.setLayout(new BoxLayout(flowPanel, BoxLayout.Y_AXIS));
        flowPanel.setBackground(bg);

        int index = 0;
        int total = APP_MAP.size();
        Dimension standardSize = new Dimension(280, 45);

        for (Map.Entry<String, String> entry : APP_MAP.entrySet()) {
            String label = entry.getKey();
            String target = entry.getValue();

            JComponent comp;
            if (target == null) {
                Color boxColor = "Run TrackMate".equals(label) ? runColor : analyseColor;
                comp = createSectionLabel(label, boxColor, Color.DARK_GRAY, standardSize);
            } else {
                JButton btn = createStyledButton(label, accent, fg, bg, standardSize);
                btn.addActionListener(e -> launchApp(target, frame));
                comp = btn;
            }

            comp.setAlignmentX(Component.CENTER_ALIGNMENT);
            flowPanel.add(comp);

            if (++index < total) {
                JLabel arrow = createBoldArrow(arrowMain, arrowShadow);
                flowPanel.add(arrow);
            }
        }

        // Add vertical glue to keep bottom visible
        flowPanel.add(Box.createVerticalGlue());
        root.add(flowPanel, BorderLayout.CENTER);

        JLabel footer = new JLabel("Herr Doctor • © 2025", SwingConstants.CENTER);
        footer.setForeground(new Color(160, 160, 160));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static JButton createStyledButton(String text, Color accent, Color fg, Color bg, Dimension size) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(size);
        btn.setMaximumSize(size);
        btn.setMinimumSize(size);
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

    private static JLabel createSectionLabel(String text, Color bgColor, Color fg, Dimension size) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(fg);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        label.setPreferredSize(size);
        label.setMaximumSize(size);
        label.setMinimumSize(size);
        return label;
    }

    private static JLabel createBoldArrow(Color main, Color shadow) {
        JLabel arrow = new JLabel("↓", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                String s = getText();
                int x = (getWidth() - fm.stringWidth(s)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 4;

                // shadow
                g2.setColor(shadow);
                g2.drawString(s, x + 1, y + 1);
                // main arrow
                g2.setColor(main);
                g2.drawString(s, x, y);
                g2.dispose();
            }
        };
        arrow.setFont(new Font("SansSerif", Font.BOLD, 32)); // larger, thicker arrow
        arrow.setBorder(new EmptyBorder(10, 0, 10, 0));
        arrow.setAlignmentX(Component.CENTER_ALIGNMENT);
        arrow.setPreferredSize(new Dimension(300, 35));
        return arrow;
    }

    private static void launchApp(String jarPrefix, Component parent) {
        File jarDir = new File(System.getProperty("user.dir"), JAR_DIR);
        if (!jarDir.exists() || !jarDir.isDirectory()) {
            JOptionPane.showMessageDialog(parent,
                                          "JAR directory not found:\n" + jarDir.getAbsolutePath(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] matches = jarDir.listFiles(f ->
                                                  f.getName().startsWith(jarPrefix) && f.getName().endsWith(".jar"));

        if (matches == null || matches.length == 0) {
            JOptionPane.showMessageDialog(parent,
                                          "No matching JAR found for prefix:\n" + jarPrefix,
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File jarFile = matches[0];
        try {
            new ProcessBuilder("java", "-jar", jarFile.getAbsolutePath())
                    .inheritIO()
                    .start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                                          "Failed to launch:\n" + jarFile.getName() + "\n\n" + e.getMessage(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PaintLauncher::createAndShowGUI);
    }
}