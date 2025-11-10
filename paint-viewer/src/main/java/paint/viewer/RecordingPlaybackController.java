package paint.viewer;

import paint.shared.utils.PaintLogger;
import paint.shared.utils.PaintPrefs;
import paint.viewer.utils.RecordingEntry;
import paint.viewer.utils.TiffMoviePlayer;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Handles playback of ND2 or TIFF files and manages UI state during playback.
 */
public final class RecordingPlaybackController {

    private final ViewerFrame frame;
    private volatile boolean playing = false;

    public RecordingPlaybackController(ViewerFrame frame) {
        this.frame = frame;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void playRecording(RecordingEntry entry) {
        if (playing) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        // Hide popups
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

        // Expected Fiji window title (filename only)
        final String expectedTitle = imagePath.getFileName().toString();

        playing = true;
        frame.disableUI();

        new Thread(() -> {
            try {
                // ✅ Ensure no old Fiji windows remain
                closeExistingMovieWindows(expectedTitle);

                // ✅ Launch movie playback
                new TiffMoviePlayer().playMovie(imagePath.toString());

                // ✅ Wait for Fiji window to appear
                Window movieWindow = waitForWindow(expectedTitle);

                if (movieWindow != null) {
                    PaintLogger.infof("Tracking movie window: %s", movieWindow.getClass().getSimpleName());

                    // ✅ Re-enable UI as soon as window closes
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

                    // Backup poll loop.
                    // Stop early if playing was already cleared by the window listener.
                    while (movieWindow.isDisplayable() && playing) {
                        Thread.sleep(200);
                    }

                    PaintLogger.infof("Movie window closed — UI re-enabled.");
                } else {
                    PaintLogger.warnf("Movie window for '%s' not detected — assuming playback finished.",
                                      expectedTitle);
                }

            } catch (Exception ex) {
                PaintLogger.errorf("Error during movie playback: %s", ex.getMessage());
                SwingUtilities.invokeLater(() ->
                                                   JOptionPane.showMessageDialog(frame,
                                                                                 "Movie playback error:\n" + ex.getMessage(),
                                                                                 "Playback Error",
                                                                                 JOptionPane.ERROR_MESSAGE));
            } finally {
                playing = false;
                SwingUtilities.invokeLater(frame::enableUI);
            }
        }, "MoviePlaybackThread").start();
    }

    // =========================================================================
    // Helper: Close any existing Fiji movie window before starting new playback
    // =========================================================================
    private void closeExistingMovieWindows(String expectedTitle) {
        for (Window w : Window.getWindows()) {
            String title = getTitle(w);
            if (title != null && title.equals(expectedTitle)) {
                PaintLogger.infof("Closing leftover movie window '%s'", title);
                w.dispose();
            }
        }
    }

    // =========================================================================
    // Helper: Wait up to 10 seconds for the window to appear
    // =========================================================================
    private Window waitForWindow(String expectedTitle) throws InterruptedException {
        Window found = null;

        for (int i = 0; i < 40 && found == null; i++) {
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

    // =========================================================================
    // Helper: Extract title from Window/Frame/Dialog
    // =========================================================================
    private String getTitle(Window w) {
        if (w instanceof Frame)  return ((Frame) w).getTitle();
        if (w instanceof Dialog) return ((Dialog) w).getTitle();
        return null;
    }
}