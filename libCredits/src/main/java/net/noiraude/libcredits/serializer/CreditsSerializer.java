package net.noiraude.libcredits.serializer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.util.PersonSortKey;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Serializes a {@link CreditsDocument} to the {@code credits.json} format.
 *
 * <p>
 * The output is pretty-printed UTF-8 JSON and is guaranteed to be parseable by
 * {@link net.noiraude.libcredits.parser.CreditsParser}.
 */
public final class CreditsSerializer {

    private CreditsSerializer() {}

    /**
     * Writes {@code doc} as a {@code credits.json} document to {@code out}.
     * The caller is responsible for closing the stream.
     *
     * @throws IOException if the stream cannot be written
     */
    public static void write(CreditsDocument doc, OutputStream out) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", 2);
        root.add("category", serializeCategories(doc.categories));
        if (!doc.persons.isEmpty()) {
            root.add("person", serializePersons(doc.persons));
        }
        String json = new GsonBuilder().setPrettyPrinting()
            .create()
            .toJson(root) + "\n";
        out.write(json.getBytes(StandardCharsets.UTF_8));
    }

    // -----------------------------------------------------------------------

    private static JsonArray serializeCategories(List<DocumentCategory> categories) {
        JsonArray arr = new JsonArray();
        for (DocumentCategory cat : categories) {
            arr.add(serializeCategory(cat));
        }
        return arr;
    }

    private static JsonObject serializeCategory(DocumentCategory cat) {
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

    private static JsonArray serializePersons(List<DocumentPerson> persons) {
        List<DocumentPerson> sorted = new ArrayList<>(persons);
        sorted.sort(Comparator.comparing(p -> PersonSortKey.of(p.name)));
        JsonArray arr = new JsonArray();
        for (DocumentPerson person : sorted) {
            arr.add(serializePerson(person));
        }
        return arr;
    }

    private static JsonObject serializePerson(DocumentPerson person) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", person.name);
        obj.add("category", serializeMemberships(person.memberships));
        return obj;
    }

    private static JsonElement serializeMemberships(List<DocumentMembership> memberships) {
        if (memberships.size() == 1) {
            return serializeMembership(memberships.getFirst());
        }
        JsonArray arr = new JsonArray();
        for (DocumentMembership m : memberships) {
            arr.add(serializeMembership(m));
        }
        return arr;
    }

    private static JsonElement serializeMembership(DocumentMembership m) {
        if (m.roles.isEmpty()) {
            return new JsonPrimitive(m.categoryId);
        }
        JsonObject obj = new JsonObject();
        if (m.roles.size() == 1) {
            obj.addProperty(m.categoryId, m.roles.getFirst());
        } else {
            JsonArray arr = new JsonArray();
            for (String role : m.roles) arr.add(role);
            obj.add(m.categoryId, arr);
        }
        return obj;
    }
}
