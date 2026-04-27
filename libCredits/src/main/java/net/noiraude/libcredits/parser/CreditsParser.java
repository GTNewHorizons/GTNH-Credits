package net.noiraude.libcredits.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public final class CreditsParser {

    /**
     * Mirrors the schema's key pattern: letter start, letter/digit end,
     * letters/digits/spaces/dots/underscores/hyphens inside; 1-32 chars.
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z]([A-Za-z0-9 ._-]*[A-Za-z0-9])?$");

    private CreditsParser() {}

    /**
     * Parses a {@code credits.json} input stream into a {@link CreditsDocument}.
     * The caller is responsible for closing the stream.
     * The returned document's {@link CreditsDocument#persons} list is sorted by name,
     * case-insensitively. {@link CreditsDocument#isDirty()} returns {@code false} on
     * the returned document.
     *
     * @throws IOException           if the stream cannot be read
     * @throws CreditsParseException if the JSON is structurally invalid
     */
    public static CreditsDocument parse(InputStream is) throws IOException, CreditsParseException {
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            JsonObject root;
            try {
                @SuppressWarnings("deprecation") // MC GSON compatible
                JsonObject parsed = new JsonParser().parse(reader)
                    .getAsJsonObject();
                root = parsed;
            } catch (JsonParseException | IllegalStateException e) {
                throw new CreditsParseException("malformed credits.json", e);
            }

            CreditsDocument doc = CreditsDocument.empty();

            for (JsonElement el : root.getAsJsonArray("category")) {
                doc.categories.add(parseCategory(el.getAsJsonObject()));
            }

            if (root.has("person")) {
                for (JsonElement el : root.getAsJsonArray("person")) {
                    doc.persons.add(parsePerson(el.getAsJsonObject()));
                }
                doc.persons.sort(Comparator.comparing(p -> stripFormatting(p.name), String.CASE_INSENSITIVE_ORDER));
            }

            doc.markClean();
            return doc;
        }
    }

    private static DocumentCategory parseCategory(JsonObject obj) throws CreditsParseException {
        String id = requireValidKey(
            obj.get("id")
                .getAsString(),
            "category id");
        DocumentCategory cat = new DocumentCategory(id);
        if (obj.has("class")) {
            cat.classes = new LinkedHashSet<>(readStringOrArray(obj.get("class")));
        }
        return cat;
    }

    private static DocumentPerson parsePerson(JsonObject obj) throws CreditsParseException {
        String name = obj.get("name")
            .getAsString();
        DocumentPerson person = new DocumentPerson(name);
        JsonElement catEl = obj.get("category");
        if (catEl.isJsonArray()) {
            for (JsonElement entry : catEl.getAsJsonArray()) parseMembershipEntry(entry, person);
        } else {
            parseMembershipEntry(catEl, person);
        }
        return person;
    }

    private static void parseMembershipEntry(JsonElement entry, DocumentPerson person) throws CreditsParseException {
        if (entry.isJsonPrimitive()) {
            person.memberships.add(new DocumentMembership(requireValidKey(entry.getAsString(), "category id")));
            return;
        }
        if (entry.isJsonObject()) {
            JsonObject obj = entry.getAsJsonObject();
            java.util.Map.Entry<String, JsonElement> kv = obj.entrySet()
                .iterator()
                .next();
            String catId = requireValidKey(kv.getKey(), "category id");
            List<String> roles = new ArrayList<>();
            for (String role : readStringOrArray(kv.getValue())) {
                roles.add(requireValidKey(role, "role"));
            }
            person.memberships.add(new DocumentMembership(catId, roles));
            return;
        }
        throw new CreditsParseException("person category entry must be a string or object");
    }

    private static List<String> readStringOrArray(JsonElement el) {
        if (el.isJsonPrimitive()) return Collections.singletonList(el.getAsString());
        List<String> result = new ArrayList<>();
        for (JsonElement item : el.getAsJsonArray()) result.add(item.getAsString());
        return result;
    }

    private static String requireValidKey(String s, String field) throws CreditsParseException {
        if (
            KEY_PATTERN.matcher(s)
                .matches()
        ) return s;
        throw new CreditsParseException("invalid " + field + ": \"" + s + "\"");
    }

    /** Strips chat formatting codes ({@code §X}) from {@code s}. {@code §§} is an escaped literal {@code §}. */
    private static String stripFormatting(String s) {
        int len = s.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == '§' && i + 1 < len) {
                if (s.charAt(i + 1) == '§') sb.append('§');
                i++;
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }
}
