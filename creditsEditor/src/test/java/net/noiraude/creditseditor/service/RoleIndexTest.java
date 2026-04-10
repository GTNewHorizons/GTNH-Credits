package net.noiraude.creditseditor.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

import org.junit.Test;

public class RoleIndexTest {

    // -----------------------------------------------------------------------
    // Empty model
    // -----------------------------------------------------------------------

    @Test
    public void build_emptyModel_emptyIndex() {
        assertTrue(
            RoleIndex.build(new EditorModel())
                .entries()
                .isEmpty());
    }

    // -----------------------------------------------------------------------
    // Entry content
    // -----------------------------------------------------------------------

    @Test
    public void build_singleRole_correctEntry() {
        EditorModel model = new EditorModel();
        EditorPerson p = new EditorPerson("Alice");
        p.memberships.add(new EditorMembership("team", List.of("lead")));
        model.persons.add(p);

        RoleIndex index = RoleIndex.build(model);

        assertEquals(
            1,
            index.entries()
                .size());
        RoleIndex.Entry e = index.entries()
            .getFirst();
        assertEquals("lead", e.raw);
        assertEquals("credits.person.role.lead", e.langKey);
        assertEquals(1, e.count);
        assertTrue(e.categoryIds.contains("team"));
    }

    @Test
    public void build_langKeyUsesKeysSanitizer() {
        EditorModel model = new EditorModel();
        EditorPerson p = new EditorPerson("Alice");
        p.memberships.add(new EditorMembership("team", List.of("gtnh-creator")));
        model.persons.add(p);

        RoleIndex.Entry e = RoleIndex.build(model)
            .entries()
            .getFirst();
        assertEquals("credits.person.role.gtnhcreator", e.langKey);
    }

    // -----------------------------------------------------------------------
    // Count: distinct persons, not occurrences
    // -----------------------------------------------------------------------

    @Test
    public void build_roleSharedByTwoPersons_countIsTwo() {
        EditorModel model = new EditorModel();
        EditorPerson alice = new EditorPerson("Alice");
        alice.memberships.add(new EditorMembership("team", List.of("lead")));
        EditorPerson bob = new EditorPerson("Bob");
        bob.memberships.add(new EditorMembership("dev", List.of("lead")));
        model.persons.add(alice);
        model.persons.add(bob);

        RoleIndex.Entry e = RoleIndex.build(model)
            .entries()
            .getFirst();
        assertEquals(2, e.count);
    }

    @Test
    public void build_samePersonRoleInMultipleCategories_countIsOne() {
        EditorModel model = new EditorModel();
        EditorPerson alice = new EditorPerson("Alice");
        alice.memberships.add(new EditorMembership("team", List.of("lead")));
        alice.memberships.add(new EditorMembership("dev", List.of("lead")));
        model.persons.add(alice);

        RoleIndex.Entry e = RoleIndex.build(model)
            .entries()
            .getFirst();
        assertEquals(1, e.count);
        assertEquals(2, e.categoryIds.size());
    }

    // -----------------------------------------------------------------------
    // Categories per role
    // -----------------------------------------------------------------------

    @Test
    public void build_roleInMultipleCategories_allCategoryIdsPresent() {
        EditorModel model = new EditorModel();
        EditorPerson p = new EditorPerson("Alice");
        p.memberships.add(new EditorMembership("team", List.of("dev")));
        p.memberships.add(new EditorMembership("contrib", List.of("dev")));
        model.persons.add(p);

        RoleIndex.Entry e = RoleIndex.build(model)
            .entries()
            .getFirst();
        assertTrue(e.categoryIds.contains("team"));
        assertTrue(e.categoryIds.contains("contrib"));
    }

    // -----------------------------------------------------------------------
    // Alphabetical sort
    // -----------------------------------------------------------------------

    @Test
    public void build_multipleRoles_sortedAlphabetically() {
        EditorModel model = new EditorModel();
        EditorPerson p = new EditorPerson("Alice");
        p.memberships.add(new EditorMembership("team", List.of("zzz", "aaa", "mmm")));
        model.persons.add(p);

        List<RoleIndex.Entry> entries = RoleIndex.build(model)
            .entries();
        assertEquals("aaa", entries.getFirst().raw);
        assertEquals("mmm", entries.get(1).raw);
        assertEquals("zzz", entries.get(2).raw);
    }

    // -----------------------------------------------------------------------
    // rolesForCategory
    // -----------------------------------------------------------------------

    @Test
    public void rolesForCategory_returnsOnlyRolesInThatCategory() {
        EditorModel model = new EditorModel();
        EditorPerson p = new EditorPerson("Alice");
        p.memberships.add(new EditorMembership("team", List.of("lead", "infra")));
        p.memberships.add(new EditorMembership("dev", List.of("backend")));
        model.persons.add(p);

        RoleIndex index = RoleIndex.build(model);
        Set<String> teamRoles = index.rolesForCategory("team");
        assertTrue(teamRoles.contains("lead"));
        assertTrue(teamRoles.contains("infra"));
        assertFalse(teamRoles.contains("backend"));
    }

    @Test
    public void rolesForCategory_unknownCategory_returnsEmptySet() {
        assertTrue(
            RoleIndex.build(new EditorModel())
                .rolesForCategory("nope")
                .isEmpty());
    }

    // -----------------------------------------------------------------------
    // contains
    // -----------------------------------------------------------------------

    @Test
    public void contains_existingRole_returnsTrue() {
        EditorModel model = new EditorModel();
        EditorPerson p = new EditorPerson("Alice");
        p.memberships.add(new EditorMembership("team", List.of("lead")));
        model.persons.add(p);
        assertTrue(
            RoleIndex.build(model)
                .contains("lead"));
    }

    @Test
    public void contains_unknownRole_returnsFalse() {
        assertFalse(
            RoleIndex.build(new EditorModel())
                .contains("lead"));
    }
}
