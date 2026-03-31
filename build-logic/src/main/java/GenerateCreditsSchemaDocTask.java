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

        JsonObject defs      = obj(schema, "definitions");
        JsonObject rootProps = obj(schema, "properties");
        JsonObject typeKey   = obj(defs, "key");
        JsonObject catDef    = obj(defs, "category");
        JsonObject catProps  = obj(catDef, "properties");
        JsonObject perDef    = obj(defs, "person");
        JsonObject perProps  = obj(perDef, "properties");

        final String rootDesc       = str(schema, "description");
        final int    versionMin     = integer(obj(rootProps, "version"), "minimum", 1);
        final String categoriesDesc = str(obj(rootProps, "category"), "description");
        final String personsDesc    = str(obj(rootProps, "person"), "description");
        final String catDesc        = str(catDef, "description");
        final String idCell         = cell(obj(catProps, "id"));
        final String classesCell    = cell(obj(catProps, "class"));
        final String perDesc        = str(perDef, "description");
        final int    nameMax        = integer(obj(perProps, "name"), "maxLength", 0);
        final String nameCell       = cell(obj(perProps, "name"));
        final String categoriesCell = cell(obj(perProps, "category"));
        final String rolesCell      = cell(obj(perProps, "role"));
        final String tkComment      = comment(typeKey);
        final int    tkMin          = integer(typeKey, "minLength", 0);
        final int    tkMax          = integer(typeKey, "maxLength", 0);
        final String tkChars        = str(typeKey, "x-characters");

        final String doc = """
            # GTNH Credits: `credits.json` Schema Reference

            _Auto-generated from [`credits.schema.json`](credits.schema.json)._

            ## Contents

            - [Root Object](#root-object)
            - [Category](#category)
            - [Person](#person)
            - [Shared Types](#shared-types)
              - [`key`](#key)

            ---

            ## Root Object

            %s

            | Property | Type | Required | Description |
            |---|---|---|---|
            | `version` | `integer` | - | Integer >= %d; defaults to 1 if absent. |
            | [`category`](#category) | `Category[]` | yes | %s |
            | [`person`](#person) | `Person[]` | - | %s |

            ---

            ## Category

            %s

            | Property | Type | Required | Constraints | Description |
            |---|---|---|---|---|
            | `id` | [`key`](#key) | yes | Unique across all categories *(build-enforced)* | %s |
            | `class` | `string` or `string[]` | - | Unique items if array | %s |

            ---

            ## Person

            %s

            | Property | Type | Required | Constraints | Description |
            |---|---|---|---|---|
            | `name` | `string` | yes | Printable UTF-8, 1-%d chars, no control characters | %s |
            | `category` | [`key`](#key) or `key[]` | yes | Non-empty, unique items if array, each must match a defined category id *(build-enforced)* | %s |
            | `role` | [`key`](#key) or `key[]` | - | Non-empty if array form, unique items | %s |

            ---

            ## Shared Types

            ### `key`

            %s

            | Constraint | Value |
            |---|---|
            | Type | `string` |
            | Min length | %d |
            | Max length | %d |
            | Characters | %s |

            ---

            _JSON Schema draft: [draft-07](https://json-schema.org/draft-07/schema)._
            """.formatted(
                rootDesc,        // Root Object description
                versionMin,      // version minimum
                categoriesDesc,  // categories description
                personsDesc,     // persons description
                catDesc,         // Category intro
                idCell,          // id $comment
                classesCell,     // classes $comment
                perDesc,         // Person intro
                nameMax,         // name maxLength
                nameCell,        // name $comment
                categoriesCell,  // categories $comment
                rolesCell,       // roles $comment
                tkComment,       // key $comment
                tkMin,           // key minLength
                tkMax,           // key maxLength
                tkChars          // key x-characters
            );

        Files.writeString(outputFile.toPath(), doc);
        getLogger().lifecycle("Generated {} -> {}", schemaFile.getName(), outputFile.getName());
    }

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
    private static int integer(JsonObject node, String key, int defaultValue) {
        JsonElement el = node.get(key);
        return (el != null && el.isJsonPrimitive()) ? el.getAsInt() : defaultValue;
    }

    /** Returns the {@code $comment} field as plain text. */
    private static String comment(JsonObject node) {
        return str(node, "$comment");
    }

    /** Returns the {@code $comment} field with pipes escaped for Markdown tables. */
    private static String cell(JsonObject node) {
        return str(node, "$comment").replace("|", "\\|");
    }
}
