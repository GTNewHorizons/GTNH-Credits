package net.noiraude.libcredits.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.parser.CreditsParser;

import org.junit.Test;

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
        p.memberships.add(new DocumentMembership(catId, Arrays.asList(roles)));
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
    public void person_emptyList_omitsPersonField() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreditsSerializer.write(oneCategory("c"), out);
        String json = out.toString("UTF-8");
        assertFalse("person field should be absent when list is empty", json.contains("\"person\""));
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
        assertEquals(Collections.singletonList("lead"), rt.persons.get(0).memberships.get(0).roles);
    }

    @Test
    public void person_singleCategoryMultipleRoles_roundTrips() throws Exception {
        CreditsDocument doc = oneCategory("dev");
        doc.persons.add(person("Bob", "dev", "lead", "infra"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals(Arrays.asList("lead", "infra"), rt.persons.get(0).memberships.get(0).roles);
    }

    @Test
    public void person_multipleCategories_roundTrips() throws Exception {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("team"));
        doc.categories.add(new DocumentCategory("dev"));
        doc.categories.add(new DocumentCategory("contrib"));

        DocumentPerson p = new DocumentPerson("Carol");
        p.memberships.add(new DocumentMembership("team", Collections.singletonList("lead")));
        p.memberships.add(new DocumentMembership("dev", Arrays.asList("backend", "infra")));
        p.memberships.add(new DocumentMembership("contrib"));
        doc.persons.add(p);

        CreditsDocument rt = roundTrip(doc);
        DocumentPerson rp = rt.persons.get(0);
        assertEquals("Carol", rp.name);
        assertEquals(3, rp.memberships.size());
        assertEquals("team", rp.memberships.get(0).categoryId);
        assertEquals(Collections.singletonList("lead"), rp.memberships.get(0).roles);
        assertEquals("dev", rp.memberships.get(1).categoryId);
        assertEquals(Arrays.asList("backend", "infra"), rp.memberships.get(1).roles);
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
    public void multiplePersons_allPreserved() throws Exception {
        CreditsDocument doc = oneCategory("c");
        doc.persons.add(person("Alice", "c"));
        doc.persons.add(person("Bob", "c"));
        doc.persons.add(person("Carol", "c"));
        CreditsDocument rt = roundTrip(doc);
        assertEquals(3, rt.persons.size());
        assertTrue(
            rt.persons.stream()
                .anyMatch(p -> p.name.equals("Alice")));
        assertTrue(
            rt.persons.stream()
                .anyMatch(p -> p.name.equals("Bob")));
        assertTrue(
            rt.persons.stream()
                .anyMatch(p -> p.name.equals("Carol")));
    }

    // -----------------------------------------------------------------------
    // Output is valid UTF-8 JSON ending with newline
    // -----------------------------------------------------------------------

    @Test
    public void output_endsWithNewline() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreditsSerializer.write(oneCategory("c"), out);
        String json = out.toString("UTF-8");
        assertTrue("output should end with newline", json.endsWith("\n"));
    }

    @Test
    public void output_containsVersionTwo() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreditsSerializer.write(oneCategory("c"), out);
        String json = out.toString("UTF-8");
        assertTrue(json.contains("\"version\": 2"));
    }

    // -----------------------------------------------------------------------
    // Full round-trip: serialize -> parse, data preserved
    // -----------------------------------------------------------------------

    @Test
    public void fullRoundTrip_noDataLost() throws Exception {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory team = new DocumentCategory("team");
        team.classes = new LinkedHashSet<>(Arrays.asList("person", "role"));
        DocumentCategory contrib = new DocumentCategory("contrib");
        contrib.classes = new LinkedHashSet<>(Arrays.asList("person", "detail"));
        doc.categories.add(team);
        doc.categories.add(contrib);

        DocumentPerson p = new DocumentPerson("Dev");
        p.memberships.add(new DocumentMembership("team", Collections.singletonList("lead")));
        p.memberships.add(new DocumentMembership("contrib"));
        doc.persons.add(p);

        CreditsDocument rt = roundTrip(doc);

        assertEquals(2, rt.categories.size());
        assertNotNull(
            rt.categories.stream()
                .filter(c -> c.id.equals("team"))
                .findFirst()
                .orElse(null));
        assertNotNull(
            rt.categories.stream()
                .filter(c -> c.id.equals("contrib"))
                .findFirst()
                .orElse(null));
        assertEquals(1, rt.persons.size());
        assertEquals("Dev", rt.persons.get(0).name);
        assertEquals(Collections.singletonList("lead"), rt.persons.get(0).memberships.get(0).roles);
    }
}
