package viewer;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.io.PrintStream;

public class TiffMoviePlayer {

    private final ImagePlus imagePlus;
    private final JLabel imageLabel;
    private final JSlider frameSlider;
    private final JLabel frameLabel;
    private final JButton playPauseButton;
    private final JSlider speedSlider;
    private final JLabel speedLabel;
    private volatile boolean playing = true;
    private volatile boolean stopped = false;
    private final int baseDelayMs;

    public TiffMoviePlayer(String tiffPath) {
        // Prevent ImageJ window
        System.setProperty("apple.awt.UIElement", "true");
        IJ.redirectErrorMessages();
        IJ.showStatus("");

        // Suppress console spam
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) {}
        }));

        this.imagePlus = IJ.openImage(tiffPath);
        System.setOut(originalOut);

        this.imageLabel = new JLabel();
        this.frameSlider = new JSlider();
        this.frameLabel = new JLabel("Frame: 0");
        this.playPauseButton = new JButton("⏸ Pause");
        this.speedSlider = new JSlider(25, 400, 100); // 25%–400%
        this.speedLabel = new JLabel("Speed: 1.0×");

        int delay = 50; // default
        if (imagePlus != null) {
            IJ.run(imagePlus, "Enhance Contrast", "saturated=0.35");
            Calibration cal = imagePlus.getCalibration();
            if (cal != null && cal.frameInterval > 0) {
                delay = (int) Math.round(cal.frameInterval * 1000);
                System.out.printf("Detected frame interval: %.1f ms%n", cal.frameInterval * 1000);
            } else {
                System.out.println("No frame interval found — using default 50 ms.");
            }
        } else {
            System.err.println("Failed to open TIFF: " + tiffPath);
        }
        this.baseDelayMs = delay;
    }

    public void show() {
        if (imagePlus == null) {
            JOptionPane.showMessageDialog(null, "Failed to open TIFF file.");
            return;
        }

        JFrame frame = new JFrame("TIFF Movie Player - " + imagePlus.getTitle());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        frame.getContentPane().add(new JScrollPane(imageLabel), BorderLayout.CENTER);
        frame.getContentPane().add(buildControls(), BorderLayout.SOUTH);

        // Auto-size to image dimensions
        int imgWidth = imagePlus.getWidth();
        int imgHeight = imagePlus.getHeight();
        int width = Math.min(imgWidth + 100, 1200);
        int height = Math.min(imgHeight + 180, 900);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> playMovie(frame)).start();
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new BorderLayout());

        int totalFrames = imagePlus.getStackSize();
        frameSlider.setMinimum(1);
        frameSlider.setMaximum(totalFrames);
        frameSlider.setValue(1);
        frameSlider.addChangeListener(e -> {
            if (!frameSlider.getValueIsAdjusting() && !playing) {
                int frame = frameSlider.getValue();
                showFrame(frame);
                frameLabel.setText("Frame: " + frame);
            }
        });

        playPauseButton.addActionListener((ActionEvent e) -> togglePlayPause());

        // Speed control slider
        speedSlider.addChangeListener(e -> {
            double speed = speedSlider.getValue() / 100.0;
            speedLabel.setText(String.format("Speed: %.2fx", speed));
        });

        JPanel topRow = new JPanel(new BorderLayout());
        JPanel left = new JPanel();
        left.add(playPauseButton);
        topRow.add(left, BorderLayout.WEST);
        topRow.add(frameSlider, BorderLayout.CENTER);
        topRow.add(frameLabel, BorderLayout.EAST);

        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.add(speedLabel, BorderLayout.WEST);
        bottomRow.add(speedSlider, BorderLayout.CENTER);

        panel.add(topRow, BorderLayout.NORTH);
        panel.add(bottomRow, BorderLayout.SOUTH);
        return panel;
    }

    private void togglePlayPause() {
        playing = !playing;
        playPauseButton.setText(playing ? "⏸ Pause" : "▶️ Play");
        if (playing) {
            new Thread(() -> playMovie(null)).start();
        }
    }

    private void playMovie(JFrame frame) {
        int totalFrames = imagePlus.getStackSize();
        while (!stopped && (frame == null || frame.isDisplayable())) {
            for (int i = frameSlider.getValue(); i <= totalFrames && (frame == null || frame.isDisplayable()); i++) {
                if (!playing) return;
                showFrame(i);
                final int currentFrame = i;
                SwingUtilities.invokeLater(() -> {
                    frameSlider.setValue(currentFrame);
                    frameLabel.setText("Frame: " + currentFrame);
                });

                try {
                    double speedFactor = speedSlider.getValue() / 100.0;
                    long delay = Math.max(1, Math.round(baseDelayMs / speedFactor));
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            frameSlider.setValue(1); // loop
        }
    }

    private void showFrame(int index) {
        imagePlus.setSlice(index);
        ImageProcessor ip = imagePlus.getProcessor();
        BufferedImage img = ip.getBufferedImage();
        SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(img)));
    }

    /** Test entry point **/
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String path = "/Volumes/Extreme Pro/Omero/221012/221012-Exp-3-A4-3.nd2"; // ← your TIFF path
            TiffMoviePlayer player = new TiffMoviePlayer(path);
            player.show();
        });
    }
}