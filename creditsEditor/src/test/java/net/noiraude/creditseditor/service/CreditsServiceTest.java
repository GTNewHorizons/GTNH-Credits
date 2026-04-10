package net.noiraude.creditseditor.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;
import net.noiraude.libcredits.model.CreditsCategory;
import net.noiraude.libcredits.model.CreditsData;
import net.noiraude.libcredits.model.CreditsPerson;

import org.junit.Test;

public class CreditsServiceTest {

    // -----------------------------------------------------------------------
    // toEditorModel
    // -----------------------------------------------------------------------

    @Test
    public void toEditorModel_categoriesPreserved() {
        CreditsData data = new CreditsData(
            Arrays.asList(
                new CreditsCategory("team", new LinkedHashSet<>(Arrays.asList("person", "role"))),
                new CreditsCategory("contrib", new LinkedHashSet<>(Collections.singletonList("person")))),
            Collections.emptyList());

        EditorModel model = CreditsService.toEditorModel(data);

        assertEquals(2, model.categories.size());
        assertEquals("team", model.categories.getFirst().id);
        assertTrue(model.categories.getFirst().classes.contains("person"));
        assertTrue(model.categories.getFirst().classes.contains("role"));
        assertEquals("contrib", model.categories.get(1).id);
    }

    @Test
    public void toEditorModel_categoryOrderPreserved() {
        CreditsData data = new CreditsData(
            Arrays.asList(
                new CreditsCategory("z", Collections.emptySet()),
                new CreditsCategory("a", Collections.emptySet()),
                new CreditsCategory("m", Collections.emptySet())),
            Collections.emptyList());

        EditorModel model = CreditsService.toEditorModel(data);

        assertEquals("z", model.categories.getFirst().id);
        assertEquals("a", model.categories.get(1).id);
        assertEquals("m", model.categories.get(2).id);
    }

    @Test
    public void toEditorModel_personsWithMemberships() {
        Map<String, List<String>> cr = new LinkedHashMap<>();
        cr.put("team", Collections.singletonList("lead"));
        cr.put("dev", Arrays.asList("backend", "infra"));

        CreditsData data = new CreditsData(
            Arrays.asList(
                new CreditsCategory("team", Collections.emptySet()),
                new CreditsCategory("dev", Collections.emptySet())),
            Collections.singletonList(new CreditsPerson("Alice", cr)));

        EditorModel model = CreditsService.toEditorModel(data);

        assertEquals(1, model.persons.size());
        EditorPerson ep = model.persons.getFirst();
        assertEquals("Alice", ep.name);
        assertEquals(2, ep.memberships.size());
        assertEquals("team", ep.memberships.getFirst().categoryId);
        assertEquals(Collections.singletonList("lead"), ep.memberships.getFirst().roles);
        assertEquals("dev", ep.memberships.get(1).categoryId);
        assertEquals(Arrays.asList("backend", "infra"), ep.memberships.get(1).roles);
    }

    @Test
    public void toEditorModel_emptyModel() {
        CreditsData data = new CreditsData(Collections.emptyList(), Collections.emptyList());
        EditorModel model = CreditsService.toEditorModel(data);
        assertTrue(model.categories.isEmpty());
        assertTrue(model.persons.isEmpty());
    }

    @Test
    public void toEditorModel_langFieldsAreEmpty() {
        CreditsData data = new CreditsData(
            Collections.singletonList(new CreditsCategory("team", Collections.emptySet())),
            Collections.emptyList());
        EditorModel model = CreditsService.toEditorModel(data);
        assertEquals("", model.categories.getFirst().displayName);
        assertEquals("", model.categories.getFirst().description);
    }

    // -----------------------------------------------------------------------
    // toCreditsData
    // -----------------------------------------------------------------------

    @Test
    public void toCreditsData_categoriesPreserved() {
        EditorModel model = new EditorModel();
        EditorCategory ec = new EditorCategory("team");
        ec.classes = new LinkedHashSet<>(Arrays.asList("person", "detail"));
        model.categories.add(ec);

        CreditsData data = CreditsService.toCreditsData(model);

        assertEquals(1, data.categories.size());
        assertEquals("team", data.categories.getFirst().id);
        assertTrue(data.categories.getFirst().classes.contains("person"));
        assertTrue(data.categories.getFirst().classes.contains("detail"));
    }

    @Test
    public void toCreditsData_personsWithMemberships() {
        EditorModel model = new EditorModel();
        EditorCategory ec = new EditorCategory("dev");
        model.categories.add(ec);

        EditorPerson ep = new EditorPerson("Bob");
        ep.memberships.add(new EditorMembership("dev", Arrays.asList("lead", "infra")));
        model.persons.add(ep);

        CreditsData data = CreditsService.toCreditsData(model);

        assertEquals(1, data.persons.size());
        assertEquals("Bob", data.persons.getFirst().name);
        assertEquals(Arrays.asList("lead", "infra"), data.persons.getFirst().categoryRoles.get("dev"));
    }

    @Test
    public void toCreditsData_emptyRolesPreserved() {
        EditorModel model = new EditorModel();
        model.categories.add(new EditorCategory("contrib"));

        EditorPerson ep = new EditorPerson("Carol");
        ep.memberships.add(new EditorMembership("contrib"));
        model.persons.add(ep);

        CreditsData data = CreditsService.toCreditsData(model);
        assertTrue(
            data.persons.getFirst().categoryRoles.get("contrib")
                .isEmpty());
    }

    // -----------------------------------------------------------------------
    // Round-trip: EditorModel -> CreditsData -> EditorModel
    // -----------------------------------------------------------------------

    @Test
    public void roundTrip_noDataLost() {
        EditorModel original = new EditorModel();
        EditorCategory ec1 = new EditorCategory("team");
        ec1.classes = new LinkedHashSet<>(Arrays.asList("person", "role"));
        EditorCategory ec2 = new EditorCategory("contrib");
        ec2.classes = new LinkedHashSet<>(Collections.singletonList("person"));
        original.categories.add(ec1);
        original.categories.add(ec2);

        EditorPerson ep = new EditorPerson("§cDev§r");
        ep.memberships.add(new EditorMembership("team", Collections.singletonList("lead")));
        ep.memberships.add(new EditorMembership("contrib"));
        original.persons.add(ep);

        EditorModel restored = CreditsService.toEditorModel(CreditsService.toCreditsData(original));

        assertEquals(2, restored.categories.size());
        assertEquals("team", restored.categories.getFirst().id);
        assertEquals("contrib", restored.categories.get(1).id);

        assertEquals(1, restored.persons.size());
        assertEquals("§cDev§r", restored.persons.getFirst().name);
        assertEquals(2, restored.persons.getFirst().memberships.size());
        assertEquals("team", restored.persons.getFirst().memberships.getFirst().categoryId);
        assertEquals(Collections.singletonList("lead"), restored.persons.getFirst().memberships.getFirst().roles);
    }

    // -----------------------------------------------------------------------
    // Mutability: EditorModel is independent of CreditsData
    // -----------------------------------------------------------------------

    @Test
    public void toEditorModel_mutatingEditorModelDoesNotAffectOriginal() {
        CreditsData data = new CreditsData(
            Collections.singletonList(new CreditsCategory("team", Collections.emptySet())),
            Collections.emptyList());

        EditorModel model = CreditsService.toEditorModel(data);
        model.categories.getFirst().id = "mutated";
        model.categories.add(new EditorCategory("extra"));

        assertEquals("team", data.categories.getFirst().id);
        assertEquals(1, data.categories.size());
    }
}
