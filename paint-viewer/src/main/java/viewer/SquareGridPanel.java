package viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SquareGridPanel extends JPanel {

    private final int rows;
    private final int cols;
    private final int width;
    private final int height;

    private Image backgroundImage;
    private final List<SquareForDisplay> squares = new ArrayList<>();

    private boolean showBorders = true;
    private int numberMode = 0; // 0 = none, 1 = label number, 2 = square number

    public SquareGridPanel(int rows, int cols, int width, int height) {
        this.rows = rows;
        this.cols = cols;
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));

        // initialize with blank squares
        int squareNumber = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                squares.add(new SquareForDisplay(r, c, squareNumber++));
            }
        }

        // 🔹 Mouse click handling
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int squareW = width / cols;
                int squareH = height / rows;
                int col = e.getX() / squareW;
                int row = e.getY() / squareH;

                for (SquareForDisplay sq : squares) {
                    if (sq.row == row && sq.col == col) {
                        sq.selected = !sq.selected; // toggle border
                        repaint();
                        break;
                    }
                }
            }
        });
    }

    public void setBackgroundImage(ImageIcon icon) {
        if (icon != null) {
            this.backgroundImage = icon.getImage();
        } else {
            this.backgroundImage = null;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // draw background image first
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, width, height, this);
        }

        int squareW = width / cols;
        int squareH = height / rows;

        for (SquareForDisplay sq : squares) {
            int x = sq.col * squareW;
            int y = sq.row * squareH;

            if (showBorders && sq.selected) {
                g.setColor(colorForCell(sq.cellId));
                g.drawRect(x, y, squareW, squareH);
            }

            if (numberMode == 1 && sq.selected) {
                drawCenteredString(g, String.valueOf(sq.labelNumber), x, y, squareW, squareH);
            } else if (numberMode == 2 && sq.selected) {
                drawCenteredString(g, String.valueOf(sq.squareNumber), x, y, squareW, squareH);
            }
        }
    }

    private void drawCenteredString(Graphics g, String text, int x, int y, int w, int h) {
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.RED);
        g.drawString(text, tx, ty);
    }

    private Color colorForCell(int cellId) {
        if (cellId == 0) return Color.WHITE;
        if (cellId == 1) return Color.GREEN;
        if (cellId == 2) return Color.MAGENTA;
        return Color.ORANGE;
    }

    // Control toggles
    public void setShowBorders(boolean show) {
        this.showBorders = show;
        repaint();
    }

    public void setNumberMode(int mode) {
        this.numberMode = mode;
        repaint();
    }
}