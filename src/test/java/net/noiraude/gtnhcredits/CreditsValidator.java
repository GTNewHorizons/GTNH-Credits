package net.noiraude.gtnhcredits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Validates a parsed credits JSON object against the credits schema rules.
 *
 * <p>
 * Performs two passes:
 * <ol>
 * <li>Structural: required fields, types, pattern and length constraints.</li>
 * <li>Semantic: category id uniqueness; person category references must name defined ids.</li>
 * </ol>
 *
 * <p>
 * Returns a list of error messages. An empty list means the document is valid.
 */
public final class CreditsValidator {

    /** Mirrors the schema key pattern. */
    static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z]([A-Za-z0-9 ._-]*[A-Za-z0-9])?$");
    static final int MAX_KEY_LENGTH = 32;
    static final int MAX_NAME_LENGTH = 80;

    private CreditsValidator() {}

    /**
     * Validates {@code root} and returns all errors found.
     * An empty list means the document is valid.
     */
    public static List<String> validate(JsonObject root) {
        List<String> errors = new ArrayList<>();

        validateTopLevel(root, errors);
        if (!errors.isEmpty()) return errors;

        JsonArray categoryArray = root.getAsJsonArray("category");
        JsonArray personArray = root.has("person") ? root.getAsJsonArray("person") : new JsonArray();

        validateCategories(categoryArray, errors);
        validatePersons(personArray, errors);
        if (!errors.isEmpty()) return errors;

        validateSemantics(categoryArray, personArray, errors);
        return errors;
    }

    // --- Pass 1 helpers ---

    private static void validateTopLevel(JsonObject root, List<String> errors) {
        if (root.has("version")) validateVersion(root.get("version"), errors);

        if (
            !root.has("category") || !root.get("category")
                .isJsonArray()
        ) {
            errors.add("category: required array is missing or not an array");
        }
        if (
            root.has("person") && !root.get("person")
                .isJsonArray()
        ) {
            errors.add("person: must be an array");
        }
    }

    private static void validateVersion(JsonElement v, List<String> errors) {
        if (
            !v.isJsonPrimitive() || !v.getAsJsonPrimitive()
                .isNumber()
        ) {
            errors.add("version: must be an integer");
        } else if (v.getAsInt() < 1) {
            errors.add("version: must be >= 1");
        }
    }

    private static void validateCategories(JsonArray categoryArray, List<String> errors) {
        for (int i = 0; i < categoryArray.size(); i++) {
            JsonElement el = categoryArray.get(i);
            String prefix = "category[" + i + "]";
            if (!el.isJsonObject()) {
                errors.add(prefix + ": must be an object");
                continue;
            }
            JsonObject cat = el.getAsJsonObject();
            validateCategoryId(cat, prefix, errors);
            if (cat.has("class")) validateStringOrStringArray(cat.get("class"), prefix + ".class", errors);
        }
    }

    private static void validatePersons(JsonArray personArray, List<String> errors) {
        for (int i = 0; i < personArray.size(); i++) {
            JsonElement el = personArray.get(i);
            String prefix = "person[" + i + "]";
            if (!el.isJsonObject()) {
                errors.add(prefix + ": must be an object");
                continue;
            }
            validatePerson(el.getAsJsonObject(), prefix, errors);
        }
    }

    private static void validatePerson(JsonObject person, String prefix, List<String> errors) {
        validatePersonName(person, prefix, errors);
        if (!person.has("category")) {
            errors.add(prefix + ".category: required field is missing");
        } else {
            validateKeyOrKeyArray(person.get("category"), prefix + ".category", errors);
        }
        if (person.has("role")) validateKeyOrKeyArray(person.get("role"), prefix + ".role", errors);
    }

    private static void validatePersonName(JsonObject person, String prefix, List<String> errors) {
        if (
            !person.has("name") || !person.get("name")
                .isJsonPrimitive()
        ) {
            errors.add(prefix + ".name: required string is missing");
            return;
        }
        String name = person.get("name")
            .getAsString();
        if (name.isEmpty()) errors.add(prefix + ".name: must not be empty");
        if (name.length() > MAX_NAME_LENGTH) errors.add(prefix + ".name: exceeds max length " + MAX_NAME_LENGTH);
    }

    // --- Pass 2 helper ---

    private static void validateSemantics(JsonArray categoryArray, JsonArray personArray, List<String> errors) {
        List<String> ids = collectCategoryIds(categoryArray);
        checkDuplicateIds(ids, errors);
        checkPersonCategoryRefs(personArray, new HashSet<>(ids), errors);
    }

    private static List<String> collectCategoryIds(JsonArray categoryArray) {
        List<String> ids = new ArrayList<>();
        for (JsonElement el : categoryArray) {
            ids.add(
                el.getAsJsonObject()
                    .get("id")
                    .getAsString());
        }
        return ids;
    }

    private static void checkDuplicateIds(List<String> ids, List<String> errors) {
        Set<String> seen = new HashSet<>();
        Set<String> dupes = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) dupes.add(id);
        }
        if (!dupes.isEmpty()) errors.add("duplicate category ids: " + dupes);
    }

    private static void checkPersonCategoryRefs(JsonArray personArray, Set<String> categoryIdSet, List<String> errors) {
        for (int i = 0; i < personArray.size(); i++) {
            JsonObject person = personArray.get(i)
                .getAsJsonObject();
            String name = person.get("name")
                .getAsString();
            for (String catId : readStringOrArray(person.get("category"))) {
                if (!categoryIdSet.contains(catId)) {
                    errors.add("person[" + i + "] (\"" + name + "\"): unknown category id \"" + catId + "\"");
                }
            }
        }
    }

    // --- Leaf validators ---

    private static void validateCategoryId(JsonObject cat, String prefix, List<String> errors) {
        if (!cat.has("id")) {
            errors.add(prefix + ".id: required field is missing");
            return;
        }
        JsonElement el = cat.get("id");
        if (
            !el.isJsonPrimitive() || !el.getAsJsonPrimitive()
                .isString()
        ) {
            errors.add(prefix + ".id: must be a string");
            return;
        }
        validateKeyValue(el.getAsString(), prefix + ".id", errors);
    }

    private static void validateKeyOrKeyArray(JsonElement el, String path, List<String> errors) {
        if (el.isJsonPrimitive()) {
            validateKeyString(el.getAsJsonPrimitive(), path, errors);
        } else if (el.isJsonArray()) {
            validateKeyArray(el.getAsJsonArray(), path, errors);
        } else {
            errors.add(path + ": must be a string or array of strings");
        }
    }

    private static void validateKeyArray(JsonArray arr, String path, List<String> errors) {
        if (arr.size() == 0) {
            errors.add(path + ": array must not be empty");
            return;
        }
        for (int i = 0; i < arr.size(); i++) {
            JsonElement item = arr.get(i);
            if (!item.isJsonPrimitive()) {
                errors.add(path + "[" + i + "]: must be a string");
            } else {
                validateKeyString(item.getAsJsonPrimitive(), path + "[" + i + "]", errors);
            }
        }
    }

    private static void validateKeyString(JsonPrimitive p, String path, List<String> errors) {
        if (!p.isString()) {
            errors.add(path + ": must be a string");
            return;
        }
        validateKeyValue(p.getAsString(), path, errors);
    }

    private static void validateKeyValue(String val, String path, List<String> errors) {
        if (val.length() > MAX_KEY_LENGTH) errors.add(path + ": exceeds max length " + MAX_KEY_LENGTH);
        if (
            !KEY_PATTERN.matcher(val)
                .matches()
        ) errors.add(path + ": invalid key format \"" + val + "\"");
    }

    private static void validateStringOrStringArray(JsonElement el, String path, List<String> errors) {
        if (el.isJsonPrimitive()) {
            if (
                !el.getAsJsonPrimitive()
                    .isString()
            ) errors.add(path + ": must be a string");
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                if (
                    !arr.get(i)
                        .isJsonPrimitive()
                        || !arr.get(i)
                            .getAsJsonPrimitive()
                            .isString()
                ) errors.add(path + "[" + i + "]: must be a string");
            }
        } else {
            errors.add(path + ": must be a string or array of strings");
        }
    }

    static List<String> readStringOrArray(JsonElement el) {
        List<String> result = new ArrayList<>();
        if (el.isJsonPrimitive()) {
            result.add(el.getAsString());
        } else if (el.isJsonArray()) {
            el.getAsJsonArray()
                .forEach(e -> result.add(e.getAsString()));
        }
        return result;
    }
}
