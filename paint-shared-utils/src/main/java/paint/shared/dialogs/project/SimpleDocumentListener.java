/*
 * ============================================================================
 *  PURPOSE
 *      Functional wrapper around Swing's DocumentListener interface.
 *
 *  DESCRIPTION
 *      The standard javax.swing.event.DocumentListener interface requires
 *      implementing three separate methods (insertUpdate, removeUpdate,
 *      changedUpdate). In most cases, an application simply wants to react to
 *      "any" document change. This interface reduces boilerplate by allowing
 *      implementers to define a single update(...) method. All inherited
 *      DocumentListener methods delegate to this unified update method.
 *
 *  KEY FEATURES
 *      - Provides a single functional method for all document change events.
 *      - Fully compatible with Java 8 method references and lambdas.
 *      - Implements all required DocumentListener methods using default
 *        implementations.
 *
 *  AUTHOR
 *      PAINT Automatic Header Generator
 *
 *  MODULE
 *      paint.shared.dialogs.project
 *
 *  UPDATED
 *      2025-11-24
 *
 *  COPYRIGHT
 *      © PAINT Project. All rights reserved.
 * ============================================================================
 */

package paint.shared.dialogs.project;

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