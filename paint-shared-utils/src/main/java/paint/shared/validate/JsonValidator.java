/*=============================================================================
 *  Class:        JsonValidator.java
 *  Package:      paint.shared.validate
 *
 *  PURPOSE:
 *    Provides utility methods to validate the syntax of JSON configuration
 *    files using the Google GSON library.
 *
 *  DESCRIPTION:
 *    The {@code JsonValidator} reads a JSON file and attempts to parse it
 *    to check for structural errors. If parsing fails, it generates a
 *    detailed error report including the line and column number of the
 *    failure, along with a visual snippet of the code where the error
 *    occurred.
 *
 *  KEY FEATURES:
 *    • Strict JSON syntax validation.
 *    • Contextual error reporting with line/column pointers.
 *    • Lightweight integration with GSON.
 *
 *  AUTHOR:
 *    Hans Bakker
 *
 *  MODULE:
 *    paint-shared-utils
 *
 *  UPDATED:
 *    2025-12-31
 *
 *  COPYRIGHT:
 *    © 2025 Hans Bakker. All rights reserved.
 *=============================================================================*/

package paint.shared.validate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Provides utility methods to validate the syntax of JSON configuration
 * files using the Google GSON library.
 */
public final class JsonValidator {

    private static final Gson GSON = new GsonBuilder()
            // Strict is default; do not call setLenient(false) — method takes no args.
            .create();

    /**
     * Represents the result of a JSON validation attempt.
     */
    public static final class Result {
        /** True if the JSON is syntactically valid. */
        public final boolean valid;
        /** The error message if validation failed, otherwise null. */
        public final String error;

        private Result(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        /** @return a successful validation result */
        public static Result ok()   { return new Result(true, null); }

        /**
         * @param error the error message
         * @return a failed validation result
         */
        public static Result fail(String error) { return new Result(false, error); }
    }

    /**
     * Validates JSON syntax for the specified file.
     *
     * @param jsonPath path to the JSON file
     * @return a {@link Result} indicating success or failure
     */
    public static Result validate(Path jsonPath) {
        String content;
        try {
            content = new String(Files.readAllBytes(jsonPath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Result.fail("Cannot read file: " + e.getMessage());
        }

        try (Reader r = new StringReader(content)) {
            JsonReader jr = new JsonReader(r);
            jr.setLenient(false); // enforce strict JSON
            JsonObject parsed = GSON.fromJson(jr, JsonObject.class);

            // Gson returns null — without throwing — for input that contains no JSON at all.
            // An empty or blank file is not a valid configuration, and reporting it as valid
            // would mask a truncated or half-written file.
            if (parsed == null) {
                return Result.fail("File is empty: expected a JSON object.");
            }
            return Result.ok();
        } catch (MalformedJsonException | com.google.gson.JsonSyntaxException ex) {
            return Result.fail(buildErrorMessage(ex.getMessage(), content, jsonPath));
        } catch (Exception ex) {
            return Result.fail("Unexpected error while validating: " + ex.getMessage());
        }
    }

    private static String buildErrorMessage(String msg, String content, Path path) {
        int line = extractInt(msg, "line ", " ");
        int col  = extractInt(msg, "column ", " ");

        String context = extractContext(content, line, col);

        return "Malformed JSON in '" + path.getFileName() + "'\n"
                + "Line " + line + ", column " + col + "\n\n"
                + context + "\n\n"
                + "Error: " + msg;
    }

    @SuppressWarnings("SameParameterValue")
    private static int extractInt(String msg, String key, String delim) {
        if (msg == null) {
            return 1;
        }
        int idx = msg.indexOf(key);
        if (idx < 0) {
            return 1;
        }
        idx += key.length();
        int end = msg.indexOf(delim, idx);
        if (end < 0) {
            end = msg.length();
        }
        try {
            return Integer.parseInt(msg.substring(idx, end).trim());
        } catch (Exception e) {
            return 1;
        }
    }

    private static String extractContext(String content, int errLine, int errCol) {
        if (content == null) {
            return "";
        }
        String[] lines = content.split("\\R", -1);
        if (errLine < 1 || errLine > lines.length) {
            return "";
        }

        String line = lines[errLine - 1];

        return String.format("%4d | %s%n", errLine, line) +
                "     | " +
                spaces(Math.max(0, errCol - 1)) +
                "^";
    }

    private static String spaces(int n) {
        char[] arr = new char[Math.max(0, n)];
        Arrays.fill(arr, ' ');
        return new String(arr);
    }

    private JsonValidator() {

    }
}