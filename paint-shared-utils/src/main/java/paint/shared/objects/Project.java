package paint.shared.objects;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Project {

    //
    // Attributes
    //

    // @formatter:off
    boolean                      status;
    public Path                  projectRootPath;
    public Path                  imagesRootPath;
    public String                projectName;
    public PaintConfig           paintConfig;
    public TrackMateConfig       trackMateConfig;
    public GenerateSquaresConfig generateSquaresConfig;
    public List<String>          experimentNames;
    public List<Experiment>      experiments;
    // @formatter:on

    //
    // Constructors
    //

    public Project(boolean status,
                   Path projectRootPath,
                   Path imagesRootPath,
                   List<String> experimentNames,
                   PaintConfig paintConfig,
                   GenerateSquaresConfig generateSquaresConfig,
                   TrackMateConfig trackMateConfig,
                   List<Experiment> experiments) {

        // @formatter:off
        this.status                = status;
        this.projectRootPath       = projectRootPath;
        this.imagesRootPath        = imagesRootPath;
        this.projectName           = projectRootPath.getFileName().toString();
        this.experimentNames       = experimentNames;
        this.paintConfig           = paintConfig;
        this.generateSquaresConfig = generateSquaresConfig;
        this.trackMateConfig       = trackMateConfig;
        // @formatter:on

        if (experiments == null) {
            this.experiments = new ArrayList<>();
        } else {
            this.experiments = experiments;
        }
    }

    public Project(Path projectRootPath,
                   List<Experiment> experiments) {

        // @formatter:off
        this.status                = false;
        this.projectRootPath       = projectRootPath;
        this.imagesRootPath        = null;
        this.projectName           = projectRootPath.getFileName().toString();
        this.experimentNames       = experimentNames;
        this.paintConfig           = null;
        this.generateSquaresConfig = null;
        this.trackMateConfig       = null;
        // @formatter:on

        if (experiments == null) {
            this.experiments = new ArrayList<>();
        } else {
            this.experiments = experiments;
        }
    }

    public Project() {
        this.experimentNames = new ArrayList<>();
    }

    public Project(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
        this.projectName = projectRootPath.getFileName().toString();
    }


    // Getters and setters

    public String getProjectName() {
        return projectName;
    }

    public Path getProjectRootPath() {
        return projectRootPath;
    }

    public void setProjectRootPath(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public Experiment getExperiment(String experimentName) {
        for (Experiment experiment : experiments) {
            if (experiment.getExperimentName().equals(experimentName)) {
                return experiment;
            }
        }
        return null;
    }

    public void setExperiments(List<Experiment> experiments) {  // ToDo Should maybe make a deep copy
        this.experiments = experiments;
    }

    public void addExperiment(Experiment experiment) { // ToDo Should maybe make a deep copy
        // this.experiments.add(experiment);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("Project: ").append(projectName).append("\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("\n");

        sb.append("\n");
        sb.append(String.format("%nExperiment %s has %d experiment%n", projectName, experiments.size()));
        for (Experiment experiment : experiments) {
            sb.append(String.format("\t%s%n", experiment.getExperimentName()));
        }

        for (Experiment experiment : experiments) {
            sb.append("\n");
            sb.append(experiment);
            List<Recording> recordings = experiment.getRecordings();
            for (Recording rec : recordings) {
                sb.append("\n");
                sb.append(rec);
            }
        }
        return sb.toString();
    }

}
