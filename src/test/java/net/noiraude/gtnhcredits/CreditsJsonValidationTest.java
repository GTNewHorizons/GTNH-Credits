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
 * Validates {@code credits.json} against the credits' schema.
 *
 * <p>
 * Two groups of tests:
 * <ul>
 * <li>Integration: the bundled {@code credits.json} must produce no errors.</li>
 * <li>Unit: individual rules are verified with crafted synthetic documents.</li>
 * </ul>
 *
 * <p>
 * Structural error message format is owned by the JSON Schema library; tests assert only
 * that an error is reported, not its exact text. Semantic error messages (duplicate ids,
 * unknown category references) come from this project's own code and are tested by
 * substring.
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
        assertInvalid(root);
    }

    @Test
    public void version_mustBeAtLeastOne() {
        JsonObject root = minimalValid();
        root.addProperty("version", 0);
        assertInvalid(root);
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
        assertInvalid(new JsonObject());
    }

    @Test
    public void category_mustBeArray() {
        JsonObject root = new JsonObject();
        root.addProperty("category", "not-an-array");
        assertInvalid(root);
    }

    @Test
    public void categoryId_mustBePresent() {
        JsonObject root = new JsonObject();
        JsonArray cats = new JsonArray();
        cats.add(new JsonObject());
        root.add("category", cats);
        assertInvalid(root);
    }

    @Test
    public void categoryId_patternEnforced() {
        assertInvalid(rootWithOneCategory("1startsWithDigit"));
    }

    @Test
    public void categoryId_lengthEnforced() {
        assertInvalid(rootWithOneCategory(nChars('x', 33))); // schema maxLength is 32
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
        assertInvalid(root);
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
        assertInvalid(root);
    }

    @Test
    public void personName_isRequired() {
        assertInvalid(rootWithOnePerson(null, "cats"));
    }

    @Test
    public void personName_mustNotBeEmpty() {
        assertInvalid(rootWithOnePerson("", "cats"));
    }

    @Test
    public void personName_lengthEnforced() {
        assertInvalid(rootWithOnePerson(nChars('a', 81), "cats")); // schema maxLength is 80
    }

    @Test
    public void personCategory_isRequired() {
        JsonObject root = rootWithOneCategory("cats");
        JsonObject person = new JsonObject();
        person.addProperty("name", "Alice");
        JsonArray persons = new JsonArray();
        persons.add(person);
        root.add("person", persons);
        assertInvalid(root);
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
        assertInvalid(root);
    }

    @Test
    public void personRole_keyFormatEnforced() {
        JsonObject root = rootWithOnePerson("Alice", "cats");
        root.getAsJsonArray("person")
            .get(0)
            .getAsJsonObject()
            .addProperty("role", "bad key!");
        assertInvalid(root);
    }

    // -------------------------------------------------------------------------
    // Semantic checks (duplicate ids, unknown refs -- checked by our own code)
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
        assertHasError(rootWithOnePerson("Alice", "nonexistent"), "unknown category id \"nonexistent\"");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
     * Document with one category {@code cats} and one person. Pass {@code null}
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

    private static String nChars(char c, int n) {
        char[] buf = new char[n];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    private static void assertValid(JsonObject root) {
        List<String> errors = CreditsValidator.validate(root);
        assertTrue("Expected no errors but got: " + errors, errors.isEmpty());
    }

    private static void assertInvalid(JsonObject root) {
        List<String> errors = CreditsValidator.validate(root);
        assertFalse("Expected at least one error but document was accepted", errors.isEmpty());
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
