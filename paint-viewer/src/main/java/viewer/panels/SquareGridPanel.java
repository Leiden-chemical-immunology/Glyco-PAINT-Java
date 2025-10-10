package viewer.panels;

import paint.shared.objects.Square;
import paint.shared.utils.SquareUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintConstants.NUMBER_PIXELS_WIDTH;

/**
 * A Swing component for displaying and interacting with a grid of {@link Square} objects.
 * <p>
 * Each cell in the grid represents a single {@link Square} within a recording or experiment.
 * The panel supports visual overlays, square selection (individual or rectangular),
 * shading, borders, and contextual pop-ups with detailed square information.
 * </p>
 *
 * <h3>Core Features</h3>
 * <ul>
 *   <li>Draws a grid of squares over an optional background image</li>
 *   <li>Allows click or drag-based square selection</li>
 *   <li>Displays overlays (shading, cell colors, selection highlights)</li>
 *   <li>Shows per-square metrics in a popup on left click</li>
 *   <li>Supports toggling of borders, shading, and numeric overlays</li>
 *   <li>Integrates with visibility filtering and control parameters from dialogs</li>
 * </ul>
 */
public class SquareGridPanel extends JPanel {

    private final int rows;
    private final int cols;

    private Image backgroundImage;
    private List<Square> squares = new ArrayList<>();
    private final Set<Integer> selectedSquares = new HashSet<>();

    // @formatter:off
    private boolean   showBorders   = true;
    private boolean   showShading   = true;
    private Rectangle selectionRect = null;
    private Point     dragStart     = null;
    // @formatter:on

    // --- Control parameters (set externally) ---
    // @formatter:off
    private double ctrlMinDensityRatio = 0.0;
    private double ctrlMaxVariability  = Double.MAX_VALUE;
    private double ctrlMinRSquared     = 0.0;
    private String ctrlNeighbourMode   = "Free";
    // @formatter:on

    private boolean selectionEnabled = false;
    private final Set<Integer> dragSelectedSquares = new HashSet<>();

    /**
     * Enumeration representing numeric display modes for squares.
     */
    public enum NumberMode {

        NONE,    // No numbers are drawn on squares.
        LABEL,   // Draws label numbers on selected squares.
        SQUARE   // Draws square numbers on selected squares.
    }

    private NumberMode numberMode = NumberMode.NONE;

    /** Default cell colors used for visually differentiating assigned cells. */
    private static final Color[] CELL_COLORS = {
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.MAGENTA,
            Color.ORANGE,
            Color.CYAN
    };

    private JWindow infoPopup;

    /**
     * Creates a {@code SquareGridPanel} with the given number of rows and columns.
     *
     * @param rows number of rows in the grid
     * @param cols number of columns in the grid
     */
    public SquareGridPanel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        setPreferredSize(new Dimension(NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT));

        // --- Mouse listener for selection and popup info ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!selectionEnabled) {
                    return;
                }
                dragStart = e.getPoint();
                selectionRect = new Rectangle(dragStart);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!selectionEnabled) {
                    return;
                }
                if (selectionRect != null) {
                    selectSquaresInRect(selectionRect);
                    selectionRect = null;
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int squareW = getWidth() / cols;
                int squareH = getHeight() / rows;
                int col = e.getX() / squareW;
                int row = e.getY() / squareH;

                if (selectionEnabled) {
                    for (Square sq : squares) {
                        if (sq.getRowNumber() == row && sq.getColNumber() == col) {
                            boolean newSel = !sq.isSelected();
                            sq.setSelected(newSel);
                            if (newSel) {
                                selectedSquares.add(sq.getSquareNumber());
                            } else {
                                selectedSquares.remove(sq.getSquareNumber());
                            }
                            repaint();
                            break;
                        }
                    }
                }

                // Always show popup on left-click; right-click closes it
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showSquareInfo(e.getX(), e.getY());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    hideInfoPopup();
                }
            }
        });

        // --- Mouse drag listener for rectangular selection ---
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!selectionEnabled) {
                    return;
                }
                int x = Math.min(dragStart.x, e.getX());
                int y = Math.min(dragStart.y, e.getY());
                int w = Math.abs(dragStart.x - e.getX());
                int h = Math.abs(dragStart.y - e.getY());
                selectionRect = new Rectangle(x, y, w, h);
                repaint();
            }
        });
    }

    /**
     * Displays an informational popup for the square under the given coordinates.
     *
     * @param mouseX x-coordinate of the mouse click
     * @param mouseY y-coordinate of the mouse click
     */
    private void showSquareInfo(int mouseX, int mouseY) {
        if (squares == null || squares.isEmpty()) {
            return;
        }

        // @formatter:off
        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;
        int row     = mouseY / squareH;
        int col     = mouseX / squareW;
        int index   = row * cols + col;
        // @formatter:on

        if (index < 0 || index >= squares.size()) {
            return;
        }

        Square sq = squares.get(index);
        int trackCount = sq.getNumberOfTracks();

        String html = String.format(
                "<html><body style='font-family:sans-serif;font-size:11px;'>"
                        + "<b>Square %d</b>"
                        + "<table style='margin-top:4px;'>"
                        + "<tr><td style='padding-right:8px;'>Density Ratio:</td><td align='right'>%.1f</td></tr>"
                        + "<tr><td>Variability:</td><td align='right'>%.1f</td></tr>"
                        + "<tr><td>R²:</td><td align='right'>%.2f</td></tr>"
                        + "<tr><td>Tracks:</td><td align='right'>%d</td></tr>"
                        + "</table>"
                        + "<div style='margin-top:4px; font-style:italic; color:#666;'>"
                        + "Right-click anywhere to close"
                        + "</div>"
                        + "</body></html>",
                sq.getSquareNumber(),
                sq.getDensityRatio(),
                sq.getVariability(),
                sq.getRSquared(),
                trackCount
        );

        if (infoPopup == null) {
            infoPopup = new JWindow(SwingUtilities.getWindowAncestor(this));
            JLabel label = new JLabel(html);
            label.setOpaque(true);
            label.setBackground(new Color(255, 255, 255, 230));
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.DARK_GRAY),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
            infoPopup.add(label);
            infoPopup.pack();
            infoPopup.setAlwaysOnTop(true);
        } else {
            ((JLabel) infoPopup.getContentPane().getComponent(0)).setText(html);
        }

        Point panelScreen = getLocationOnScreen();
        int popupX = panelScreen.x + col * squareW + squareW + 8;
        int popupY = panelScreen.y + row * squareH + squareH / 4;

        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        if (popupX + infoPopup.getWidth() > screen.x + screen.width) {
            popupX = panelScreen.x + col * squareW - infoPopup.getWidth() - 8;
        }
        if (popupY + infoPopup.getHeight() > screen.y + screen.height) {
            popupY = screen.y + screen.height - infoPopup.getHeight() - 8;
        }

        infoPopup.setLocation(popupX, popupY);
        infoPopup.setVisible(true);
    }

    /** Hides and disposes the square information popup if present. */
    private void hideInfoPopup() {
        if (infoPopup != null) {
            infoPopup.setVisible(false);
            infoPopup.dispose();
            infoPopup = null;
        }
    }

    /**
     * Selects all squares intersecting the given rectangle.
     *
     * @param rect the selection rectangle in panel coordinates
     */
    private void selectSquaresInRect(Rectangle rect) {
        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;

        dragSelectedSquares.clear();

        for (Square sq : squares) {
            Rectangle r = new Rectangle(
                    sq.getColNumber() * squareW,
                    sq.getRowNumber() * squareH,
                    squareW, squareH
            );
            if (rect.intersects(r)) {
                sq.setSelected(true);
                selectedSquares.add(sq.getSquareNumber());
                dragSelectedSquares.add(sq.getSquareNumber());
            }
        }

        repaint();
    }

    // === Public API ===

    /** Enables or disables interactive square selection. */
    public void setSelectionEnabled(boolean enabled) {
        this.selectionEnabled = enabled;
    }

    /** @return a copy of the set of currently selected square numbers */
    public Set<Integer> getSelectedSquares() {
        return new HashSet<>(selectedSquares);
    }

    /** Clears all selected squares and repaints the panel. */
    public void clearSelection() {
        selectedSquares.clear();
        for (Square sq : squares) {
            sq.setSelected(false);
        }
        repaint();
    }

    /** @return the list of {@link Square} objects currently displayed */
    public List<Square> getSquares() {
        return squares;
    }

    /**
     * Sets the list of squares to display.
     *
     * @param newSquares list of squares; if {@code null}, an empty list is used
     */
    public void setSquares(List<Square> newSquares) {
        this.squares = newSquares != null ? newSquares : new ArrayList<>();
    }

    /**
     * Sets the background image displayed beneath the grid.
     *
     * @param icon an {@link ImageIcon} representing the background image
     */
    public void setBackgroundImage(ImageIcon icon) {
        this.backgroundImage = (icon != null) ? icon.getImage() : null;
        repaint();
    }

    /** Paints the square grid, overlays, numbers, and selection box. */
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

        // --- Draw cells, shading, and borders ---
        for (Square sq : squares) {
            int x = sq.getColNumber() * squareW;
            int y = sq.getRowNumber() * squareH;

            // --- Step 1: Fill base overlays (shading independent of borders) ---
            if (showShading && sq.getCellId() <= 0) {
                if (selectedSquares.contains(sq.getSquareNumber())) {
                    g2.setColor(new Color(255, 235, 0, 200));
                    g2.fillRect(x, y, squareW, squareH);
                } else if (sq.isSelected()) {
                    g2.setColor(new Color(255, 255, 255, 80));
                    g2.fillRect(x, y, squareW, squareH);
                }
            }

            // --- Step 2: Shaded cell fill (for assigned cells) ---
            if (sq.getCellId() > 0 && showShading) {
                Color baseColor = getColorForCell(sq.getCellId());
                int rVal = Math.min(255, baseColor.getRed() + 40);
                int gVal = Math.min(255, baseColor.getGreen() + 40);
                int bVal = Math.min(255, baseColor.getBlue() + 40);
                g2.setColor(new Color(rVal, gVal, bVal, 100));
                g2.fillRect(x, y, squareW, squareH);
            }

            // --- Step 3: Borders (only if enabled) ---
            if (showBorders) {
                if (sq.getCellId() > 0 || sq.isSelected()) {
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.setColor(Color.WHITE);
                    g2.drawRect(x, y, squareW, squareH);
                }
            }
        }

        // --- Step 4: Numbers ---
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

        // --- Step 5: Draw drag-selection rectangle ---
        if (selectionRect != null && selectionEnabled) {
            g2.setColor(new Color(255, 255, 180, 100));
            g2.fill(selectionRect);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(selectionRect);
        }
    }

    /**
     * Draws a centered text string inside a square cell.
     *
     * @param g graphics context
     * @param text text to draw
     * @param x x-coordinate of cell
     * @param y y-coordinate of cell
     * @param w cell width
     * @param h cell height
     */
    private void drawCenteredString(Graphics g, String text, int x, int y, int w, int h) {
        Font original = g.getFont();
        Font small = original.deriveFont(original.getSize2D() * 0.8f);
        g.setFont(small);

        FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.WHITE);
        g.drawString(text, tx, ty);
        g.setFont(original);
    }

    /** Enables or disables border drawing. */
    public void setShowBorders(boolean show) {
        this.showBorders = show;
        repaint();
    }

    /** Sets the numeric display mode for selected squares. */
    public void setNumberMode(NumberMode mode) {
        this.numberMode = mode;
        repaint();
    }

    /** @return number of supported cell colors */
    public static int getSupportedCellCount() {
        return CELL_COLORS.length;
    }

    /**
     * Returns a color associated with a given cell ID.
     *
     * @param cellId the cell identifier
     * @return a consistent color for that cell
     */
    public static Color getColorForCell(int cellId) {
        if (cellId <= 0) {
            return Color.GRAY;
        }
        return CELL_COLORS[(cellId - 1) % CELL_COLORS.length];
    }

    /**
     * Updates the internal visibility control parameters.
     *
     * @param densityRatio minimum required density ratio
     * @param variability maximum allowed variability
     * @param rSquared minimum required R² value
     * @param neighbourMode neighbour visibility mode
     */
    public void setControlParameters(double densityRatio,
                                     double variability,
                                     double rSquared,
                                     String neighbourMode) {
        this.ctrlMinDensityRatio = densityRatio;
        this.ctrlMaxVariability = variability;
        this.ctrlMinRSquared = rSquared;
        this.ctrlNeighbourMode = (neighbourMode != null) ? neighbourMode : "Free";
    }

    /** Re-applies square visibility filtering using the current control parameters. */
    public void applyVisibilityFilter() {
        SquareUtils.applyVisibilityFilter(squares, ctrlMinDensityRatio, ctrlMaxVariability, ctrlMinRSquared, ctrlNeighbourMode);
        repaint();
    }

    /**
     * Assigns the currently selected squares to a given cell ID.
     *
     * @param cellId the cell identifier to assign
     */
    public void assignSelectedToCell(int cellId) {
        if (squares == null) {
            return;
        }
        for (Square sq : squares) {
            if (sq.isSelected()) {
                sq.setCellId(cellId);
            }
        }
        repaint();
    }

    /** Clears all mouse-based selection highlights. */
    public void clearMouseSelection() {
        selectedSquares.clear();
        dragSelectedSquares.clear();
        selectionRect = null;
        dragStart = null;
        repaint();
    }

    /** Enables or disables shading overlays. */
    public void setShowShading(boolean show) {
        this.showShading = show;
        repaint();
    }
}