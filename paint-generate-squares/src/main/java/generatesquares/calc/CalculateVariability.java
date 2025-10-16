package generatesquares.calc;


import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;

import static paint.shared.constants.PaintConstants.IMAGE_WIDTH;

public class CalculateVariability {

    // Main variability calculation
    public static double calcVariability(Table tracks,
                                         int squareNumber,
                                         int numberOfSquaresInRecording,
                                         int granularity) {

        // Matrix for variability analysis
        int[][] matrix = new int[granularity][granularity];

        // Width and height of a square
        int dimension = (int) Math.sqrt(numberOfSquaresInRecording);
        double width  = IMAGE_WIDTH / dimension;
        double height = IMAGE_WIDTH / dimension;

        // Access the columns once
        DoubleColumn xCol = tracks.doubleColumn("Track X Location");
        DoubleColumn yCol = tracks.doubleColumn("Track Y Location");

        // Loop over the tracks and fill the matrix
        for (int i = 0; i < tracks.rowCount(); i++) {
            double x = xCol.get(i);  // The x-coordinate of the track
            double y = yCol.get(i);  // The y-coordinate of the track

            // Get grid indices
            int[] indices = getIndices(x, y, width, height, squareNumber, dimension, granularity);
            int xi = indices[0];
            int yi = indices[1];

            if (xi >= 0 && xi < granularity && yi >= 0 && yi < granularity) {
                matrix[yi][xi]++;
            }
        }

        // Flatten matrix into an 1D array for stats
        int totalCells = granularity * granularity;
        double[] values = new double[totalCells];
        int idx = 0;
        for (int r = 0; r < granularity; r++) {
            for (int c = 0; c < granularity; c++) {
                values[idx++] = matrix[r][c];
            }
        }

        double mean = mean(values);
        if (mean == 0) {
            return 0.0;
        }

        double std = std(values, mean);
        return std / mean; // coefficient of variation
    }

    // Utility: compute mean
    private static double mean(double[] values) {
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    // Utility: compute std (population standard deviation)
    private static double std(double[] values, double mean) {
        double sumSq = 0.0;
        for (double v : values) {
            double diff = v - mean;
            sumSq += diff * diff;
        }
        return Math.sqrt(sumSq / values.length);
    }

    private static int[] getIndices(double x1,
                                    double y1,
                                    double width,
                                    double height,
                                    int squareSeqNr,
                                    int nrOfSquaresInRow,
                                    int granularity) {
        // Calculate the top-left corner (x0, y0) of the square
        double x0 = (squareSeqNr % nrOfSquaresInRow) * width;
        double y0 = (squareSeqNr / nrOfSquaresInRow) * height;

        // Calculate the grid indices (xi, yi) for the track
        int xi = (int) (((x1 - x0) / width) * granularity);
        int yi = (int) (((y1 - y0) / height) * granularity);

        return new int[]{xi, yi};
    }
}

