package paint.fijiUtilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;

public class BatchReader {

    public static void main(String[] args) {
        String jsonFileName = "/Users/hans/IdeaProjects/Fiji/src/main/resources/project.json";
        BatchReaderData project = readProjectJson(new File(jsonFileName));

        if (project != null) {
            System.out.println("Project: " + project.project);
            System.out.println("Image Source: " + project.imageSource);
            System.out.println("Experiments to process: " + project.getExperimentsToProcess());
            System.out.println("Experiments to ignore: " + project.getExperimentsToIgnore());
        }
    }

    public static BatchReaderData readProjectJson(File jsonFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try (FileReader reader = new FileReader(jsonFile)) {
            return gson.fromJson(reader, BatchReaderData.class);
        } catch (Exception e) {
            System.err.println("Failed to read project file: " + e.getMessage());
            return null;
        }
    }
}

// ---------- POJOs for Gson ----------


class Experiment {
    public String experimentName;
    public boolean process;
}