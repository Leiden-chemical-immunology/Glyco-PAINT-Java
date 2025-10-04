package viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SquareGridPanel extends JPanel {

    private final int rows;
    private final int cols;
    private final int width;
    private final int height;

    private Image backgroundImage;
    private final List<SquareForDisplay> squares = new ArrayList<>();
    private final Set<Integer> selectedSquares = new HashSet<>();

    private boolean showBorders = true;
    private Rectangle selectionRect = null;
    private Point dragStart = null;

    // 🔹 Selection toggle
    private boolean selectionEnabled = false;

    public enum NumberMode {
        NONE,
        LABEL,
        SQUARE
    }

    private NumberMode numberMode = NumberMode.NONE;

    // 🔹 Palette used also by CellAssignmentDialog
    private static final Color[] CELL_COLORS = {
            Color.RED, Color.GREEN, Color.BLUE,
            Color.MAGENTA, Color.ORANGE, Color.CYAN
    };

    public SquareGridPanel(int rows, int cols, int width, int height) {
        this.rows = rows;
        this.cols = cols;
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));

        int squareNumber = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                squares.add(new SquareForDisplay(r, c, squareNumber++));
            }
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!selectionEnabled) return;
                dragStart = e.getPoint();
                selectionRect = new Rectangle(dragStart);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!selectionEnabled) return;
                if (selectionRect != null) {
                    selectSquaresInRect(selectionRect);
                    selectionRect = null; // finalize
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!selectionEnabled) return;
                int squareW = width / cols;
                int squareH = height / rows;
                int col = e.getX() / squareW;
                int row = e.getY() / squareH;

                for (SquareForDisplay sq : squares) {
                    if (sq.row == row && sq.col == col) {
                        // toggle selection on click
                        sq.selected = !sq.selected;
                        if (sq.selected) {
                            selectedSquares.add(sq.squareNumber);
                        } else {
                            selectedSquares.remove(sq.squareNumber);
                        }
                        repaint();
                        break;
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!selectionEnabled) return;
                int x = Math.min(dragStart.x, e.getX());
                int y = Math.min(dragStart.y, e.getY());
                int w = Math.abs(dragStart.x - e.getX());
                int h = Math.abs(dragStart.y - e.getY());
                selectionRect = new Rectangle(x, y, w, h);
                repaint();
            }
        });
    }

    private void selectSquaresInRect(Rectangle rect) {
        int squareW = width / cols;
        int squareH = height / rows;
        for (SquareForDisplay sq : squares) {
            Rectangle r = new Rectangle(sq.col * squareW, sq.row * squareH, squareW, squareH);
            if (rect.intersects(r)) {
                sq.selected = true;
                selectedSquares.add(sq.squareNumber);
            }
        }
    }

    // === API ===
    public void setSelectionEnabled(boolean enabled) {
        this.selectionEnabled = enabled;
    }

    public Set<Integer> getSelectedSquares() {
        return new HashSet<>(selectedSquares);
    }

    public void clearSelection() {
        selectedSquares.clear();
        for (SquareForDisplay sq : squares) {
            sq.selected = false;
        }
        repaint();
    }

    public List<SquareForDisplay> getSquares() {
        return squares;
    }

    public void setBackgroundImage(ImageIcon icon) {
        this.backgroundImage = (icon != null) ? icon.getImage() : null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, width, height, this);
        }

        int squareW = width / cols;
        int squareH = height / rows;
        Graphics2D g2 = (Graphics2D) g;

        // --- Step 1: Draw unassigned grid lines ---
        if (showBorders) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1f));
            for (SquareForDisplay sq : squares) {
                if (sq.cellId == 0) {
                    int x = sq.col * squareW;
                    int y = sq.row * squareH;
                    g2.drawRect(x, y, squareW, squareH);
                }
            }
        }

        // --- Step 2: Fill selection overlays ---
        for (SquareForDisplay sq : squares) {
            if (sq.selected) {
                int x = sq.col * squareW;
                int y = sq.row * squareH;
                g2.setColor(new Color(255, 255, 255, 120)); // whitish fill
                g2.fillRect(x, y, squareW, squareH);
            }
        }

        // --- Step 3: Draw numbers if enabled ---
        for (SquareForDisplay sq : squares) {
            if (sq.selected) {
                int x = sq.col * squareW;
                int y = sq.row * squareH;
                if (numberMode == NumberMode.LABEL) {
                    drawCenteredString(g2, String.valueOf(sq.labelNumber), x, y, squareW, squareH);
                } else if (numberMode == NumberMode.SQUARE) {
                    drawCenteredString(g2, String.valueOf(sq.squareNumber), x, y, squareW, squareH);
                }
            }
        }

        // --- Step 4: Draw assigned cell borders LAST ---
        g2.setStroke(new BasicStroke(2f));
        for (SquareForDisplay sq : squares) {
            if (sq.cellId > 0) {
                int x = sq.col * squareW;
                int y = sq.row * squareH;
                g2.setColor(getColorForCell(sq.cellId));
                g2.drawRect(x, y, squareW, squareH);
            }
        }

        // --- Step 5: Drag rectangle overlay ---
        if (selectionRect != null && selectionEnabled) {
            g2.setColor(new Color(255, 255, 255, 120)); // whitish preview fill
            g2.fill(selectionRect);

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f)); // thin outline
            g2.draw(selectionRect);
        }
    }

    private void drawCenteredString(Graphics g, String text, int x, int y, int w, int h) {
        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.WHITE);
        g.drawString(text, tx, ty);
    }

    // === Toggles ===
    public void setShowBorders(boolean show) {
        this.showBorders = show;
        repaint();
    }

    public void setNumberMode(NumberMode mode) {
        this.numberMode = mode;
        repaint();
    }

    public void setSquaresGrid(List<SquareForDisplay> newSquares) {
        this.squares.clear();
        this.squares.addAll(newSquares);
        clearSelection();
    }

    // === Shared color helpers for dialog + panel ===
    public static int getSupportedCellCount() {
        return CELL_COLORS.length;
    }

    public static Color getColorForCell(int cellId) {
        if (cellId <= 0) return Color.GRAY;
        return CELL_COLORS[(cellId - 1) % CELL_COLORS.length];
    }
}