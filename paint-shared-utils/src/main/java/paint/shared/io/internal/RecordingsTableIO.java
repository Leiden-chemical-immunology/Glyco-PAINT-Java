/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.io.internal;

import paint.shared.objects.Recording;

import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.util.ArrayList;
import java.util.List;

import static paint.shared.constants.PaintStringConstants.*;

/**
 * Handles low-level I/O and data conversion between PAINT {@link Recording} objects and
 * Tablesaw {@link Table} structures.
 * <p>
 * The {@code RecordingsTableIO} class provides implementation for mapping recording entity
 * fields to CSV columns and vice versa. It manages the schema specific to "recordings.csv"
 * files, ensuring that data types and headers are correctly applied during conversion.
 * </p>
 */
public class RecordingsTableIO extends BaseTableIO {

    // =====================================================================
    //  TABLE CREATION
    // =====================================================================

    /**
     * @return a new empty {@link Table} with the Recordings schema.
     */
    public Table emptyTable() {
        return newEmptyTable("Recordings", Recording.Column.values(), c -> c.header, c -> c.type);
    }

    // =====================================================================
    //  ENTITY → TABLE CONVERSION
    // =====================================================================

    /**
     * Converts a list of {@link Recording} entities into a Tablesaw {@link Table}.
     *
     * @param recordings list of recording objects to convert
     * @return a table containing the recording data
     */
    public Table toTable(List<Recording> recordings) {
        Table table = emptyTable();

        for (Recording recording : recordings) {
            Row row = table.appendRow();

            row.setString(  EXPERIMENT_NAME,                 recording.getExperimentName());
            row.setString(  RECORDING_NAME,                  recording.getRecordingName());
            row.setInt(     CONDITION_NUMBER,                recording.getConditionNumber());
            row.setInt(     REPLICATE_NUMBER,                recording.getReplicateNumber());
            row.setString(  PROBE_NAME,                      recording.getProbeName());
            row.setString(  PROBE_TYPE,                      recording.getProbeType());
            row.setString(  CELL_TYPE,                       recording.getCellType());
            row.setString(  ADJUVANT,                        recording.getAdjuvant());
            row.setDouble(  CONCENTRATION,                   recording.getConcentration());
            row.setBoolean( PROCESS_FLAG,                    recording.isProcessFlagSet());
            row.setDouble(  THRESHOLD,                       recording.getThreshold());
            row.setInt(     NUMBER_OF_SPOTS,                 recording.getNumberOfSpots());
            row.setInt(     NUMBER_OF_TRACKS,                recording.getNumberOfTracks());
            row.setInt(     NUMBER_OF_SQUARES_IN_BACKGROUND, recording.getNumberOfSquaresInBackground());
            row.setInt(     NUMBER_OF_TRACKS_IN_BACKGROUND,  recording.getNumberOfTracksInBackground());
            row.setDouble(  AVERAGE_TRACKS_IN_BACKGROUND,    recording.getAverageTracksInBackGround());
            row.setInt(     NUMBER_OF_SPOTS_IN_ALL_TRACKS,   recording.getNumberOfSpotsInAllTracks());
            row.setInt(     NUMBER_OF_FRAMES,                recording.getNumberOfFrames());
            row.setDouble(  RUN_TIME,                        recording.getRunTime());
            row.setDateTime(TIME_STAMP,                      recording.getTimeStamp());
            row.setBoolean( EXCLUDE,                         recording.isExcluded());
            row.setDouble(  TAU,                             recording.getTau());
            row.setDouble(  R_SQUARED,                       recording.getRSquared());
            row.setDouble(  DENSITY,                         recording.getDensity());
            row.setDouble(  MIN_REQUIRED_DENSITY_RATIO,      recording.getMinRequiredDensityRatio());
            row.setDouble(  MIN_REQUIRED_R_SQUARED,          recording.getMinRequiredRSquared());
            row.setDouble(  MAX_ALLOWABLE_VARIABILITY,       recording.getMaxAllowableVariability());
            row.setString(  NEIGHBOUR_MODE,                  recording.getNeighbourMode());
        }

        return table;
    }

    // =====================================================================
    //  TABLE → ENTITY CONVERSION
    // =====================================================================

    /**
     * Converts a Tablesaw {@link Table} into a list of {@link Recording} entities.
     *
     * @param table the table to convert
     * @return a list of recording objects
     */
    public List<Recording> toEntities(Table table) {
        List<Recording> recordings = new ArrayList<>();

        for (Row tablesawRow : table) {
            Recording recording = new Recording();

            recording.setExperimentName(              tablesawRow.getString(   EXPERIMENT_NAME));
            recording.setRecordingName(               tablesawRow.getString(   RECORDING_NAME));
            recording.setConditionNumber(             tablesawRow.getInt(      CONDITION_NUMBER));
            recording.setReplicateNumber(             tablesawRow.getInt(      REPLICATE_NUMBER));
            recording.setProbeName(                   tablesawRow.getString(   PROBE_NAME));
            recording.setProbeType(                   tablesawRow.getString(   PROBE_TYPE));
            recording.setCellType(                    tablesawRow.getString(   CELL_TYPE));
            recording.setAdjuvant(                    tablesawRow.getString(   ADJUVANT));
            recording.setConcentration(               tablesawRow.getDouble(   CONCENTRATION));
            recording.setProcessFlag(                 tablesawRow.getBoolean(  PROCESS_FLAG));
            recording.setThreshold(                   tablesawRow.getDouble(   THRESHOLD));
            recording.setNumberOfSpots(               tablesawRow.getInt(      NUMBER_OF_SPOTS));
            recording.setNumberOfTracks(              tablesawRow.getInt(      NUMBER_OF_TRACKS));
            recording.setNumberOfSquaresInBackground( tablesawRow.getInt(      NUMBER_OF_SQUARES_IN_BACKGROUND));
            recording.setNumberOfTracksInBackground(  tablesawRow.getInt(      NUMBER_OF_TRACKS_IN_BACKGROUND));
            recording.setAverageTracksInBackGround(   tablesawRow.getDouble(   AVERAGE_TRACKS_IN_BACKGROUND));
            recording.setNumberOfSpotsInAllTracks(    tablesawRow.getInt(      NUMBER_OF_SPOTS_IN_ALL_TRACKS));
            recording.setNumberOfFrames(              tablesawRow.getInt(      NUMBER_OF_FRAMES));
            recording.setRunTime(                     tablesawRow.getDouble(   RUN_TIME));
            recording.setTimeStamp(                   tablesawRow.getDateTime( TIME_STAMP));
            recording.setExcluded(                    tablesawRow.getBoolean(  EXCLUDE));
            recording.setTau(                         tablesawRow.getDouble(   TAU));
            recording.setRSquared(                    tablesawRow.getDouble(   R_SQUARED));
            recording.setDensity(                     tablesawRow.getDouble(   DENSITY));
            recording.setMinRequiredDensityRatio(     tablesawRow.getDouble(   MIN_REQUIRED_DENSITY_RATIO));
            recording.setMinRequiredRSquared(         tablesawRow.getDouble(   MIN_REQUIRED_R_SQUARED));
            recording.setMaxAllowableVariability(     tablesawRow.getDouble(   MAX_ALLOWABLE_VARIABILITY));
            recording.setNeighbourMode(               tablesawRow.getString(   NEIGHBOUR_MODE));

            recordings.add(recording);
        }

        return recordings;
    }
}