/******************************************************************************
 *  Class:        TiffMoviePlayer.java
 *  Package:      paint.viewer.utils
 *
 *  PURPOSE:
 *    Provides an interactive viewer for playing multi-frame TIFF recordings
 *    as time-lapse movies within a graphical interface.
 *
 *  DESCRIPTION:
 *    The {@code TiffMoviePlayer} loads and displays multi-frame TIFF image
 *    stacks using ImageJ’s core libraries, allowing users to control playback
 *    speed, pause, and navigate through frames.
 *
 *    It combines ImageJ for data handling with Swing for GUI rendering,
 *    featuring lightweight playback controls and frame navigation.
 *
 *  KEY FEATURES:
 *    • Loads and plays multi-frame TIFF image stacks.
 *    • Adjustable playback speed and pause/resume control.
 *    • Manual frame navigation via a slider.
 *    • Displays a simple loading dialog during image preparation.
 *    • Uses ImageJ for robust TIFF handling and calibration-based timing.
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
 ******************************************************************************/

package paint.viewer.utils;

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

/**
 * Provides functionality for playing multi-frame TIFF files as movie-like sequences.
 * <p>
 * The {@code TiffMoviePlayer} uses ImageJ for image handling and Swing for the GUI.
 * It includes playback controls such as play/pause, speed adjustment, and frame navigation.
 * </p>
 */
public class TiffMoviePlayer {

    /**
     * Plays a multi-frame TIFF file as a movie sequence.
     *
     * @param tiffPath the absolute or relative file path of the TIFF image stack
     */
    public void playMovie(String tiffPath) {
        final String fileName = new File(tiffPath).getName();

        // --- Simple static loading dialog (no progress bar) ---
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

        // --- Load + show UI on background thread ---
        new Thread(() -> {
            System.setProperty("apple.awt.UIElement", "true");
            IJ.redirectErrorMessages();
            IJ.showStatus("");

            // Silence ImageJ console
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(new OutputStream() {
                @Override
                public void write(int b) {
                }
            }));

            final ImagePlus imp = IJ.openImage(tiffPath);

            System.setOut(originalOut);
            SwingUtilities.invokeLater(loadingDialog::dispose);

            if (imp == null) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null,
                        "Failed to open image file:\n" + tiffPath,
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                ));
                return;
            }

            IJ.run(imp, "Enhance Contrast", "saturated=0.35");

            int delay = 50;
            Calibration cal = imp.getCalibration();
            if (cal != null && cal.frameInterval > 0) {
                delay = (int) Math.round(cal.frameInterval * 1000);
            }
            final int baseDelayMs = delay;

            SwingUtilities.invokeLater(() -> {
                final JFrame frame = new JFrame("Movie Player - " + fileName);
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setLayout(new BorderLayout());

                final JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
                imageLabel.setOpaque(true);
                imageLabel.setBackground(Color.DARK_GRAY);

                JPanel imagePanel = new JPanel(new BorderLayout());
                imagePanel.setBackground(Color.DARK_GRAY);
                imagePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
                imagePanel.add(imageLabel, BorderLayout.CENTER);
                frame.add(imagePanel, BorderLayout.CENTER);

                // @formatter:off
                final int totalFrames         = imp.getStackSize();
                final JSlider frameSlider     = new JSlider(1, totalFrames, 1);
                final JLabel frameLabel       = new JLabel("Frame: 1");
                final JButton playPauseButton = new JButton("⏸ Pause");
                final JSlider speedSlider     = new JSlider(50, 400, 100);
                final JLabel speedLabel       = new JLabel("Speed: 1.0×");
                // @formatter:on

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

                // --- Add Close button below controls ---
                JButton closeButton = new JButton("Close");
                closeButton.addActionListener(e -> {
                    frame.dispose(); // closes the window
                });
                JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
                closePanel.setBackground(controls.getBackground());
                closePanel.add(closeButton);

                JPanel bottomPanel = new JPanel(new BorderLayout());
                bottomPanel.add(controls, BorderLayout.CENTER);
                bottomPanel.add(closePanel, BorderLayout.SOUTH);

                frame.add(bottomPanel, BorderLayout.SOUTH);

                // --- Show frame ---
                frame.setSize(Math.min(imp.getWidth() + 40, 1000),
                              Math.min(imp.getHeight() + 180, 900));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                final boolean[] playing = {true};
                final int[] currentFrame = {1};

                playPauseButton.addActionListener(e -> {
                    playing[0] = !playing[0];
                    playPauseButton.setText(playing[0] ? "⏸ Pause" : "▶️ Play");
                });

                speedSlider.addChangeListener(e -> {
                    double raw = speedSlider.getValue() / 100.0;
                    double speed = Math.round(raw * 2) / 2.0;
                    speedLabel.setText(String.format("Speed: %.1fx", speed));
                });

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

                ImageProcessor ip0 = imp.getStack().getProcessor(1);
                imageLabel.setIcon(new ImageIcon(ip0.getBufferedImage()));

                new Thread(() -> {
                    while (frame.isVisible()) {
                        if (playing[0]) {
                            int frameIdx = currentFrame[0];
                            ImageProcessor ip = imp.getStack().getProcessor(frameIdx);
                            final BufferedImage img = ip.getBufferedImage();
                            final int finalFrameIdx = frameIdx;

                            SwingUtilities.invokeLater(() -> {
                                imageLabel.setIcon(new ImageIcon(img));
                                frameLabel.setText("Frame: " + finalFrameIdx);
                                frameSlider.setValue(finalFrameIdx);
                            });

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
                            } catch (InterruptedException ignored) {
                            }

                            currentFrame[0]++;
                            if (currentFrame[0] > totalFrames) {
                                currentFrame[0] = 1;
                            }
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ignored) {
                            }
                        }
                    }
                }, "TiffMoviePlaybackThread").start();
            });
        }, "TiffLoaderThread").start();
    }

    /**
     * Manual test entry point for running the TIFF player independently.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TiffMoviePlayer().playMovie("/Volumes/Extreme Pro/Omero/221012/221012-Exp-3-A4-3.tif"));
    }
}