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
 * Handles playback of ND2 or TIFF files and manages UI state during playback.
 * Guarantees: only one movie window at a time; UI is disabled during playback
 * and re-enabled as soon as the Fiji window closes (listener + backup poll).
 */
public final class RecordingPlaybackController {

    private final    ViewerFrame frame;
    private volatile boolean     playing = false;

    public RecordingPlaybackController(ViewerFrame frame) {
        this.frame = frame;
    }

    public boolean isPlaying() {
        return playing;
    }

    /**
     * Starts playback of the given recording entry. Disables the frame UI,
     * launches the movie in a background thread, and re-enables the UI when finished.
     * Also closes any leftover Fiji window from a previous run that has the same title.
     */
    public void playRecording(RecordingEntry entry) {
        if (playing) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        // Hide popups on the grid before launching
        frame.getLeftGridPanel().hideSquareInfoIfVisible();

        // Resolve images root
        Path imagesRoot = frame.getProject().getImagesRootPath();
        if (imagesRoot == null) {
            String stored = PaintPrefs.getString("Path", "Images Root", "");
            if (stored != null && !stored.isEmpty()) {
                imagesRoot = Paths.get(stored);
            } else {
                JOptionPane.showMessageDialog(frame,
                                              "No Images Root is defined.\nPlease set it in the Project Specification dialog.",
                                              "Configuration Error",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String experimentName = entry.getExperimentName();
        String recordingName  = entry.getRecordingName();

        Path imagePath = imagesRoot.resolve(experimentName).resolve(recordingName + ".nd2");
        if (!Files.exists(imagePath)) {
            JOptionPane.showMessageDialog(frame,
                                          "Recording file not found:\n" + imagePath,
                                          "File Missing",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        // The Fiji window title we expect is the filename (e.g., "foo.nd2")
        final String expectedTitle = imagePath.getFileName().toString();

        playing = true;
        frame.disableUI();

        new Thread(() -> {
            try {
                // 1) Close any leftover Fiji window with the same title (from a previous run)
                closeExistingMovieWindows(expectedTitle);

                // 2) Launch movie
                new TiffMoviePlayer().playMovie(imagePath.toString());

                // 3) Wait for Fiji window to appear so we can listen for close events
                Window movieWindow = waitForWindow(expectedTitle);

                if (movieWindow != null) {
                    PaintLogger.infof("Tracking movie window: %s", movieWindow.getClass().getSimpleName());

                    // Re-enable UI immediately when Fiji window closes
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

                    // Backup: stop after 200 ms if listener did not fire (rare Fiji bug).
                    Thread.sleep(200);
                    if (playing) {
                        SwingUtilities.invokeLater(() -> {
                            playing = false;
                            frame.enableUI();
                        });
                    }

                    PaintLogger.infof("Movie window closed — UI re-enabled.");
                } else {
                    // If we never detected the window, still flip the UI back on.
                    // PaintLogger.warnf("Movie window for '%s' not detected — assuming playback finished.", expectedTitle);
                }

            } catch (Exception ex) {
                PaintLogger.errorf("Error during movie playback: %s", ex.getMessage());
                SwingUtilities.invokeLater(() ->
                                                   JOptionPane.showMessageDialog(frame,
                                                                                 "Movie playback error:\n" + ex.getMessage(),
                                                                                 "Playback Error",
                                                                                 JOptionPane.ERROR_MESSAGE));
            } finally {
                // Ensure UI is re-enabled even if something went wrong
                playing = false;
                SwingUtilities.invokeLater(frame::enableUI);
            }
        }, "MoviePlaybackThread").start();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    /** Close any existing Fiji movie window with exactly the same title. */
    private void closeExistingMovieWindows(String expectedTitle) {
        for (Window w : Window.getWindows()) {
            String title = getTitle(w);
            if (title != null && title.equals(expectedTitle)) {
                PaintLogger.infof("Closing leftover movie window '%s'", title);
                w.dispose();
            }
        }
    }

    /** Wait up to ~10 seconds for a window whose title exactly matches expectedTitle. */
    private Window waitForWindow(String expectedTitle) throws InterruptedException {
        Window found = null;
        for (int i = 0; i < 40 && found == null; i++) { // 40 * 250ms = 10s
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

    /** Extracts a title from Frame/Dialog windows. */
    private String getTitle(Window w) {
        if (w instanceof Frame)  return ((Frame) w).getTitle();
        if (w instanceof Dialog) return ((Dialog) w).getTitle();
        return null;
    }
}