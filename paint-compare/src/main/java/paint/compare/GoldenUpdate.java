/*
 * Copyright (c) 2026 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.compare;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads which regression gates may overwrite (re-bless) their reference with new output.
 * <p>
 * {@code -Dpaint.updateGolden} names the gates, comma-separated:
 * {@code squares} (Generate Squares), {@code trackmate} (TrackMate stage), or both. Naming them
 * one by one means that accepting an intentional change in one gate cannot silently accept an
 * unintended change in the other. The old form {@code -Dpaint.updateGolden=true}, which
 * re-blessed every gate, is refused with an explanation.
 */
public final class GoldenUpdate {

    /** The system property. */
    public static final String PROPERTY = "paint.updateGolden";

    /** Gate name of the Generate Squares gate. */
    public static final String SQUARES = "squares";

    /** Gate name of the TrackMate gate. */
    public static final String TRACKMATE = "trackmate";

    private static final Set<String> KNOWN = Set.of(SQUARES, TRACKMATE);

    private GoldenUpdate() {
    }

    /**
     * @param gate the gate's name ({@link #SQUARES} or {@link #TRACKMATE})
     * @return whether {@code -Dpaint.updateGolden} asks this gate to overwrite its reference
     * @throws IllegalArgumentException if the property holds {@code true} or an unknown name
     */
    public static boolean requestedFor(String gate) {
        return requested(System.getProperty(PROPERTY)).contains(gate);
    }

    static Set<String> requested(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        Set<String> names = Arrays.stream(value.split(","))
                                  .map(s -> s.trim().toLowerCase(Locale.ROOT))
                                  .filter(s -> !s.isEmpty())
                                  .collect(Collectors.toSet());
        for (String name : names) {
            if (!KNOWN.contains(name)) {
                throw new IllegalArgumentException(
                        "-D" + PROPERTY + "=" + value + ": name the gate(s) to re-bless: "
                                + SQUARES + ", " + TRACKMATE + " or " + SQUARES + "," + TRACKMATE
                                + " ('true' is no longer accepted, so one gate cannot be re-blessed by accident)");
            }
        }
        return names;
    }

    /**
     * The {@code src/test/resources} folder of the module that holds {@code testClass}, where a
     * gate writes a re-blessed reference. Found from where the test classes were loaded
     * ({@code <module>/target/test-classes}), so it does not depend on the working directory
     * (Maven and IntelliJ may differ there).
     *
     * @param testClass the gate's test class
     * @return the module's test resources folder
     */
    public static Path sourceTestResources(Class<?> testClass) {
        try {
            Path classes = Path.of(testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path module  = classes.getParent().getParent();   // <module>/target/test-classes
            Path dir     = module.resolve("src").resolve("test").resolve("resources");
            if (!Files.isDirectory(dir)) {
                throw new IllegalStateException("test resources folder not found: " + dir);
            }
            return dir;
        } catch (URISyntaxException e) {
            throw new IllegalStateException("cannot locate the test classes of " + testClass.getName(), e);
        }
    }
}
