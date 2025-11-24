/*==============================================================================
 *  Class:        SquareGridPanel.java
 *  Package:      paint.viewer.ui.panels
 *
 *  PURPOSE:
 *    Renders and manages an interactive grid of squares inside the PAINT Viewer,
 *    supporting visual selection, annotation, shading, numeric display modes,
 *    and visibility filtering based on configurable analysis parameters.
 *
 *  DESCRIPTION:
 *    The {@code SquareGridPanel} provides a graphical representation of
 *    {@link paint.shared.objects.Square} objects arranged in a fixed grid.
 *    Each square corresponds to a defined spatial region of an experiment and may
 *    contain metrics such as density, density ratio, variability, R², and track count.
 *
 *    The panel supports:
 *      • User-driven selection (click and drag).
 *      • Dynamic visibility filtering (via {@link SharedSquareUtils}).
 *      • Colored cell assignments and shading overlays.
 *      • Flexible numeric display modes (label numbers or square numbers).
 *      • On-demand contextual popups showing square-specific statistics.
 *
 *    It is typically coordinated by {@link paint.viewer.control.SquareControlHandler}
 *    as part of the full viewer interface (see {@code ViewerFrame}).
 *
 *  KEY FEATURES:
 *    • High-performance grid rendering with overlays and borders.
 *    • Click or drag-based interactive selection with visibility-aware behavior.
 *    • Supports contextual info popups for any visible square.
 *    • Integrates directly with visibility filters and control parameters.
 *    • Stable and deterministic color mapping for assigned cell IDs.
 *    • Independent component usable outside of the viewer if needed.
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
==============================================================================*/
package paint.viewer.ui.panels;

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

import static paint.shared.constants.PaintGeometry.NUMBER_PIXELS_HEIGHT;
import static paint.shared.constants.PaintGeometry.NUMBER_PIXELS_WIDTH;

/**
 * A Swing component that renders a grid of {@link Square} objects and provides
 * extensive interactivity including selection, shading, numeric labels, and
 * contextual information popups.
 *
 * <p>This panel is designed for integration with the PAINT Viewer and is used
 * to display region-level quantitative metrics overlaid on a TrackMate or
 * Brightfield image. Each square corresponds to a fixed spatial region whose
 * properties can be visualized, filtered, or interactively selected.</p>
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Drag or click-based selection of visible squares.</li>
 *   <li>Visibility filtering based on density ratio, variability, R², and neighbour mode.</li>
 *   <li>Deterministic color shading based on assigned cell IDs.</li>
 *   <li>Configurable numeric display modes (none, label, square number).</li>
 *   <li>Contextual statistical popups for detailed inspection.</li>
 * </ul>
 *
 * <p>Rendering is resolution-adaptive and automatically scales to the component's
 * current size. Interaction can be selectively enabled or disabled by calling
 * {@link #setInteractionEnabled(boolean)} or {@link #setSelectionEnabled(boolean)}.</p>
 */
public class SquareGridPanel extends JPanel {

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
    private       boolean      interactionEnabled      = true;
    private       boolean      infoPopupsEnabled       = true;


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
                int col     = e.getX() / squareW;
                int row     = e.getY() / squareH;

                if (selectionEnabled) {
                    for (Square square : squares) {
                        if (square.getRowNumber() == row && square.getColNumber() == col) {

                            // 🔹 Only allow user selection if the square is visible
                            if (!square.isVisible()) {
                                Toolkit.getDefaultToolkit().beep();
                                return;
                            }

                            int sqNum = square.getSquareNumber();
                            if (selectedSquaresNumbers.contains(sqNum)) {
                                selectedSquaresNumbers.remove(sqNum);
                            } else {
                                selectedSquaresNumbers.add(sqNum);
                            }

                            SquareGridPanel.this.repaint();
                            break;
                        }
                    }
                }

                // Left-click shows info only if popups enabled; right-click hides popup
                if (SwingUtilities.isLeftMouseButton(e) && infoPopupsEnabled) {
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
        if (!infoPopupsEnabled) {
            return;
        }

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

        Square square     = squares.get(index);
        int    trackCount = square.getNumberOfTracks();

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

            // 🔹 Only add squares that are visible
            if (rect.intersects(r) && square.isVisible()) {
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
        applyVisibilityFilter();
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

            boolean visible      = square.isVisible(); // selected == visible (from filter)
            boolean userSelected = selectedSquaresNumbers.contains(square.getSquareNumber());
            boolean hasCell      = square.getCellId() > 0;

            // --- Base fill ---
            if (!visible) {
                // Filtered-out squares: subtle gray fill + faint border
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRect(x, y, squareW, squareH);

                if (showBorders) {
                    g2.setStroke(new BasicStroke(0.5f));
                    g2.setColor(new Color(255, 255, 255, 25));
                    g2.drawRect(x, y, squareW, squareH);
                }
                continue;
            }

            // --- Visible squares ---
            if (showShading) {
                // Assigned cell color base layer
                if (hasCell) {
                    Color baseColor = getColorForCell(square.getCellId());
                    g2.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100));
                    g2.fillRect(x, y, squareW, squareH);
                }

                // Yellow user-selection overlay
                if (userSelected) {
                    g2.setColor(new Color(255, 235, 0, 180));
                    g2.fillRect(x, y, squareW, squareH);
                }

                // Light white tint for visible but not selected/assigned
                if (!userSelected && !hasCell) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRect(x, y, squareW, squareH);
                }
            }

            // --- Borders for all visible squares ---
            if (showBorders) {
                g2.setStroke(new BasicStroke(1.0f));
                g2.setColor(new Color(255, 255, 255, 150)); // soft white
                g2.drawRect(x, y, squareW, squareH);
            }
        }

        // --- Draw numbers inside selected squares ---
        for (Square square : squares) {
            if (square.isVisible()) {
                int x = square.getColNumber() * squareW;
                int y = square.getRowNumber() * squareH;
                if (numberMode == NumberMode.LABEL) {
                    drawCenteredString(g2, String.valueOf(square.getLabelNumber()), x, y, squareW, squareH);
                } else if (numberMode == NumberMode.SQUARE) {
                    drawCenteredString(g2, String.valueOf(square.getSquareNumber()), x, y, squareW, squareH);
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
        Font small    = original.deriveFont(original.getSize2D() * 0.8f);
        g.setFont(small);

        FontMetrics fm = g.getFontMetrics();
        int         tx = x + (w - fm.stringWidth(text)) / 2;
        int         ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;
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
     * Clears all mouse-based selection highlights.
     */
    public void clearMouseSelection() {
        selectedSquaresNumbers.clear();
        dragSelectedSquares.clear();
        selectionRect = null;
        dragStart     = null;
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

    public void setInteractionEnabled(boolean enabled) {
        this.interactionEnabled = enabled;
        if (!enabled) {
            hideSquareInfoIfVisible(); // Ensure any info popup is closed
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (!interactionEnabled) {
            return;
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (!interactionEnabled) {
            return;
        }
        super.processMouseMotionEvent(e);
    }

    public void setInfoPopupsEnabled(boolean enabled) {
        if (!enabled) {
            hideSquareInfoIfVisible();
        }
        this.infoPopupsEnabled = enabled;
    }
}