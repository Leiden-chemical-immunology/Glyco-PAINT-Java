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
    private boolean playing = false;

    public RecordingPlaybackController(ViewerFrame frame) {
        this.frame = frame;
    }

    public boolean isPlaying() {
        return playing;
    }

    /**
     * Starts playback of the given recording entry. This method disables the frame’s
     * UI, launches the movie in a background thread, and re‑enables the UI when finished.
     */
    public void playRecording(RecordingEntry entry) {
        if (playing) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        frame.getLeftGridPanel().hideSquareInfoIfVisible();

        // Determine the ND2/TIFF file to play
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
        Path imagePath        = imagesRoot.resolve(experimentName).resolve(recordingName + ".nd2");
        if (!Files.exists(imagePath)) {
            JOptionPane.showMessageDialog(frame,
                                          "Recording file not found:\n" + imagePath,
                                          "File Missing",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }

        playing = true;
        frame.disableUI();

        // Launch playback in a separate thread
        new Thread(() -> {
            try {
                new TiffMoviePlayer().playMovie(imagePath.toString());

                // Monitor open windows to re‑enable UI as soon as the player closes
                String expectedTitle = imagePath.getFileName().toString();
                Window targetWindow  = null;
                for (int i = 0; i < 40 && targetWindow == null; i++) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException ignored) {}
                    for (Window w : Window.getWindows()) {
                        String title = (w instanceof Frame)  ? ((Frame) w).getTitle()
                                : (w instanceof Dialog) ? ((Dialog) w).getTitle()
                                : null;
                        if (title != null && title.contains(expectedTitle)) {
                            targetWindow = w;
                            break;
                        }
                    }
                }
                if (targetWindow != null) {
                    Window finalTarget = targetWindow;
                    finalTarget.addWindowListener(new java.awt.event.WindowAdapter() {
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
                    while (finalTarget.isDisplayable()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ignored) {}
                    }
                }
            } catch (Exception ex) {
                PaintLogger.errorf("Error during movie playback: %s", ex.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        frame,
                        "Movie playback error:\n" + ex.getMessage(),
                        "Playback Error",
                        JOptionPane.ERROR_MESSAGE));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    playing = false;
                    frame.enableUI();
                });
            }
        }, "MoviePlaybackThread").start();
    }
}