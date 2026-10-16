package net.noiraude.libcredits.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.CreditsKeys;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;
import net.noiraude.libcredits.validation.CreditsValidator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Reads version-2 documents and version-3 documents containing external translation references. */
public final class CreditsParser {

    public static final int CURRENT_VERSION = 3;

    private CreditsParser() {}

    /**
     * Reads and validates UTF-8 credits without taking ownership of the supplied stream.
     * Persons are sorted by name, ignoring formatting and case; the result is marked clean.
     * Legacy version-2 username metadata is ignored, and combined category objects are expanded.
     * Unsupported versions and malformed or inconsistent documents produce a CreditsParseException.
     */
    public static CreditsDocument parse(InputStream input) throws IOException, CreditsParseException {
        try {
            @SuppressWarnings("deprecation") // Minecraft's Gson predates JsonParser.parseReader.
            JsonObject root = new JsonParser().parse(new InputStreamReader(input, StandardCharsets.UTF_8))
                .getAsJsonObject();
            properties(root, "$schema", "version", "category", "person");
            JsonElement versionElement = root.get("version");
            if (
                versionElement == null || !versionElement.isJsonPrimitive()
                    || !versionElement.getAsJsonPrimitive()
                        .isNumber()
            ) {
                throw new CreditsParseException("version must be an integer");
            }
            int version = versionElement.getAsBigDecimal()
                .intValueExact();
            if (version != 2 && version != CURRENT_VERSION) {
                throw new CreditsParseException(
                    "Unsupported credits format version " + version + "; supported versions: 2, 3");
            }
            if (root.has("$schema")) string(root.get("$schema"));
            CreditsDocument document = CreditsDocument.empty();
            for (JsonElement value : root.getAsJsonArray("category")) {
                JsonObject category = value.getAsJsonObject();
                properties(category, "id", "class");
                DocumentCategory entry = new DocumentCategory(id(string(category.get("id")), "category id"));
                if (category.has("class")) {
                    List<String> classes = strings(category.get("class"));
                    if (new LinkedHashSet<>(classes).size() != classes.size()) {
                        throw new CreditsParseException("duplicate category class markers");
                    }
                    entry.classes.addAll(classes);
                }
                document.categories.add(entry);
            }
            if (root.has("person")) {
                for (JsonElement value : root.getAsJsonArray("person")) {
                    document.persons.add(person(value.getAsJsonObject(), version));
                }
            }
            List<String> errors = CreditsValidator.validate(document);
            if (!errors.isEmpty()) throw new CreditsParseException(String.join("; ", errors));
            document.persons.sort(Comparator.comparing(p -> stripFormatting(p.name), String.CASE_INSENSITIVE_ORDER));
            document.markClean();
            return document;
        } catch (RuntimeException ex) {
            throw new CreditsParseException("malformed credits.json: " + ex.getMessage(), ex);
        }
    }

    private static DocumentPerson person(JsonObject object, int version) throws CreditsParseException {
        if (version == 2) properties(object, "name", "category", "username");
        else properties(object, "name", "category");
        DocumentPerson person = new DocumentPerson(string(object.get("name")));
        JsonElement category = object.get("category");
        if (category.isJsonArray()) {
            if (
                category.getAsJsonArray()
                    .size() == 0
            ) throw new CreditsParseException("person category list must not be empty");
            for (JsonElement entry : category.getAsJsonArray()) membership(entry, person, version);
        } else {
            membership(category, person, version);
        }
        return person;
    }

    private static void membership(JsonElement element, DocumentPerson person, int version)
        throws CreditsParseException {
        if (element.isJsonPrimitive()) {
            person.memberships.add(new DocumentMembership(id(string(element), "category id")));
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (
            version != 2 && object.entrySet()
                .size() != 1
        ) throw new CreditsParseException("category membership must contain exactly one category");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            List<DocumentRole> roles = new ArrayList<>();
            JsonElement labels = entry.getValue();
            if (labels.isJsonArray()) {
                if (
                    labels.getAsJsonArray()
                        .size() == 0
                ) throw new CreditsParseException("role list must not be empty");
                for (JsonElement label : labels.getAsJsonArray()) roles.add(role(label, version));
            } else {
                roles.add(role(labels, version));
            }
            person.memberships.add(new DocumentMembership(id(entry.getKey(), "category id"), roles));
        }
    }

    /** Reads one version-3 role JSON value, for explicit references in tabular imports. */
    public static DocumentRole parseRoleJson(String json) throws CreditsParseException {
        try {
            @SuppressWarnings("deprecation")
            JsonElement value = new JsonParser().parse(json);
            return role(value, CURRENT_VERSION);
        } catch (RuntimeException ex) {
            throw new CreditsParseException("invalid role JSON: " + ex.getMessage(), ex);
        }
    }

    private static DocumentRole role(JsonElement element, int version) throws CreditsParseException {
        if (element.isJsonPrimitive()) return DocumentRole.local(string(element));
        if (version < CURRENT_VERSION)
            throw new CreditsParseException("translationKey references require credits format version 3");
        JsonObject reference = element.getAsJsonObject();
        properties(reference, "translationKey");
        return DocumentRole.translation(string(reference.get("translationKey")));
    }

    private static String id(String value, String field) throws CreditsParseException {
        if (!CreditsKeys.isValidId(value)) throw new CreditsParseException("invalid " + field + ": \"" + value + "\"");
        return value;
    }

    private static String string(JsonElement element) {
        if (
            element == null || !element.isJsonPrimitive()
                || !element.getAsJsonPrimitive()
                    .isString()
        ) {
            throw new IllegalArgumentException("expected a string");
        }
        return element.getAsString();
    }

    private static List<String> strings(JsonElement element) {
        if (element.isJsonPrimitive()) return Collections.singletonList(string(element));
        List<String> result = new ArrayList<>();
        for (JsonElement value : element.getAsJsonArray()) result.add(string(value));
        return result;
    }

    private static void properties(JsonObject object, String... allowed) throws CreditsParseException {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (
                !Arrays.asList(allowed)
                    .contains(entry.getKey())
            ) {
                throw new CreditsParseException("unknown property: " + entry.getKey());
            }
        }
    }

    private static String stripFormatting(String value) {
        StringBuilder stripped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '§' && i + 1 < value.length()) {
                if (value.charAt(i + 1) == '§') stripped.append('§');
                i++;
            } else stripped.append(character);
        }
        return stripped.toString();
    }
}
