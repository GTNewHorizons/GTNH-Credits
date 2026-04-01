import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generates a Markdown-formatted reference document from credits.schema.json.
 * <p>
 * All descriptions and constraints are pulled directly from the schema so the
 * generated file stays in sync automatically whenever the schema changes.
 */
public class GenerateCreditsSchemaDocTask extends DefaultTask {

    private File schemaFile;
    private File outputFile;

    @InputFile
    public File getSchemaFile() { return schemaFile; }
    public void setSchemaFile(File f) { schemaFile = f; }

    @OutputFile
    public File getOutputFile() { return outputFile; }
    public void setOutputFile(File f) { outputFile = f; }

    @TaskAction
    public void generate() throws IOException {
        JsonObject schema;
        try (FileReader reader = new FileReader(schemaFile, StandardCharsets.UTF_8)) {
            schema = JsonParser.parseReader(reader).getAsJsonObject();
        }

        JsonObject defs   = obj(schema, "definitions");
        JsonObject typeKey = obj(defs, "key");
        JsonObject catDef  = obj(defs, "category");
        JsonObject perDef  = obj(defs, "person");
        JsonObject pceDef  = obj(defs, "personCategoryEntry");
        JsonObject pceAddlProps = pceObjFormAdditionalProperties(pceDef);

        final String rootDesc    = str(schema, "description");
        final String rootRows    = rootTableRows(requiredSet(schema), obj(schema, "properties"));

        final String catDesc     = str(catDef, "description");
        final String catRows     = defTableRows(requiredSet(catDef), obj(catDef, "properties"));

        final String perDesc     = str(perDef, "description");
        final String perRows     = defTableRows(requiredSet(perDef), obj(perDef, "properties"));

        final String pceDesc     = str(pceDef, "description");
        final String pceTypeLabel = xCell(pceAddlProps, "x-type-label");
        final String pceRolesDesc = descCell(pceAddlProps);

        final String tkComment   = comment(typeKey);
        final String tkType      = str(typeKey, "type");
        final int    tkMin       = integer(typeKey, "minLength");
        final int    tkMax       = integer(typeKey, "maxLength");
        final String tkChars     = str(typeKey, "x-characters");

        final String doc = """
            # GTNH Credits: `credits.json` Schema Reference

            _Auto-generated from [`credits.schema.json`](credits.schema.json)._

            ## Contents

            - [Root Object](#root-object)
            - [Category](#category)
            - [Person](#person)
            - [PersonCategoryEntry](#personcategoryentry)
            - [Shared Types](#shared-types)
              - [`key`](#key)

            ---

            ## Root Object

            %s

            | Property | Type | Required | Description |
            |---|---|---|---|
            %s

            ---

            ## Category

            %s

            | Property | Type | Required | Constraints | Description |
            |---|---|---|---|---|
            %s

            ---

            ## Person

            %s

            | Property | Type | Required | Constraints | Description |
            |---|---|---|---|---|
            %s

            ---

            ## PersonCategoryEntry

            %s

            | Property key | Value type | Description |
            |---|---|---|
            | (category id) | %s | %s |

            ---

            ## Shared Types

            ### `key`

            %s

            | Constraint | Value |
            |---|---|
            | Type | `%s` |
            | Min length | %d |
            | Max length | %d |
            | Characters | %s |

            ---

            _JSON Schema draft: [draft-07](https://json-schema.org/draft-07/schema)._
            """.formatted(
                rootDesc,
                rootRows,
                catDesc,
                catRows,
                perDesc,
                perRows,
                pceDesc,
                pceTypeLabel,
                pceRolesDesc,
                tkComment,
                tkType,
                tkMin,
                tkMax,
                tkChars
            );

        //noinspection BlockingMethodInNonBlockingContext
        Files.writeString(outputFile.toPath(), doc);
        getLogger().lifecycle("Generated {} -> {}", schemaFile.getName(), outputFile.getName());
    }

    // -------------------------------------------------------------------------
    // Table row generators
    // -------------------------------------------------------------------------

    /**
     * Generates rows for a 4-column root-level table (Property, Type, Required, Description).
     * Reads {@code description} for the description column.
     * Property names that carry an {@code x-ref} field are rendered as Markdown links.
     */
    private static String rootTableRows(Set<String> required, JsonObject properties) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String name = entry.getKey();
            JsonObject prop = entry.getValue().getAsJsonObject();
            String nameDisplay = nameDisplay(name, prop);
            String typeLabel   = typeLabel(prop);
            String req         = required.contains(name) ? "yes" : "-";
            String desc        = str(prop, "description").replace("|", "\\|");
            sb.append("| ").append(nameDisplay).append(" | ").append(typeLabel)
              .append(" | ").append(req).append(" | ").append(desc).append(" |");
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * Generates rows for a 5-column definition table (Property, Type, Required, Constraints, Description).
     * Reads {@code $comment} for the description column and {@code x-constraints} for constraints.
     */
    private static String defTableRows(Set<String> required, JsonObject properties) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, JsonElement> entry : properties.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            String name = entry.getKey();
            JsonObject prop = entry.getValue().getAsJsonObject();
            String nameDisplay  = nameDisplay(name, prop);
            String typeLabel    = typeLabel(prop);
            String req          = required.contains(name) ? "yes" : "-";
            String constraints  = xCell(prop, "x-constraints");
            String desc         = commentCell(prop);
            sb.append("| ").append(nameDisplay).append(" | ").append(typeLabel)
              .append(" | ").append(req).append(" | ").append(constraints)
              .append(" | ").append(desc).append(" |");
            sb.append('\n');
        }
        return sb.toString().stripTrailing();
    }

    // -------------------------------------------------------------------------
    // Field helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code required} array of {@code node} as a set, preserving order.
     */
    private static Set<String> requiredSet(JsonObject node) {
        Set<String> result = new LinkedHashSet<>();
        JsonElement req = node.get("required");
        if (req != null && req.isJsonArray()) {
            for (JsonElement el : req.getAsJsonArray()) {
                if (el.isJsonPrimitive()) result.add(el.getAsString());
            }
        }
        return result;
    }

    /**
     * Returns the Markdown display form of a property name.
     * If the property has an {@code x-ref} field the name is wrapped in a link;
     * otherwise it is wrapped in backticks.
     */
    private static String nameDisplay(String name, JsonObject prop) {
        String ref = str(prop, "x-ref");
        return ref.isEmpty() ? "`" + name + "`" : "[`" + name + "`](" + ref + ")";
    }

    /**
     * Returns the type label for a property.
     * Uses {@code x-type-label} if present, otherwise wraps the {@code type} field in backticks.
     */
    private static String typeLabel(JsonObject prop) {
        String xLabel = str(prop, "x-type-label");
        if (!xLabel.isEmpty()) return xLabel;
        String type = str(prop, "type");
        return type.isEmpty() ? "" : "`" + type + "`";
    }

    /**
     * Returns the {@code additionalProperties} object from the object-form branch of a
     * {@code personCategoryEntry}-style definition (the second element of its {@code oneOf} array).
     */
    private static JsonObject pceObjFormAdditionalProperties(JsonObject pceDef) {
        JsonElement oneOfEl = pceDef.get("oneOf");
        if (oneOfEl == null || !oneOfEl.isJsonArray()) return new JsonObject();
        JsonArray oneOf = oneOfEl.getAsJsonArray();
        if (oneOf.size() < 2) return new JsonObject();
        JsonElement objForm = oneOf.get(1);
        if (!objForm.isJsonObject()) return new JsonObject();
        return obj(objForm.getAsJsonObject(), "additionalProperties");
    }

    // -------------------------------------------------------------------------
    // Primitive readers
    // -------------------------------------------------------------------------

    /** Returns the nested object at {@code key}, or an empty object if absent. */
    private static JsonObject obj(JsonObject parent, String key) {
        JsonElement el = parent.get(key);
        return (el != null && el.isJsonObject()) ? el.getAsJsonObject() : new JsonObject();
    }

    /** Returns a string field, or {@code ""} if absent. */
    private static String str(JsonObject node, String key) {
        JsonElement el = node.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsString() : "";
    }

    /** Returns an integer field, or {@code defaultValue} if absent. */
    private static int integer(JsonObject node, String key) {
        JsonElement el = node.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsInt() : 0;
    }

    /** Returns the {@code $comment} field as plain text. */
    private static String comment(JsonObject node) {
        return str(node, "$comment");
    }

    /** Returns the {@code $comment} field with pipes escaped for Markdown tables. */
    private static String commentCell(JsonObject node) {
        return str(node, "$comment").replace("|", "\\|");
    }

    /** Returns the {@code description} field with pipes escaped for Markdown tables. */
    private static String descCell(JsonObject node) {
        return str(node, "description").replace("|", "\\|");
    }

    /** Returns a custom {@code x-*} extension field with pipes escaped for Markdown tables. */
    private static String xCell(JsonObject node, String xKey) {
        return str(node, xKey).replace("|", "\\|");
    }
}
