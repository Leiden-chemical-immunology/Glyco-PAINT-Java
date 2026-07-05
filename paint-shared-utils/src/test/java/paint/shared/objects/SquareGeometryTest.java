package paint.shared.objects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static paint.shared.constants.PaintGeometry.IMAGE_HEIGHT;
import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;

/**
 * Characterization tests for the pure geometry on {@link Square}:
 * the theoretical square-area formula and the coordinate rounding applied
 * by the constructor. Both are deterministic and free of configuration or I/O,
 * so these tests pin current behaviour without touching production code.
 */
@DisplayName("Square — pure geometry")
class SquareGeometryTest {

    private static final double EPS = 1e-9;

    @Test
    @DisplayName("calculateSquareArea returns (imageWidth * imageHeight) / n")
    void squareAreaFollowsFormula() {
        // Relationship holds for any grid count.
        for (int n : new int[]{1, 4, 100, 400, 441}) {
            assertEquals(IMAGE_WIDTH * IMAGE_HEIGHT / n,
                    Square.calculateSquareArea(n), EPS,
                    "area formula changed for n=" + n);
        }
    }

    @Test
    @DisplayName("calculateSquareArea: whole grid of n squares tiles the full image area")
    void squareAreasSumToImageArea() {
        int n = 400;
        double totalImageArea = IMAGE_WIDTH * IMAGE_HEIGHT;
        assertEquals(totalImageArea, Square.calculateSquareArea(n) * n, 1e-6,
                "n squares should tile the full image area");
    }

    @Test
    @DisplayName("constructor rounds x0/y0/x1/y1 to two decimals (half-up)")
    void constructorRoundsCoordinates() {
        // round(v, 2) == Math.round(v * 100) / 100.0  (half rounds up)
        Square s = new Square(
                "rec-0", "exp", "rec",
                /* squareNumber */ 7,
                /* rowNumber    */ 2,
                /* colNumber    */ 3,
                /* x0 */ 1.236,   // -> 1.24
                /* y0 */ 2.001,   // -> 2.00
                /* x1 */ 3.005,   // -> 3.01 (half up)
                /* y1 */ 4.994    // -> 4.99
        );

        assertEquals(1.24, s.getX0(), EPS);
        assertEquals(2.00, s.getY0(), EPS);
        assertEquals(3.01, s.getX1(), EPS);
        assertEquals(4.99, s.getY1(), EPS);
    }

    @Test
    @DisplayName("constructor passes grid indices through unchanged")
    void constructorKeepsIndices() {
        Square s = new Square("rec-7", "exp", "rec", 7, 2, 3, 0.0, 0.0, 1.0, 1.0);
        assertEquals(7, s.getSquareNumber());
        assertEquals(2, s.getRowNumber());
        assertEquals(3, s.getColNumber());
    }
}
