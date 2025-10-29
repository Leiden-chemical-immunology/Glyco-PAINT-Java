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

/**
 * The PaintLauncher class provides functionality for launching and managing
 * a graphical user interface (GUI) designed to assist users in navigating through
 * workflow-related applications in the Glyco-PAINT software suite.
 * <p>
 * The GUI includes components like buttons, labels, and panels arranged in a specific layout
 * to represent a workflow. The class utilizes mappings between user-friendly labels
 * and associated application identifiers, enabling the conditional display of actionable
 * buttons or visual-only workflow steps based on the map contents.
 * <p>
 * Features:
 * - Customized GUI appearance with consistent sizing, fonts, and colors.
 * - Creation of customizable buttons and section labels with hover effects and styling.
 * - Dynamic rendering of workflow paths using arrows and defined visual elements.
 * - Integration to external application launching using associated identifiers.
 * - Non-resizable, centered application window with a footer.
 * <p>
 * The GUI is intended to provide a user-friendly gateway for interacting
 * with Glyco-PAINT tools or associated processes.
 */
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


    /**
     * Initializes and displays the main graphical user interface (GUI) for the Glyco-PAINT Launcher.
     *
     * This method sets up the main window of the application with a fixed size and layout, featuring
     * a title, a workflow panel with buttons and visual elements, and a footer. Buttons in the workflow
     * panel are dynamically created based on the {@code APP_MAP} field, each associated with specific
     * functionalities or applications. The GUI elements use customized styles, colors, and layouts
     * optimized for a consistent appearance.
     *
     * The window is designed to be non-resizable, centered on the screen, and includes functionality
     * for launching external applications defined by app mappings.
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Glyco-PAINT Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(420, 600);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);

        // @formatter:off
        Color bg           = new Color(35, 39, 42);
        Color fg           = new Color(240, 240, 240);
        Color accent       = new Color(0x4E9AF1);
        Color runColor     = new Color(0xBFD7F8);      // "Run TrackMate"
        Color analyseColor = new Color(0xD8E7FB);  // "Analyse Results"
        Color arrowMain    = new Color(210, 210, 210);
        Color arrowShadow  = new Color(80, 80, 80);
        // @formatter:on

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

        JLabel footer = new JLabel("Hans Bakker • © 2025", SwingConstants.CENTER);
        footer.setForeground(new Color(160, 160, 160));
        footer.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));
        root.add(footer, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    /**
     * Creates a styled JButton with custom appearance and behavior.
     * <p>
     * This method sets properties such as font, colors, size, cursor, and border
     * to create a visually consistent button. It also adds mouse listeners to
     * provide a hover effect by adjusting the background color.
     *
     * @param text   The text to be displayed on the button.
     * @param accent The primary accent color for the button (used as background color).
     * @param fg     The color for the button's text.
     * @param bg     The color for the background (though not directly set here, it may be relevant for overridden methods).
     * @param size   The dimensions of the button.
     * @return A JButton instance with the applied styles and behavior.
     */
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

    /**
     * Creates a styled JLabel with custom appearance and size. The label is centered
     * with the specified text, background color, foreground color, and dimensions.
     *
     * @param text    The text to be displayed on the label.
     * @param bgColor The background color of the label.
     * @param fg      The foreground color (text color) of the label.
     * @param size    The dimensions of the label (preferred, maximum, and minimum sizes).
     * @return A JLabel instance with the applied styles and specified properties.
     */
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

    /**
     * Creates a JLabel representing a bold arrow with customized appearance.
     * <p>
     * The arrow is displayed as a large, bold, and centered downward arrow ("↓")
     * with a shadow effect, using the specified main and shadow colors. This method
     * customizes the label's font, alignment, border, and preferred size.
     *
     * @param main   The primary color of the arrow.
     * @param shadow The color used to create the shadow effect for the arrow.
     * @return A JLabel instance with a styled bold arrow and shadow effect.
     */
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

    /**
     * Launches an external application by locating a JAR file with a specified prefix
     * and executing it as a new process. The method searches for the JAR files in a
     * predefined directory and displays error messages if no matching JAR is found or
     * if the process fails to start.
     *
     * @param jarPrefix The prefix of the JAR file name to be located and executed.
     * @param parent    The parent component for displaying error messages in dialog boxes.
     */
    private static void launchApp(String jarPrefix, Component parent) {
        File jarDir = new File(System.getProperty("user.dir"), JAR_DIR);
        if (!jarDir.exists() || !jarDir.isDirectory()) {
            JOptionPane.showMessageDialog(parent,
                                          "JAR directory not found:\n" + jarDir.getAbsolutePath(),
                                          "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File[] matches = jarDir.listFiles(f -> f.getName().startsWith(jarPrefix) && f.getName().endsWith(".jar"));

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