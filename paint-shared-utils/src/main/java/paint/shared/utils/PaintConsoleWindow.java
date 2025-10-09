package paint.shared.utils;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PaintConsoleWindow {

    private static JFrame frame;
    private static JTextPane textPane;
    private static StyledDocument doc;
    private static JCheckBox scrollLock;

    // --- Public API ---

    public static synchronized void log(String message) {
        log(message, Color.BLACK);
    }

    public static synchronized void log(String message, Color color) {
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(message + "\n", color));
    }

    public static synchronized void print(String message) {
        print(message, Color.BLACK);
    }

    public static synchronized void print(String message, Color color) {
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(message, color));
    }

    public static synchronized void printChar(char c) {
        printChar(c, Color.BLACK);
    }

    public static synchronized void printChar(char c, Color color) {
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(String.valueOf(c), color));
    }

    public static synchronized void close() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.dispose();
                frame = null;
                textPane = null;
                doc = null;
                scrollLock = null;
            });
        }
    }

    public static void closeOnDialogDispose(JDialog dialog) {
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                close();
            }
        });
    }

    // --- Internal helpers ---

    private static void ensureConsoleCreated() {
        if (frame == null) {
            createConsole();
        }
    }

    private static void createConsole() {
        frame = new JFrame("Paint Console");
        frame.setSize(1200, 400);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        doc = textPane.getStyledDocument();

        // Prevent auto-scroll unless we explicitly do it
        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textPane);

        // Control panel: scroll lock (left), save & close (right)
        JPanel controlPanel = new JPanel(new BorderLayout());

        scrollLock = new JCheckBox("Scroll Lock");
        controlPanel.add(scrollLock, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Save");
        JButton closeButton = new JButton("Close");
        saveButton.addActionListener(PaintConsoleWindow::saveConsoleContent);
        closeButton.addActionListener(e -> close());

        buttonPanel.add(saveButton);
        buttonPanel.add(closeButton);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(controlPanel, BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void appendText(String text, Color color) {
        try {
            Style style = textPane.addStyle("Style", null);
            StyleConstants.setForeground(style, color);
            doc.insertString(doc.getLength(), text, style);

            if (!scrollLock.isSelected()) {
                textPane.setCaretPosition(doc.getLength());
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void saveConsoleContent(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Console Output");
        int choice = chooser.showSaveDialog(frame);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(textPane.getText());
                JOptionPane.showMessageDialog(frame, "Console saved to " + file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to save file:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}