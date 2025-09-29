package viewer;

public class SquareForDisplay {
    public int row;            // 0..rows-1
    public int col;            // 0..cols-1
    public int squareNumber;   // row*cols + col
    public int labelNumber;    // if selected; otherwise -1
    public boolean selected;   // draw border if true
    public int showNumber;     // 0 = none, 1 = label, 2 = square
    public int cellId;         // 0 = none, 1..N = cell groups

    public SquareForDisplay() {
    }

    public SquareForDisplay(int row, int col, int cols) {
        this.row = row;
        this.col = col;
        this.squareNumber = row * cols + col;
        this.labelNumber = -1;
        this.selected = false;
        this.showNumber = 0;
        this.cellId = 0;
    }
}