package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static paint.shared.constants.PaintConstants.RECORDINGS_CSV;

public class FixDuplicateHeaders {

    public static void main(String[] args) throws IOException {
        Path startDir = Paths.get("/Users/hans/Paint Test Project");
        try (Stream<Path> paths = Files.walk(startDir)) {
            paths.filter(p -> p.getFileName().toString().equals(RECORDINGS_CSV))
                    .forEach(FixDuplicateHeaders::processFile);
        }
    }

    private static void processFile(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

            if (lines.size() >= 2) {
                String first = lines.get(0).trim();
                String second = lines.get(1).trim();

                // both first fields start with "Recording Name"
                if (first.startsWith("Recording Name") && second.startsWith("Recording Name")) {
                    System.out.println("Fixing duplicate header in: " + file);
                    List<String> newLines = new ArrayList<>(lines.subList(1, lines.size())); // drop first
                    Files.write(file, newLines, StandardCharsets.UTF_8,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to process file: " + file + " → " + e.getMessage());
        }
    }
}