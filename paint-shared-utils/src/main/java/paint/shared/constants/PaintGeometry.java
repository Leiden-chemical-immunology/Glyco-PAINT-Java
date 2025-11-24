/*=============================================================================
 *  Class:        PaintGeometry.java
 *  Package:      paint.shared.constants
 *
 *  PURPOSE:
 *    Holds geometry-related constants such as pixel sizes and image dimensions.
 *============================================================================*/

package paint.shared.constants;

public final class PaintGeometry {

    private PaintGeometry() {
        // Prevent instantiation
    }

    public static final double PIXEL_WIDTH          = 0.1603251;                           // Pixel width in µm (specified by Nikon).
    public static final double PIXEL_HEIGHT         = 0.1603251;                           // Pixel height in µm (specified by Nikon).
    public static final int    NUMBER_PIXELS_WIDTH  = 512;                                 // Number of pixels in image width (specified by Nikon).
    public static final int    NUMBER_PIXELS_HEIGHT = 512;                                 // Number of pixels in image width (specified by Nikon).
    public static final double IMAGE_WIDTH          = PIXEL_WIDTH * NUMBER_PIXELS_WIDTH;   // in µm
    public static final double IMAGE_HEIGHT         = PIXEL_HEIGHT * NUMBER_PIXELS_HEIGHT; // in µm
}