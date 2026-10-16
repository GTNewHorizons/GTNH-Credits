package net.noiraude.gtnhcredits;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
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

    private static final Gson GSON = new Gson();

    private CreditsValidator() {}

    /**
     * Validates {@code root} and returns all errors found.
     * An empty list means the document is valid.
     */
    public static List<String> validate(JsonObject root) {
        List<String> errors = new ArrayList<>();

        try (InputStream schemaStream = new FileInputStream("credits.schema.json")) {
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

        try {
            net.noiraude.libcredits.parser.CreditsParser.parse(
                new java.io.ByteArrayInputStream(
                    GSON.toJson(root)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            errors.add(ex.getMessage());
        }
        return errors;
    }
}
