package viewer;

import paint.shared.objects.Square;
import paint.shared.utils.PaintLogger;
import paint.shared.utils.SquareUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SquareGridPanel extends JPanel {

    private final int rows;
    private final int cols;

    private Image backgroundImage;
    private List<Square> squares = new ArrayList<>();
    private final Set<Integer> selectedSquares = new HashSet<>();

    private boolean   showBorders   = true;
    private Rectangle selectionRect = null;
    private Point     dragStart     = null;

    // --- Control params (set from RecordingViewerFrame / SquareControlDialog)
    private double ctrlMinDensityRatio = 0.0;
    private double ctrlMaxVariability  = Double.MAX_VALUE;
    private double ctrlMinRSquared     = 0.0;
    private String ctrlNeighbourMode   = "Free";

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
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.MAGENTA,
            Color.ORANGE, Color.CYAN
    };

    private JWindow infoPopup;

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
                            }
                            else {
                                selectedSquares.remove(sq.getSquareNumber());
                            }
                            repaint();
                            break;
                        }
                    }
                }

                // 🔹 Always allow popup, even if selection is off
                if (SwingUtilities.isLeftMouseButton(e)) {
                    showSquareInfo(e.getX(), e.getY());
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    hideInfoPopup();
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

    // 🔹 Popup feature
    private void showSquareInfo(int mouseX, int mouseY) {
        if (squares == null || squares.isEmpty()) return;

        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;
        int row = mouseY / squareH;
        int col = mouseX / squareW;
        int index = row * cols + col;
        if (index < 0 || index >= squares.size()) return;

        Square sq = squares.get(index);

        String html = String.format(
                "<html><body style='font-family:sans-serif;font-size:11px;'>"
                        + "<b>Square %d</b><br>"
                        + "Density Ratio: %.2f<br>"
                        + "Variability: %.2f<br>"
                        + "R²: %.2f<br>"
                        + "Tracks: %d<br>"
                        + "<i>(Right-click anywhere to close)</i>"
                        + "</body></html>",
                sq.getSquareNumber(),
                sq.getDensityRatio(),
                sq.getVariability(),
                sq.getRSquared(),
                sq.getTracks() != null ? sq.getTracks().size() : 0
        );

        // 🔹 Create or update popup
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

        // 🔹 Reposition near the clicked square each time
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

    private void hideInfoPopup() {
        if (infoPopup != null) {
            infoPopup.setVisible(false);
            infoPopup.dispose();
            infoPopup = null;
        }
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

        if (squares == null) {
            return;
        }
        int squareW = getWidth() / cols;
        int squareH = getHeight() / rows;
        Graphics2D g2 = (Graphics2D) g;

        // 🟡 stop drawing borders if disabled
        if (!showBorders) {
            // still allow drawing drag rectangle if active
            if (selectionRect != null && selectionEnabled) {
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fill(selectionRect);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(selectionRect);
            }
            return;
        }

        // --- Step 1: Draw borders for assigned or selected squares ---
        for (Square sq : squares) {
            int x = sq.getColNumber() * squareW;
            int y = sq.getRowNumber() * squareH;

            if (sq.isSelected() && sq.getCellId() <= 0) {
                Color fillColor;
                if (backgroundImage instanceof BufferedImage) {
                    int sampleX = Math.min(((BufferedImage) backgroundImage).getWidth() - 1, x + squareW / 2);
                    int sampleY = Math.min(((BufferedImage) backgroundImage).getHeight() - 1, y + squareH / 2);
                    int rgb = ((BufferedImage) backgroundImage).getRGB(sampleX, sampleY);
                    int r = (rgb >> 16) & 0xFF;
                    int bg = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    double brightness = (0.299 * r + 0.587 * bg + 0.114 * b);
                    fillColor = (brightness < 128)
                            ? new Color(255, 255, 255, 60) // translucent white for dark bg
                            : new Color(0, 0, 0, 60);      // translucent black for light bg
                } else {
                    fillColor = new Color(255, 255, 255, 60);
                }

                g2.setColor(fillColor);
                g2.fillRect(x, y, squareW, squareH);
            }
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

    /** Update current control parameters (no UI side-effects here). */
    public void setControlParameters(double densityRatio,
                                     double variability,
                                     double rSquared,
                                     String neighbourMode) {
        this.ctrlMinDensityRatio = densityRatio;
        this.ctrlMaxVariability  = variability;
        this.ctrlMinRSquared     = rSquared;
        this.ctrlNeighbourMode   = (neighbourMode != null) ? neighbourMode : "Free";
    }

    /** Re-apply selection/visibility based on the current control params. */
    public void applyVisibilityFilter() {
        SquareUtils.applyVisibilityFilter(squares, ctrlMinDensityRatio, ctrlMaxVariability, ctrlMinRSquared);
        repaint();
    }
}