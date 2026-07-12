/*
 * Copyright (c) 2025 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.fiji.trackmate;

import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import paint.shared.config.TrackMateConfig;
import paint.shared.config.paintconfig.PaintConfig;
import paint.ui.dialogs.ProjectDialog;
import paint.shared.io.MainIOInterface;
import paint.shared.objects.ExperimentInfo;
import paint.shared.objects.Project;
import paint.shared.utils.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static paint.fiji.trackmate.RunTrackMateOnRecording.runTrackMateOnRecording;
import static paint.shared.constants.PaintFileNames.EXPERIMENT_INFO_CSV;
import static paint.ui.dialogs.ProjectPathResolver.getValidProjectPath;

/**
 * Main user interface class for running TrackMate interactively within the
 * PAINT environment. Handles initialization, configuration, dialog management,
 * and execution of TrackMate processes.
 */
@SuppressWarnings("unused")
@Plugin(type = Command.class, menuPath = "Plugins>Glyco-PAINT>Run Single")
public class TrackMateUISingle implements Command {

    /**
     * Prevents concurrent execution of multiple TrackMate runs.
     */
    private static volatile boolean               running = false;
    private        volatile TrackMateSingleDialog singleDialog;
    private        volatile String                lastRecordingName = "";
    private        volatile int                   lastThreshold     = 5;

    /**
     * Executes the TrackMate workflow through an interactive GUI dialog.
     * <p>
     * The method:
     * <ul>
     *   <li>Ensures single-instance execution.</li>
     *   <li>Initializes logging and configuration state.</li>
     *   <li>Displays a project dialog for experiment selection.</li>
     *   <li>Runs the appropriate TrackMate pipeline (sweep or standard).</li>
     *   <li>Optionally triggers GENERATE_SQUARES after completion.</li>
     * </ul>
     */
    @Override
    public void run() {

        // ---------------------------------------------------------------------
        // Step 1 – Setup exception handler and concurrency lock
        // ---------------------------------------------------------------------
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                                                          PaintLogger.debugf("AWT complained: %s", throwable.getMessage())
        );

        if (running) {
            showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
            return;
        }

        // ---------------------------------------------------------------------
        // Step 2 – Retrieve project root
        // ---------------------------------------------------------------------
        Path projectPath = getValidProjectPath();
        if (projectPath == null) {
            return;
        }

        // ---------------------------------------------------------------------
        // Step 3 – Initialize logging, configuration, and runtime settings
        // ---------------------------------------------------------------------
        paint.ui.console.PaintConsoleWindow.createConsoleFor("TrackMate");
        PaintConfig.initialise(projectPath);

        String debugLevel = PaintPrefs.getString("Runtime", "Log Level", "INFO");
        PaintLogger.setLevel(debugLevel);
        PaintLogger.initialise(projectPath, "TrackMateOnProject.log");
        PaintLogger.debugf("TrackMateUISingle.run - TrackMate plugin started (Interactive).");

        PaintRuntime.initialiseFromPrefs();

        // Log version and timestamp
        JarInfoLogger.JarInfo info = JarInfoLogger.getJarInfo(TrackMateUISingle.class);
        if (info != null) {
            PaintLogger.infof("Compilation date: %s", info.implementationDate);
            PaintLogger.infof("Version: %s", info.implementationVersion);
        }

        LocalDateTime  now           = LocalDateTime.now();
        String         formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        PaintLogger.infof("Current time: %s", formattedTime);
        PaintLogger.blankline();

        // ---------------------------------------------------------------------
        // Step 4 – Show experiment dialog
        // ---------------------------------------------------------------------
        ProjectDialog projDialog = new ProjectDialog(null, projectPath, ProjectDialog.DialogMode.TRACKMATE_SINGLE);
        paint.ui.console.PaintConsoleWindow.closeOnDialogDispose(projDialog.getDialog());

        // ---------------------------------------------------------------------
        // Step 5 – Handle OK action callback
        projDialog.setCalculationCallback(project -> runTrackMateSingle(project, projDialog));
        projDialog.showDialog();
    }

    // -------------------------------------------------------------------------
    // Utility Methods
    // -------------------------------------------------------------------------

    /**
     * Displays a warning dialog with the specified message.
     *
     * @param message warning message text to display
     */
    @SuppressWarnings("SameParameterValue")
    private void showWarning(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE);
        JDialog warnDialog = optionPane.createDialog(null, "Warning");
        warnDialog.setAlwaysOnTop(true);
        warnDialog.setVisible(true);
    }

    @SuppressWarnings("unused")
    private boolean runTrackMateSingle(Project project, ProjectDialog projDialog) {

        if (running) {
            showWarning("TrackMate processing is already running.\nPlease wait until it finishes.");
            return false;
        }

        // SINGLE mode expects exactly one experiment
        if (project.getExperimentNames() == null || project.getExperimentNames().size() != 1) {
            showWarning("Please select exactly one experiment.");
            return false;
        }

        final Path   projectRoot    = project.getProjectRootPath();
        final Path   imagesPath     = project.getImagesRootPath();
        final String experimentName = project.getExperimentNames().get(0);
        final Path   experimentPath = projectRoot.resolve(experimentName);

        List<ExperimentInfo> infos = MainIOInterface.readExperimentInfo(experimentPath);
        if (infos == null || infos.isEmpty()) {
            showWarning("No ExperimentInfo.csv found or it is empty for:\n" + experimentPath);
            return false;
        }

        List<String> recordingNames = new ArrayList<>();
        for (ExperimentInfo ei : infos) {
            if (ei != null && ei.getRecordingName() != null && !ei.getRecordingName().trim().isEmpty()) {
                recordingNames.add(ei.getRecordingName().trim());
            }
        }

        if (recordingNames.isEmpty()) {
            showWarning("No recording names found in ExperimentInfo.csv.");
            return false;
        }

        // ---------------------------------------------------------------------
        // Show TrackMateSingleDialog (MODELESS) so Calculate does NOT close it
        // ---------------------------------------------------------------------

        // Build TrackMate config exactly how you do it in batch
        final TrackMateConfig trackMateConfig      = new TrackMateConfig();
        final Path            imagePath            = imagesPath.resolve(experimentName);
        final String          initialRecordingName = chooseInitialRecording(recordingNames, lastRecordingName);
        final int             initialThreshold     = chooseInitialThreshold(infos, initialRecordingName, lastThreshold);

        lastRecordingName = initialRecordingName;
        lastThreshold     = initialThreshold;

        PaintLogger.infof(trackMateConfig.toString());

        SwingUtilities.invokeLater(() -> {
            Window owner = projDialog.getDialog();

            // Prevent opening duplicates
            if (singleDialog != null && singleDialog.isDisplayable()) {
                singleDialog.toFront();
                singleDialog.requestFocus();
                return;
            }

            singleDialog = new TrackMateSingleDialog(
                    owner,
                    recordingNames,
                    initialRecordingName,
                    initialThreshold,
                    (recName, threshold) -> handleCalculate(
                            recName,
                            threshold,
                            infos,
                            experimentPath,
                            imagePath,
                            trackMateConfig,
                            projDialog
                    ),
                    (recName, threshold) -> handleSave(
                            recName,
                            threshold,
                            infos,
                            experimentPath
                    )
            );

            singleDialog.setThresholdProvider(recordingName -> chooseInitialThreshold(infos, recordingName, lastThreshold));

            singleDialog.setAlwaysOnTop(true);
            singleDialog.setLocationRelativeTo(owner);
            singleDialog.setVisible(true);
        });

        return true;

    }

    private void handleCalculate(String recName,
            int                  threshold,
            List<ExperimentInfo> infos,
            Path                 experimentPath,
            Path                 imagePath,
            TrackMateConfig      trackMateConfig,
            ProjectDialog        projDialog) throws Exception {

        ExperimentInfo experimentInfoRecord = findExperimentInfoByRecording(infos, recName);
        if (experimentInfoRecord == null) {
            throw new IllegalArgumentException("Selected recording not found in ExperimentInfo.csv: " + recName);
        }

        running = true;
        // Log the recording the user actually selected. This used to log infos.get(0), i.e. the
        // first recording in Experiment Info.csv, whichever one was being processed.
        PaintLogger.infof("   Recording '%s' started TrackMate processing with threshold %d.",
                          experimentInfoRecord.getRecordingName(), threshold);
        try {
            runTrackMateOnRecording(
                    experimentPath,
                    imagePath,
                    trackMateConfig,
                    threshold,
                    experimentInfoRecord,
                    projDialog
            );
        } finally {
            running = false;
        }
        PaintLogger.blankline();

        lastRecordingName = recName;
        lastThreshold     = threshold;
    }

    private void handleSave(String recName,
            int threshold,
            List<ExperimentInfo> infos,
            Path experimentPath) throws Exception {

        ExperimentInfo experimentInfoRecord = findExperimentInfoByRecording(infos, recName);
        if (experimentInfoRecord == null) {
            throw new IllegalArgumentException("Selected recording not found in ExperimentInfo.csv: " + recName);
        }

        experimentInfoRecord.setThreshold(threshold);

        MainIOInterface.writeSpecificExperimentInfoFile(
                experimentPath.resolve(EXPERIMENT_INFO_CSV),
                infos
        );

        lastRecordingName = recName;
        lastThreshold     = threshold;
    }

    private ExperimentInfo findExperimentInfoByRecording(List<ExperimentInfo> infos, String recName) {
        if (infos == null || recName == null) {
            return null;
        }
        String target = recName.trim();

        for (ExperimentInfo ei : infos) {
            if (ei == null) {
                continue;
            }
            String rn = ei.getRecordingName();
            if (rn == null) {
                continue;
            }
            if (rn.trim().equals(target)) {
                return ei;
            }
        }
        return null;
    }

    private String chooseInitialRecording(List<String> recordingNames, String preferred) {
        if (recordingNames == null || recordingNames.isEmpty()) {
            return "";
        }
        if (preferred != null) {
            String p = preferred.trim();
            if (!p.isEmpty()) {
                for (String r : recordingNames) {
                    if (r != null && r.trim().equals(p)) {
                        return p;
                    }
                }
            }
        }
        return recordingNames.get(0).trim();
    }

    private int chooseInitialThreshold(List<ExperimentInfo> infos, String recName, int fallback) {
        ExperimentInfo experimentInfo = findExperimentInfoByRecording(infos, recName);
        if (experimentInfo == null) {
            return fallback;
        }
        double threshold = experimentInfo.getThreshold();
        if (threshold < 1) {
            return 1;
        }
        if (threshold > 50) {
            return 50;
        }
        return (int) threshold;
    }
}