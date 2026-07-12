/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a scientific or research experiment containing
 * a name and a collection of {@link Recording} instances.
 *
 * <p>Provides constructors for initialization and utility methods
 * for managing the set of associated recordings.</p>
 */
public class Experiment {

    // ───────────────────────────────────────────────────────────────────────────────
    // FIELDS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * The name identifying this experiment.
     */
    private final String experimentName;

    /**
     * The collection of recordings associated with this experiment.
     */
    private final ArrayList<Recording> recordings;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Constructs an {@code Experiment} with a specified name.
     *
     * @param experimentName the name of the experiment
     */
    public Experiment(String experimentName) {
        this.experimentName = experimentName;
        this.recordings     = new ArrayList<>();
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS & MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * @return the name of this experiment.
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Adds a recording to this experiment.
     *
     * @param recording the {@link Recording} to add
     */
    public void   addRecording(Recording recording) {
        this.recordings.add(recording);
    }

    /**
     * @return the list of {@link Recording} objects in this experiment.
     */
    public List<Recording> getRecordings() {
        return recordings;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // STRING REPRESENTATION
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Returns a formatted string summary of the experiment and its recordings.
     *
     * @return formatted string representation of the experiment
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("Experiment: ").append(experimentName).append("\n");
        sb.append("----------------------------------------------------------------------\n");
        sb.append("\n");
        sb.append(String.format("%nExperiment %s has %d recordings%n", experimentName, recordings.size()));
        for (Recording recording : recordings) {
            sb.append(String.format("\t%s%n", recording.getRecordingName()));
        }
        return sb.toString();
    }
}