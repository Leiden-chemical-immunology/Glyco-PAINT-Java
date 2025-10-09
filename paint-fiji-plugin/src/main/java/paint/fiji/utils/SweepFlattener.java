package paint.fiji.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static paint.shared.constants.PaintConstants.*;

public class SweepFlattener {

    /**
     * Flattens a sweep parameter directory by concatenating CSVs,
     * merging image directories, and saving ParameterUsed.txt files.
     *
     * @param sweepParamDir the sweep parameter directory (e.g. Sweep/[MAX_FRAME_GAP]-[4])
     * @param deleteSubdirs if true, numeric subdirectories are deleted afterwards
     */
    public static void flattenSweep(Path sweepParamDir, boolean deleteSubdirs) throws IOException {
        Path allRecordingsOut = sweepParamDir.resolve(RECORDINGS_CSV);
        Path allTracksOut = sweepParamDir.resolve(TRACKS_CSV);
        Path brightfieldOut = sweepParamDir.resolve(DIR_BRIGHTFIELD_IMAGES);
        Path trackmateOut = sweepParamDir.resolve(DIR_TRACKMATE_IMAGES);

        Files.createDirectories(brightfieldOut);
        Files.createDirectories(trackmateOut);

        // Prepare CSV writers
        try (
                BufferedWriter recWriter = Files.newBufferedWriter(allRecordingsOut);
                CSVPrinter recPrinter = new CSVPrinter(recWriter, CSVFormat.DEFAULT);
                BufferedWriter tracksWriter = Files.newBufferedWriter(allTracksOut);
                CSVPrinter tracksPrinter = new CSVPrinter(tracksWriter, CSVFormat.DEFAULT)
        ) {
            boolean headerWrittenRecordings = false;
            boolean headerWrittenTracks = false;

            // iterate over numeric subdirs
            try (DirectoryStream<Path> dirs = Files.newDirectoryStream(sweepParamDir)) {
                for (Path sub : dirs) {
                    if (!Files.isDirectory(sub)) {
                        continue;
                    }
                    String name = sub.getFileName().toString();
                    if (!name.matches("\\d+")) {
                        continue; // only numeric dirs like 221012
                    }

                    Path recCsv = sub.resolve("All Recordings.csv");
                    Path tracksCsv = sub.resolve("All Tracks.csv");
                    Path brightfield = sub.resolve("Brightfield Images");
                    Path trackmate = sub.resolve("TrackMate Images");
                    Path paramFile = sub.resolve("ParameterUsed.txt");

                    // concat All Recordings
                    if (Files.exists(recCsv)) {
                        try (Reader in = Files.newBufferedReader(recCsv);
                             CSVParser parser = new CSVParser(in, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                            if (!headerWrittenRecordings) {
                                recPrinter.printRecord(parser.getHeaderMap().keySet());
                                headerWrittenRecordings = true;
                            }
                            for (CSVRecord r : parser) {
                                recPrinter.printRecord(r);
                            }
                        }
                    }

                    // concat All Tracks
                    if (Files.exists(tracksCsv)) {
                        try (Reader in = Files.newBufferedReader(tracksCsv);
                             CSVParser parser = new CSVParser(in, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                            if (!headerWrittenTracks) {
                                tracksPrinter.printRecord(parser.getHeaderMap().keySet());
                                headerWrittenTracks = true;
                            }
                            for (CSVRecord r : parser) {
                                tracksPrinter.printRecord(r);
                            }
                        }
                    }

                    // merge Brightfield Images
                    if (Files.exists(brightfield)) {
                        try (DirectoryStream<Path> imgs = Files.newDirectoryStream(brightfield)) {
                            for (Path img : imgs) {
                                Files.copy(img, brightfieldOut.resolve(img.getFileName()), REPLACE_EXISTING);
                            }
                        }
                    }

                    // merge TrackMate Images
                    if (Files.exists(trackmate)) {
                        try (DirectoryStream<Path> imgs = Files.newDirectoryStream(trackmate)) {
                            for (Path img : imgs) {
                                Files.copy(img, trackmateOut.resolve(img.getFileName()), REPLACE_EXISTING);
                            }
                        }
                    }

                    // save ParameterUsed.txt with suffix
                    if (Files.exists(paramFile)) {
                        Path target = sweepParamDir.resolve("ParameterUsed-" + name + ".txt");
                        Files.copy(paramFile, target, REPLACE_EXISTING);
                    }

                    // optionally delete numeric subdir
                    if (deleteSubdirs) {
                        deleteRecursively(sub);
                    }
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }
}