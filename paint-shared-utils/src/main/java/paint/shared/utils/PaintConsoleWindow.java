package paint.shared.utils;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PaintConsoleWindow {

    private static JFrame frame;
    private static JTextPane textPane;
    private static StyledDocument doc;
    private static JCheckBox scrollLock;

    private static final List<Integer> problemPositions = new ArrayList<>();
    private static int currentProblemIndex = -1;

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
                problemPositions.clear();
                currentProblemIndex = -1;
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

    // --- Added public API for title handling ---

    public static synchronized void setConsoleTitle(String title) {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.setTitle(title != null ? title : "Paint Console"));
        }
    }

    public static synchronized void createConsoleFor(String creatorName) {
        if (frame == null) {
            createConsole("Paint Console – " + (creatorName != null ? creatorName : "Unknown"));
        } else {
            setConsoleTitle("Paint Console – " + (creatorName != null ? creatorName : "Unknown"));
        }
    }

    // --- Internal helpers ---

    private static void ensureConsoleCreated() {
        if (frame == null) {
            createConsole();
        }
    }

    private static void createConsole() {
        createConsole("Paint Console");
    }

    private static void createConsole(String title) {
        frame = new JFrame(title != null ? title : "Paint Console");
        frame.setSize(1200, 400);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        doc = textPane.getStyledDocument();

        DefaultCaret caret = (DefaultCaret) textPane.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textPane);

        JPanel controlPanel = new JPanel(new BorderLayout());
        scrollLock = new JCheckBox("Scroll Lock");
        scrollLock.addActionListener(e -> updateTitleForScrollLock(scrollLock.isSelected()));
        controlPanel.add(scrollLock, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton highlightButton = new JButton("Highlight Problems");
        highlightButton.addActionListener(e -> highlightProblemsOrNext());
        JButton saveButton = new JButton("Save");
        JButton closeButton = new JButton("Close");
        saveButton.addActionListener(PaintConsoleWindow::saveConsoleContent);
        closeButton.addActionListener(e -> close());

        buttonPanel.add(highlightButton);
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
                JOptionPane.showMessageDialog(frame, "Failed to save file:\n" + ex.getMessage(),
                                              "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // --- Problem highlighting and navigation ---

    private static void highlightProblemsOrNext() {
        if (scrollLock != null && !scrollLock.isSelected()) {
            scrollLock.setSelected(true);
            updateTitleForScrollLock(true);
        }

        if (problemPositions.isEmpty()) {
            // First click: find and highlight
            highlightProblems();
            if (!problemPositions.isEmpty()) {
                currentProblemIndex = 0;
                selectProblem(problemPositions.get(currentProblemIndex));
            } else {
                JOptionPane.showMessageDialog(frame, "No problems found (error, warning, exception).");
            }
        } else {
            // Subsequent clicks: move to next match
            currentProblemIndex++;
            if (currentProblemIndex >= problemPositions.size()) {
                currentProblemIndex = 0; // wrap around
            }
            selectProblem(problemPositions.get(currentProblemIndex));
        }
    }

    private static void highlightProblems() {
        Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();
        problemPositions.clear();
        currentProblemIndex = -1;

        String text = textPane.getText().toLowerCase();

        highlightPattern(text, "error", Color.PINK);
        highlightPattern(text, "warn", Color.ORANGE);
    }

    private static void highlightPattern(String fullText, String keyword, Color color) {
        int index = fullText.indexOf(keyword);
        while (index >= 0) {
            try {
                textPane.getHighlighter().addHighlight(
                        index,
                        index + keyword.length(),
                        new DefaultHighlighter.DefaultHighlightPainter(color)
                );
                problemPositions.add(index);
                index = fullText.indexOf(keyword, index + keyword.length());
            } catch (BadLocationException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private static void selectProblem(int pos) {
        textPane.requestFocus();
        textPane.setCaretPosition(pos);
        textPane.select(pos, pos + 1);

        // Ensure the selected problem is visible
        try {
            Rectangle viewRect = textPane.modelToView(pos);
            if (viewRect != null) textPane.scrollRectToVisible(viewRect);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void updateTitleForScrollLock(boolean locked) {
        if (frame == null) return;
        String baseTitle = frame.getTitle();
        baseTitle = baseTitle.replace(" [Scroll Locked]", "");
        if (locked) {
            frame.setTitle(baseTitle + " [Scroll Locked]");
        } else {
            frame.setTitle(baseTitle);
        }
    }
}