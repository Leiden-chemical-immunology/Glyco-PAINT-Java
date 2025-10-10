package viewer.panels;

import javax.swing.*;
import java.awt.*;

public class NavigationPanel {
    public interface Listener {
        void onFirst();
        void onPrev();
        void onNext();
        void onLast();
    }

    private final JPanel root;
    private final JButton firstBtn = new JButton("|<");
    private final JButton prevBtn  = new JButton("<");
    private final JButton nextBtn  = new JButton(">");
    private final JButton lastBtn  = new JButton(">|");

    public NavigationPanel(final Listener listener) {
        root = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        root.add(firstBtn);
        root.add(prevBtn);
        root.add(nextBtn);
        root.add(lastBtn);

        firstBtn.addActionListener(e -> listener.onFirst());
        prevBtn.addActionListener(e -> listener.onPrev());
        nextBtn.addActionListener(e -> listener.onNext());
        lastBtn.addActionListener(e -> listener.onLast());
    }

    public JComponent getComponent() { return root; }

    public void setEnabledState(boolean hasPrev, boolean hasNext) {
        firstBtn.setEnabled(hasPrev);
        prevBtn.setEnabled(hasPrev);
        nextBtn.setEnabled(hasNext);
        lastBtn.setEnabled(hasNext);
    }
}