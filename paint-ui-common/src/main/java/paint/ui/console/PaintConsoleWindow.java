/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.ui.console;

import paint.shared.utils.PaintLogger;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static       JFrame         frame;
    private static       JTextPane      textPane;
    private static       StyledDocument doc;
    private static       JCheckBox      scrollLock;
    private static final List<Integer>  problemPositions = new ArrayList<>();
    private static       int            currentProblemIndex = -1;

    // ───────────────────────────────────────────────────────────────────────────────
    // PUBLIC LOGGING API
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Adapter that lets {@link PaintLogger} feed this console without knowing it exists.
     * Registered in {@link #createConsole(String)}, detached in {@link #close()}.
     */
    private static final PaintLogger.Sink CONSOLE_SINK = new PaintLogger.Sink() {
        @Override
        public void log(String line, Color color) {
            PaintConsoleWindow.log(line, color);
        }

        @Override
        public void print(String text) {
            PaintConsoleWindow.print(text);
        }
    };

    /**
     * True when there is no display available — a headless pipeline run, a CI runner,
     * or a server. Constructing any Swing component in that situation throws
     * {@link HeadlessException}.
     * <p>
     * With the sink inversion this should be unreachable (a headless run never creates a
     * console), but it is kept as a cheap belt-and-braces guard: the console is a GUI
     * convenience only, and {@link PaintLogger} writes every line to the log file
     * regardless, so doing nothing here loses no output.
     */
    private static boolean noDisplay() {
        return GraphicsEnvironment.isHeadless();
    }

    /**
     * Logs a message with a specified color. No-op when there is no display.
     */
    public static synchronized void log(String message, Color color) {
        if (noDisplay()) {
            return;
        }
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(message + "\n", color));
    }

    /**
     * Prints a message in black text without newline.
     */
    public static synchronized void print(String message) {
        print(message, Color.BLACK);
    }

    /**
     * Prints a message with the specified color without newline. No-op when there is no display.
     */
    public static synchronized void print(String message, Color color) {
        if (noDisplay()) {
            return;
        }
        ensureConsoleCreated();
        SwingUtilities.invokeLater(() -> appendText(message, color));
    }


    // ───────────────────────────────────────────────────────────────────────────────
    // WINDOW CONTROL
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Closes and disposes of the console window, clearing all references and data.
     * Also detaches this console from {@link PaintLogger}, so nothing tries to render
     * into a window that no longer exists.
     */
    public static synchronized void close() {
        disposeConsole();
    }

    /**
     * Attaches automatic console closure when a given dialog is disposed.
     */
    public static void closeOnDialogDispose(JDialog dialog) {
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                close();
            }
        });
    }

    /**
     * Closes the console window if it is currently on screen.
     */
    public static synchronized void closeIfVisible() {
        if (frame != null && frame.isDisplayable()) {
            disposeConsole();
        }
    }

    /**
     * Detaches this console from {@link PaintLogger} and disposes the window.
     * <p>
     * Both {@link #close()} and {@link #closeIfVisible()} route through here. They used to carry
     * their own copy of this teardown, and the copies drifted: {@code closeIfVisible()} disposed
     * the frame without clearing the sink, so the logger kept feeding a window that no longer
     * existed — and the next log line simply popped a fresh one. Clearing the sink must happen
     * whenever the window goes away, so it happens in exactly one place.
     */
    private static void disposeConsole() {
        PaintLogger.clearSink();

        if (frame == null) {
            return;
        }
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

    // ───────────────────────────────────────────────────────────────────────────────
    // TITLE MANAGEMENT
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the title of the console window.
     */
    public static synchronized void setConsoleTitle(String title) {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.setTitle(title != null ? title : "Paint Console"));
        }
    }

    /**
     * Creates or updates the console window for a specific creator name.
     * No-op when there is no display.
     */
    public static synchronized void createConsoleFor(String creatorName) {
        if (noDisplay()) {
            return;
        }
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

    /**
     * Creates and displays the console window with default components and layout,
     * and attaches it to {@link PaintLogger} as the output sink.
     * <p>
     * This registration is what keeps the dependency pointing the right way: the logger
     * knows nothing about Swing, and the console opts in. A headless run never creates a
     * console, so it never registers a sink and never loads a UI class.
     */
    private static void createConsole(String title) {
        PaintLogger.setSink(CONSOLE_SINK);

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

        // Smooth scrolling
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

        JPanel controlPanel = new JPanel(new BorderLayout());
        scrollLock = new JCheckBox("Scroll Lock");
        scrollLock.addActionListener(e -> updateTitleForScrollLock(scrollLock.isSelected()));
        controlPanel.add(scrollLock, BorderLayout.WEST);

        JPanel buttonPanel      = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton highlightButton = new JButton("Highlight Problems");
        highlightButton.addActionListener(e -> highlightProblemsOrNext());
        JButton saveButton  = new JButton("Save");
        JButton closeButton = new JButton("Close");
        saveButton.addActionListener( e -> PaintConsoleWindow.saveConsoleContent());
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

    /**
     * Appends colored text to the console.
     */
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

    /**
     * Saves console output to a user-selected file.
     */
    private static void saveConsoleContent() {
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
     * <p>
     * This method performs two primary actions:
     * 1. If no problems have been highlighted yet, it identifies problems in the text output
     * (e.g., errors, warnings, exceptions) and highlights them.
     * 2. If problems are already highlighted, it navigates to the next problem in the list,
     * looping back to the first problem when the end of the list is reached.
     * <p>
     * Behavior:
     * - If the scroll lock button is present and not already selected, it is activated to
     * ensure that the highlighting and navigation operate properly.
     * - Problems are identified using a predefined pattern (e.g., "Error", "Warning", "Exception").
     * - If no problems are found during the initial search, a dialog is displayed informing
     * the user that no issues were detected.
     * - Navigation across detected problems is cyclical, wrapping around to the first
     * problem after reaching the last one.
     * <p>
     * Thread Safety:
     * This method is not explicitly synchronized and may require synchronization if called
     * in a multithreaded environment to ensure that shared state (such as problemPositions
     * and currentProblemIndex) is safely updated.
     * <p>
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
     * <p>
     * This method scans the console's text content and highlights specific terms
     * that indicate potential problems, such as "error", "warning", and "exception".
     * The method uses regular expressions to identify these terms, including
     * variations in capitalization and tolerance for ANSI color codes.
     * <p>
     * The detected terms are highlighted with different colors:
     * - "Error" and "Exception" are highlighted in pink.
     * - "Warning" and "Warn" are highlighted in orange.
     * <p>
     * Detected problem positions are stored in a list for further navigation or processing.
     * <p>
     * Functionality:
     * - Removes all existing highlights before applying new ones.
     * - Clears the previously recorded problem positions and resets the current
     * problem index to indicate no problem is currently selected.
     * - Performs a case-insensitive search, tolerating leading/trailing spaces
     * and ANSI formatting codes.
     * - Marks detected terms with appropriate highlight colors.
     * - Adds the start position of each detected problem to the list for future reference.
     * <p>
     * Design notes:
     * - The method is private and applies highlights within the text pane associated
     * with the console.
     * - If an exception occurs while applying highlights (due to invalid positions),
     * it is logged but does not interrupt execution.
     * <p>
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

    /**
     * Updates window title to reflect scroll lock state.
     */
    private static void updateTitleForScrollLock(boolean locked) {
        if (frame == null) {
            return;
        }
        String baseTitle = frame.getTitle().replace(" [Scroll Locked]", "");
        frame.setTitle(locked ? baseTitle + " [Scroll Locked]" : baseTitle);
    }
}