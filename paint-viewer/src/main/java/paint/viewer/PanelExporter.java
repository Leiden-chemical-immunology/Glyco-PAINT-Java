package paint.viewer.export;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PanelExporter {

    private PanelExporter() {
    }

    /**
     * Exports a Swing panel (including overlays) as a high-resolution PNG.
     *
     * @param panel the Swing component to render
     * @param output PNG destination file
     * @param scale resolution scaling (1.0 = screen, 2.0 = double res)
     * @throws Exception if exporting fails
     */
    public static void export(JPanel panel, Path output, double scale) throws Exception {

        int width  = (int) (panel.getWidth()  * scale);
        int height = (int) (panel.getHeight() * scale);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g2.scale(scale, scale);
        panel.paintAll(g2);
        g2.dispose();

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        javax.imageio.ImageIO.write(img, "png", output.toFile());
    }
}