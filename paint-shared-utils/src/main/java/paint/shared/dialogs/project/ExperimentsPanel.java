package paint.shared.dialogs.project;

import paint.shared.config.paintconfig.PaintConfig;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static paint.shared.constants.PaintConstants.EXPERIMENT_INFO_CSV;

public class ExperimentsPanel {

    private final JPanel          panel = new JPanel(new BorderLayout());
    private final JPanel          list  = new JPanel();
    private final List<JCheckBox> boxes = new ArrayList<>();

    private Runnable onChanged = () -> {
    };

    public ExperimentsPanel(Path projectRoot) {
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(680, 240));
        scroll.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

        JPanel controls   = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton selectAll = new JButton("Select All");
        JButton clearAll  = new JButton("Clear All");
        controls.add(selectAll);
        controls.add(clearAll);

        selectAll.addActionListener(e -> {
            for (JCheckBox cb : boxes) {
                cb.setSelected(true);
            }
            onChanged.run();
        });
        clearAll.addActionListener(e -> {
            for (JCheckBox cb : boxes) {
                cb.setSelected(false);
            }
            onChanged.run();
        });

        panel.add(controls, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);

        reload(projectRoot);
    }

    public JPanel component() {
        return panel;
    }

    public void onSelectionChanged(Runnable r) {
        this.onChanged = (r != null ? r : () -> {
        });
    }

    public boolean anySelected() {
        for (JCheckBox cb : boxes) {
            if (cb.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public List<String> selectedExperimentNames() {
        List<String> out = new ArrayList<>();
        for (JCheckBox cb : boxes) {
            if (cb.isSelected()) {
                out.add(cb.getText());
            }
        }
        return out;
    }

    public void setEnabled(boolean enabled) {
        for (JCheckBox cb : boxes) {
            cb.setEnabled(enabled);
        }
    }

    public void reload(Path projectRoot) {
        list.removeAll();
        boxes.clear();

        File[] subs = (projectRoot != null ? projectRoot.toFile().listFiles() : null);
        if (subs != null) {
            Arrays.sort(subs, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File sub : subs) {
                if (!sub.isDirectory()) {
                    continue;
                }

                File ei = new File(sub, EXPERIMENT_INFO_CSV);
                if (!ei.isFile()) {
                    continue;
                }
                if ("Sweep".equals(sub.getName())) {
                    continue;
                }

                JCheckBox cb = new JCheckBox(sub.getName());
                boolean saved = PaintConfig.getBoolean("Experiments", sub.getName(), false);
                cb.setSelected(saved);
                cb.addActionListener(e -> onChanged.run());
                boxes.add(cb);
                list.add(cb);
            }
        }
        list.revalidate();
        list.repaint();
        onChanged.run();
    }
}