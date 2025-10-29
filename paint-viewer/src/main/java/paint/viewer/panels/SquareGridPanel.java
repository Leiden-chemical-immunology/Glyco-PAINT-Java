/******************************************************************************
 *  Class:        SquareGridPanel.java
 *  Package:      paint.viewer.panels
 *
 *  PURPOSE:
 *    Displays and manages an interactive grid of squares within the PAINT
 *    viewer, supporting visual selection, annotation, and visibility control
 *    features for experimental image data.
 *
 *  DESCRIPTION:
 *    The {@code SquareGridPanel} provides a graphical interface that renders
 *    a structured grid of {@link paint.shared.objects.Square} elements. Each
 *    square can represent data such as density, variability, or R² statistics
 *    and may be interactively selected, filtered, or annotated.
 *
 *    The panel supports dynamic updates, overlay shading, numeric display
 *    modes, and contextual popups showing detailed square information. It can
 *    operate independently or as part of a larger viewer managed by
 *    {@link paint.viewer.logic.SquareControlHandler}.
 *
 *  KEY FEATURES:
 *    • Renders an interactive, data-driven grid of squares.
 *    • Supports shaded overlays, numeric modes, and border control.
 *    • Allows user-driven selection and assignment of cell IDs.
 *    • Integrates with visibility filtering based on configurable parameters.
 *    • Provides contextual info popups for detailed square statistics.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-viewer
 *
 *  UPDATED:
 *    2025-10-29
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.viewer.panels;

import paint.shared.objects.Recording;
import paint.shared.objects.Square;
import paint.shared.utils.SharedSquareUtils;

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
 * The {@code SquareGridPanel} is a graphical component that renders a grid of
 * {@link paint.shared.objects.Square} objects, each representing an individual
 * region of interest. It provides rich interactivity such as selection,
 * annotation, and contextual information popups.
 *
 * <p>Users can interactively select squares, assign them to cells, and toggle
 * visibility or shading modes. Control parameters for filtering and display
 * can be applied through integration with {@code SquareControlHandler}.</p>
 *
 * <p>The panel automatically handles painting of overlays, borders, and
 * numerical annotations depending on its configured state.</p>
 */
public class SquareGridPanel extends JPanel {

    private       Recording    recording;
    private final int          rows;
    private final int          cols;

    private       Image        backgroundImage;
    private       List<Square> squares                 = new ArrayList<>();
    private final Set<Integer> selectedSquaresNumbers  = new HashSet<>();

    private       boolean      showBorders             = true;
    private       boolean      showShading             = true;
    private       Rectangle    selectionRect           = null;
    private       Point        dragStart               = null;

    private       double       minRequiredDensityRatio = 0.0;
    private       double       maxAllowableVariability = Double.MAX_VALUE;
    private       double       minRequiredRSquared     = 0.0;
    private       String       neighbourMode           = "Free";
    private       boolean      selectionEnabled        = false;
    private final Set<Integer> dragSelectedSquares     = new HashSet<>();

    /**
     * Enumeration defining numeric display modes for squares.
     * <ul>
     *   <li>{@code NONE}: No numbers are displayed.</li>
     *   <li>{@code LABEL}: Shows label numbers for selected squares.</li>
     *   <li>{@code SQUARE}: Shows square indices for selected squares.</li>
     * </ul>
     */
    public enum NumberMode {
        NONE,    // No numbers are drawn on squares.
        LABEL,   // Draws label numbers on selected squares.
        SQUARE   // Draws square numbers on selected squares.
    }

    private NumberMode numberMode = NumberMode.NONE;

    /**
     * Predefined, consistent colors for assigned cell IDs.
     */
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
     * Constructs a {@code SquareGridPanel} with the specified grid size and sets
     * up mouse listeners for interaction (selection and popups).
     *
     * @param rows number of grid rows
     * @param cols number of grid columns
     */
    public SquareGridPanel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        setPreferredSize(new Dimension(NUMBER_PIXELS_WIDTH, NUMBER_PIXELS_HEIGHT));

        // --- Mouse listener for selection and info popups ---
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
                    SquareGridPanel.this.repaint();
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
                                selectedSquaresNumbers.add(sq.getSquareNumber());
                            } else {
                                selectedSquaresNumbers.remove(sq.getSquareNumber());
                            }
                            SquareGridPanel.this.repaint();
                            break;
                        }
                    }
                }

                // Left-click shows info; right-click hides popup
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showSquareInfo(e.getX(), e.getY());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    hideInfoPopup();
                }
            }
        });

        // --- Mouse motion listener for drag-selection ---
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
                SquareGridPanel.this.repaint();
            }
        });
    }

    /**
     * Displays an informational popup for the square under the given coordinates.
     *
     * @param mouseX x-coordinate of the mouse
     * @param mouseY y-coordinate of the mouse
     */
    private void showSquareInfo(int mouseX, int mouseY) {
        if (squares == null || squares.isEmpty()) {
            return;
        }

        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;
        int row     = mouseY / squareH;
        int col     = mouseX / squareW;
        int index   = row * cols + col;

        if (index < 0 || index >= squares.size()) {
            return;
        }

        Square square = squares.get(index);
        int trackCount = square.getNumberOfTracks();

        String html = String.format(
                "<html><body style='font-family:sans-serif;font-size:11px;'>"
                        + "<b>Square %d</b>"
                        + "<table style='margin-top:4px;'>"
                        + "<tr><td style='padding-right:8px;'>Density:</td><td align='right'>%.4f</td></tr>"
                        + "<tr><td style='padding-right:8px;'>Density Ratio:</td><td align='right'>%.1f</td></tr>"
                        + "<tr><td>Variability:</td><td align='right'>%.1f</td></tr>"
                        + "<tr><td>R²:</td><td align='right'>%.2f</td></tr>"
                        + "<tr><td>Tracks:</td><td align='right'>%d</td></tr>"
                        + "</table>"
                        + "<div style='margin-top:4px; font-style:italic; color:#666;'>"
                        + "Right-click anywhere to close"
                        + "</div>"
                        + "</body></html>",
                square.getSquareNumber(),
                square.getDensity(),
                square.getDensityRatio(),
                square.getVariability(),
                square.getRSquared(),
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

    /**
     * Hides and disposes the info popup if currently visible.
     */
    private void hideInfoPopup() {
        if (infoPopup != null) {
            infoPopup.setVisible(false);
            infoPopup.dispose();
            infoPopup = null;
        }
    }

    /**
     * Selects all squares that intersect the specified rectangle.
     *
     * @param rect selection area in panel coordinates
     */
    private void selectSquaresInRect(Rectangle rect) {
        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;

        dragSelectedSquares.clear();

        for (Square square : squares) {
            Rectangle r = new Rectangle(
                    square.getColNumber() * squareW,
                    square.getRowNumber() * squareH,
                    squareW, squareH
            );
            if (rect.intersects(r)) {
                square.setSelected(true);
                selectedSquaresNumbers.add(square.getSquareNumber());
                dragSelectedSquares.add(square.getSquareNumber());
            }
        }

        this.repaint();
    }

    /**
     * Enables or disables user-driven square selection.
     */
    public void setSelectionEnabled(boolean enabled) {
        this.selectionEnabled = enabled;
    }

    /**
     * @return a copy of the currently selected square IDs.
     */
    public Set<Integer> getUserSelectedSquaresNumbers() {
        return new HashSet<>(selectedSquaresNumbers);
    }

    /**
     * Clears all selections and refreshes the panel.
     */
    public void clearSelection() {
        selectedSquaresNumbers.clear();
        for (Square sq : squares) {
            sq.setSelected(false);
        }
        this.repaint();
    }

    /**
     * @return list of all squares currently rendered in the grid.
     */
    public List<Square> getSquares() {
        return squares;
    }

    /**
     * Sets the list of squares to be displayed.
     */
    public void setSquares(List<Square> newSquares) {
        this.squares = newSquares != null ? newSquares : new ArrayList<>();
    }

    /**
     * Sets the background image to display beneath the grid.
     */
    public void setBackgroundImage(ImageIcon icon) {
        this.backgroundImage = (icon != null) ? icon.getImage() : null;
        this.repaint();
    }

    /**
     * Paints all visual elements including the grid, overlays, and selections.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
        if (squares == null) {
            return;
        }

        int        squareW = getWidth() / cols;
        int        squareH = getHeight() / rows;
        Graphics2D g2      = (Graphics2D) g;

        // --- Draw cells, overlays, and borders ---
        for (Square square : squares) {
            int x = square.getColNumber() * squareW;
            int y = square.getRowNumber() * squareH;

            // 1) Assigned-cell shading (base layer)
            if (square.getCellId() > 0 && showShading) {
                Color baseColor = getColorForCell(square.getCellId());
                int   rVal      = Math.min(255, baseColor.getRed() + 40);
                int   gVal      = Math.min(255, baseColor.getGreen() + 40);
                int   bVal      = Math.min(255, baseColor.getBlue() + 40);
                g2.setColor(new Color(rVal, gVal, bVal, 100));
                g2.fillRect(x, y, squareW, squareH);
            }

            // 2) User-selection highlight (always on top of any assigned shading)
            if (showShading) {
                if (selectedSquaresNumbers.contains(square.getSquareNumber())) {
                    g2.setColor(new Color(255, 235, 0, 200));
                    g2.fillRect(x, y, squareW, squareH);
                } else if (square.isSelected()) {
                    g2.setColor(new Color(255, 255, 255, 80));
                    g2.fillRect(x, y, squareW, squareH);
                }
            }

            // 3) Borders (only if enabled)
            if (showBorders) {
                if (square.getCellId() > 0 || square.isSelected() || selectedSquaresNumbers.contains(square.getSquareNumber())) {
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.setColor(Color.WHITE);
                    g2.drawRect(x, y, squareW, squareH);
                }
            }
        }

        // --- Draw numbers inside selected squares ---
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

        // --- Draw drag-selection rectangle ---
        if (selectionRect != null && selectionEnabled) {
            g2.setColor(new Color(255, 255, 180, 100));
            g2.fill(selectionRect);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(selectionRect);
        }
    }

    /**
     * Draws centered text inside a given square.
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

    /**
     * Enables or disables square border rendering.
     */
    public void setShowBorders(boolean show) {
        this.showBorders = show;
        this.repaint();
    }

    /**
     * Sets the numeric display mode for selected squares.
     */
    public void setNumberMode(NumberMode mode) {
        this.numberMode = mode;
        this.repaint();
    }

    /**
     * @return total number of supported cell colors.
     */
    public static int getSupportedCellCount() {
        return CELL_COLORS.length;
    }

    /**
     * Returns the consistent color associated with a specific cell ID.
     */
    public static Color getColorForCell(int cellId) {
        if (cellId <= 0) {
            return Color.GRAY;
        }
        return CELL_COLORS[(cellId - 1) % CELL_COLORS.length];
    }

    /**
     * Updates visibility control parameters for the panel.
     */
    public void setControlParameters(double minRequiredDensityRatio,
                                     double maxAllowableVariability,
                                     double minRequiredRSquared,
                                     String neighbourMode) {
        this.minRequiredDensityRatio = minRequiredDensityRatio;
        this.maxAllowableVariability = maxAllowableVariability;
        this.minRequiredRSquared     = minRequiredRSquared;
        this.neighbourMode           = (neighbourMode != null) ? neighbourMode : "Free";
    }

    /**
     * Applies the active visibility filter using {@link SharedSquareUtils}.
     */
    public void applyVisibilityFilter() {
        SharedSquareUtils.applyVisibilityFilter(
                squares,
                minRequiredDensityRatio,
                maxAllowableVariability,
                minRequiredRSquared,
                neighbourMode);
        this.repaint();
    }

    /**
     * Assigns all selected squares to a given cell ID.
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
        this.repaint();
    }

    /**
     * Clears all mouse-based selection highlights.
     */
    public void clearMouseSelection() {
        selectedSquaresNumbers.clear();
        dragSelectedSquares.clear();
        selectionRect = null;
        dragStart = null;
        this.repaint();
    }

    /**
     * Enables or disables square shading overlays.
     */
    public void setShowShading(boolean show) {
        this.showShading = show;
        this.repaint();
    }

    /**
     * Sets the associated {@link Recording} object for this panel.
     */
    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    /**
     * @return the {@link Recording} currently associated with this panel.
     */
    public Recording getRecording() {
        return recording;
    }

    /**
     * Hides the square information popup if it is currently visible.
     * This is equivalent to {@link #hideInfoPopup()} but can be called externally.
     */
    public void hideSquareInfoIfVisible() {
        if (infoPopup != null) {
            infoPopup.setVisible(false);
            infoPopup.dispose();
            infoPopup = null;
        }
    }
}