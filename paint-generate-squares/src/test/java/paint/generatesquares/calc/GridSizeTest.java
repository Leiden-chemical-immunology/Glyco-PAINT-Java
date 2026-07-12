package paint.generatesquares.calc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.shared.objects.Recording;
import paint.shared.objects.Square;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static paint.shared.constants.PaintGeometry.IMAGE_HEIGHT;
import static paint.shared.constants.PaintGeometry.IMAGE_WIDTH;
import static paint.shared.constants.PaintStringConstants.GENERATE_SQUARES;
import static paint.shared.constants.PaintStringConstants.NUMBER_OF_SQUARES_IN_RECORDING;

/**
 * Pins the geometry of the generated square grid: the right number of squares, in the right
 * order, tiling the image evenly.
 *
 * <p>The square count always comes from a fixed dropdown (5x5 … 40x40), so it is a perfect
 * square by construction and {@code (int) Math.sqrt(n)} is exact. These tests lock that in.
 */
class GridSizeTest {

    private static GenerateSquaresConfig configWith(int numberOfSquares, Path projectDir) {
        PaintConfig.reinitialise(projectDir);
        PaintConfig.setInt(GENERATE_SQUARES, NUMBER_OF_SQUARES_IN_RECORDING, numberOfSquares);
        return new GenerateSquaresConfig();
    }

    private static Recording aRecording() {
        Recording recording = new Recording();
        recording.setExperimentName("221012");
        recording.setRecordingName("test-recording");
        return recording;
    }

    @Test
    @DisplayName("400 squares produces a full 20x20 grid, numbered row by row")
    void perfectSquareProducesExactCount(@TempDir Path projectDir) {
        List<Square> squares =
                GenerateSquaresProcessor.generateSquaresForRecording(aRecording(), configWith(400, projectDir));

        assertEquals(400, squares.size(), "400 squares should produce a full 20x20 grid");

        assertEquals(0,   squares.get(0).getSquareNumber());
        assertEquals(399, squares.get(399).getSquareNumber());
        assertEquals(19,  squares.get(399).getRowNumber());
        assertEquals(19,  squares.get(399).getColNumber());
    }

    @Test
    @DisplayName("every dropdown grid size yields exactly that many squares")
    void allSelectableGridSizesAreExact(@TempDir Path projectDir) {
        // The dropdown offers 5x5 .. 40x40.
        for (int side = 5; side <= 40; side += 5) {
            int n = side * side;
            List<Square> squares =
                    GenerateSquaresProcessor.generateSquaresForRecording(aRecording(), configWith(n, projectDir));
            assertEquals(n, squares.size(), side + "x" + side + " should produce " + n + " squares");
        }
    }

    @Test
    @DisplayName("the squares tile the image evenly, starting at the origin")
    void gridTilesTheImage(@TempDir Path projectDir) {
        List<Square> squares =
                GenerateSquaresProcessor.generateSquaresForRecording(aRecording(), configWith(400, projectDir));

        Square first = squares.get(0);
        assertEquals(0.0, first.getX0(), 1e-9, "the grid must start at the origin");
        assertEquals(0.0, first.getY0(), 1e-9, "the grid must start at the origin");

        // Square stores its corners rounded to 2 decimals, so a square's width can differ from
        // the nominal one by up to 0.01 at each edge. Assert the nominal size within that
        // rounding budget rather than demanding they all be bit-identical.
        double nominalWidth  = IMAGE_WIDTH  / 20.0;
        double nominalHeight = IMAGE_HEIGHT / 20.0;
        double roundingSlack = 0.02 + 1e-9;

        for (Square s : squares) {
            assertEquals(nominalWidth, s.getX1() - s.getX0(), roundingSlack,
                    "square " + s.getSquareNumber() + " should be one grid cell wide");
            assertEquals(nominalHeight, s.getY1() - s.getY0(), roundingSlack,
                    "square " + s.getSquareNumber() + " should be one grid cell high");
        }

        // The grid must reach the far corner of the image (within the same rounding budget).
        Square last = squares.get(squares.size() - 1);
        assertEquals(IMAGE_WIDTH,  last.getX1(), roundingSlack, "the grid must span the image width");
        assertEquals(IMAGE_HEIGHT, last.getY1(), roundingSlack, "the grid must span the image height");
    }
}
