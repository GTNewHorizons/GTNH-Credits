package net.noiraude.creditseditor.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.junit.Test;

public class RenameRoleCommandTest {

    private static LangDocument emptyLang() throws IOException {
        return LangParser.parse(new ByteArrayInputStream(new byte[0]));
    }

    private static LangDocument langWith(String content) throws IOException {
        return LangParser.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }

    // -----------------------------------------------------------------------
    // Basic rename
    // -----------------------------------------------------------------------

    @Test
    public void execute_renamesRoleInAllMemberships() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("dev");
        doc.categories.add(cat);

        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", List.of("lead", "coder")));
        DocumentPerson bob = new DocumentPerson("Bob");
        bob.memberships.add(new DocumentMembership("dev", List.of("lead")));
        doc.persons.add(alice);
        doc.persons.add(bob);

        RenameRoleCommand cmd = new RenameRoleCommand(doc, emptyLang(), "lead", "leader");
        cmd.execute();

        assertTrue(alice.memberships.getFirst().roles.contains("leader"));
        assertFalse(alice.memberships.getFirst().roles.contains("lead"));
        assertTrue(bob.memberships.getFirst().roles.contains("leader"));
        assertFalse(bob.memberships.getFirst().roles.contains("lead"));
    }

    @Test
    public void undo_restoresOriginalRoles() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", Arrays.asList("lead", "coder")));
        doc.persons.add(alice);

        RenameRoleCommand cmd = new RenameRoleCommand(doc, emptyLang(), "lead", "leader");
        cmd.execute();
        cmd.undo();

        assertEquals(Arrays.asList("lead", "coder"), alice.memberships.getFirst().roles);
    }

    // -----------------------------------------------------------------------
    // Lang key handling
    // -----------------------------------------------------------------------

    @Test
    public void execute_movesLangKeyWhenTargetHasNone() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", List.of("lead")));
        doc.persons.add(alice);

        LangDocument lang = langWith("credits.person.role.lead=Team Lead\n");
        RenameRoleCommand cmd = new RenameRoleCommand(doc, lang, "lead", "leader");
        cmd.execute();

        assertNull(lang.get("credits.person.role.lead"));
        assertEquals("Team Lead", lang.get("credits.person.role.leader"));
    }

    @Test
    public void execute_removesSourceLangKeyWhenTargetAlreadyHasOne() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", List.of("lead")));
        doc.persons.add(alice);

        LangDocument lang = langWith("credits.person.role.lead=Old Lead\ncredits.person.role.leader=Existing Leader\n");
        RenameRoleCommand cmd = new RenameRoleCommand(doc, lang, "lead", "leader");
        cmd.execute();

        assertNull(lang.get("credits.person.role.lead"));
        assertEquals("Existing Leader", lang.get("credits.person.role.leader"));
    }

    @Test
    public void undo_restoresLangKeyAfterMove() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", List.of("lead")));
        doc.persons.add(alice);

        LangDocument lang = langWith("credits.person.role.lead=Team Lead\n");
        RenameRoleCommand cmd = new RenameRoleCommand(doc, lang, "lead", "leader");
        cmd.execute();
        cmd.undo();

        assertEquals("Team Lead", lang.get("credits.person.role.lead"));
        assertNull(lang.get("credits.person.role.leader"));
    }

    // -----------------------------------------------------------------------
    // Duplicate handling
    // -----------------------------------------------------------------------

    @Test
    public void execute_removesDuplicateWhenTargetAlreadyInMembership() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        // Alice has both "lead" and "leader" in the same membership
        alice.memberships.add(new DocumentMembership("dev", Arrays.asList("lead", "leader")));
        doc.persons.add(alice);

        RenameRoleCommand cmd = new RenameRoleCommand(doc, emptyLang(), "lead", "leader");
        cmd.execute();

        // Should not have duplicates
        assertEquals(List.of("leader"), alice.memberships.getFirst().roles);
    }

    @Test
    public void undo_restoresDuplicateCase() throws IOException {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev", Arrays.asList("lead", "leader")));
        doc.persons.add(alice);

        RenameRoleCommand cmd = new RenameRoleCommand(doc, emptyLang(), "lead", "leader");
        cmd.execute();
        cmd.undo();

        assertEquals(Arrays.asList("lead", "leader"), alice.memberships.getFirst().roles);
    }

    // -----------------------------------------------------------------------
    // Display name and metadata
    // -----------------------------------------------------------------------

    @Test
    public void displayName_containsBothRoles() throws IOException {
        RenameRoleCommand cmd = new RenameRoleCommand(CreditsDocument.empty(), emptyLang(), "lead", "leader");
        assertTrue(
            cmd.getDisplayName()
                .contains("lead"));
        assertTrue(
            cmd.getDisplayName()
                .contains("leader"));
    }

    @Test
    public void isLightEdit_returnsFalse() throws IOException {
        RenameRoleCommand cmd = new RenameRoleCommand(CreditsDocument.empty(), emptyLang(), "lead", "leader");
        assertFalse(cmd.isLightEdit());
    }
}
