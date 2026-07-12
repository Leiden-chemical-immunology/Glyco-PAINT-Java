/*==============================================================================
 *  Class:        TiffMoviePlayer.java
 *  Package:      paint.viewer.io
 *
 *  PURPOSE:
 *    Provides an ImageJ-powered movie viewer for multi-frame TIFF recordings.
 *    Allows playback, pausing, speed control, and manual frame navigation.
 *
 *  DESCRIPTION:
 *    The {@code TiffMoviePlayer} loads multi-frame TIFF files using ImageJ
 *    and renders them inside a Swing window. A lightweight control panel enables:
 *
 *      • Play / Pause toggling
 *      • Frame navigation via slider
 *      • Playback speed adjustment (0.5× – 4×)
 *      • Clean closing of movie windows
 *
 *    A short loading dialog is displayed while images are prepared in a
 *    background thread. All UI rendering occurs on the Swing EDT as required.
 *
 *  KEY FEATURES:
 *    • Plays TIFF stacks as movies using ImageJ processors.
 *    • Uses calibration metadata (frame interval) when available.
 *    • Thread-safe interplay between worker threads and Swing.
 *    • Graceful cleanup when the movie window is closed.
 *    • Suppresses ImageJ console output for clean UI integration.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
==============================================================================*/

package paint.viewer.io;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import paint.shared.utils.PaintLogger;

/**
 * A lightweight GUI-based TIFF stack movie player built on ImageJ and Swing.
 * <p>
 * Loads a TIFF file, extracts frame timing, and provides a movie player with:
 * play/pause, playback speed control, and frame navigation. Rendering is
 * performed on Swing components, while TIFF handling is delegated to ImageJ.
 * </p>
 */
public class TiffMoviePlayer {

    /**
     * Loads and plays a multi-frame TIFF stack in a dedicated Swing window.
     *
     * @param tiffPath absolute or relative path to the TIFF file
     */
    public void playMovie(String tiffPath) {
        playMovie(tiffPath, null);
    }

    /**
     * Loads and plays a multi-frame TIFF stack in a dedicated Swing window, notifying the
     * caller when playback has finished.
     * <p>
     * This method returns immediately: the TIFF is loaded on a background thread and the
     * player window is built afterwards on the EDT. {@code onFinished} is therefore the only
     * reliable way for a caller to know when the player is gone.
     * </p>
     * <p>
     * {@code onFinished} is invoked exactly once, on the EDT, whichever way playback ends:
     * when the user closes the player window, or immediately if the TIFF cannot be loaded.
     * Callers that disable UI while a movie plays can rely on it always being called back,
     * and so on never being left permanently disabled.
     * </p>
     *
     * @param tiffPath   absolute or relative path to the TIFF file
     * @param onFinished run on the EDT when the player window closes or loading fails;
     *                   may be {@code null}
     */
    public void playMovie(String tiffPath, Runnable onFinished) {

        // Guarantee "exactly once", however playback ends.
        final AtomicBoolean finishedFired = new AtomicBoolean(false);
        final Runnable finish = () -> {
            if (onFinished != null && finishedFired.compareAndSet(false, true)) {
                SwingUtilities.invokeLater(onFinished);
            }
        };

        final String fileName = new File(tiffPath).getName();

        // ---------------------------------------------------------------------
        // LOADING DIALOG (lightweight splash while ImageJ loads the TIFF)
        // ---------------------------------------------------------------------
        final JDialog loadingDialog = new JDialog((Frame) null, "Loading Recording", false);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setLayout(new BorderLayout());

        JLabel label = new JLabel("Loading " + fileName + "…", SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setBorder(new EmptyBorder(25, 20, 25, 20));
        loadingDialog.add(label, BorderLayout.CENTER);

        loadingDialog.setSize(300, 120);
        loadingDialog.setResizable(false);
        loadingDialog.setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));

        // ==========================================================================
        // BACKGROUND THREAD: load TIFF, apply contrast enhancement, open UI window
        // ==========================================================================
        new Thread(() -> {

            System.setProperty("apple.awt.UIElement", "true");
            IJ.redirectErrorMessages();
            IJ.showStatus("");

            // Silence ImageJ's console
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) { /* ignore console output */ }
            }));

            ImagePlus imp;
            try {
                imp = IJ.openImage(tiffPath);

                if (imp == null) {
                    try {
                        ImporterOptions opts = new ImporterOptions();
                        opts.setId(tiffPath);
                        opts.setAutoscale(true);
                        opts.setStackOrder(ImporterOptions.ORDER_XYCZT);

                        ImagePlus[] imps = BF.openImagePlus(opts);
                        if (imps != null && imps.length > 0) {
                            imp = imps[0];
                        }
                    } catch (Exception bfErr) {
                        PaintLogger.error("Failed to open image with Bio-Formats", bfErr);
                    }
                }
            } finally {
                // Always restore stdout, even if opening throws, so the process's
                // console output isn't left permanently silenced.
                System.setOut(originalOut);
            }

            // Close loading dialog
            SwingUtilities.invokeLater(loadingDialog::dispose);

            if (imp == null) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null,
                        "Failed to open image file:\n" + tiffPath,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                ));
                finish.run();   // no window will ever open: release the caller now
                return;
            }

            // Optional contrast enhancement
            IJ.run(imp, "Enhance Contrast", "saturated=0.35");

            // Determine playback speed using calibration metadata
            int delay = 50; // fallback
            Calibration cal = imp.getCalibration();
            if (cal != null && cal.frameInterval > 0) {
                delay = (int) Math.round(cal.frameInterval * 1000);
            }
            final int baseDelayMs = delay;

            // ------------------------------------------------------------
            // UI CONSTRUCTION — must occur on the EDT
            // ------------------------------------------------------------

            final ImagePlus impFinal       = imp;

            SwingUtilities.invokeLater(() -> buildAndRunMovieWindow(impFinal, fileName, baseDelayMs, finish));

        }, "TiffLoaderThread").start();
    }

    // =============================================================================================
    // INTERNAL: Build movie UI window + player loop
    // =============================================================================================
    private void buildAndRunMovieWindow(ImagePlus imp, String fileName, int baseDelayMs, Runnable onFinished) {

        final JFrame frame = new JFrame("Movie Player - " + fileName);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Tell the caller when this window is gone. The playback thread below stops on its own,
        // because it loops on frame.isVisible().
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                onFinished.run();
            }
        });

        // ----- Image display area -----
        final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.DARK_GRAY);

        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(Color.DARK_GRAY);
        imagePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        frame.add(imagePanel, BorderLayout.CENTER);

        // ----- Movie metadata -----
        final int totalFrames         = imp.getStackSize();
        final JSlider frameSlider     = new JSlider(1, totalFrames, 1);
        final JLabel frameLabel       = new JLabel("Frame: 1");
        final JButton playPauseButton = new JButton("⏸ Pause");
        final JSlider speedSlider     = new JSlider(50, 400, 100); // 0.5× – 4.0×
        final JLabel speedLabel       = new JLabel("Speed: 1.0×");

        // ----- Layout for controls -----
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 3));
        controls.setBorder(new EmptyBorder(6, 6, 6, 6));
        controls.setBackground(new Color(245, 245, 245));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        topRow.setBackground(controls.getBackground());
        frameSlider.setPreferredSize(new Dimension(260, 25));
        topRow.add(playPauseButton);
        topRow.add(frameSlider);
        topRow.add(frameLabel);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        bottomRow.setBackground(controls.getBackground());
        speedSlider.setPreferredSize(new Dimension(100, 25));
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        bottomRow.add(speedLabel);
        bottomRow.add(speedSlider);

        controls.add(topRow);
        controls.add(bottomRow);

        // ----- Close button -----
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> frame.dispose());

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        closePanel.setBackground(controls.getBackground());
        closePanel.add(closeButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controls, BorderLayout.CENTER);
        bottomPanel.add(closePanel, BorderLayout.SOUTH);

        frame.add(bottomPanel, BorderLayout.SOUTH);

        // ----- Initial layout -----
        frame.setSize(
                Math.min(imp.getWidth() + 40, 1000),
                Math.min(imp.getHeight() + 180, 900)
        );
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // =======================================================================================
        // Player state + listeners
        // =======================================================================================
        final boolean[] playing = {true};
        final int[] currentFrame = {1};

        // Toggle play/pause
        playPauseButton.addActionListener(e -> {
            playing[0] = !playing[0];
            playPauseButton.setText(playing[0] ? "⏸ Pause" : "▶️ Play");
        });

        // Playback speed label
        speedSlider.addChangeListener(e -> {
            double raw = speedSlider.getValue() / 100.0;
            double speed = Math.round(raw * 2) / 2.0;  // step of 0.5×
            speedLabel.setText(String.format("Speed: %.1fx", speed));
        });

        // Manual frame navigation (only when paused)
        frameSlider.addChangeListener(e -> {
            if (!frameSlider.getValueIsAdjusting() && !playing[0]) {
                final int frameIndex = frameSlider.getValue();
                ImageProcessor ip2 = imp.getStack().getProcessor(frameIndex);
                final BufferedImage img2 = ip2.getBufferedImage();
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setIcon(new ImageIcon(img2));
                    frameLabel.setText("Frame: " + frameIndex);
                });
                currentFrame[0] = frameIndex;
            }
        });

        // Show frame 1 immediately
        ImageProcessor ip0 = imp.getStack().getProcessor(1);
        imageLabel.setIcon(new ImageIcon(ip0.getBufferedImage()));

        // =======================================================================================
        // MOVIE PLAYBACK THREAD
        // =======================================================================================
        new Thread(() -> {
            while (frame.isVisible()) {

                if (playing[0]) {
                    int frameIdx = currentFrame[0];
                    ImageProcessor ip = imp.getStack().getProcessor(frameIdx);
                    final BufferedImage img = ip.getBufferedImage();

                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(new ImageIcon(img));
                        frameLabel.setText("Frame: " + frameIdx);
                        frameSlider.setValue(frameIdx);
                    });

                    // Speed multiplier
                    double raw = speedSlider.getValue() / 100.0;
                    double speed = Math.round(raw * 2) / 2.0;
                    if (speed <= 0.0) {
                        speed = 0.5;
                    }

                    long sleepTime = (long) (baseDelayMs / speed);
                    if (sleepTime < 5) {
                        sleepTime = 5;
                    }

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ignored) { }

                    // Next frame
                    currentFrame[0]++;
                    if (currentFrame[0] > totalFrames) {
                        currentFrame[0] = 1;
                    }

                } else {
                    // Paused: reduce CPU usage
                    try { Thread.sleep(100); }
                    catch (InterruptedException ignored) { }
                }
            }
        }, "TiffMoviePlaybackThread").start();
    }

    /**
     * Manual test runner for local development.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                                           new TiffMoviePlayer().playMovie(
                                                   "/Volumes/Extreme Pro/Omero/221012/221012-Exp-3-A4-3.tif"
                                           ));
    }
}