/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.ui.dialogs.project;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * A simplified functional DocumentListener implementation.
 * <p>
 * Swing's {@link DocumentListener} requires implementing three separate
 * callbacks. This interface consolidates them into a single method
 * {@link #update(DocumentEvent)}, making it easier to attach listeners using
 * lambdas or method references.
 * <p>
 * Example usage:
 * <pre>
 *     textField.getDocument().addDocumentListener((SimpleDocumentListener) e -> {
 *         System.out.println("Text changed: " + e.getDocument());
 *     });
 * </pre>
 */
@FunctionalInterface
public interface SimpleDocumentListener extends DocumentListener {

    /**
     * Invoked when any document change occurs.
     *
     * @param e the document event
     */
    void update(DocumentEvent e);

    /**
     * Called when text is inserted into the document. Delegates to update().
     */
    @Override
    default void insertUpdate(DocumentEvent e) {
        update(e);
    }

    /**
     * Called when text is removed from the document. Delegates to update().
     */
    @Override
    default void removeUpdate(DocumentEvent e) {
        update(e);
    }

    /**
     * Called when document attributes change. Delegates to update().
     */
    @Override
    default void changedUpdate(DocumentEvent e) {
        update(e);
    }
}