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

    public enum NumberMode {
        NONE,
        LABEL,
        SQUARE
    }

    private NumberMode numberMode = NumberMode.NONE;

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

        // Mouse press/release for drag selection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                selectionRect = new Rectangle(dragStart);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (selectionRect != null) {
                    boolean additive = isAdditive(e);
                    selectSquaresInRect(selectionRect, additive);
                    selectionRect = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int squareW = width / cols;
                int squareH = height / rows;
                int col = e.getX() / squareW;
                int row = e.getY() / squareH;

                for (SquareForDisplay sq : squares) {
                    if (sq.row == row && sq.col == col) {
                        boolean additive = isAdditive(e);
                        if (!additive) {
                            // replace selection
                            clearSelection();
                        }
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
                int x = Math.min(dragStart.x, e.getX());
                int y = Math.min(dragStart.y, e.getY());
                int w = Math.abs(dragStart.x - e.getX());
                int h = Math.abs(dragStart.y - e.getY());
                selectionRect = new Rectangle(x, y, w, h);
                repaint();
            }
        });
    }

    private boolean isAdditive(MouseEvent e) {
        // Support Ctrl on Windows/Linux and Command on macOS
        return e.isControlDown() || e.isMetaDown();
    }

    private void selectSquaresInRect(Rectangle rect, boolean additive) {
        if (!additive) {
            clearSelectionWithoutRepaint();
        }
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

    // === API for RecordingViewerFrame ===
    public Set<Integer> getSelectedSquares() {
        return new HashSet<>(selectedSquares);
    }

    public void clearSelection() {
        clearSelectionWithoutRepaint();
        repaint();
    }

    private void clearSelectionWithoutRepaint() {
        selectedSquares.clear();
        for (SquareForDisplay sq : squares) {
            sq.selected = false;
        }
    }

    public List<SquareForDisplay> getSquares() {
        return squares;
    }

    // === Background image ===
    public void setBackgroundImage(ImageIcon icon) {
        if (icon != null) {
            this.backgroundImage = icon.getImage();
        } else {
            this.backgroundImage = null;
        }
        repaint();
    }

    // === Painting ===
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, width, height, this);
        }

        int squareW = width / cols;
        int squareH = height / rows;

        for (SquareForDisplay sq : squares) {
            int x = sq.col * squareW;
            int y = sq.row * squareH;

            // Fill with cell color if assigned
            if (sq.cellId > 0) {
                g.setColor(colorForCell(sq.cellId));
                g.fillRect(x, y, squareW, squareH);
            }

            // Selected squares get a semi-transparent highlight
            if (sq.selected) {
                g.setColor(new Color(0, 120, 215, 80));
                g.fillRect(x, y, squareW, squareH);
            }

            // Borders always drawn (unless disabled)
            if (showBorders) {
                g.setColor(Color.BLACK);
                g.drawRect(x, y, squareW, squareH);
            }

            // Number display if enabled (only when selected, as in your original)
            if (numberMode == NumberMode.LABEL && sq.selected) {
                drawCenteredString(g, String.valueOf(sq.labelNumber), x, y, squareW, squareH);
            } else if (numberMode == NumberMode.SQUARE && sq.selected) {
                drawCenteredString(g, String.valueOf(sq.squareNumber), x, y, squareW, squareH);
            }
        }

        // Draw drag rectangle overlay
        if (selectionRect != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 255, 50));
            g2.fill(selectionRect);
            g2.setColor(Color.BLUE);
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

    private Color colorForCell(int cellId) {
        switch (cellId) {
            case 1: return Color.GREEN;
            case 2: return Color.MAGENTA;
            case 3: return Color.ORANGE;
            case 4: return Color.CYAN;
            case 5: return Color.RED;
            case 6: return Color.BLUE;
            default: return Color.WHITE;
        }
    }

    // === Control toggles ===
    public void setShowBorders(boolean show) {
        this.showBorders = show;
        repaint();
    }

    public void setNumberMode(NumberMode mode) {
        this.numberMode = mode;
        repaint();
    }

    // Replace default squares with CSV-loaded ones
    public void setSquares(List<SquareForDisplay> newSquares) {
        this.squares.clear();
        this.squares.addAll(newSquares);
        // When squares are replaced, clear selection to avoid stale indices
        clearSelection();
    }
}