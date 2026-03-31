package net.noiraude.gtnhcredits;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Validates {@code credits.json} against the credits schema rules.
 *
 * <p>
 * Two groups of tests:
 * <ul>
 * <li>Integration: the bundled {@code credits.json} must produce no errors.</li>
 * <li>Unit: individual rules are verified with crafted synthetic documents.</li>
 * </ul>
 */
public class CreditsJsonValidationTest {

    // -------------------------------------------------------------------------
    // Integration test
    // -------------------------------------------------------------------------

    @Test
    public void bundledCreditsJsonIsValid() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
            Objects.requireNonNull(getClass().getResourceAsStream("/assets/gtnhcredits/credits.json")),
            StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();
            List<String> errors = CreditsValidator.validate(root);
            assertTrue("credits.json has validation errors:\n" + String.join("\n", errors), errors.isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // version field
    // -------------------------------------------------------------------------

    @Test
    public void version_mustBeInteger() {
        JsonObject root = minimalValid();
        root.addProperty("version", "foo");
        assertHasError(root, "version: must be an integer");
    }

    @Test
    public void version_mustBeAtLeastOne() {
        JsonObject root = minimalValid();
        root.addProperty("version", 0);
        assertHasError(root, "version: must be >= 1");
    }

    @Test
    public void version_absentIsAllowed() {
        assertValid(minimalValid());
    }

    // -------------------------------------------------------------------------
    // category array
    // -------------------------------------------------------------------------

    @Test
    public void category_isRequired() {
        JsonObject root = new JsonObject();
        assertHasError(root, "category: required array is missing or not an array");
    }

    @Test
    public void category_mustBeArray() {
        JsonObject root = new JsonObject();
        root.addProperty("category", "not-an-array");
        assertHasError(root, "category: required array is missing or not an array");
    }

    @Test
    public void categoryId_mustBePresent() {
        JsonObject root = new JsonObject();
        JsonArray cats = new JsonArray();
        cats.add(new JsonObject()); // object without id
        root.add("category", cats);
        assertHasError(root, "category[0].id: required field is missing");
    }

    @Test
    public void categoryId_patternEnforced() {
        assertHasError(rootWithOneCategory("1startsWithDigit"), "category[0].id: invalid key format");
    }

    @Test
    public void categoryId_lengthEnforced() {
        String longId = nChars('x', CreditsValidator.MAX_KEY_LENGTH + 1);
        assertHasError(
            rootWithOneCategory(longId),
            "category[0].id: exceeds max length " + CreditsValidator.MAX_KEY_LENGTH);
    }

    @Test
    public void categoryClass_acceptsString() {
        JsonObject root = rootWithOneCategory("cats");
        root.getAsJsonArray("category")
            .get(0)
            .getAsJsonObject()
            .addProperty("class", "person");
        assertValid(root);
    }

    @Test
    public void categoryClass_acceptsArray() {
        JsonObject root = rootWithOneCategory("cats");
        JsonArray cls = new JsonArray();
        cls.add(new com.google.gson.JsonPrimitive("person"));
        cls.add(new com.google.gson.JsonPrimitive("role"));
        root.getAsJsonArray("category")
            .get(0)
            .getAsJsonObject()
            .add("class", cls);
        assertValid(root);
    }

    @Test
    public void categoryClass_rejectsNonString() {
        JsonObject root = rootWithOneCategory("cats");
        root.getAsJsonArray("category")
            .get(0)
            .getAsJsonObject()
            .addProperty("class", 42);
        assertHasError(root, "category[0].class: must be a string");
    }

    // -------------------------------------------------------------------------
    // person array
    // -------------------------------------------------------------------------

    @Test
    public void person_arrayIsOptional() {
        assertValid(rootWithOneCategory("cats"));
    }

    @Test
    public void person_mustBeArray() {
        JsonObject root = rootWithOneCategory("cats");
        root.addProperty("person", "oops");
        assertHasError(root, "person: must be an array");
    }

    @Test
    public void personName_isRequired() {
        JsonObject root = rootWithOnePerson(null, "cats");
        assertHasError(root, "person[0].name: required string is missing");
    }

    @Test
    public void personName_mustNotBeEmpty() {
        JsonObject root = rootWithOnePerson("", "cats");
        assertHasError(root, "person[0].name: must not be empty");
    }

    @Test
    public void personName_lengthEnforced() {
        String longName = nChars('a', CreditsValidator.MAX_NAME_LENGTH + 1);
        JsonObject root = rootWithOnePerson(longName, "cats");
        assertHasError(root, "person[0].name: exceeds max length " + CreditsValidator.MAX_NAME_LENGTH);
    }

    @Test
    public void personCategory_isRequired() {
        JsonObject root = rootWithOneCategory("cats");
        JsonObject person = new JsonObject();
        person.addProperty("name", "Alice");
        JsonArray persons = new JsonArray();
        persons.add(person);
        root.add("person", persons);
        assertHasError(root, "person[0].category: required field is missing");
    }

    @Test
    public void personCategory_emptyArrayRejected() {
        JsonObject root = rootWithOneCategory("cats");
        JsonObject person = new JsonObject();
        person.addProperty("name", "Alice");
        person.add("category", new JsonArray());
        JsonArray persons = new JsonArray();
        persons.add(person);
        root.add("person", persons);
        assertHasError(root, "person[0].category: array must not be empty");
    }

    @Test
    public void personRole_keyFormatEnforced() {
        JsonObject root = rootWithOnePerson("Alice", "cats");
        root.getAsJsonArray("person")
            .get(0)
            .getAsJsonObject()
            .addProperty("role", "bad key!");
        assertHasError(root, "person[0].role: invalid key format");
    }

    // -------------------------------------------------------------------------
    // Semantic checks
    // -------------------------------------------------------------------------

    @Test
    public void duplicateCategoryIds_rejected() {
        JsonObject root = new JsonObject();
        JsonArray cats = new JsonArray();
        JsonObject c1 = new JsonObject();
        c1.addProperty("id", "alpha");
        JsonObject c2 = new JsonObject();
        c2.addProperty("id", "alpha");
        cats.add(c1);
        cats.add(c2);
        root.add("category", cats);
        assertHasError(root, "duplicate category ids");
    }

    @Test
    public void personUnknownCategoryRef_rejected() {
        JsonObject root = rootWithOnePerson("Alice", "nonexistent");
        assertHasError(root, "unknown category id \"nonexistent\"");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Minimal valid document: one category, no persons. */
    private static JsonObject minimalValid() {
        return rootWithOneCategory("alpha");
    }

    private static JsonObject rootWithOneCategory(String id) {
        JsonObject root = new JsonObject();
        JsonArray cats = new JsonArray();
        JsonObject cat = new JsonObject();
        cat.addProperty("id", id);
        cats.add(cat);
        root.add("category", cats);
        return root;
    }

    /**
     * Document with one category {@code catId} and one person. Pass {@code null}
     * for {@code personName} to omit the name field entirely.
     */
    private static JsonObject rootWithOnePerson(String personName, String personCatRef) {
        JsonObject root = rootWithOneCategory("cats");
        JsonObject person = new JsonObject();
        if (personName != null) person.addProperty("name", personName);
        person.addProperty("category", personCatRef);
        JsonArray persons = new JsonArray();
        persons.add(person);
        root.add("person", persons);
        return root;
    }

    /** Returns a string of {@code n} copies of {@code c}, using only Java 8 APIs. */
    private static String nChars(char c, int n) {
        char[] buf = new char[n];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    private static void assertValid(JsonObject root) {
        List<String> errors = CreditsValidator.validate(root);
        assertTrue("Expected no errors but got: " + errors, errors.isEmpty());
    }

    private static void assertHasError(JsonObject root, String substring) {
        List<String> errors = CreditsValidator.validate(root);
        assertFalse("Expected at least one error containing \"" + substring + "\" but got none", errors.isEmpty());
        boolean found = errors.stream()
            .anyMatch(e -> e.contains(substring));
        assertTrue(
            "Expected an error containing \"" + substring + "\" but errors were:\n" + String.join("\n", errors),
            found);
    }
}
