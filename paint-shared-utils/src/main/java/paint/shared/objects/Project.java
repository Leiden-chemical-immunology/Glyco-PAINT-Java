package paint.shared.objects;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Project {

    // -------------------------------------------------------------------------
    // Attributes
    // -------------------------------------------------------------------------

    // @formatter:off
    private boolean                status;
    private Path                   projectRootPath;
    private Path                   imagesRootPath;
    private String                 projectName;
    private PaintConfig            paintConfig;
    private TrackMateConfig        trackMateConfig;
    private GenerateSquaresConfig  generateSquaresConfig;
    private List<String>           experimentNames;
    private List<Experiment>       experiments;
    // @formatter:on

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public Project(boolean status,
                   Path projectRootPath,
                   Path imagesRootPath,
                   List<String> experimentNames,
                   PaintConfig paintConfig,
                   GenerateSquaresConfig generateSquaresConfig,
                   TrackMateConfig trackMateConfig,
                   List<Experiment> experiments) {

        this.status                = status;
        this.projectRootPath       = projectRootPath;
        this.imagesRootPath        = imagesRootPath;
        this.projectName           = projectRootPath != null ? projectRootPath.getFileName().toString() : "(none)";
        this.experimentNames       = experimentNames != null ? experimentNames : new ArrayList<>();
        this.paintConfig           = paintConfig;
        this.generateSquaresConfig = generateSquaresConfig;
        this.trackMateConfig       = trackMateConfig;
        this.experiments           = experiments != null ? experiments : new ArrayList<>();
    }

    public Project(Path projectRootPath, List<Experiment> experiments) {
        this(false, projectRootPath, null, null, null, null, null, experiments);
    }

    public Project() {
        this.experimentNames = new ArrayList<>();
        this.experiments     = new ArrayList<>();
    }

    public Project(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
        this.projectName     = projectRootPath != null ? projectRootPath.getFileName().toString() : "(none)";
        this.experimentNames = new ArrayList<>();
        this.experiments     = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------------------

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Path getProjectRootPath() {
        return projectRootPath;
    }

    public void setProjectRootPath(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
        if (projectRootPath != null)
            this.projectName = projectRootPath.getFileName().toString();
    }

    public Path getImagesRootPath() {
        return imagesRootPath;
    }

    public void setImagesRootPath(Path imagesRootPath) {
        this.imagesRootPath = imagesRootPath;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public PaintConfig getPaintConfig() {
        return paintConfig;
    }

    public void setPaintConfig(PaintConfig paintConfig) {
        this.paintConfig = paintConfig;
    }

    public TrackMateConfig getTrackMateConfig() {
        return trackMateConfig;
    }

    public void setTrackMateConfig(TrackMateConfig trackMateConfig) {
        this.trackMateConfig = trackMateConfig;
    }

    public GenerateSquaresConfig getGenerateSquaresConfig() {
        return generateSquaresConfig;
    }

    public void setGenerateSquaresConfig(GenerateSquaresConfig generateSquaresConfig) {
        this.generateSquaresConfig = generateSquaresConfig;
    }

    public List<String> getExperimentNames() {
        return experimentNames;
    }

    public void setExperimentNames(List<String> experimentNames) {
        this.experimentNames = experimentNames;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
    }

    public void addExperiment(Experiment experiment) {
        if (this.experiments == null) {
            this.experiments = new ArrayList<>();
        }
        this.experiments.add(experiment);
    }

    public Experiment getExperiment(String experimentName) {
        if (experiments == null) return null;
        for (Experiment experiment : experiments) {
            if (experimentName.equals(experiment.getExperimentName())) {
                return experiment;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // toString()
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("Project: ").append(projectName).append("\n");
        sb.append("----------------------------------------------------------------------\n\n");

        if (experiments == null || experiments.isEmpty()) {
            sb.append("No experiments.\n");
            return sb.toString();
        }

        sb.append(String.format("%nExperiment %s has %d experiments%n", projectName, experiments.size()));
        for (Experiment experiment : experiments) {
            sb.append(String.format("\t%s%n", experiment.getExperimentName()));
        }

        for (Experiment experiment : experiments) {
            sb.append("\n").append(experiment);
            List<Recording> recordings = experiment.getRecordings();
            for (Recording rec : recordings) {
                sb.append("\n").append(rec);
            }
        }

        return sb.toString();
    }
}