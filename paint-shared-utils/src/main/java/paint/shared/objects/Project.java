/******************************************************************************
 *  Class:        Project.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents a complete PAINT analysis project that encapsulates
 *    configuration data, experiments, and related metadata.
 *
 *  DESCRIPTION:
 *    The {@code Project} class serves as the top-level container for the
 *    PAINT framework, grouping multiple {@link Experiment} objects along
 *    with global configuration instances such as {@link PaintConfig},
 *    {@link TrackMateConfig}, and {@link GenerateSquaresConfig}.
 *    It manages the project structure, file paths, and experiment hierarchy.
 *
 *  KEY FEATURES:
 *    • Encapsulates root paths, configuration, and experiment data.
 *    • Provides constructors for flexible initialization.
 *    • Includes methods for retrieving and managing experiments.
 *    • Provides a detailed string representation for logging.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-10-28
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 ******************************************************************************/

package paint.shared.objects;

import paint.shared.config.GenerateSquaresConfig;
import paint.shared.config.PaintConfig;
import paint.shared.config.TrackMateConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a PAINT project that contains experiments, configuration objects,
 * and references to relevant directories.
 *
 * <p>This class manages metadata and provides accessor methods for project-level
 * configuration and experiment data.</p>
 */
public class Project {

    // ───────────────────────────────────────────────────────────────────────────────
    // ATTRIBUTES
    // ───────────────────────────────────────────────────────────────────────────────

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

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Constructs a fully-specified {@code Project} instance.
     *
     * @param status                the project status
     * @param projectRootPath       the root directory of the project
     * @param imagesRootPath        the images directory path
     * @param experimentNames       list of experiment names
     * @param paintConfig           PAINT configuration instance
     * @param generateSquaresConfig GenerateSquares configuration instance
     * @param trackMateConfig       TrackMate configuration instance
     * @param experiments           list of experiment objects
     */
    public Project(boolean status,
                   Path                  projectRootPath,
                   Path                  imagesRootPath,
                   List<String>          experimentNames,
                   PaintConfig           paintConfig,
                   GenerateSquaresConfig generateSquaresConfig,
                   TrackMateConfig       trackMateConfig,
                   List<Experiment>      experiments) {

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

    /**
     * Constructs a {@code Project} using only a root path and experiments list.
     *
     * @param projectRootPath the project’s root directory
     * @param experiments     list of experiments in the project
     */
    public Project(Path projectRootPath, List<Experiment> experiments) {
        this(false, projectRootPath, null, null, null, null, null, experiments);
    }

    /**
     * Constructs an empty {@code Project} instance.
     */
    public Project() {
        this.experimentNames = new ArrayList<>();
        this.experiments     = new ArrayList<>();
    }

    /**
     * Constructs a {@code Project} with a root path and an empty experiment list.
     *
     * @param projectRootPath the root path of the project
     */
    public Project(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
        this.projectName     = projectRootPath != null ? projectRootPath.getFileName().toString() : "(none)";
        this.experimentNames = new ArrayList<>();
        this.experiments     = new ArrayList<>();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS AND MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    /**
     * Sets the root path for the project and updates the project name.
     *
     * @param projectRootPath the new root directory path
     */
    public void setProjectRootPath(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
        if (projectRootPath != null)
            this.projectName = projectRootPath.getFileName().toString();
    }

    public Path getImagesRootPath() { return imagesRootPath; }
    public void setImagesRootPath(Path imagesRootPath) { this.imagesRootPath = imagesRootPath; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public PaintConfig getPaintConfig() { return paintConfig; }
    public void setPaintConfig(PaintConfig paintConfig) { this.paintConfig = paintConfig; }

    public TrackMateConfig getTrackMateConfig() { return trackMateConfig; }
    public void setTrackMateConfig(TrackMateConfig trackMateConfig) { this.trackMateConfig = trackMateConfig; }

    public GenerateSquaresConfig getGenerateSquaresConfig() { return generateSquaresConfig; }
    public void setGenerateSquaresConfig(GenerateSquaresConfig generateSquaresConfig) { this.generateSquaresConfig = generateSquaresConfig; }

    public List<String> getExperimentNames() { return experimentNames; }
    public void setExperimentNames(List<String> experimentNames) { this.experimentNames = experimentNames; }

    public List<Experiment> getExperiments() { return experiments; }
    public void setExperiments(List<Experiment> experiments) { this.experiments = experiments; }

    // ───────────────────────────────────────────────────────────────────────────────
    // EXPERIMENT MANAGEMENT
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Adds an {@link Experiment} instance to this project.
     *
     * @param experiment the experiment to add
     */
    public void addExperiment(Experiment experiment) {
        if (this.experiments == null)
            this.experiments = new ArrayList<>();
        this.experiments.add(experiment);
    }

    /**
     * Retrieves an {@link Experiment} by name.
     *
     * @param experimentName the name of the experiment
     * @return the matching experiment, or {@code null} if not found
     */
    public Experiment getExperiment(String experimentName) {
        if (experiments == null) return null;
        for (Experiment experiment : experiments) {
            if (experimentName.equals(experiment.getExperimentName())) {
                return experiment;
            }
        }
        return null;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // STRING REPRESENTATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a detailed formatted string representation of the project,
     * including its experiments and recordings.
     *
     * @return formatted project summary string
     */
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

        sb.append(String.format("%nProject %s contains %d experiments%n", projectName, experiments.size()));
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