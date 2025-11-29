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
import java.nio.file.Paths;

public final class JsonValidator {

    private static final Gson GSON = new GsonBuilder()
            // Strict is default; do not call setLenient(false) — method takes no args.
            .create();

    public static final class Result {
        public final boolean valid;
        public final String error;

        private Result(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }

        public static Result ok()   { return new Result(true, null); }
        public static Result fail(String error) { return new Result(false, error); }
    }

    /** Validates JSON syntax only. */
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
            GSON.fromJson(jr, JsonObject.class);
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

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%4d | %s%n", errLine, line));
        sb.append("     | ")
          .append(spaces(Math.max(0, errCol - 1)))
          .append("^");
        return sb.toString();
    }

    private static String spaces(int n) {
        char[] arr = new char[Math.max(0, n)];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = ' ';
        }
        return new String(arr);
    }

    private JsonValidator() {

    }

    public static void main(String[] args) {
        Result validation = validate(Paths.get("/Users/hans/Paint Test Project/Paint Sweep Configuration error.json"));
        if (validation.valid) {
            System.out.println("Valid JSON");
        } else {
            System.out.println("Invalid JSON: " + validation.error);
        }
    }
}