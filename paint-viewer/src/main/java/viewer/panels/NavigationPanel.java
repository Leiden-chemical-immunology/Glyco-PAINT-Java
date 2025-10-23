package viewer.panels;

import javax.swing.*;
import java.awt.*;

/**
 * The NavigationPanel class provides a graphical user interface component that allows users
 * to navigate between items (e.g., pages or records) using first, previous, next, and last buttons.
 * It is a reusable panel designed for navigation purposes and requires a Listener implementation
 * to handle navigation events.
 */
public class NavigationPanel {
    public interface Listener {
        void onFirst();
        void onPrev();
        void onNext();
        void onLast();
    }

    private final JPanel root;
    private final JButton firstBtn = new JButton("|<");
    private final JButton prevBtn  = new JButton("<");
    private final JButton nextBtn  = new JButton(">");
    private final JButton lastBtn  = new JButton(">|");

    /**
     * Constructs a NavigationPanel with a set of buttons for navigation (first, previous, next, last)
     * and wires them to the provided listener to handle navigation actions.
     *
     * @param listener an implementation of the Listener interface, used to handle actions triggered
     *                 by the navigation buttons. This listener defines the behavior for the first,
     *                 previous, next, and last actions.
     */
    public NavigationPanel(final Listener listener) {
        root = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        root.add(firstBtn);
        root.add(prevBtn);
        root.add(nextBtn);
        root.add(lastBtn);

        firstBtn.addActionListener(e -> listener.onFirst());
        prevBtn.addActionListener(e -> listener.onPrev());
        nextBtn.addActionListener(e -> listener.onNext());
        lastBtn.addActionListener(e -> listener.onLast());
    }

    public JComponent getComponent() {
        return root;
    }

    /**
     * Updates the enabled state of the navigation buttons based on the given parameters.
     *
     * @param hasPrev a boolean indicating whether the first and previous buttons should be enabled.
     *                If true, the buttons are enabled; if false, they are disabled.
     * @param hasNext a boolean indicating whether the next and last buttons should be enabled.
     *                If true, the buttons are enabled; if false, they are disabled.
     */
    public void setEnabledState(boolean hasPrev, boolean hasNext) {
        firstBtn.setEnabled(hasPrev);
        prevBtn.setEnabled(hasPrev);
        nextBtn.setEnabled(hasNext);
        lastBtn.setEnabled(hasNext);
    }
}