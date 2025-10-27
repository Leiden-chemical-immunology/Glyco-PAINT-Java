package paint.viewer.panels;

import paint.shared.objects.Recording;
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
 * A graphical panel that displays a grid of squares and provides interactive features such as selection,
 * annotation, and filtering. The {@code SquareGridPanel} can be customized with different display settings
 * and includes capabilities for visual overlays, background images, and informational popups.
 *
 * Fields:
 * - {@code recording}: Associated recording object for the panel.
 * - {@code rows}: Number of rows in the grid.
 * - {@code cols}: Number of columns in the grid.
 * - {@code backgroundImage}: The background image displayed beneath the grid.
 * - {@code squares}: List of squares currently displayed in the grid.
 * - {@code selectedSquares}: Set of currently selected square IDs.
 * - {@code showBorders}: Flag indicating whether borders around the squares are displayed.
 * - {@code showShading}: Flag indicating whether shading overlays are displayed.
 * - {@code selectionRect}: Temporary selection rectangle during drag actions.
 * - {@code dragStart}: Coordinates of the start point of a drag selection.
 * - {@code ctrlMinDensityRatio}: Minimum density ratio for visibility filtering.
 * - {@code ctrlMaxVariability}: Maximum allowed variability for visibility filtering.
 * - {@code ctrlMinRSquared}: Minimum required R² value for visibility filtering.
 * - {@code ctrlNeighbourMode}: Mode controlling neighbour-based visibility.
 * - {@code selectionEnabled}: Flag indicating if interactive selection is enabled.
 * - {@code dragSelectedSquares}: Set of squares temporarily selected during a drag.
 * - {@code numberMode}: Numeric display mode for selected squares.
 * - {@code CELL_COLORS}: Array of consistent cell colors.
 * - {@code infoPopup}: Popup component for displaying square-related information.
 *
 * Methods:
 * - Constructor: {@link SquareGridPanel(int, int)} initializes the panel with dimensions.
 * - {@link #showSquareInfo(int, int)} displays an informational popup for a square under certain coordinates.
 * - {@link #hideInfoPopup()} hides the square information popup if it is displayed.
 * - {@link #selectSquaresInRect(Rectangle)} selects all squares intersecting a given selection rectangle.
 * - {@link #setSelectionEnabled(boolean)} enables or disables interactive square selection.
 * - {@link #getSelectedSquares()} returns the set of currently selected square IDs.
 * - {@link #clearSelection()} clears all selected squares and refreshes the panel.
 * - {@link #getSquares()} retrieves the list of currently displayed squares.
 * - {@link #setSquares(List)} sets the list of squares to display on the panel.
 * - {@link #setBackgroundImage(ImageIcon)} sets the background image displayed beneath the grid.
 * - {@link #paintComponent(Graphics)} handles the rendering of the square grid and overlays.
 * - {@link #drawCenteredString(Graphics, String, int, int, int, int)} draws centered text within a square cell.
 * - {@link #setShowBorders(boolean)} toggles border visibility around the grid squares.
 * - {@link #setNumberMode(NumberMode)} sets the numeric display mode for selected squares.
 * - {@link #getSupportedCellCount()} retrieves the number of supported cell colors.
 * - {@link #getColorForCell(int)} retrieves a consistent color associated with a given cell ID.
 * - {@link #setControlParameters(double, double, double, String)} updates visibility control parameters.
 * - {@link #applyVisibilityFilter()} reapplies square visibility filtering using current control parameters.
 * - {@link #assignSelectedToCell(int)} assigns the currently selected squares to a specific cell ID.
 * - {@link #clearMouseSelection()} clears all mouse-selection highlights.
 * - {@link #setShowShading(boolean)} toggles shading overlay visibility.
 * - {@link #setRecording(Recording)} sets the associated {@code Recording} object.
 * - {@link #getRecording()} retrieves the associated {@code Recording} object.
 *
 * Inherits functionality from {@link JPanel}, allowing it to integrate seamlessly into UI applications.
 */
public class SquareGridPanel extends JPanel {

    // @formatter:off
    private       Recording    recording;
    private final int          rows;
    private final int          cols;

    private       Image        backgroundImage;
    private       List<Square> squares         = new ArrayList<>();
    private final Set<Integer> selectedSquares = new HashSet<>();

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
     * The NumberMode enumeration defines the modes for displaying numbers
     * on squares within a grid. It provides three distinct modes:
     * NONE, LABEL, and SQUARE, each controlling how numbers are rendered.
     *
     * Enumeration values:
     * - NONE: Disables the drawing of numbers on the squares.
     * - LABEL: Displays label numbers on specific selected squares.
     * - SQUARE: Displays numbers representing square indices on selected squares.
     *
     * This enumeration is used to control the numeric display mode
     * within the grid managed by the containing panel.
     */
    public enum NumberMode {

        NONE,    // No numbers are drawn on squares.
        LABEL,   // Draws label numbers on selected squares.
        SQUARE   // Draws square numbers on selected squares.
    }

    private NumberMode numberMode = NumberMode.NONE;

    /**
     * An array of {@link Color} objects representing predefined cell colors used for
     * coloring cells within the square grid. This is used to ensure consistent and
     * distinguishable colors for different cells in the grid.
     *
     * Colors in the array:
     * - {@link Color#RED}
     * - {@link Color#GREEN}
     * - {@link Color#BLUE}
     * - {@link Color#MAGENTA}
     * - {@link Color#ORANGE}
     * - {@link Color#CYAN}
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
     * Constructs a new {@code SquareGridPanel} with the specified number of rows and columns.
     * Adds mouse listeners to enable interactive square selection, displaying information popups,
     * and handling rectangular selection through mouse dragging.
     *
     * @param rows the number of rows in the square grid
     * @param cols the number of columns in the square grid
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

                // Always show the popup on left-click; right-click closes it
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
     * Displays detailed information about the square in the grid that corresponds
     * to the specified mouse coordinates. The information is presented in the form
     * of a popup window showing characteristics such as density ratio,
     * variability, R² value, and the number of tracks.
     *
     * @param mouseX the x-coordinate of the mouse position in the panel
     * @param mouseY the y-coordinate of the mouse position in the panel
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

    /**
     * Hides the information popup if it is currently visible by setting its visibility
     * to false and disposing of it. Once hidden, the reference to the popup is set to null
     * to free up resources.
     *
     * This method ensures that any active information popup is closed and cleaned up properly.
     */
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
        SquareUtils.applyVisibilityFilter(
                recording,
                ctrlMinDensityRatio,
                ctrlMaxVariability,
                ctrlMinRSquared,
                ctrlNeighbourMode);
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

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public Recording getRecording() {
        return recording;
    }
}