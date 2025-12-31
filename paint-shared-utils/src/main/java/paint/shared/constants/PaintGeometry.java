/*=============================================================================
 *  Class:        PaintGeometry.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Defines geometric constants used for spatial calculations and image
 *    rendering within the PAINT application.
 *
 *  DESCRIPTION:
 *    The {@code PaintGeometry} class centralizes constants related to
 *    pixel dimensions, grid scaling, and coordinate systems.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *============================================================================*/

package paint.shared.constants;

/**
 * Defines geometric constants used for spatial calculations and image
 * rendering within the PAINT application.
 */
public final class PaintGeometry {

    private PaintGeometry() {
        // Prevent instantiation
    }

    /** Pixel width in µm (specified by Nikon). */
    public static final double PIXEL_WIDTH          = 0.1603251;
    /** Pixel height in µm (specified by Nikon). */
    public static final double PIXEL_HEIGHT         = 0.1603251;
    /** Number of pixels in image width (specified by Nikon). */
    public static final int    NUMBER_PIXELS_WIDTH  = 512;
    /** Number of pixels in image height (specified by Nikon). */
    public static final int    NUMBER_PIXELS_HEIGHT = 512;
    /** Total image width in µm. */
    public static final double IMAGE_WIDTH          = PIXEL_WIDTH * NUMBER_PIXELS_WIDTH;
    /** Total image height in µm. */
    public static final double IMAGE_HEIGHT         = PIXEL_HEIGHT * NUMBER_PIXELS_HEIGHT;
}