package net.noiraude.libcredits.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import net.noiraude.libcredits.model.CreditsCategory;
import net.noiraude.libcredits.model.CreditsData;
import net.noiraude.libcredits.model.CreditsPerson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class CreditsParser {

    /**
     * Mirrors the schema's key pattern: letter start, letter/digit end,
     * letters/digits/spaces/dots/underscores/hyphens inside; 1-32 chars.
     */
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z]([A-Za-z0-9 ._-]*[A-Za-z0-9])?$");

    private CreditsParser() {}

    /**
     * Parses a {@code credits.json} input stream into a {@link CreditsData} object.
     * The caller is responsible for closing the stream.
     * The returned {@link CreditsData#persons} list is sorted by name, case-insensitively.
     *
     * @throws IOException           if the stream cannot be read
     * @throws CreditsParseException if the JSON is structurally invalid
     */
    public static CreditsData parse(InputStream is) throws IOException, CreditsParseException {
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            @SuppressWarnings("deprecation") // MC GSON compatible
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();
            List<CreditsCategory> categories = parseCategories(root);
            List<CreditsPerson> persons = parsePersons(root);
            persons.sort(Comparator.comparing(p -> stripFormatting(p.name), String.CASE_INSENSITIVE_ORDER));
            return new CreditsData(categories, persons);
        }
    }

    private static List<CreditsCategory> parseCategories(JsonObject root) throws CreditsParseException {
        List<CreditsCategory> categories = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("category")) {
            categories.add(parseCategory(el.getAsJsonObject()));
        }
        return categories;
    }

    private static CreditsCategory parseCategory(JsonObject obj) throws CreditsParseException {
        String id = requireValidKey(
            obj.get("id")
                .getAsString(),
            "category id");
        Set<String> classes = new HashSet<>();
        if (obj.has("class")) classes.addAll(readStringOrArray(obj.get("class")));
        return new CreditsCategory(id, Collections.unmodifiableSet(classes));
    }

    private static List<CreditsPerson> parsePersons(JsonObject root) throws CreditsParseException {
        List<CreditsPerson> persons = new ArrayList<>();
        if (!root.has("person")) return persons;
        for (JsonElement el : root.getAsJsonArray("person")) {
            persons.add(parsePerson(el.getAsJsonObject()));
        }
        return persons;
    }

    private static CreditsPerson parsePerson(JsonObject obj) throws CreditsParseException {
        String name = obj.get("name")
            .getAsString();
        Map<String, List<String>> categoryRoles = parseCategoryRoles(obj.get("category"));
        return new CreditsPerson(name, categoryRoles);
    }

    private static Map<String, List<String>> parseCategoryRoles(JsonElement catEl) throws CreditsParseException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (catEl.isJsonArray()) {
            for (JsonElement entry : catEl.getAsJsonArray()) parseCategoryEntry(entry, result);
        } else {
            parseCategoryEntry(catEl, result);
        }
        return result;
    }

    private static void parseCategoryEntry(JsonElement entry, Map<String, List<String>> result)
        throws CreditsParseException {
        if (entry.isJsonPrimitive()) {
            result.put(requireValidKey(entry.getAsString(), "category id"), Collections.emptyList());
            return;
        }
        if (entry.isJsonObject()) {
            parseCategoryEntryFromObject(entry.getAsJsonObject(), result);
            return;
        }
        throw new CreditsParseException("person category entry must be a string or object");
    }

    private static void parseCategoryEntryFromObject(JsonObject obj, Map<String, List<String>> result)
        throws CreditsParseException {
        Map.Entry<String, JsonElement> kv = obj.entrySet()
            .iterator()
            .next();
        String catId = requireValidKey(kv.getKey(), "category id");
        result.put(catId, parseRoles(kv.getValue()));
    }

    private static List<String> parseRoles(JsonElement rolesEl) throws CreditsParseException {
        List<String> roles = new ArrayList<>();
        for (String role : readStringOrArray(rolesEl)) {
            roles.add(requireValidKey(role, "role"));
        }
        return roles;
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
