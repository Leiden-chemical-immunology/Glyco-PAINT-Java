package viewer;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
        System.setProperty("apple.awt.UIElement", "true");
        IJ.redirectErrorMessages();
        IJ.showStatus("");

        // Silence ImageJ output
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override public void write(int b) {}
        }));

        this.imagePlus = IJ.openImage(tiffPath);
        System.setOut(originalOut);

        this.imageLabel = new JLabel("", SwingConstants.CENTER);
        this.imageLabel.setBackground(Color.DARK_GRAY);
        this.imageLabel.setOpaque(true);
        this.imageLabel.setBorder(null);

        this.frameSlider = new JSlider();
        this.frameLabel = new JLabel("Frame: 0");
        this.playPauseButton = new JButton("⏸ Pause");
        this.speedSlider = new JSlider(50, 400, 100);
        this.speedLabel = new JLabel("Speed: 1.0×");

        int delay = 50;
        if (imagePlus != null) {
            IJ.run(imagePlus, "Enhance Contrast", "saturated=0.35");
            Calibration cal = imagePlus.getCalibration();
            if (cal != null && cal.frameInterval > 0) {
                delay = (int) Math.round(cal.frameInterval * 1000);
            }
        }
        this.baseDelayMs = delay;
    }

    public void show() {
        if (imagePlus == null) {
            JOptionPane.showMessageDialog(null, "Failed to open image file.");
            return;
        }

        JFrame frame = new JFrame("Movie Player - " + imagePlus.getTitle());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        // Tight 4px gray frame, no layout expansion
        JPanel imagePanel = new JPanel(new BorderLayout());
        imagePanel.setBackground(Color.DARK_GRAY);
        imagePanel.setBorder(new EmptyBorder(4, 4, 4, 4));
        imagePanel.add(imageLabel, BorderLayout.CENTER);
        frame.getContentPane().add(imagePanel, BorderLayout.CENTER);

        frame.getContentPane().add(buildControls(), BorderLayout.SOUTH);

        int imgWidth = imagePlus.getWidth();
        int imgHeight = imagePlus.getHeight();
        int width = Math.min(imgWidth + 40, 1000);
        int height = Math.min(imgHeight + 140, 850);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        new Thread(() -> playMovie(frame)).start();
    }

    private JPanel buildControls() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 3));
        panel.setBorder(new EmptyBorder(6, 6, 6, 6));
        panel.setBackground(new Color(245, 245, 245));

        int totalFrames = imagePlus.getStackSize();
        frameSlider.setMinimum(1);
        frameSlider.setMaximum(totalFrames);
        frameSlider.setValue(1);
        frameSlider.setPreferredSize(new Dimension(260, 25));
        frameSlider.addChangeListener(e -> {
            if (!frameSlider.getValueIsAdjusting() && !playing) {
                int frame = frameSlider.getValue();
                showFrame(frame);
                frameLabel.setText("Frame: " + frame);
            }
        });

        playPauseButton.addActionListener((ActionEvent e) -> togglePlayPause());

        // Speed control slider (0.5× increments)
        speedSlider.setPreferredSize(new Dimension(100, 25));
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(e -> {
            double speed = Math.round(speedSlider.getValue() / 50.0) * 0.5;
            speedLabel.setText(String.format("Speed: %.1fx", speed));
        });

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        topRow.setBackground(panel.getBackground());
        topRow.add(playPauseButton);
        topRow.add(frameSlider);
        topRow.add(frameLabel);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        bottomRow.setBackground(panel.getBackground());
        bottomRow.add(speedLabel);
        bottomRow.add(speedSlider);

        panel.add(topRow);
        panel.add(bottomRow);
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
                    double speedFactor = Math.round(speedSlider.getValue() / 50.0) * 0.5;
                    long delay = Math.max(1, Math.round(baseDelayMs / speedFactor));
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            frameSlider.setValue(1);
        }
    }

    private void showFrame(int index) {
        imagePlus.setSlice(index);
        ImageProcessor ip = imagePlus.getProcessor();
        BufferedImage img = ip.getBufferedImage();
        SwingUtilities.invokeLater(() -> imageLabel.setIcon(new ImageIcon(img)));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String path = "/Volumes/Extreme Pro/Omero/221012/221012-Exp-3-A4-3.nd2";
            TiffMoviePlayer player = new TiffMoviePlayer(path);
            player.show();
        });
    }
}