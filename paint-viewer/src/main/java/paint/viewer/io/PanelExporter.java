/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.viewer.io;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Static utility for exporting Swing panels as high-resolution PNG images. This class cannot
 * be instantiated.
 * <p>
 * Provides a high-resolution PNG export for Swing panels used in the PAINT Viewer (e.g., the
 * left square grid panel).
 * </p>
 * <p>
 * This utility class renders any Swing {@link JPanel} to an off-screen {@link BufferedImage}
 * at an arbitrary scaling factor. High-quality rendering hints are applied to ensure smooth
 * edges, sharp text, and correct scaling of overlays such as square outlines or shading
 * layers. The exported image is written as a PNG to the specified file path. Directories are
 * created automatically if necessary.
 * </p>
 * <ul>
 *   <li>High-resolution PNG exports using scalable rendering.</li>
 *   <li>High-quality antialiasing, interpolation, and rendering hints.</li>
 *   <li>Simple one-method API: {@code export(panel, output, scale)}.</li>
 *   <li>Pure utility class — cannot be instantiated.</li>
 * </ul>
 */
public final class PanelExporter {

    /** Prevent instantiation. */
    private PanelExporter() { }

    /**
     * Exports the given {@link JPanel} to a PNG image at a specified scaling factor.
     * All visual overlays painted on the panel are included.
     *
     * <p><strong>Example:</strong><br>
     * {@code export(leftGridPanel, Paths.get("output.png"), 2.0);} produces a
     * double-resolution export suitable for publication-quality figures.</p>
     *
     * @param panel  the panel to render
     * @param output the destination PNG file
     * @param scale  scale factor (1.0 = current on-screen size)
     *
     * @throws Exception if rendering or writing fails
     */
    public static void export(JPanel panel, Path output, double scale) throws Exception {

        // Compute scaled output dimensions
        int width  = (int) (panel.getWidth()  * scale);
        int height = (int) (panel.getHeight() * scale);

        // Create image buffer
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2 = img.createGraphics();

        // High-quality rendering hints
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Apply scaling and render the panel
        g2.scale(scale, scale);
        panel.paintAll(g2);
        g2.dispose();

        // Ensure directory exists
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        // Write PNG
        javax.imageio.ImageIO.write(img, "png", output.toFile());
    }
}