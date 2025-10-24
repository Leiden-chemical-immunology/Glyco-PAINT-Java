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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The PaintConsoleWindow class provides a graphical console for logging and displaying
 * messages in a text pane. It includes functionality for dynamic message highlighting,
 * saving console content, and controlling scroll behavior within the console. The console
 * is implemented using Swing components, making it suitable for GUI-based applications.
 */
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

    /**
     * Closes the console window and releases associated resources.
     *
     * This method disposes of the current frame if it exists, clears references to its
     * components and any related data structures, and resets the internal state of the console.
     * The operation is performed on the Event Dispatch Thread (EDT) to ensure thread safety
     * when modifying Swing components.
     *
     * Thread Safety:
     * The method is synchronized to prevent race conditions when invoked from multiple threads.
     *
     * Effects:
     * - Releases the frame and associated resources by setting them to null.
     * - Clears the problem positions and resets the current problem index.
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

    public static void closeOnDialogDispose(JDialog dialog) {
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                close();
            }
        });
    }

    // --- Added public API for title handling ---

    /**
     * Sets the title of the console window.
     *
     * This method updates the title of the frame associated with the console
     * on the Event Dispatch Thread (EDT). If the provided title is null,
     * a default title ("Paint Console") is used instead.
     *
     * Thread Safety:
     * The method is synchronized to ensure thread-safe updates to the frame title.
     *
     * @param title the new title to set for the console window; if null,
     *              a default title ("Paint Console") is used.
     */
    public static synchronized void setConsoleTitle(String title) {
        if (frame != null) {
            SwingUtilities.invokeLater(() -> frame.setTitle(title != null ? title : "Paint Console"));
        }
    }

    /**
     * Creates or updates the console window for the specified creator.
     *
     * If the console window does not already exist, this method initializes
     * a new console window with a title that includes the creator's name.
     * If the console window exists, this method updates its title to reflect
     * the creator's name.
     *
     * Thread Safety:
     * The method is synchronized to ensure thread-safe operations when creating
     * or updating the console window.
     *
     * @param creatorName the name of the creator to display in the console
     *                    window's title; if null, "Unknown" is used instead.
     */
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

    /**
     * Creates and displays a console window with interactive components such as text output,
     * control buttons, and scroll lock functionality.
     *
     * This method initializes a GUI console window with a specified title. The console window
     * includes a non-editable text pane for displaying content, a scroll lock toggle button,
     * as well as buttons for highlighting problems, saving console output, and closing the window.
     *
     * @param title the title to set for the console window; if null, the default title
     *              "Paint Console" is used.
     */
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

    /**
     * Appends the specified text to the console text pane with the given color.
     *
     * This method creates a styled text with the specified color and adds it
     * to the text pane document. It ensures that the text pane's caret position
     * is updated to the end unless the scroll lock is enabled. In case of an
     * error inserting text, it logs the exception.
     *
     * @param text the text content to append to the console
     * @param color the color to apply to the appended text
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
     * Saves the current content of the console to a file chosen by the user.
     *
     * This method opens a file chooser dialog, allowing the user to select
     * a location and file name for saving the console output. The console
     * content is written to the specified file. If an error occurs during
     * the saving process, an error message is displayed to the user.
     *
     * @param e the ActionEvent triggered by the user's interaction,
     *          such as clicking the save button
     */
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
                Color color = match.contains("error") || match.contains("exception") ? Color.PINK : Color.ORANGE;

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
            Highlighter.HighlightPainter focusPainter =
                    new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

            // Clear the previous yellow focus
            for (Highlighter.Highlight h : highlighter.getHighlights()) {
                if (h.getPainter() instanceof DefaultHighlighter.DefaultHighlightPainter) {
                    // reset pink/orange/red remains; do not remove those
                }
            }

            // Apply focus highlight
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

    /**
     * Updates the title of the frame to reflect the scroll lock status.
     * If scroll lock is enabled, "[Scroll Locked]" is appended to the title.
     * If scroll lock is disabled, any existing "[Scroll Locked]" in the title is removed.
     *
     * @param locked a boolean indicating whether scroll lock is enabled (true) or disabled (false)
     */
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
}