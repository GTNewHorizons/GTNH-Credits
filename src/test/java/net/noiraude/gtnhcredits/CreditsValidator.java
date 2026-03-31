package net.noiraude.gtnhcredits;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

/**
 * Validates a parsed credits JSON object against the credits' schema.
 *
 * <p>
 * Performs two passes:
 * <ol>
 * <li>Structural: delegates to the bundled JSON Schema file.</li>
 * <li>Semantic: category id uniqueness; person category references must name defined ids.
 * These constraints cannot be expressed in JSON Schema and are checked here.</li>
 * </ol>
 *
 * <p>
 * Returns a list of error messages. An empty list means the document is valid.
 */
public final class CreditsValidator {

    private static final String SCHEMA_RESOURCE = "/assets/gtnhcredits/credits.schema.json";
    private static final Gson GSON = new Gson();

    private CreditsValidator() {}

    /**
     * Validates {@code root} and returns all errors found.
     * An empty list means the document is valid.
     */
    public static List<String> validate(JsonObject root) {
        List<String> errors = new ArrayList<>();

        try (InputStream schemaStream = CreditsValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (schemaStream == null) {
                errors.add("credits schema not found on classpath: " + SCHEMA_RESOURCE);
                return errors;
            }
            Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
                .getSchema(schemaStream);
            for (Error e : schema.validate(GSON.toJson(root), InputFormat.JSON)) {
                errors.add(e.getMessage());
            }
        } catch (Exception e) {
            errors.add("schema validation error: " + e.getMessage());
            return errors;
        }

        if (!errors.isEmpty()) return errors;

        JsonArray categoryArray = root.getAsJsonArray("category");
        JsonArray personArray = root.has("person") ? root.getAsJsonArray("person") : new JsonArray();
        validateSemantics(categoryArray, personArray, errors);
        return errors;
    }

    private static void validateSemantics(JsonArray categoryArray, JsonArray personArray, List<String> errors) {
        List<String> ids = collectCategoryIds(categoryArray);
        checkDuplicateIds(ids, errors);
        checkPersonCategoryRefs(personArray, new HashSet<>(ids), errors);
    }

    private static List<String> collectCategoryIds(JsonArray categoryArray) {
        List<String> ids = new ArrayList<>();
        for (JsonElement el : categoryArray) {
            if (
                el.isJsonObject() && el.getAsJsonObject()
                    .has("id")
            ) {
                ids.add(
                    el.getAsJsonObject()
                        .get("id")
                        .getAsString());
            }
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
            if (
                !personArray.get(i)
                    .isJsonObject()
            ) continue;
            JsonObject person = personArray.get(i)
                .getAsJsonObject();
            if (!person.has("name") || !person.has("category")) continue;
            String name = person.get("name")
                .getAsString();
            for (String catId : readStringOrArray(person.get("category"))) {
                if (!categoryIdSet.contains(catId)) {
                    errors.add("person[" + i + "] (\"" + name + "\"): unknown category id \"" + catId + "\"");
                }
            }
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
