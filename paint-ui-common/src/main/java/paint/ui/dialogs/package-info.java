/**
 * Shared Swing dialogs: the project dialog and the interactive project-path chooser.
 * <p>
 * These live outside {@code paint-shared-utils} on purpose. Every module depends on
 * shared-utils — including headless ones such as the Generate Squares pipeline, the
 * regression gate and {@code paint-compare} — so the base layer must contain no UI.
 * Only applications that actually show an interface depend on {@code paint-ui-common}.
 */
package paint.ui.dialogs;
