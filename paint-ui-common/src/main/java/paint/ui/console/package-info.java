/**
 * The Swing console window.
 * <p>
 * The console attaches itself to {@code PaintLogger} as a
 * {@code PaintLogger.Sink}. The logger itself has no knowledge of Swing, so a
 * headless run (the Generate Squares pipeline, the regression gate, CI) never
 * registers a sink and never loads a UI class.
 */
package paint.ui.console;
