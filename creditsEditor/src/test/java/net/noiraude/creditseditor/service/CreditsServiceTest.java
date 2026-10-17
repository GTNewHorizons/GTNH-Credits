package net.noiraude.creditseditor.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

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
        assertFalse(doc.isDirty());
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
        p.memberships.add(new DocumentMembership("team", localRoles("lead")));
        doc.persons.add(p);
        doc.markClean();
        assertFalse(doc.isDirty());
    }

    private static java.util.List<DocumentRole> localRoles(String... values) {
        return java.util.Arrays.stream(values)
            .map(DocumentRole::local)
            .collect(java.util.stream.Collectors.toList());
    }

}
