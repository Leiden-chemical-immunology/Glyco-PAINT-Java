/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.objects;

import paint.shared.config.GenerateSquaresConfig;

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

    private       Path                   projectRootPath;
    private       Path                   imagesRootPath;
    private       String                 projectName;
    private       GenerateSquaresConfig  generateSquaresConfig;
    private       List<String>           experimentNames;
    private final List<Experiment>       experiments;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Constructs a fully-specified {@code Project} instance.
     *
     * @param projectRootPath       the root directory of the project
     * @param imagesRootPath        the images directory path
     * @param experimentNames       list of experiment names
     * @param generateSquaresConfig GenerateSquares configuration instance
     * @param experiments           list of experiment objects
     */
    public Project(Path                  projectRootPath,
                   Path                  imagesRootPath,
                   List<String>          experimentNames,
                   GenerateSquaresConfig generateSquaresConfig,
                   List<Experiment>      experiments) {
        this.projectRootPath       = projectRootPath;
        this.imagesRootPath        = imagesRootPath;
        this.projectName           = projectRootPath != null ? projectRootPath.getFileName().toString() : "(none)";
        this.experimentNames       = experimentNames != null ? experimentNames : new ArrayList<>();
        this.generateSquaresConfig = generateSquaresConfig;
        this.experiments           = experiments != null ? experiments : new ArrayList<>();
    }

    /**
     * Constructs an empty {@code Project} instance.
     */
    public Project() {
        this.experimentNames = new ArrayList<>();
        this.experiments     = new ArrayList<>();
    }


    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS AND MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the root path for the project and updates the project name.
     *
     * @param projectRootPath the new root directory path
     */
    public void setProjectRootPath(Path projectRootPath) {
        this.projectRootPath = projectRootPath;
        if (projectRootPath != null) {
            this.projectName = projectRootPath.getFileName().toString();
        }
    }

    /**
     * @return the root directory path of the project.
     */
    public Path getProjectRootPath() {
        return projectRootPath;
    }

    /**
     * @return the directory path where experiment images are stored.
     */
    public Path getImagesRootPath() {
        return imagesRootPath;
    }

    /**
     * @return the current configuration for the Generate Squares workflow.
     */
    public GenerateSquaresConfig getGenerateSquaresConfig() {
        return generateSquaresConfig;
    }

    /**
     * @param generateSquaresConfig the configuration to set.
     */
    public void setGenerateSquaresConfig(GenerateSquaresConfig generateSquaresConfig) {
        this.generateSquaresConfig = generateSquaresConfig;
    }

    /**
     * @return the list of names of experiments in this project.
     */
    public List<String> getExperimentNames() {
        return experimentNames;
    }

    /**
     * @param experimentNames the list of experiment names to set.
     */
    public void setExperimentNames(List<String> experimentNames) {
        this.experimentNames = experimentNames;
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