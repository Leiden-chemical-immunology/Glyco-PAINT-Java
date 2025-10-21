package utils;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility for cleaning up generated CSV files and experiment image directories
 * in a Paint project workspace.
 * <p>
 * The tool supports both command-line and programmatic usage.
 * </p>
 *
 * <h3>Command-line usage</h3>
 * <pre>{@code
 * java utils.CleanupUtility <root-directory> <mode> [--dry-run] [--old | --all]
 * }</pre>
 *
 * <h4>Modes</h4>
 * <ul>
 *   <li>{@code TRACKMATE} – deletes {@code Recordings*}, {@code All Tracks*},
 *       and {@code SQUARES_CSV*}  files, plus the directories
 *       {@code Brightfield Images} and {@code TrackMate Images}.</li>
 *   <li>{@code GENERATE_SQUARES} – deletes only {@code SQUARES_CSV*} files.</li>
 * </ul>
 *
 * <h4>Options</h4>
 * <ul>
 *   <li>{@code --dry-run} – only prints what would be deleted, without deleting anything.</li>
 *   <li>{@code --old} – targets legacy CSV names (without {@code " Java"} suffix).</li>
 *   <li>{@code --all} – targets both legacy and new CSV names. If {@code --old} and
 *       {@code --all} are both specified, {@code --all} takes precedence.</li>
 * </ul>
 *
 * <h3>Programmatic usage</h3>
 * <pre>{@code
 * CleanupUtility.runCleanup(Paths.get("/path/to/project"), "TRACKMATE", true, false, true);
 * }</pre>
 * This call would perform a dry run of deleting all matching files (both legacy and new names)
 * in TRACKMATE mode.
 */
public class CleanupUtility {

    // --- Constants for file names ---
    private static final String RECORDINGS_CSV = "Recordings.csv";
    private static final String TRACKS_CSV = "Tracks.csv";
    private static final String SQUARES_CSV = "Squares.csv";

    private static final String RECORDINGS_OLD = "All Recordings.csv";
    private static final String TRACKS_OLD = "All Tracks.csv";
    private static final String SQUARES_OLD = "All Squares.csv";

    private static final String DIR_BRIGHTFIELD = "Brightfield Images";
    private static final String DIR_TRACKMATE = "TrackMate Images";

    /**
     * Command-line entry point.
     * <p>
     * Parses CLI arguments, validates options, and delegates to
     * {@link #runCleanup(Path, String, boolean, boolean, boolean)}.
     * Terminates the JVM with a non-zero status code on errors.
     * </p>
     *
     * @param args CLI arguments (see class-level JavaDoc for usage)
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            printUsageAndExit();
        }

        final Path rootDir = Paths.get(args[0]);
        if (!Files.isDirectory(rootDir)) {
            System.err.println("The specified path is not a directory: " + rootDir);
            System.exit(1);
        }

        String rawMode = args[1];
        String mode = normalizeMode(rawMode);
        if (!"TRACKMATE".equals(mode) && !"GENERATE_SQUARES".equals(mode)) {
            System.err.println("Invalid mode: " + rawMode);
            System.err.println("Valid modes: TRACKMATE or GENERATE_SQUARES");
            System.exit(2);
        }

        // Parse options
        boolean dryRun = false;
        boolean old = false;
        boolean all = false;

        for (int i = 2; i < args.length; i++) {
            String opt = normalizeOption(args[i]);
            switch (opt) {
                case "--dry-run":
                    dryRun = true;
                    break;
                case "--old":
                    old = true;
                    break;
                case "--all":
                    all = true;
                    break;
                default:
                    System.err.println("Warning: Ignoring unknown option '" + args[i] + "'");
                    break;
            }
        }

        // If both are specified, prefer --all
        if (old && all) {
            System.err.println("Warning: Both --old and --all specified. Defaulting to --all.");
            old = false;
        }

        try {
            runCleanup(rootDir, mode, dryRun, old, all);
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
            System.exit(3);
        }
    }

    /**
     * Executes cleanup of CSV files and experiment directories.
     *
     * @param rootDir project root directory
     * @param mode    cleanup mode, must be either {@code TRACKMATE} or {@code GENERATE_SQUARES}
     * @param dryRun  if {@code true}, only print what would be deleted without removing anything
     * @param old     if {@code true}, target only legacy CSV names (without {@code " Java"} suffix);
     *                ignored if {@code all} is {@code true}
     * @param all     if {@code true}, target both legacy and new CSV names (overrides {@code old} if both are set)
     * @throws IOException if filesystem traversal or deletion fails
     */
    public static void runCleanup(Path rootDir, String mode, boolean dryRun, boolean old, boolean all) throws IOException {
        final List<String> recordingsTargets = all
                ? Arrays.asList(RECORDINGS_CSV, RECORDINGS_OLD)
                : Collections.singletonList(old ? RECORDINGS_OLD : RECORDINGS_CSV);

        final List<String> tracksTargets = all
                ? Arrays.asList(TRACKS_CSV, TRACKS_OLD)
                : Collections.singletonList(old ? TRACKS_OLD : TRACKS_CSV);

        final List<String> squaresTargets = all
                ? Arrays.asList(SQUARES_CSV, SQUARES_OLD)
                : Collections.singletonList(old ? SQUARES_OLD : SQUARES_CSV);

        final AtomicInteger deleteCount = new AtomicInteger(0);

        // Phase 1: CSV deletions
        Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if ("TRACKMATE".equals(mode)) {
                    if (recordingsTargets.contains(fileName) || tracksTargets.contains(fileName) || squaresTargets.contains(fileName)) {
                        handleFile(file, dryRun, deleteCount);
                    }
                } else if ("GENERATE_SQUARES".equals(mode)) {
                    if (squaresTargets.contains(fileName)) {
                        handleFile(file, dryRun, deleteCount);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        // Phase 2: directory deletions (always in TRACKMATE mode)
        if ("TRACKMATE".equals(mode)) {
            deleteTargetDirectories(rootDir, dryRun, deleteCount);
        }

        // summary
        if (deleteCount.get() == 0) {
            System.out.println("No matching files or directories found under " + rootDir);
        } else if (dryRun) {
            System.out.println("Dry run complete. " + deleteCount.get() + " item(s) would be deleted.");
        } else {
            System.out.println("Deleted " + deleteCount.get() + " item(s).");
        }
    }

    // --- Helpers (unchanged) ---

    private static void deleteTargetDirectories(Path root, final boolean dryRun, final AtomicInteger count) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                String name = dir.getFileName() != null ? dir.getFileName().toString() : "";
                if (equalsDirName(name, DIR_BRIGHTFIELD) || equalsDirName(name, DIR_TRACKMATE)) {
                    handleDirectory(dir, dryRun, count);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static boolean equalsDirName(String actual, String target) {
        return actual != null && target != null && actual.trim().equalsIgnoreCase(target.trim());
    }

    private static void handleFile(Path file, boolean dryRun, AtomicInteger count) throws IOException {
        if (dryRun) {
            System.out.println("[Dry run] Would delete file: " + file);
        } else {
            System.out.println("Deleting file: " + file);
            Files.delete(file);
        }
        count.incrementAndGet();
    }

    private static void handleDirectory(Path dir, boolean dryRun, AtomicInteger count) throws IOException {
        if (dryRun) {
            System.out.println("[Dry run] Would delete directory: " + dir);
        } else {
            System.out.println("Deleting directory: " + dir);
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        count.incrementAndGet();
    }

    private static String normalizeMode(String raw) {
        if (raw == null) return "";
        String m = raw.trim().toUpperCase().replace('-', '_').replace(" ", "");
        if ("GENERATESQUARES".equals(m)) return "GENERATE_SQUARES";
        return m;
    }

    private static String normalizeOption(String raw) {
        if (raw == null) return "";
        String o = raw.replace('\u2013', '-')
                .replace('\u2014', '-')
                .trim()
                .toLowerCase();
        switch (o) {
            case "--dry-run":
                return "--dry-run";
            case "--old":
                return "--old";
            case "--all":
                return "--all";
        }
        return o;
    }

    private static void printUsageAndExit() {
        System.err.println("Usage: java utils.CleanupUtility <root-directory> <mode> [--dry-run] [--old | --all]");
        System.err.println("  <mode>    : TRACKMATE | GENERATE_SQUARES");
        System.err.println("  --dry-run : show what would be deleted without deleting");
        System.err.println("  --old     : target legacy CSV names without \" Java\"");
        System.err.println("  --all     : target both legacy and new CSV names (overrides --old if both are given)");
        System.err.println();
        System.err.println("If neither --old nor --all is specified, only new-style CSV names are targeted.");
        System.exit(1);
    }
}