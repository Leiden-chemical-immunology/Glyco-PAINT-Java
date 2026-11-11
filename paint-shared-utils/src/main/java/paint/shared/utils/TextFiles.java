/*
 * Copyright (c) 2026 Hans Bakker
 *
 * Licensed under the MIT License. See the LICENSE file in the project root
 * for the full licence text.
 */

package paint.shared.utils;

import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads text files (the CSV files of a project) in the right character set.
 * <p>
 * The application writes UTF-8. Files edited in Excel on Windows may instead be Windows-1252
 * ("CSV (Comma delimited)") or UTF-8 preceded by a byte order mark ("CSV UTF-8"). A file is read as
 * UTF-8 when it is valid UTF-8, otherwise as Windows-1252, which is logged as a warning; a byte
 * order mark is skipped. Text with characters such as µ, é or ° in Windows-1252 is practically
 * never valid UTF-8, so the choice is reliable.
 * <p>
 * Since Java 18 the default character set is UTF-8 on every system, so without this a
 * Windows-1252 file would lose those characters silently (or stop the reading with an error).
 */
public final class TextFiles {

    /** Character set of files saved by Excel on Windows as "CSV (Comma delimited)". */
    public static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private static final char BYTE_ORDER_MARK = '﻿';

    private TextFiles() {
        // Prevent instantiation
    }

    /**
     * The character set to read the file with: UTF-8 if the file is valid UTF-8, otherwise
     * Windows-1252 (with a warning in the log). The file is checked in a streaming pass, so
     * large files are not held in memory.
     *
     * @param file the file to check
     * @return {@link StandardCharsets#UTF_8} or {@link #WINDOWS_1252}
     * @throws IOException if the file cannot be read
     */
    public static Charset charsetOf(Path file) throws IOException {
        CharsetDecoder strictUtf8 = StandardCharsets.UTF_8.newDecoder()
                                                          .onMalformedInput(CodingErrorAction.REPORT)
                                                          .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (Reader r = new InputStreamReader(Files.newInputStream(file), strictUtf8)) {
            char[] buffer = new char[64 * 1024];
            while (r.read(buffer) >= 0) {
                // only checking
            }
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            PaintLogger.warnf("'%s' is not in UTF-8; it was read as Windows-1252 (as saved by Excel on Windows). "
                                      + "Check that special characters such as µ, é or ° came through correctly.",
                              file);
            return WINDOWS_1252;
        }
    }

    /**
     * Opens the file for reading in the character set of {@link #charsetOf(Path)}, after a byte
     * order mark if there is one.
     *
     * @param file the file to read
     * @return a reader over the text of the file
     * @throws IOException if the file cannot be read
     */
    public static BufferedReader newReader(Path file) throws IOException {
        BufferedReader reader = Files.newBufferedReader(file, charsetOf(file));
        reader.mark(1);
        if (reader.read() != BYTE_ORDER_MARK) {
            reader.reset();
        }
        return reader;
    }

    /**
     * Reads the whole file as text, see {@link #newReader(Path)}.
     *
     * @param file the file to read
     * @return the text of the file
     * @throws IOException if the file cannot be read
     */
    public static String readText(Path file) throws IOException {
        String text = Files.readString(file, charsetOf(file));
        return !text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK ? text.substring(1) : text;
    }

    /**
     * Reads the file as lines, see {@link #newReader(Path)}. Like
     * {@link Files#readAllLines(Path)}, the list can be modified.
     *
     * @param file the file to read
     * @return the lines of the file, without line terminators
     * @throws IOException if the file cannot be read
     */
    public static List<String> readAllLines(Path file) throws IOException {
        List<String> lines = Files.readAllLines(file, charsetOf(file));
        if (!lines.isEmpty() && !lines.get(0).isEmpty() && lines.get(0).charAt(0) == BYTE_ORDER_MARK) {
            lines.set(0, lines.get(0).substring(1));
        }
        return lines;
    }

    /**
     * Tablesaw CSV read options for the file, see {@link #newReader(Path)}. The table is named
     * after the file, as {@code Table.read().csv(file)} does.
     *
     * @param file the CSV file to read
     * @return a builder to add further options to
     * @throws IOException if the file cannot be read
     */
    public static CsvReadOptions.Builder csvReadOptions(Path file) throws IOException {
        return CsvReadOptions.builder(newReader(file))
                             .tableName(file.getFileName().toString());
    }
}
