package net.noiraude.libcredits.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.noiraude.libcredits.model.CreditsCategory;
import net.noiraude.libcredits.model.CreditsData;
import net.noiraude.libcredits.model.CreditsPerson;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Serializes a {@link CreditsData} object to the {@code credits.json} format.
 *
 * <p>
 * The output is pretty-printed UTF-8 JSON and is guaranteed to be parseable by
 * {@link net.noiraude.libcredits.parser.CreditsParser}.
 */
public final class CreditsSerializer {

    private CreditsSerializer() {}

    /**
     * Writes {@code data} as a {@code credits.json} document to {@code out}.
     * The caller is responsible for closing the stream.
     *
     * @throws IOException if the stream cannot be written
     */
    public static void write(CreditsData data, OutputStream out) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", 2);
        root.add("category", serializeCategories(data.categories));
        if (!data.persons.isEmpty()) {
            root.add("person", serializePersons(data.persons));
        }
        String json = new GsonBuilder().setPrettyPrinting()
            .create()
            .toJson(root) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
    }

    // -----------------------------------------------------------------------

    private static JsonArray serializeCategories(List<CreditsCategory> categories) {
        JsonArray arr = new JsonArray();
        for (CreditsCategory cat : categories) {
            arr.add(serializeCategory(cat));
        }
        return arr;
    }

    private static JsonObject serializeCategory(CreditsCategory cat) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", cat.id);
        if (!cat.classes.isEmpty()) {
            List<String> sorted = new ArrayList<>(cat.classes);
            Collections.sort(sorted);
            if (sorted.size() == 1) {
                obj.addProperty("class", sorted.getFirst());
            } else {
                JsonArray arr = new JsonArray();
                for (String cls : sorted) arr.add(cls);
                obj.add("class", arr);
            }
        }
        return obj;
    }

    private static JsonArray serializePersons(List<CreditsPerson> persons) {
        JsonArray arr = new JsonArray();
        for (CreditsPerson person : persons) {
            arr.add(serializePerson(person));
        }
        return arr;
    }

    private static JsonObject serializePerson(CreditsPerson person) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", person.name);
        obj.add("category", serializeMemberships(person.categoryRoles));
        return obj;
    }

    private static JsonElement serializeMemberships(Map<String, List<String>> categoryRoles) {
        List<Map.Entry<String, List<String>>> entries = new ArrayList<>(categoryRoles.entrySet());
        if (entries.size() == 1) {
            return serializeEntry(entries.getFirst());
        }
        JsonArray arr = new JsonArray();
        for (Map.Entry<String, List<String>> entry : entries) {
            arr.add(serializeEntry(entry));
        }
        return arr;
    }

    private static JsonElement serializeEntry(Map.Entry<String, List<String>> entry) {
        String catId = entry.getKey();
        List<String> roles = entry.getValue();
        if (roles.isEmpty()) {
            return new JsonPrimitive(catId);
        }
        JsonObject obj = new JsonObject();
        if (roles.size() == 1) {
            obj.addProperty(catId, roles.getFirst());
        } else {
            JsonArray arr = new JsonArray();
            for (String role : roles) arr.add(role);
            obj.add(catId, arr);
        }
        return obj;
    }
}
