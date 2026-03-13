import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a credits JSON file against credits.schema.json.
 *
 * Performs two passes:
 *   1. JSON Schema validation (draft-07) via networknt json-schema-validator.
 *   2. Semantic checks that JSON Schema cannot express:
 *        - category id uniqueness across the categories array
 *        - person category ids all reference defined categories
 */
@SuppressWarnings("Since15")
public class ValidateCreditsJsonTask extends DefaultTask {

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
        ObjectMapper mapper = new ObjectMapper();
        var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        var schema = factory.getSchema(mapper.readTree(schemaFile));
        var errors = schema.validate(mapper.readTree(jsonFile));

        if (!errors.isEmpty()) {
            String msg = errors.stream()
                .map(e -> "  - " + e.getMessage())
                .collect(Collectors.joining("\n"));
            throw new GradleException(jsonFile.getName() + " failed schema validation:\n" + msg);
        }

        JsonNode data = mapper.readTree(jsonFile);
        List<JsonNode> categories = new ArrayList<>();
        data.path("category").forEach(categories::add);
        List<JsonNode> persons = new ArrayList<>();
        data.path("person").forEach(persons::add);

        // Category id uniqueness (not expressible in JSON Schema for object arrays)
        List<String> ids = categories.stream()
            .map(c -> c.path("id").asText())
            .toList();
        Set<String> seen = new HashSet<>();
        Set<String> dupes = new LinkedHashSet<>();
        for (String id : ids) {
            if (!seen.add(id)) dupes.add(id);
        }
        if (!dupes.isEmpty()) {
            throw new GradleException(jsonFile.getName() + ": duplicate category ids: " + dupes);
        }

        // Person category id existence (JSON Schema cannot cross-reference array values)
        Set<String> categoryIdSet = new HashSet<>(ids);
        List<String> violations = new ArrayList<>();
        for (int i = 0; i < persons.size(); i++) {
            JsonNode person = persons.get(i);
            String name = person.path("name").asText();
            JsonNode catNode = person.path("category");
            List<String> personCatIds = new ArrayList<>();
            if (catNode.isTextual()) {
                personCatIds.add(catNode.asText());
            } else {
                catNode.forEach(c -> personCatIds.add(c.asText()));
            }
            for (String id : personCatIds) {
                if (!categoryIdSet.contains(id)) {
                    violations.add("persons[" + i + "] (\"" + name + "\"): unknown category id \"" + id + "\"");
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
        //noinspection BlockingMethodInNonBlockingContext
        Files.writeString(markerFile.toPath(),
            "OK: " + categories.size() + " categories, " + persons.size() + " persons\n");
        getLogger().lifecycle("{} is valid ({} categories, {} persons)",
            jsonFile.getName(), categories.size(), persons.size());
    }
}
