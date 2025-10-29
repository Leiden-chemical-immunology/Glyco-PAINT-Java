/******************************************************************************
 *  Class:        NavigationPanel.java
 *  Package:      paint.viewer.panels
 *
 *  PURPOSE:
 *    Provides a reusable navigation control panel with buttons for moving
 *    between records, images, or pages in the PAINT viewer interface.
 *
 *  DESCRIPTION:
 *    Displays a simple four-button layout for navigation:
 *      • “|<” — Jump to the first item
 *      • “<”  — Move to the previous item
 *      • “>”  — Move to the next item
 *      • “>|” — Jump to the last item
 *
 *    A listener interface is used to connect navigation actions to custom
 *    logic in the parent component. Button enabled states can be updated
 *    dynamically to reflect navigation bounds.
 *
 *  KEY FEATURES:
 *    • Simple reusable navigation panel.
 *    • Listener-based event handling for flexibility.
 *    • Dynamic enable/disable control for navigation limits.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.viewer.panels;

import javax.swing.*;
import java.awt.*;

/**
 * A simple reusable navigation component providing First, Previous, Next,
 * and Last buttons for paging or record traversal within the viewer.
 */
public class NavigationPanel {

    /**
     * Listener interface for responding to navigation button events.
     */
    public interface Listener {
        void onFirst();
        void onPrev();
        void onNext();
        void onLast();
    }

    private final JPanel  root;
    private final JButton firstBtn = new JButton("|<");
    private final JButton prevBtn  = new JButton("<");
    private final JButton nextBtn  = new JButton(">");
    private final JButton lastBtn  = new JButton(">|");

    /**
     * Constructs a NavigationPanel with navigation buttons wired to a listener.
     *
     * @param listener a {@link Listener} implementation handling First, Previous,
     *                 Next, and Last button events
     */
    public NavigationPanel(final Listener listener) {
        root = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        root.add(firstBtn);
        root.add(prevBtn);
        root.add(nextBtn);
        root.add(lastBtn);

        firstBtn.addActionListener(e -> listener.onFirst());
        prevBtn.addActionListener( e -> listener.onPrev());
        nextBtn.addActionListener( e -> listener.onNext());
        lastBtn.addActionListener( e -> listener.onLast());
    }

    /**
     * Returns the root Swing component representing this navigation panel.
     *
     * @return the root {@link JComponent}
     */
    public JComponent getComponent() {
        return root;
    }

    /**
     * Updates the enabled/disabled state of the navigation buttons.
     *
     * @param hasPrev {@code true} to enable the First and Previous buttons
     * @param hasNext {@code true} to enable the Next and Last buttons
     */
    public void setEnabledState(boolean hasPrev, boolean hasNext) {
        firstBtn.setEnabled(hasPrev);
        prevBtn.setEnabled(hasPrev);
        nextBtn.setEnabled(hasNext);
        lastBtn.setEnabled(hasNext);
    }
}