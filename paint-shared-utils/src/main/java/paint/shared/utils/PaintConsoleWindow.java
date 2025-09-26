package paint.shared.utils;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

/**
 * Simple Swing console window for PaintLogger.
 * Supports colored log messages per level.
 */
public class PaintConsoleWindow {

    private static JFrame frame;
    private static JTextPane textPane; // JTextPane to support colors/styles
    private static StyledDocument doc;

    // --- Public API ---

    /** Log with default (black) color. */
    public static synchronized void log(String message) {
        log(message, Color.BLACK);
    }

    /** Log with custom color. */
    public static synchronized void log(String message, Color color) {
        if (frame == null) {
            createConsole();
        }
        SwingUtilities.invokeLater(() -> {
            try {
                Style style = textPane.addStyle("Style", null);
                StyleConstants.setForeground(style, color);
                doc.insertString(doc.getLength(), message + "\n", style);
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /** Close and dispose of the console window. */
    public static synchronized void close() {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                frame.setVisible(false);
                frame.dispose();
                frame = null;
                textPane = null;
                doc = null;
            });
        }
    }

    /**
     * Automatically closes the console when the given dialog is disposed.
     */
    public static void closeOnDialogDispose(JDialog dialog) {
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                close();
            }
        });
    }

    // --- Internal helpers ---

    private static void createConsole() {
        frame = new JFrame("Paint Console");
        frame.setSize(800, 400);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        doc = textPane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(textPane);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /** Print a string without newline (default black color). */
    public static synchronized void print(String message) {
        print(message, Color.BLACK);
    }

    /** Print a string without newline with custom color. */
    public static synchronized void print(String message, Color color) {
        if (frame == null) {
            createConsole();
        }
        SwingUtilities.invokeLater(() -> {
            try {
                Style style = textPane.addStyle("InlineStyle", null);
                StyleConstants.setForeground(style, color);
                doc.insertString(doc.getLength(), message, style);
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /** Print a single character without newline (default black color). */
    public static synchronized void printChar(char c) {
        printChar(c, Color.BLACK);
    }

    /** Print a single character without newline with custom color. */
    public static synchronized void printChar(char c, Color color) {
        if (frame == null) {
            createConsole();
        }
        SwingUtilities.invokeLater(() -> {
            try {
                Style style = textPane.addStyle("CharStyle", null);
                StyleConstants.setForeground(style, color);
                doc.insertString(doc.getLength(), String.valueOf(c), style);
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }


}