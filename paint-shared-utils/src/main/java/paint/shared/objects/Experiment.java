/******************************************************************************
 *  Class:        Experiment.java
 *  Package:      paint.shared.objects
 *
 *  PURPOSE:
 *    Represents a scientific experiment that encapsulates metadata
 *    and associated recordings belonging to a single experiment context.
 *
 *  DESCRIPTION:
 *    This class defines the Experiment entity, which maintains a name
 *    and a collection of {@link Recording} objects. It provides basic
 *    constructors, accessors, and a formatted string representation for
 *    logging or console output.
 *
 *  KEY FEATURES:
 *    • Encapsulates experiment-level metadata.
 *    • Manages a dynamic list of {@link Recording} entities.
 *    • Provides a clear, formatted textual summary via {@link #toString()}.
 *    • Compatible with Java 8 collections.
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

    /** The name identifying this experiment. */
    private String experimentName;

    /** The collection of recordings associated with this experiment. */
    private final ArrayList<Recording> recordings;

    // ───────────────────────────────────────────────────────────────────────────────
    // CONSTRUCTORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Constructs an empty {@code Experiment} instance
     * with an initialized but empty recording list.
     */
    public Experiment() {
        this.recordings = new ArrayList<>();
    }

    /**
     * Constructs an {@code Experiment} with a specified name.
     *
     * @param experimentName the name of the experiment
     */
    public Experiment(String experimentName) {
        this.experimentName = experimentName;
        this.recordings     = new ArrayList<>();
    }

    /**
     * Constructs an {@code Experiment} with a name and pre-populated recordings.
     *
     * @param experimentName the name of the experiment
     * @param recordings the list of recordings to associate
     */
    public Experiment(String experimentName, ArrayList<Recording> recordings) {
        this.experimentName = experimentName;
        this.recordings     = recordings;
    }

    // ───────────────────────────────────────────────────────────────────────────────
    // ACCESSORS & MUTATORS
    // ───────────────────────────────────────────────────────────────────────────────

    /**
     * Sets the experiment name.
     *
     * @param experimentName the name to assign
     */
    public void setExperimentName(String experimentName) {
        this.experimentName = experimentName;
    }

    /**
     * Returns the experiment name.
     *
     * @return the experiment name
     */
    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Adds a {@link Recording} to this experiment.
     *
     * @param recording the recording to add
     */
    public void addRecording(Recording recording) {
        this.recordings.add(recording);
    }

    /**
     * Returns the list of {@link Recording} objects for this experiment.
     *
     * @return list of recordings
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