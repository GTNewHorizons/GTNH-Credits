package net.noiraude.libcredits.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;
import net.noiraude.libcredits.parser.CreditsParser;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("unused")
public class CreditsSerializerTest {

    // -----------------------------------------------------------------------
    // Round-trip helpers
    // -----------------------------------------------------------------------

    private static CreditsDocument roundTrip(CreditsDocument doc) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreditsSerializer.write(doc, out);
        return CreditsParser.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    private static CreditsDocument oneCategory(String id, String... classes) {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory(id);
        cat.classes = new LinkedHashSet<>(Arrays.asList(classes));
        doc.categories.add(cat);
        return doc;
    }

    private static DocumentPerson person(String name, String catId, String... roles) {
        DocumentPerson p = new DocumentPerson(name);
        p.memberships.add(new DocumentMembership(catId, localRoles(roles)));
        return p;
    }

    // -----------------------------------------------------------------------
    // Category serialization
    // -----------------------------------------------------------------------

    @Test
    public void category_noClasses_omitsClassField() throws Exception {
        CreditsDocument rt = roundTrip(oneCategory("team"));
        assertEquals(1, rt.categories.size());
        assertEquals("team", rt.categories.get(0).id);
        assertTrue(rt.categories.get(0).classes.isEmpty());
    }

    @Test
    public void category_singleClass_roundTrips() throws Exception {
        CreditsDocument rt = roundTrip(oneCategory("team", "person"));
        assertEquals(1, rt.categories.get(0).classes.size());
        assertTrue(rt.categories.get(0).classes.contains("person"));
    }

    @Test
    public void category_multipleClasses_roundTrips() throws Exception {
        CreditsDocument rt = roundTrip(oneCategory("team", "person", "role", "detail"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("detail", "person", "role")), rt.categories.get(0).classes);
    }

    @Test
    public void category_orderPreserved() throws Exception {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("alpha"));
        doc.categories.add(new DocumentCategory("beta"));
        doc.categories.add(new DocumentCategory("gamma"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals(
            Arrays.asList("alpha", "beta", "gamma"),
            Arrays.asList(rt.categories.get(0).id, rt.categories.get(1).id, rt.categories.get(2).id));
    }

    // -----------------------------------------------------------------------
    // Person / membership serialization
    // -----------------------------------------------------------------------

    @Test
    public void output_isVersionThreeWithNoEmptyPersonsAndEndsWithNewline() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreditsSerializer.write(oneCategory("c"), out);
        String json = out.toString("UTF-8");
        JsonObject root = new JsonParser().parse(json)
            .getAsJsonObject();
        assertFalse("person field should be absent when list is empty", root.has("person"));
        assertEquals(
            3,
            root.get("version")
                .getAsInt());
        assertTrue("output should end with newline", json.endsWith("\n"));
    }

    @Test
    public void person_singleCategoryNoRoles_roundTrips() throws Exception {
        CreditsDocument doc = oneCategory("contrib");
        doc.persons.add(person("Alice", "contrib"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals(1, rt.persons.size());
        assertEquals("Alice", rt.persons.get(0).name);
        assertEquals(1, rt.persons.get(0).memberships.size());
        assertEquals("contrib", rt.persons.get(0).memberships.get(0).categoryId);
        assertTrue(rt.persons.get(0).memberships.get(0).roles.isEmpty());
    }

    @Test
    public void person_singleCategoryOneRole_roundTrips() throws Exception {
        CreditsDocument doc = oneCategory("dev");
        doc.persons.add(person("Bob", "dev", "lead"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals(localRoles("lead"), rt.persons.get(0).memberships.get(0).roles);
    }

    @Test
    public void person_singleCategoryMultipleRoles_roundTrips() throws Exception {
        CreditsDocument doc = oneCategory("dev");
        doc.persons.add(person("Bob", "dev", "lead", "infra"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals(localRoles("lead", "infra"), rt.persons.get(0).memberships.get(0).roles);
    }

    @Test
    public void person_multipleCategories_roundTrips() throws Exception {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("team"));
        doc.categories.add(new DocumentCategory("dev"));
        doc.categories.add(new DocumentCategory("contrib"));

        DocumentPerson p = new DocumentPerson("Carol");
        p.memberships.add(new DocumentMembership("team", localRoles("lead")));
        p.memberships.add(new DocumentMembership("dev", localRoles("backend", "infra")));
        p.memberships.add(new DocumentMembership("contrib"));
        doc.persons.add(p);

        CreditsDocument rt = roundTrip(doc);
        DocumentPerson rp = rt.persons.get(0);
        assertEquals("Carol", rp.name);
        assertEquals(3, rp.memberships.size());
        assertEquals("team", rp.memberships.get(0).categoryId);
        assertEquals(localRoles("lead"), rp.memberships.get(0).roles);
        assertEquals("dev", rp.memberships.get(1).categoryId);
        assertEquals(localRoles("backend", "infra"), rp.memberships.get(1).roles);
        assertEquals("contrib", rp.memberships.get(2).categoryId);
        assertTrue(rp.memberships.get(2).roles.isEmpty());
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void person_nameWithFormattingCodes_preserved() throws Exception {
        CreditsDocument doc = oneCategory("team");
        doc.persons.add(person("§cRed §lBold§r", "team"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals("§cRed §lBold§r", rt.persons.get(0).name);
    }

    @Test
    public void multiplePersons_arePreservedInNameOrder() throws Exception {
        CreditsDocument doc = oneCategory("c");
        doc.persons.add(person("Carol", "c"));
        doc.persons.add(person("Bob", "c"));
        doc.persons.add(person("Alice", "c"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CreditsSerializer.write(doc, output);
        JsonArray persons = new JsonParser().parse(output.toString("UTF-8"))
            .getAsJsonObject()
            .getAsJsonArray("person");
        assertEquals(3, persons.size());
        assertEquals(
            Arrays.asList("Alice", "Bob", "Carol"),
            Arrays.asList(
                persons.get(0)
                    .getAsJsonObject()
                    .get("name")
                    .getAsString(),
                persons.get(1)
                    .getAsJsonObject()
                    .get("name")
                    .getAsString(),
                persons.get(2)
                    .getAsJsonObject()
                    .get("name")
                    .getAsString()));
    }

    private static java.util.List<DocumentRole> localRoles(String... values) {
        return java.util.Arrays.stream(values)
            .map(DocumentRole::local)
            .collect(java.util.stream.Collectors.toList());
    }

}
