package trackMatePaint;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import paint.shared.utils.PaintLogger;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;
import static paint.shared.utils.Miscellaneous.deleteAssociatedFiles;

public class TrackMateExperiment {

    public TrackMateExperiment(Path experimentPath, Path omeroExperimentPath, boolean convert, String sweepCaseName) {
        go(experimentPath, omeroExperimentPath, convert, sweepCaseName);
    }

    void go(Path experimentPath, Path omeroExperimentPath, boolean convertFlag, String caseName) {
        Path experimentInfoFilePath = experimentPath.resolve("Experiment Info.csv");

        System.out.printf("Experiment path: %s%n", experimentPath);
        System.out.printf("Omero Experiment path: %s%n", omeroExperimentPath);
        System.out.printf("Experiment Info Path : %s%n", experimentInfoFilePath);
        processExperimentInfoFile(experimentInfoFilePath, omeroExperimentPath, experimentPath);
    }

    private static boolean checkDirectory(Path path, String label, int rowIndex) {
        if (!Files.isDirectory(path)) {
            System.out.printf("\uD83D\uDEAB Row %2d: %s is not a valid directory: %s%n", rowIndex, label, path);
            return false;
        }
        return true;
    }

    private static boolean validateHeader(List<Map<String, String>> recordings, String[] requiredColumns) {

        boolean validated = true;

        if (recordings.isEmpty()) {
            System.out.println("⚠️ CSV file is empty — no records to validate.");
            validated = false;
        } else {
            Set<String> availableColumns = recordings.get(0).keySet();

            for (String required : requiredColumns) {
                if (!availableColumns.contains(required)) {
                    System.out.printf("\uD83D\uDEAB Missing required column: '%s'%n", required);
                    validated = false;
                }
            }
        }
        return validated;
    }

    private static void processExperimentInfoFile(Path experimentInfoFile,
                                                  Path omeroExperimentPath,
                                                  Path experimentPath) {

        int nrRecordingsInBatchFile = 0;
        int nrRecordingsToProcess = 0;
        int nrRecordingsProcessed = 0;

        final String[] REQUIRED_EXPERIMENT_INFO_COLUMNS = {
                "Recording Sequence Nr", "Recording Name", "Experiment Date", "Experiment Name",
                "Condition Nr", "Replicate Nr", "Probe", "Probe Type", "Cell Type", "Adjuvant",
                "Concentration", "Threshold", "Process"};

        Set<String> yesValues = new HashSet<>(Arrays.asList("y", "yes"));
        List<Map<String, String>> recordings = new ArrayList<>();

        // Read in the batch file into 'records'
        // You can then cycle through it later twice
        try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(experimentInfoFile.toFile()))) {
            Map<String, String> recording;
            while ((recording = reader.readMap()) != null) {
                recordings.add(new HashMap<>(recording)); // Defensive copy
            }
        } catch (IOException | CsvValidationException e) {
            System.out.println("🚫 Error reading CSV file: " + e.getMessage());
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            PaintLogger.errorf("An exception occurred:\n" + sw);
            System.exit(1);
        }

        // Verify that the expected headers are present
        if (!validateHeader(recordings, REQUIRED_EXPERIMENT_INFO_COLUMNS)) {
            System.out.println("🚫 Abort because of invalid 'Experiment Info' file format.");
            System.exit(1);
        }

        // Determine how many recordings need to be processed
        for (Map<String, String> recording : recordings) {
            String process = recording.get("Process");
            if (process != null && yesValues.contains(process.trim().toLowerCase())) {
                nrRecordingsToProcess += 1;
            }
            nrRecordingsInBatchFile += 1;
        }
        if (nrRecordingsInBatchFile == 0) {
            System.out.println("⚠️ There are no recordings in the batch file.");
        }
        else if (nrRecordingsToProcess == 0) {
            System.out.println("✅ There are no recordings that require processing.");
        }
        else {
            System.out.printf("✅ Processing %d recordings out of %d.%n", nrRecordingsToProcess, nrRecordingsInBatchFile);
            // System.out.println("");
        }

        // Delete All Recordings.csv and All Tracks.csv if they exist
        deleteAssociatedFiles(experimentInfoFile);

        // Then do the actual processing
        Instant start = Instant.now();

        // Extend the Experiment Info file with new columns to bring it into All Recordings format
        recordings = addOrderedColumns(recordings);

        // Now cycle through again and fand do the work
        for (Map<String, String> row : recordings) {
            String processFlag = row.get("Process");

            if (processFlag != null && yesValues.contains(processFlag.trim().toLowerCase())) {
                String recordingName = row.get("Recording Name");
                new TrackMateRecording(experimentPath, omeroExperimentPath, recordingName);
            }
        }

        // Now write the All Recordings.csv file
        Path outputFile = experimentInfoFile.getParent().resolve(RECORDINGS_CSV);
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile.toFile()))) {
            // Use headers from the first row (original plus new)
            Set<String> headers = recordings.get(0).keySet();
            writer.writeNext(headers.toArray(new String[0]));

            for (Map<String, String> row : recordings) {
                List<String> values = new ArrayList<>();
                for (String header : headers) {
                    values.add(row.getOrDefault(header, ""));
                }
                writer.writeNext(values.toArray(new String[0]));
            }

            // System.out.println("✅ All Recordings.csv written to: " + outputFile);

        } catch (IOException e) {
            System.out.println("🚫 Failed to write All Recordings.csv: " + e.getMessage());
        }
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        //System.out.println("");
        System.out.printf("✅ Number of recordings processed %d out of %d.%n", nrRecordingsProcessed, nrRecordingsToProcess);
        // System.out.println(String.format("✅ Time taken to process: %s.", formatDuration(duration)));
        System.out.println();
    }

    public static List<Map<String, String>> addOrderedColumns(List<Map<String, String>> records) {

        // Define new columns to add
        String[] newColumns = {
                "Nr Spots", "Nr Tracks", "Run Time", "Ext Recording Name", "Recording Size", "Time Stamp",
                "Max Frame Gap", "Gap Closing Max Distance", "Linking Max Distance", "Median Filtering",
                "Nr Spots in All Tracks", "Min Spots in Track", "Case"
        };

        if (records.isEmpty()) return records;

        // Extract original column order from first row
        List<String> originalColumns = new ArrayList<>(records.get(0).keySet());
        List<String> allColumns = new ArrayList<>(originalColumns);
        allColumns.addAll(Arrays.asList(newColumns));

        List<Map<String, String>> orderedRecords = new ArrayList<>();

        for (Map<String, String> row : records) {
            Map<String, String> orderedRow = new LinkedHashMap<>();

            for (String column : allColumns) {
                String value = row.getOrDefault(column, "");
                orderedRow.put(column, value);
            }

            orderedRecords.add(orderedRow);
        }

        return orderedRecords;
    }
}
