package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.junit.jupiter.api.Test;

public class CreditsServiceTest {

    // -----------------------------------------------------------------------
    // empty() factory
    // -----------------------------------------------------------------------

    @Test
    public void empty_listsAreEmpty() {
        CreditsDocument doc = CreditsDocument.empty();
        assertTrue(doc.categories.isEmpty());
        assertTrue(doc.persons.isEmpty());
    }

    @Test
    public void empty_isNotDirty() {
        assertFalse(
            CreditsDocument.empty()
                .isDirty());
    }

    // -----------------------------------------------------------------------
    // isDirty / markClean
    // -----------------------------------------------------------------------

    @Test
    public void isDirty_afterAddingCategory_returnsTrue() {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("team"));
        assertTrue(doc.isDirty());
    }

    @Test
    public void isDirty_afterMarkClean_returnsFalse() {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("team"));
        doc.markClean();
        assertFalse(doc.isDirty());
    }

    @Test
    public void isDirty_afterRevertingToOriginalState_returnsFalse() {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("team"));
        doc.markClean();
        DocumentCategory added = new DocumentCategory("extra");
        doc.categories.add(added);
        assertTrue(doc.isDirty());
        doc.categories.remove(added);
        assertFalse(doc.isDirty());
    }

    @Test
    public void isDirty_afterModifyingCategoryId_returnsTrue() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("team");
        doc.categories.add(cat);
        doc.markClean();
        cat.id = "modified";
        assertTrue(doc.isDirty());
    }

    @Test
    public void isDirty_afterModifyingClasses_returnsTrue() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("team");
        doc.categories.add(cat);
        doc.markClean();
        cat.classes.add("person");
        assertTrue(doc.isDirty());
    }

    @Test
    public void isDirty_afterAddingPerson_returnsTrue() {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("team"));
        doc.markClean();
        doc.persons.add(new DocumentPerson("Alice"));
        assertTrue(doc.isDirty());
    }

    @Test
    public void markClean_afterMultipleEdits_clearsAllDirty() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("team");
        cat.classes = new LinkedHashSet<>(Arrays.asList("person", "role"));
        doc.categories.add(cat);
        DocumentPerson p = new DocumentPerson("Alice");
        p.memberships.add(new DocumentMembership("team", List.of("lead")));
        doc.persons.add(p);
        doc.markClean();
        assertFalse(doc.isDirty());
    }

    // -----------------------------------------------------------------------
    // markClean isolates baseline from live mutations
    // -----------------------------------------------------------------------

    @Test
    public void markClean_baselineIsDeepCopy_mutatingLiveDoesNotAffectClean() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("team");
        doc.categories.add(cat);
        doc.markClean();

        // Mutate the live category
        cat.id = "mutated";
        assertTrue(doc.isDirty(), "mutation after markClean should be detected as dirty");
    }

    // -----------------------------------------------------------------------
    // Structure
    // -----------------------------------------------------------------------

    @Test
    public void categories_preserveInsertionOrder() {
        CreditsDocument doc = CreditsDocument.empty();
        doc.categories.add(new DocumentCategory("z"));
        doc.categories.add(new DocumentCategory("a"));
        doc.categories.add(new DocumentCategory("m"));
        assertEquals("z", doc.categories.getFirst().id);
        assertEquals("a", doc.categories.get(1).id);
        assertEquals("m", doc.categories.get(2).id);
    }

    @Test
    public void memberships_preserveRoleOrder() {
        DocumentPerson p = new DocumentPerson("Alice");
        p.memberships.add(new DocumentMembership("team", Arrays.asList("lead", "dev", "infra")));
        assertEquals(Arrays.asList("lead", "dev", "infra"), p.memberships.getFirst().roles);
    }
}
