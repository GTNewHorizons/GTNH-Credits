package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.junit.jupiter.api.Test;

public class RoleIndexTest {

    // -----------------------------------------------------------------------
    // Empty document
    // -----------------------------------------------------------------------

    @Test
    public void build_emptyDocument_emptyIndex() {
        assertTrue(
            RoleIndex.build(CreditsDocument.empty())
                .entries()
                .isEmpty());
    }

    // -----------------------------------------------------------------------
    // Entry content
    // -----------------------------------------------------------------------

    @Test
    public void build_singleRole_correctEntry() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson p = new DocumentPerson("Alice");
        p.memberships.add(new DocumentMembership("team", localRoles("lead")));
        doc.persons.add(p);

        RoleIndex index = RoleIndex.build(doc);

        assertEquals(
            1,
            index.entries()
                .size());
        RoleIndex.Entry e = index.entries()
            .getFirst();
        assertEquals("lead", e.raw.value());
        assertEquals("credits.person.role.lead", e.langKey);
        assertEquals(1, e.count);
        assertEquals(List.of("team"), e.categoryIds);
        assertTrue(index.contains(DocumentRole.local("lead")));
        assertFalse(index.contains(DocumentRole.local("unknown")));
    }

    @Test
    public void build_langKeyUsesKeysSanitizer() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson p = new DocumentPerson("Alice");
        p.memberships.add(new DocumentMembership("team", localRoles("gtnh-creator")));
        doc.persons.add(p);

        RoleIndex.Entry e = RoleIndex.build(doc)
            .entries()
            .getFirst();
        assertEquals("credits.person.role.gtnhcreator", e.langKey);
    }

    // -----------------------------------------------------------------------
    // Count: distinct persons, not occurrences
    // -----------------------------------------------------------------------

    @Test
    public void build_roleSharedByTwoPersons_countIsTwo() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("team", localRoles("lead")));
        DocumentPerson bob = new DocumentPerson("Bob");
        bob.memberships.add(new DocumentMembership("dev", localRoles("lead")));
        doc.persons.add(alice);
        doc.persons.add(bob);

        RoleIndex.Entry e = RoleIndex.build(doc)
            .entries()
            .getFirst();
        assertEquals(2, e.count);
    }

    @Test
    public void build_samePersonRoleInMultipleCategories_countIsOne() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("team", localRoles("lead")));
        alice.memberships.add(new DocumentMembership("dev", localRoles("lead")));
        doc.persons.add(alice);

        RoleIndex.Entry e = RoleIndex.build(doc)
            .entries()
            .getFirst();
        assertEquals(1, e.count);
        assertEquals(List.of("team", "dev"), e.categoryIds);
    }

    // -----------------------------------------------------------------------
    // Alphabetical sort
    // -----------------------------------------------------------------------

    @Test
    public void build_multipleRoles_sortedAlphabetically() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson p = new DocumentPerson("Alice");
        p.memberships.add(new DocumentMembership("team", localRoles("zzz", "aaa", "mmm")));
        doc.persons.add(p);

        List<RoleIndex.Entry> entries = RoleIndex.build(doc)
            .entries();
        assertEquals("aaa", entries.getFirst().raw.value());
        assertEquals("mmm", entries.get(1).raw.value());
        assertEquals("zzz", entries.get(2).raw.value());
    }

    // -----------------------------------------------------------------------
    // rolesForCategory
    // -----------------------------------------------------------------------

    @Test
    public void rolesForCategory_returnsOnlyRolesInThatCategory() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson p = new DocumentPerson("Alice");
        p.memberships.add(new DocumentMembership("team", localRoles("lead", "infra")));
        p.memberships.add(new DocumentMembership("dev", localRoles("backend")));
        doc.persons.add(p);

        RoleIndex index = RoleIndex.build(doc);
        Set<DocumentRole> teamRoles = index.rolesForCategory("team");
        assertTrue(teamRoles.contains(DocumentRole.local("lead")));
        assertTrue(teamRoles.contains(DocumentRole.local("infra")));
        assertFalse(teamRoles.contains(DocumentRole.local("backend")));
    }

    @Test
    public void rolesForCategory_unknownCategory_returnsEmptySet() {
        assertTrue(
            RoleIndex.build(CreditsDocument.empty())
                .rolesForCategory("nope")
                .isEmpty());
    }

    @Test
    public void matchingLocalAndExternalValuesRemainSeparateEntries() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson p = new DocumentPerson("Alice");
        DocumentRole local = DocumentRole.local("Exact.Key");
        DocumentRole external = DocumentRole.translation("Exact.Key");
        p.memberships.add(new DocumentMembership("team", List.of(external, local)));
        p.memberships.add(new DocumentMembership("dev", List.of(external)));
        doc.persons.add(p);

        RoleIndex index = RoleIndex.build(doc);

        assertEquals(
            List.of(local, external),
            index.entries()
                .stream()
                .map(entry -> entry.raw)
                .toList());
        assertEquals(
            "credits.person.role.exactkey",
            index.entries()
                .getFirst().langKey);
        assertEquals(
            "Exact.Key",
            index.entries()
                .get(1).langKey);
        assertEquals(
            1,
            index.entries()
                .get(1).count);
        assertEquals(Set.of(external), index.rolesForCategory("dev"));
        assertTrue(index.contains(external));
    }

    private static java.util.List<DocumentRole> localRoles(String... values) {
        return java.util.Arrays.stream(values)
            .map(DocumentRole::local)
            .collect(java.util.stream.Collectors.toList());
    }

}
