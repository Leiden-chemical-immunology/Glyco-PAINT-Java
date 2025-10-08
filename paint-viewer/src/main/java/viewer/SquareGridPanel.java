package viewer;

import paint.shared.objects.Square;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class SquareGridPanel extends JPanel {

    private final int rows;
    private final int cols;

    private Image backgroundImage;
    private List<Square> squares = new ArrayList<>();
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

    public SquareGridPanel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        setPreferredSize(new Dimension(512, 512));

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
                    selectionRect = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!selectionEnabled) return;
                int squareW = getWidth() / cols;
                int squareH = getHeight() / rows;
                int col = e.getX() / squareW;
                int row = e.getY() / squareH;

                for (Square sq : squares) {
                    if (sq.getRowNumber() == row && sq.getColNumber() == col) {
                        boolean newSel = !sq.isSelected();
                        sq.setSelected(newSel);
                        if (newSel) selectedSquares.add(sq.getSquareNumber());
                        else selectedSquares.remove(sq.getSquareNumber());
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
        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;
        for (Square sq : squares) {
            Rectangle r = new Rectangle(
                    sq.getColNumber() * squareW,
                    sq.getRowNumber() * squareH,
                    squareW, squareH
            );
            if (rect.intersects(r)) {
                sq.setSelected(true);
                selectedSquares.add(sq.getSquareNumber());
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
        for (Square sq : squares) {
            sq.setSelected(false);
        }
        repaint();
    }

    public List<Square> getSquares() {
        return squares;
    }

    public void setSquares(List<Square> newSquares) {
        this.squares = newSquares != null ? newSquares : new ArrayList<>();
    }

    public void setBackgroundImage(ImageIcon icon) {
        this.backgroundImage = (icon != null) ? icon.getImage() : null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        if (squares == null) return;
        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;
        Graphics2D g2 = (Graphics2D) g;

        // --- Step 1: Draw borders for assigned or selected squares ---
        for (Square sq : squares) {
            int x = sq.getColNumber() * squareW;
            int y = sq.getRowNumber() * squareH;

            if (sq.getCellId() > 0) {
                // Assigned square → thick colored border
                g2.setStroke(new BasicStroke(4f));
                g2.setColor(getColorForCell(sq.getCellId()));
                g2.drawRect(x, y, squareW, squareH);
            } else if (sq.isSelected()) {
                // Selected but unassigned → thin white border
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(Color.WHITE);
                g2.drawRect(x, y, squareW, squareH);
            }
        }

        // --- Step 2: Draw numbers if enabled (for selected squares only) ---
        for (Square sq : squares) {
            if (sq.isSelected()) {
                int x = sq.getColNumber() * squareW;
                int y = sq.getRowNumber() * squareH;
                if (numberMode == NumberMode.LABEL) {
                    drawCenteredString(g2, String.valueOf(sq.getLabelNumber()), x, y, squareW, squareH);
                } else if (numberMode == NumberMode.SQUARE) {
                    drawCenteredString(g2, String.valueOf(sq.getSquareNumber()), x, y, squareW, squareH);
                }
            }
        }

        // --- Step 3: Drag rectangle overlay ---
        if (selectionRect != null && selectionEnabled) {
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fill(selectionRect);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
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

    // === Shared color helpers ===
    public static int getSupportedCellCount() {
        return CELL_COLORS.length;
    }

    public static Color getColorForCell(int cellId) {
        if (cellId <= 0) return Color.GRAY;
        return CELL_COLORS[(cellId - 1) % CELL_COLORS.length];
    }
}