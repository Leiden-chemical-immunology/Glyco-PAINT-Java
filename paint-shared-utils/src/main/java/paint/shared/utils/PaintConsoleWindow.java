/******************************************************************************
 *  Class:        PaintConsoleWindow.java
 *  Package:      paint.shared.utils
 *
 *  PURPOSE:
 *    Provides a Swing-based graphical console window for logging, monitoring,
 *    and visually inspecting messages from the PAINT framework.
 *
 *  DESCRIPTION:
 *    The {@code PaintConsoleWindow} class implements an interactive console
 *    using Swing components. It supports colored message output, problem
 *    highlighting (e.g., "Error", "Warning", "Exception"), scroll lock control,
 *    and the ability to save or close the console from within the GUI.
 *
 *    The console is intended as a visual log window for PAINT-based desktop
 *    applications, allowing both text-based and GUI-integrated feedback.
 *
 *  KEY FEATURES:
 *    • Thread-safe message logging and dynamic color highlighting.
 *    • Scroll lock and caret control for controlled auto-scrolling.
 *    • Interactive buttons for saving, closing, and navigating problem messages.
 *    • Pattern-based highlighting of "Error", "Warning", and "Exception".
 *    • Fully self-contained Swing UI with no external dependencies.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

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
import java.util.regex.*;

/**
 * Provides a Swing-based console window for real-time message output.
 * <p>
 * Supports message coloring, saving logs to disk, problem highlighting,
 * and scroll lock behavior.
 */
public final class PaintConsoleWindow {

    // ───────────────────────────────────────────────────────────────────────────────
    // FIELDS
    // ───────────────────────────────────────────────────────────────────────────────

    private static JFrame frame;
    private static JTextPane textPane;
    private static StyledDocument doc;
    private static JCheckBox scrollLock;

    private static final List<Integer> problemPositions = new ArrayList<>();
    private static       int currentProblemIndex = -1;

    // ───────────────────────────────────────────────────────────────────────────────
    // PUBLIC LOGGING API
    // ───────────────────────────────────────────────────────────────────────────────

    /** Logs a message in black text. */
    public static synchronized void log(String message) {
        log(message, Color.BLACK);
    }

    /** Logs a message with a specified color. */
    public static synchronized void log(String message, Color color) {
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(message + "\n", color));
    }

    /** Prints a message in black text without newline. */
    public static synchronized void print(String message) {
        print(message, Color.BLACK);
    }

    /** Prints a message with the specified color without newline. */
    public static synchronized void print(String message, Color color) {
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(message, color));
    }

    /** Prints a single character in black. */
    public static synchronized void printChar(char c) {
        printChar(c, Color.BLACK);
    }

    /** Prints a single character with the specified color. */
    public static synchronized void printChar(char c, Color color) {
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(String.valueOf(c), color));
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // WINDOW CONTROL
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Closes and disposes of the console window, clearing all references and data.
     */
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

    /** Attaches automatic console closure when a given dialog is disposed. */
    public static void closeOnDialogDispose(JDialog dialog) {
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                close();
            }
        });
    }

    /** Closes the console window if currently visible. */
    public static void closeIfVisible() {
        if (frame != null && frame.isDisplayable()) {
            SwingUtilities.invokeLater(() -> {
                frame.setVisible(false);
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

    // ───────────────────────────────────────────────────────────────────────────────
    // TITLE MANAGEMENT
    // ───────────────────────────────────────────────────────────────────────────────

    /** Sets the title of the console window. */
    public static synchronized void setConsoleTitle(String title) {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.setTitle(title != null ? title : "Paint Console"));
        }
    }

    /** Creates or updates the console window for a specific creator name. */
    public static synchronized void createConsoleFor(String creatorName) {
        if (frame == null) {
            createConsole("Paint Console – " + (creatorName != null ? creatorName : "Unknown"));
        } else {
            setConsoleTitle("Paint Console – " + (creatorName != null ? creatorName : "Unknown"));
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // INTERNAL CONSOLE CREATION
    // ───────────────────────────────────────────────────────────────────────────────

    private static void ensureConsoleCreated() {
        if (frame == null) {
            createConsole();
        }
    }

    private static void createConsole() {
        createConsole("Paint Console");
    }

    /** Creates and displays the console window with default components and layout. */
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

        JPanel buttonPanel      = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton highlightButton = new JButton("Highlight Problems");
        highlightButton.addActionListener(e -> highlightProblemsOrNext());
        JButton saveButton  = new JButton("Save");
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

    // ───────────────────────────────────────────────────────────────────────────────
    // TEXT HANDLING AND SAVING
    // ───────────────────────────────────────────────────────────────────────────────

    /** Appends colored text to the console. */
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

    /** Saves console output to a user-selected file. */
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
                JOptionPane.showMessageDialog(frame,
                                              "Failed to save file:\n" + ex.getMessage(),
                                              "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // PROBLEM HIGHLIGHTING AND NAVIGATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Highlights detected problems in the console output or navigates to the next highlighted problem.
     *
     * This method performs two primary actions:
     * 1. If no problems have been highlighted yet, it identifies problems in the text output
     *    (e.g., errors, warnings, exceptions) and highlights them.
     * 2. If problems are already highlighted, it navigates to the next problem in the list,
     *    looping back to the first problem when the end of the list is reached.
     *
     * Behavior:
     * - If the scroll lock button is present and not already selected, it is activated to
     *   ensure that the highlighting and navigation operate properly.
     * - Problems are identified using a predefined pattern (e.g., "Error", "Warning", "Exception").
     * - If no problems are found during the initial search, a dialog is displayed informing
     *   the user that no issues were detected.
     * - Navigation across detected problems is cyclical, wrapping around to the first
     *   problem after reaching the last one.
     *
     * Thread Safety:
     * This method is not explicitly synchronized and may require synchronization if called
     * in a multithreaded environment to ensure that shared state (such as problemPositions
     * and currentProblemIndex) is safely updated.
     *
     * Effects:
     * - Updates the scroll lock state and the console title if necessary.
     * - Highlights newly identified problem areas in the console output.
     * - Modifies the `currentProblemIndex` to track the currently selected problem.
     */
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

    /**
     * Highlights problems in the console output based on predefined patterns.
     *
     * This method scans the console's text content and highlights specific terms
     * that indicate potential problems, such as "error", "warning", and "exception".
     * The method uses regular expressions to identify these terms, including
     * variations in capitalization and tolerance for ANSI color codes.
     *
     * The detected terms are highlighted with different colors:
     * - "Error" and "Exception" are highlighted in pink.
     * - "Warning" and "Warn" are highlighted in orange.
     *
     * Detected problem positions are stored in a list for further navigation or processing.
     *
     * Functionality:
     * - Removes all existing highlights before applying new ones.
     * - Clears the previously recorded problem positions and resets the current
     *   problem index to indicate no problem is currently selected.
     * - Performs a case-insensitive search, tolerating leading/trailing spaces
     *   and ANSI formatting codes.
     * - Marks detected terms with appropriate highlight colors.
     * - Adds the start position of each detected problem to the list for future reference.
     *
     * Design notes:
     * - The method is private and applies highlights within the text pane associated
     *   with the console.
     * - If an exception occurs while applying highlights (due to invalid positions),
     *   it is logged but does not interrupt execution.
     *
     * Thread Safety:
     * This method is not thread-safe and should be externally synchronized if
     * accessed from multiple threads, as shared structures like problemPositions
     * and currentProblemIndex are modified.
     */
    private static void highlightProblems() {
        Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();
        problemPositions.clear();
        currentProblemIndex = -1;

        // Get full text as-is
        String fullText = textPane.getText();

        // Unified regex to catch most variants: ERROR, Error, WARN, Warning, etc.
        // Also tolerates ANSI color codes (\u001B[...]m) and leading/trailing spaces.
        Pattern pattern = Pattern.compile(
                "(?i)(?:\\u001B\\[[;\\d]*m)?\\b(?:error|warn|warning|exception)\\b(?:\\u001B\\[[;\\d]*m)?"
        );

        Matcher matcher = pattern.matcher(fullText);
        while (matcher.find()) {
            try {
                String match = matcher.group().toLowerCase();
                Color color = match.contains("error") || match.contains("exception")
                        ? Color.PINK : Color.ORANGE;

                highlighter.addHighlight(
                        matcher.start(),
                        matcher.end(),
                        new DefaultHighlighter.DefaultHighlightPainter(color)
                );
                problemPositions.add(matcher.start());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Highlights all occurrences of a given regex pattern in the provided text with the specified color.
     *
     * @param fullText the text in which to search for the pattern
     * @param regex the regular expression pattern to search for
     * @param color the color used for highlighting the matched pattern
     */
    private static void highlightPattern(String fullText, String regex, Color color) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(fullText);

        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            try {
                textPane.getHighlighter().addHighlight(
                        start, end,
                        new DefaultHighlighter.DefaultHighlightPainter(color)
                );
                problemPositions.add(start);
            } catch (BadLocationException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Highlights and selects a specific text region in a text pane based on the given position.
     * This method applies a yellow highlight to the specified region and sets the text caret within the region,
     * making the region both focused and visible. Previous focus highlights are cleared before applying the new one.
     *
     * @param pos the starting position of the text region to be highlighted and selected
     */
    private static void selectProblem(int pos) {
        try {
            Highlighter highlighter = textPane.getHighlighter();
            Highlighter.HighlightPainter focusPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

            for (Highlighter.Highlight h : highlighter.getHighlights()) {
                if (h.getStartOffset() == pos) {
                    textPane.requestFocus();
                    textPane.setCaretPosition(h.getEndOffset());
                    textPane.select(h.getStartOffset(), h.getEndOffset());
                    highlighter.addHighlight(h.getStartOffset(), h.getEndOffset(), focusPainter);
                    textPane.scrollRectToVisible(textPane.modelToView(h.getStartOffset()));
                    return;
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // TITLE AND SCROLL LOCK STATUS
    // ───────────────────────────────────────────────────────────────────────────────

    /** Updates window title to reflect scroll lock state. */
    private static void updateTitleForScrollLock(boolean locked) {
        if (frame == null) {
            return;
        }
        String baseTitle = frame.getTitle().replace(" [Scroll Locked]", "");
        frame.setTitle(locked ? baseTitle + " [Scroll Locked]" : baseTitle);
    }
}