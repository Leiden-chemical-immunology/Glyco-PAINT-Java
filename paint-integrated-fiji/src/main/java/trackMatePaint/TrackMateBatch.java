package trackMatePaint;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.time.Duration;
import java.time.Instant;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;
import paint.shared.utils.AppLogger;

// import paint.utilities.DurationFormatter.formatDuration;

public class TrackMateBatch {

    static final String EXTENSION = "csv";
    private static Logger log;

    public static void main(String[] args) {

        // Change the defaults of the UI
        setupLookAndFeelOfUI();

        // Initialize the logger before anything else uses it

        // Get the batch file name from the user
        File trackMateBatchFile = getTrackMateBatchFile();
        if (trackMateBatchFile == null) {
            log.warning("🚫 No batch file selected.");
            return;
        }
        log.info("✅ Processing TrackMate batch file: " + trackMateBatchFile.getAbsolutePath());

        // Cycle through the batch file and invoke TrackMate
        processTrackMateBatchFile(trackMateBatchFile);
    }

    static File getTrackMateBatchFile() {
        final File[] selectedFile = new File[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select a Batch (.csv) File");

                fileChooser.setFileFilter(
                        new javax.swing.filechooser.FileNameExtensionFilter("CSV Batch Files", EXTENSION)
                );

                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();

                    if (file.exists() && file.isFile() && file.getName().toLowerCase().endsWith("." + EXTENSION)) {
                        selectedFile[0] = file;
                    } else {
                        JOptionPane.showMessageDialog(null,
                                "🚫 The selected file does not exist or is not a valid ." + EXTENSION + " file.",
                                "Invalid File", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else if (result == JFileChooser.CANCEL_OPTION) {
                    selectedFile[0] = null;
                }
            });
        } catch (InterruptedException | InvocationTargetException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw);
            JOptionPane.showMessageDialog(null,
                    "🚫 Error showing file chooser:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        return selectedFile[0];
    }


    public static void processTrackMateBatchFile(File csvFile) {

        int nrExperimentsInBatchFile = 0;
        int nrExperimentsToProcess = 0;
        int nrExperimentsProcessed = 0;

        final String[] REQUIRED_TRACKMATE_BATCH_FILE_COLUMNS = {"Project", "Image Source", "Experiment", "Process"};

        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "yes"));
        List<Map<String, String>> experiments = new ArrayList<>();

        // Read in the batch file into 'records'
        // You can then cycle through it later twice
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(csvFile))) {
            Map<String, String> experiment;
            while ((experiment = reader.readMap()) != null) {
                experiments.add(new HashMap<>(experiment)); // Defensive copy
            }
        } catch (IOException | CsvValidationException e) {
            log.severe("🚫 Error reading TrackMate batch file: " + e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AppLogger.errorf("An exception occurred:\n" + sw);
            System.exit(1);
        }

        // Verify that the expected headers are present
        if (!validateHeaders(experiments, REQUIRED_TRACKMATE_BATCH_FILE_COLUMNS)) {
            log.severe("🚫 Abort because of invalid TrackMate batch file format.");
            System.exit(1);
        }

        // Determine how many recordings need to be processed
        for (Map<String, String> experiment : experiments) {
            String process = experiment.get("Process");
            if (process != null && yesValues.contains(process.trim().toLowerCase())) {
                nrExperimentsToProcess += 1;
            }
            nrExperimentsInBatchFile += 1;
        }
        if (nrExperimentsInBatchFile == 0) {
            log.warning("⚠️ There are no experiments in the batch file.");
        }
        else if (nrExperimentsToProcess == 0) {
            log.info("✅ There are no experiments that require processing.");
        }
        else {
            log.info(String.format("✅ Processing %d experiments out of %d.", nrExperimentsToProcess, nrExperimentsInBatchFile));
            //log.info("");
        }

        // Then do the actual processing
        int rowIndex = 1;
        Instant start = Instant.now();
        for (Map<String, String> experiment : experiments) {

            Path projectPath = Paths.get(experiment.get("Project"));
            Path omeroPath = Paths.get(experiment.get("Image Source"));
            String experimentName = experiment.get("Experiment");
            String processFlag = experiment.get("Process");
            Path experimentPath = projectPath.resolve(experimentName);
            Path omeroExperimentPath = omeroPath.resolve(experimentName);

            if (processFlag != null && yesValues.contains(processFlag.trim().toLowerCase())) {

                if (checkDirectory(projectPath, "Project", rowIndex) &&
                    checkDirectory(omeroPath, "Image Source", rowIndex) &&
                    checkDirectory(experimentPath, "Experiment path", rowIndex) &&
                    checkDirectory(omeroExperimentPath, "Image path", rowIndex)) {

                    log.info("");
                    //log.info("");
                    log.info(String.format("✅ Row %2d: Project='%s', Image='%s', Experiment='%s', Process='%s'",
                            rowIndex, projectPath, omeroPath, experimentName, processFlag));
                    new TrackMateExperiment(experimentPath, omeroExperimentPath, false, "");
                    nrExperimentsProcessed += 1;
                }
                else {
                    log.warning(String.format("🚫️ Row %2d: Skipped because of invalid directories.", rowIndex));
                }
            } else {
                log.warning(String.format("⚠️ Row %2d: Skipped because of 'Process' flag.", rowIndex));
            }

            rowIndex++;
        }
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        log.info("");
        log.info(String.format("✅ Number of experiments processed %d out of %d.", nrExperimentsProcessed, nrExperimentsToProcess));
        // log.info(String.format("✅ Time taken to process: %s.", formatDuration(duration)));
    }

    private static void setupLookAndFeelOfUI() {

        // Set the UI Style to be consistent cross-platform.
        // It also allows showing just the file name.
        // Select a lighter font than the default.

        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            Font plainFont = new Font("Segoe UI", Font.PLAIN, 13);
            String[] keys = {
                    "Label.font", "Tree.font", "List.font", "table.font", "TextField.font", "TextArea.font",
                    "ComboBox.font", "Button.font", "TabbedPane.font", "Menu.font", "MenuItem.font",
                    "PopupMenu.font", "ToolTip.font"
            };
            for (String key : keys) {
                UIManager.put(key, plainFont);
            }
        } catch (Exception e) {
            log.warning("⚠️ Failed to set Look and Feel: " + e.getMessage());
        }
    }

    private static boolean validateHeaders(List<Map<String, String>> records, String[] requiredColumns) {

        boolean validated = true;

        if (records.isEmpty()) {
            log.warning("⚠️ Batch file is empty — no records to validate.");
            validated = false;
        }
        else {
            Set<String> availableColumns = records.get(0).keySet();

            for (String required : requiredColumns) {
                if (!availableColumns.contains(required)) {
                    log.severe(String.format("🚫 Missing required column: '%s'", required));
                    validated = false;
                }
            }
        }
        return validated;
    }

    private static boolean checkDirectory(Path path, String label, int rowIndex) {
        if (!Files.isDirectory(path)) {
            log.warning(String.format("🚫 Row %2d: %s is not a valid directory: %s", rowIndex, label, path));
            return false;
        }
        return true;
    }
}