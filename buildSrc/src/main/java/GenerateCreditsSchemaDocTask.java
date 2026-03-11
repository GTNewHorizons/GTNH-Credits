import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Generates a Markdown-formatted reference document from credits.schema.json.
 * <p>
 * All descriptions and constraints are pulled directly from the schema so the
 * generated file stays in sync automatically whenever the schema changes.
 */
@SuppressWarnings("Since15")
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
        final JsonNode schema    = new ObjectMapper().readTree(schemaFile);
        final JsonNode defs      = schema.path("definitions");
        final JsonNode rootProps = schema.path("properties");
        final JsonNode typeKey   = defs.path("key");
        final JsonNode catDef    = defs.path("category");
        final JsonNode catProps  = catDef.path("properties");
        final JsonNode perDef    = defs.path("person");
        final JsonNode perProps  = perDef.path("properties");

        final String rootDesc      = schema.path("description").asText();
        final int    versionMin    = rootProps.path("version").path("minimum").asInt();
        final String categoriesDesc = rootProps.path("category").path("description").asText();
        final String personsDesc   = rootProps.path("person").path("description").asText();
        final String catDesc       = catDef.path("description").asText();
        final String idCell        = cell(catProps.path("id"));
        final String classesCell   = cell(catProps.path("class"));
        final String perDesc       = perDef.path("description").asText();
        final int    nameMax       = perProps.path("name").path("maxLength").asInt();
        final String nameCell      = cell(perProps.path("name"));
        final String categoriesCell = cell(perProps.path("category"));
        final String rolesCell     = cell(perProps.path("role"));
        final String tkComment     = comment(typeKey);
        final int    tkMin         = typeKey.path("minLength").asInt();
        final int    tkMax         = typeKey.path("maxLength").asInt();
        final String tkChars       = typeKey.path("x-characters").asText();

        final String doc = """
            # GTNH Credits: `credits.json` Schema Reference

            _Auto-generated from [`credits.schema.json`](src/main/resources/assets/gtnhcredits/credits.schema.json)._

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

        //noinspection BlockingMethodInNonBlockingContext
        Files.writeString(outputFile.toPath(), doc);
        getLogger().lifecycle("Generated {} -> {}", schemaFile.getName(), outputFile.getName());
    }

    /** Returns the {@code $comment} field of this node as plain text. */
    private static String comment(JsonNode node) {
        return node.path("$comment").asText();
    }

    /** Returns the {@code $comment} field of this node with pipes escaped for Markdown tables. */
    private static String cell(JsonNode node) {
        return node.path("$comment").asText().replace("|", "\\|");
    }
}
