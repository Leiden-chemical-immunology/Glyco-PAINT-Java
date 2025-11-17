/*==============================================================================
 *  Class:        RecordingPlaybackController.java
 *  Package:      paint.viewer.ui
 *
 *  PURPOSE:
 *    Controls playback of ND2 or TIFF recordings and manages the state of the
 *    PAINT Viewer UI during playback. Ensures that only one movie window
 *    exists at a time, and automatically disables and re-enables the viewer UI
 *    around the external Fiji playback window.
 *
 *  DESCRIPTION:
 *    This controller launches playback via {@link paint.viewer.io.TiffMoviePlayer}
 *    inside a background thread. Before starting a new playback session, it closes
 *    any existing Fiji movie window with the same filename to avoid duplicates.
 *
 *    When playback begins, the ViewerFrame UI is disabled. The controller waits
 *    for Fiji to create the window that displays the movie, adds close listeners,
 *    and restores the UI when the movie window closes.
 *
 *    Due to rare inconsistent behaviors in Fiji, a small backup timeout is used
 *    to ensure the UI is restored even if Fiji does not emit window events.
 *
 *  KEY FEATURES:
 *    • Guarantees single-window playback.
 *    • Ensures UI is disabled during playback and restored afterward.
 *    • Detects and closes leftover Fiji windows before playback.
 *    • Thread-safe use of SwingUtilities for UI changes.
 *    • Robust fallback logic in case Fiji does not fire close events.
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
 *==============================================================================*/

package paint.viewer.ui;

import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;
import paint.viewer.model.RecordingEntry;
import paint.viewer.io.TiffMoviePlayer;
import paint.viewer.ui.frames.ViewerFrame;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller responsible for playing ND2/TIFF recordings through Fiji and keeping
 * the PAINT Viewer UI in a consistent state during playback.
 *
 * <p>This class prevents concurrent playback, launches the movie in a background
 * thread, disables the entire ViewerFrame UI during playback, and re-enables it
 * as soon as the Fiji playback window closes (or after a backup timeout).</p>
 *
 * <p>Use {@link #playRecording(RecordingEntry)} to start playback.</p>
 */
public final class RecordingPlaybackController {

    private final ViewerFrame frame;
    private volatile boolean playing = false;

    /**
     * Creates a new Playback Controller bound to a given {@link ViewerFrame}.
     *
     * @param frame the viewer frame whose UI will be disabled/enabled during playback
     */
    public RecordingPlaybackController(ViewerFrame frame) {
        this.frame = frame;
    }

    /**
     * @return {@code true} if a playback session is currently active.
     */
    public boolean isPlaying() {
        return playing;
    }

    /**
     * Starts playback of the given recording. This method:
     *
     * <ol>
     *   <li>Ensures only one playback session can run at the same time.</li>
     *   <li>Closes any leftover Fiji window from previous runs.</li>
     *   <li>Determines the correct ND2/TIFF path.</li>
     *   <li>Disables the ViewerFrame UI.</li>
     *   <li>Launches Fiji playback on a background thread.</li>
     *   <li>Waits for the Fiji window and attaches listeners for close events.</li>
     *   <li>Restores UI when playback finishes or times out.</li>
     * </ol>
     *
     * @param entry the recording to play
     */
    public void playRecording(RecordingEntry entry) {
        if (playing) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        // Hide info popup before playback begins
        frame.getLeftGridPanel().hideSquareInfoIfVisible();

        // Determine the images root directory
        Path imagesRoot = frame.getProject().getImagesRootPath();
        if (imagesRoot == null) {
            String stored = PaintPrefs.getString("Path", "Images Root", "");
            if (stored != null && !stored.isEmpty()) {
                imagesRoot = Paths.get(stored);
            } else {
                JOptionPane.showMessageDialog(
                        frame,
                        "No Images Root is defined.\nPlease set it in the Project Specification dialog.",
                        "Configuration Error",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        String experimentName = entry.getExperimentName();
        String recordingName  = entry.getRecordingName();

        // Build full ND2/TIFF path
        Path imagePath = imagesRoot.resolve(experimentName).resolve(recordingName + ".nd2");
        if (!Files.exists(imagePath)) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Recording file not found:\n" + imagePath,
                    "File Missing",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Expected title of the Fiji display window (Fiji uses filename)
        final String expectedTitle = imagePath.getFileName().toString();

        // Mark playback active and disable UI
        playing = true;
        frame.disableUI();

        // Launch playback in background thread
        new Thread(() -> {
            try {
                // 1) Close leftovers from prior playback attempts
                closeExistingMovieWindows(expectedTitle);

                // 2) Launch movie in Fiji
                new TiffMoviePlayer().playMovie(imagePath.toString());

                // 3) Wait for Fiji to open the window
                Window movieWindow = waitForWindow(expectedTitle);

                if (movieWindow != null) {
                    PaintLogger.infof("Tracking movie window: %s", movieWindow.getClass().getSimpleName());

                    movieWindow.addWindowListener(new java.awt.event.WindowAdapter() {

                        @Override
                        public void windowClosed(java.awt.event.WindowEvent e) {
                            SwingUtilities.invokeLater(() -> {
                                playing = false;
                                frame.enableUI();
                            });
                        }

                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            SwingUtilities.invokeLater(() -> {
                                playing = false;
                                frame.enableUI();
                            });
                        }
                    });

                    // Backup in case Fiji never fires the event
                    Thread.sleep(200);
                    if (playing) {
                        SwingUtilities.invokeLater(() -> {
                            playing = false;
                            frame.enableUI();
                        });
                    }

                    PaintLogger.infof("Movie window closed — UI re-enabled.");
                } else {
                    // Fiji never created the window or closed before detection
                    playing = false;
                    SwingUtilities.invokeLater(frame::enableUI);
                }

            } catch (Exception ex) {
                PaintLogger.errorf("Error during movie playback: %s", ex.getMessage());
                SwingUtilities.invokeLater(() ->
                                                   JOptionPane.showMessageDialog(
                                                           frame,
                                                           "Movie playback error:\n" + ex.getMessage(),
                                                           "Playback Error",
                                                           JOptionPane.ERROR_MESSAGE
                                                   )
                );
            } finally {
                // Final safety catch to ensure UI is restored
                playing = false;
                SwingUtilities.invokeLater(frame::enableUI);
            }
        }, "MoviePlaybackThread").start();
    }

    // -------------------------------------------------------------------------
    // Internal helper methods
    // -------------------------------------------------------------------------

    /**
     * Closes any existing Fiji movie window whose title matches the expected file.
     *
     * @param expectedTitle the filename-based title Fiji uses
     */
    private void closeExistingMovieWindows(String expectedTitle) {
        for (Window w : Window.getWindows()) {
            String title = getTitle(w);
            if (title != null && title.equals(expectedTitle)) {
                PaintLogger.infof("Closing leftover movie window '%s'", title);
                w.dispose();
            }
        }
    }

    /**
     * Waits up to approximately 10 seconds for Fiji to open a playback window
     * whose title matches the expected filename.
     *
     * @param expectedTitle expected title of the Fiji window
     * @return the detected window, or {@code null} if none was found
     * @throws InterruptedException if sleep is interrupted
     */
    private Window waitForWindow(String expectedTitle) throws InterruptedException {
        Window found = null;
        for (int i = 0; i < 40 && found == null; i++) { // 40 × 250 ms = ~10 s
            Thread.sleep(250);
            for (Window w : Window.getWindows()) {
                String title = getTitle(w);
                if (title != null && title.equals(expectedTitle)) {
                    found = w;
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Returns the window title if it is a {@link Frame} or {@link Dialog}.
     *
     * @param w the window to inspect
     * @return window title or {@code null} if unavailable
     */
    private String getTitle(Window w) {
        if (w instanceof Frame)  return ((Frame) w).getTitle();
        if (w instanceof Dialog) return ((Dialog) w).getTitle();
        return null;
    }
}