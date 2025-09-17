package paint.fijiUtilities;

import java.util.ArrayList;
import java.util.List;

public class BatchReaderData {
    public String project;
    public String imageSource;
    public List<Experiment> experiments;

    public List<String> getExperimentsToProcess() {
        List<String> list = new ArrayList<>();
        for (Experiment e : experiments) {
            if (e.process) list.add(e.experimentName);
        }
        return list;
    }

    public List<String> getExperimentsToIgnore() {
        List<String> list = new ArrayList<>();
        for (Experiment e : experiments) {
            if (!e.process) list.add(e.experimentName);
        }
        return list;
    }
}
