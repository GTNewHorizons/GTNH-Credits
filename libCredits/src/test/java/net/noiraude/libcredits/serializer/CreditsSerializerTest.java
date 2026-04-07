package net.noiraude.libcredits.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.noiraude.libcredits.model.CreditsCategory;
import net.noiraude.libcredits.model.CreditsData;
import net.noiraude.libcredits.model.CreditsPerson;
import net.noiraude.libcredits.parser.CreditsParser;

import org.junit.Test;

public class CreditsSerializerTest {

    // -----------------------------------------------------------------------
    // Round-trip helpers
    // -----------------------------------------------------------------------

    private static CreditsData roundTrip(CreditsData data) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CreditsSerializer.write(data, out);
        return CreditsParser.parse(new ByteArrayInputStream(out.toByteArray()));
    }

    private static CreditsData oneCategory(String id, String... classes) {
        return new CreditsData(
            Collections.singletonList(new CreditsCategory(id, new LinkedHashSet<>(Arrays.asList(classes)))),
            Collections.emptyList());
    }

    private static CreditsPerson person(String name, String catId, String... roles) {
        Map<String, List<String>> cr = new LinkedHashMap<>();
        cr.put(catId, Arrays.asList(roles));
        return new CreditsPerson(name, cr);
    }

    // -----------------------------------------------------------------------
    // Category serialization
    // -----------------------------------------------------------------------

    @Test
    public void category_noClasses_omitsClassField() throws Exception {
        CreditsData rt = roundTrip(oneCategory("team"));
        assertEquals(1, rt.categories.size());
        assertEquals("team", rt.categories.get(0).id);
        assertTrue(rt.categories.get(0).classes.isEmpty());
    }

    @Test
    public void category_singleClass_roundTrips() throws Exception {
        CreditsData rt = roundTrip(oneCategory("team", "person"));
        assertEquals(Collections.singleton("person"), rt.categories.get(0).classes);
    }

    @Test
    public void category_multipleClasses_roundTrips() throws Exception {
        CreditsData rt = roundTrip(oneCategory("team", "person", "role", "detail"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("detail", "person", "role")), rt.categories.get(0).classes);
    }

    @Test
    public void category_orderPreserved() throws Exception {
        CreditsData data = new CreditsData(
            Arrays.asList(
                new CreditsCategory("alpha", Collections.emptySet()),
                new CreditsCategory("beta", Collections.emptySet()),
                new CreditsCategory("gamma", Collections.emptySet())),
            Collections.emptyList());
        CreditsData rt = roundTrip(data);
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
        CreditsData data = new CreditsData(
            Collections.singletonList(new CreditsCategory("contrib", Collections.emptySet())),
            Collections.singletonList(person("Alice", "contrib")));
        CreditsData rt = roundTrip(data);
        assertEquals(1, rt.persons.size());
        assertEquals("Alice", rt.persons.get(0).name);
        assertTrue(rt.persons.get(0).categoryRoles.containsKey("contrib"));
        assertTrue(
            rt.persons.get(0).categoryRoles.get("contrib")
                .isEmpty());
    }

    @Test
    public void person_singleCategoryOneRole_roundTrips() throws Exception {
        CreditsData data = new CreditsData(
            Collections.singletonList(new CreditsCategory("dev", Collections.emptySet())),
            Collections.singletonList(person("Bob", "dev", "lead")));
        CreditsData rt = roundTrip(data);
        assertEquals(Collections.singletonList("lead"), rt.persons.get(0).categoryRoles.get("dev"));
    }

    @Test
    public void person_singleCategoryMultipleRoles_roundTrips() throws Exception {
        CreditsData data = new CreditsData(
            Collections.singletonList(new CreditsCategory("dev", Collections.emptySet())),
            Collections.singletonList(person("Bob", "dev", "lead", "infra")));
        CreditsData rt = roundTrip(data);
        assertEquals(Arrays.asList("lead", "infra"), rt.persons.get(0).categoryRoles.get("dev"));
    }

    @Test
    public void person_multipleCategories_roundTrips() throws Exception {
        Map<String, List<String>> cr = new LinkedHashMap<>();
        cr.put("team", Collections.singletonList("lead"));
        cr.put("dev", Arrays.asList("backend", "infra"));
        cr.put("contrib", Collections.emptyList());
        CreditsPerson p = new CreditsPerson("Carol", cr);

        CreditsData data = new CreditsData(
            Arrays.asList(
                new CreditsCategory("team", Collections.emptySet()),
                new CreditsCategory("dev", Collections.emptySet()),
                new CreditsCategory("contrib", Collections.emptySet())),
            Collections.singletonList(p));
        CreditsData rt = roundTrip(data);

        CreditsPerson rp = rt.persons.get(0);
        assertEquals("Carol", rp.name);
        assertEquals(Collections.singletonList("lead"), rp.categoryRoles.get("team"));
        assertEquals(Arrays.asList("backend", "infra"), rp.categoryRoles.get("dev"));
        assertTrue(
            rp.categoryRoles.get("contrib")
                .isEmpty());
    }

    @Test
    public void person_nameWithFormattingCodes_preserved() throws Exception {
        CreditsData data = new CreditsData(
            Collections.singletonList(new CreditsCategory("team", Collections.emptySet())),
            Collections.singletonList(person("§cRed §lBold§r", "team")));
        CreditsData rt = roundTrip(data);
        assertEquals("§cRed §lBold§r", rt.persons.get(0).name);
    }

    @Test
    public void multiplePersons_allPreserved() throws Exception {
        List<CreditsCategory> cats = Collections.singletonList(new CreditsCategory("c", Collections.emptySet()));
        List<CreditsPerson> persons = Arrays.asList(person("Alice", "c"), person("Bob", "c"), person("Carol", "c"));
        CreditsData rt = roundTrip(new CreditsData(cats, persons));
        // Parser sorts by name; verify all names present
        List<String> names = Arrays.asList(rt.persons.get(0).name, rt.persons.get(1).name, rt.persons.get(2).name);
        assertTrue(names.containsAll(Arrays.asList("Alice", "Bob", "Carol")));
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
    // Full round-trip: parse -> serialize -> parse, data equals original
    // -----------------------------------------------------------------------

    @Test
    public void fullRoundTrip_noDataLost() throws Exception {
        Map<String, List<String>> cr = new LinkedHashMap<>();
        cr.put("team", Collections.singletonList("lead"));
        cr.put("contrib", Collections.emptyList());

        CreditsData original = new CreditsData(
            Arrays.asList(
                new CreditsCategory("team", new LinkedHashSet<>(Arrays.asList("person", "role"))),
                new CreditsCategory("contrib", new LinkedHashSet<>(Arrays.asList("person", "detail")))),
            Collections.singletonList(new CreditsPerson("Dev", cr)));

        CreditsData rt = roundTrip(original);

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
        assertEquals(Collections.singletonList("lead"), rt.persons.get(0).categoryRoles.get("team"));
    }
}
