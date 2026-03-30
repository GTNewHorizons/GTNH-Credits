import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates a credits JSON file against the credits schema rules.
 *
 * Performs two passes:
 *   1. Structural validation: required fields, types, pattern and length constraints.
 *   2. Semantic checks that cannot be expressed structurally:
 *        - category id uniqueness across the categories array
 *        - person category ids all reference defined categories
 */
public class ValidateCreditsJsonTask extends DefaultTask {

    /** Mirrors the schema key pattern. */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z]([A-Za-z0-9 ._-]*[A-Za-z0-9])?$");
    private static final int MAX_KEY_LENGTH  = 32;
    private static final int MAX_NAME_LENGTH = 80;

    private File jsonFile;
    private File schemaFile;
    private File markerFile;

    @InputFile
    public File getJsonFile() { return jsonFile; }
    public void setJsonFile(File f) { jsonFile = f; }

    @InputFile
    public File getSchemaFile() { return schemaFile; }
    public void setSchemaFile(File f) { schemaFile = f; }

    /** Marker file written on success, used by Gradle for up-to-date checking. */
    @OutputFile
    public File getMarkerFile() { return markerFile; }
    public void setMarkerFile(File f) { markerFile = f; }

    @TaskAction
    public void validate() throws IOException {
        JsonObject root;
        try (FileReader reader = new FileReader(jsonFile, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new GradleException(jsonFile.getName() + ": failed to parse JSON: " + e.getMessage(), e);
        }

        List<String> errors = new ArrayList<>();

        // --- Pass 1: structural validation ---

        // version (optional integer >= 1)
        if (root.has("version")) {
            JsonElement v = root.get("version");
            if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber()) {
                errors.add("version: must be an integer");
            } else if (v.getAsInt() < 1) {
                errors.add("version: must be >= 1");
            }
        }

        // category (required array)
        if (!root.has("category") || !root.get("category").isJsonArray()) {
            errors.add("category: required array is missing or not an array");
        }

        // person (optional array)
        if (root.has("person") && !root.get("person").isJsonArray()) {
            errors.add("person: must be an array");
        }

        if (!errors.isEmpty()) fail(errors);

        JsonArray categoryArray = root.getAsJsonArray("category");
        JsonArray personArray = root.has("person") ? root.getAsJsonArray("person") : new JsonArray();

        // validate each category
        for (int i = 0; i < categoryArray.size(); i++) {
            JsonElement el = categoryArray.get(i);
            String prefix = "category[" + i + "]";
            if (!el.isJsonObject()) { errors.add(prefix + ": must be an object"); continue; }
            JsonObject cat = el.getAsJsonObject();
            validateKey(cat, "id", prefix, true, errors);
            if (cat.has("class")) {
                validateStringOrStringArray(cat.get("class"), prefix + ".class", errors);
            }
        }

        // validate each person
        for (int i = 0; i < personArray.size(); i++) {
            JsonElement el = personArray.get(i);
            String prefix = "person[" + i + "]";
            if (!el.isJsonObject()) { errors.add(prefix + ": must be an object"); continue; }
            JsonObject person = el.getAsJsonObject();
            // name: required string, 1-MAX_NAME_LENGTH printable chars
            if (!person.has("name") || !person.get("name").isJsonPrimitive()) {
                errors.add(prefix + ".name: required string is missing");
            } else {
                String name = person.get("name").getAsString();
                if (name.isEmpty()) errors.add(prefix + ".name: must not be empty");
                if (name.length() > MAX_NAME_LENGTH)
                    errors.add(prefix + ".name: exceeds max length " + MAX_NAME_LENGTH);
            }
            // category: required key or key[]
            if (!person.has("category")) {
                errors.add(prefix + ".category: required field is missing");
            } else {
                validateKeyOrKeyArray(person.get("category"), prefix + ".category", errors);
            }
            // role: optional key or key[]
            if (person.has("role")) {
                validateKeyOrKeyArray(person.get("role"), prefix + ".role", errors);
            }
        }

        if (!errors.isEmpty()) fail(errors);

        // --- Pass 2: semantic checks ---

        List<String> ids = new ArrayList<>();
        for (JsonElement el : categoryArray) {
            ids.add(el.getAsJsonObject().get("id").getAsString());
        }

        // category id uniqueness
        Set<String> seen = new HashSet<>();
        Set<String> dupes = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) dupes.add(id);
        }
        if (!dupes.isEmpty()) {
            throw new GradleException(jsonFile.getName() + ": duplicate category ids: " + dupes);
        }

        // person category id existence
        Set<String> categoryIdSet = new HashSet<>(ids);
        List<String> violations = new ArrayList<>();
        for (int i = 0; i < personArray.size(); i++) {
            JsonObject person = personArray.get(i).getAsJsonObject();
            String name = person.get("name").getAsString();
            for (String catId : readStringOrArray(person.get("category"))) {
                if (!categoryIdSet.contains(catId)) {
                    violations.add("person[" + i + "] (\"" + name + "\"): unknown category id \"" + catId + "\"");
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new GradleException(
                jsonFile.getName() + ": unknown category references:\n" +
                    violations.stream().map(v -> "  - " + v).collect(Collectors.joining("\n")));
        }

        File markerDir = markerFile.getParentFile();
        if (markerDir != null && !markerDir.exists() && !markerDir.mkdirs()) {
            throw new GradleException("Failed to create directory: " + markerDir);
        }
        Files.writeString(markerFile.toPath(),
            "OK: " + categoryArray.size() + " categories, " + personArray.size() + " persons\n");
        getLogger().lifecycle("{} is valid ({} categories, {} persons)",
            jsonFile.getName(), categoryArray.size(), personArray.size());
    }

    private void validateKey(JsonObject obj, String field, String prefix, boolean required,
        List<String> errors) {
        if (!obj.has(field)) {
            if (required) errors.add(prefix + "." + field + ": required field is missing");
            return;
        }
        JsonElement el = obj.get(field);
        if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
            errors.add(prefix + "." + field + ": must be a string");
            return;
        }
        String val = el.getAsString();
        if (val.length() > MAX_KEY_LENGTH)
            errors.add(prefix + "." + field + ": exceeds max length " + MAX_KEY_LENGTH);
        if (!KEY_PATTERN.matcher(val).matches())
            errors.add(prefix + "." + field + ": invalid key format \"" + val + "\"");
    }

    private void validateKeyOrKeyArray(JsonElement el, String path, List<String> errors) {
        if (el.isJsonPrimitive()) {
            validateKeyString(el.getAsJsonPrimitive(), path, errors);
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.isEmpty()) { errors.add(path + ": array must not be empty"); return; }
            for (int i = 0; i < arr.size(); i++) {
                JsonElement item = arr.get(i);
                if (!item.isJsonPrimitive()) {
                    errors.add(path + "[" + i + "]: must be a string");
                } else {
                    validateKeyString(item.getAsJsonPrimitive(), path + "[" + i + "]", errors);
                }
            }
        } else {
            errors.add(path + ": must be a string or array of strings");
        }
    }

    private void validateKeyString(JsonPrimitive p, String path, List<String> errors) {
        if (!p.isString()) { errors.add(path + ": must be a string"); return; }
        String val = p.getAsString();
        if (val.length() > MAX_KEY_LENGTH)
            errors.add(path + ": exceeds max length " + MAX_KEY_LENGTH);
        if (!KEY_PATTERN.matcher(val).matches())
            errors.add(path + ": invalid key format \"" + val + "\"");
    }

    private void validateStringOrStringArray(JsonElement el, String path, List<String> errors) {
        if (el.isJsonPrimitive()) {
            if (!el.getAsJsonPrimitive().isString()) errors.add(path + ": must be a string");
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                if (!arr.get(i).isJsonPrimitive() || !arr.get(i).getAsJsonPrimitive().isString())
                    errors.add(path + "[" + i + "]: must be a string");
            }
        } else {
            errors.add(path + ": must be a string or array of strings");
        }
    }

    private static List<String> readStringOrArray(JsonElement el) {
        List<String> result = new ArrayList<>();
        if (el.isJsonPrimitive()) {
            result.add(el.getAsString());
        } else if (el.isJsonArray()) {
            el.getAsJsonArray().forEach(e -> result.add(e.getAsString()));
        }
        return result;
    }

    private void fail(List<String> errors) {
        String msg = errors.stream().map(e -> "  - " + e).collect(Collectors.joining("\n"));
        throw new GradleException(jsonFile.getName() + " failed validation:\n" + msg);
    }
}
