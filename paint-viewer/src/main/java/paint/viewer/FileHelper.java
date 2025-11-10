package paint.viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileHelper {

    private FileHelper() {
        // prevent instantiation
    }

    /**
     * Exports the given panel (component) and its overlay as a high‑resolution PNG.
     *
     * @param panel the panel to export (e.g. your leftGridPanel)
     * @param outputPath the file path to write to
     * @param scale the scale factor (1.0 = current size, 2.0 = double size, etc.)
     * @throws IOException if the export fails
     */
    public static void exportPanelAsImage(JComponent panel, Path outputPath, double scale) throws IOException {
        int width  = (int) (panel.getWidth() * scale);
        int height = (int) (panel.getHeight() * scale);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.scale(scale, scale);
        panel.paintAll(g2);
        g2.dispose();

        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }
        javax.imageio.ImageIO.write(image, "png", outputPath.toFile());
    }

    /**
     * Creates a temporary CSV file containing only rows from the original squares.csv
     * that match the given recordingName, then opens that file. If no matching rows
     * are found, it still opens the empty file so the user sees nothing matched.
     *
     * @param projectRoot the project root (where your experiment folders live)
     * @param experimentName the current experiment name
     * @param recordingName the current recording name
     * @throws IOException if IO errors occur
     */
    public static void filterAndOpenSquaresCsv(Path projectRoot,
            String experimentName,
            String recordingName) throws IOException {
        Path origCsv = projectRoot.resolve(experimentName).resolve("squares.csv");
        if (!Files.exists(origCsv)) {
            throw new IOException("Squares.csv not found: " + origCsv);
        }

        Path tempFile = Files.createTempFile("Squares " + recordingName, ".csv");
        try (BufferedReader r = Files.newBufferedReader(origCsv);
             BufferedWriter w = Files.newBufferedWriter(tempFile)) {
            String header = r.readLine();
            if (header != null) {
                w.write(header);
                w.newLine();
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains(recordingName)) {
                        w.write(line);
                        w.newLine();
                    }
                }
            }
        }
        tempFile.toFile().setReadOnly();
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(tempFile.toFile());
        } else {
            throw new IOException("Desktop integration not supported.");
        }
    }
}