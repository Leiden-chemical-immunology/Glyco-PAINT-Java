/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

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
 * Controller responsible for playing ND2/TIFF recordings and keeping the PAINT Viewer
 * UI in a consistent state during playback.
 *
 * <p>This class prevents concurrent playback, disables the entire ViewerFrame UI while a
 * recording plays, and re-enables it as soon as the player window closes.</p>
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
     *   <li>Beeps and returns if a recording is already playing.</li>
     *   <li>Resolves the recording's image file under the project's Images Root.</li>
     *   <li>Disables the ViewerFrame UI and opens the player.</li>
     *   <li>Re-enables the UI when the player window closes, or if loading fails.</li>
     * </ol>
     *
     * <p>Returns as soon as the player has been started; playback itself is asynchronous.</p>
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

        // The file can exist and still not be readable. On macOS the images usually live on an
        // external drive, and access to removable volumes is gated: the file is visible, but
        // opening it fails with "Operation not permitted". Say so here, rather than letting the
        // image loader report it as an obscure Bio-Formats failure.
        if (!Files.isReadable(imagePath)) {
            PaintLogger.errorf("Not permitted to read %s", imagePath);
            JOptionPane.showMessageDialog(
                    frame,
                    "The recording exists but cannot be read:\n" + imagePath
                            + "\n\nIf it is on an external drive, macOS may be blocking access."
                            + "\nGrant the application access under System Settings > Privacy &"
                            + " Security > Files and Folders (Removable Volumes), or Full Disk"
                            + " Access, and restart it.",
                    "Cannot Read Recording",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Mark playback active and disable the viewer UI. TiffMoviePlayer calls us back on the
        // EDT when its window closes (or immediately if the file cannot be loaded), so the UI
        // is always re-enabled exactly once.
        //
        // This used to work by polling Window.getWindows() for up to 10 seconds looking for a
        // window whose title equalled the *file name*. TiffMoviePlayer titles its window
        // "Movie Player - <file name>", so the match never succeeded: the viewer sat disabled
        // for the full 10 s of polling, the close listeners were never attached, and the UI was
        // only restored by the fall-through at the end.
        playing = true;
        frame.disableUI();

        try {
            new TiffMoviePlayer().playMovie(imagePath.toString(), () -> {
                playing = false;
                frame.enableUI();
            });
        } catch (RuntimeException ex) {
            // playMovie only *starts* playback, so this catches a failure to launch, not a
            // playback error. Re-enable the UI: no callback is coming.
            playing = false;
            frame.enableUI();
            PaintLogger.error("Could not start movie playback", ex);
            JOptionPane.showMessageDialog(
                    frame,
                    "Movie playback error:\n" + ex.getMessage(),
                    "Playback Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}