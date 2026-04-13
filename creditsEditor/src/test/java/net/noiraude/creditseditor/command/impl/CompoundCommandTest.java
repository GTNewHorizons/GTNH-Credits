package net.noiraude.creditseditor.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.junit.Test;

public class CompoundCommandTest {

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    @Test(expected = IllegalStateException.class)
    public void build_empty_throws() {
        new CompoundCommand.Builder("empty").build();
    }

    @Test
    public void build_singleChild_returnsThatChild() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("team");
        Command single = new AddCategoryCommand(doc, cat);

        Command result = new CompoundCommand.Builder("single").add(single)
            .build();

        assertTrue("single-child build should unwrap", result instanceof AddCategoryCommand);
    }

    @Test
    public void build_multipleChildren_returnsCompound() {
        CreditsDocument doc = CreditsDocument.empty();
        Command result = new CompoundCommand.Builder("multi")
            .add(new AddCategoryCommand(doc, new DocumentCategory("a")))
            .add(new AddCategoryCommand(doc, new DocumentCategory("b")))
            .build();

        assertTrue(result instanceof CompoundCommand);
        assertEquals(2, ((CompoundCommand) result).size());
    }

    @Test
    public void isEmpty_noChildren_returnsTrue() {
        assertTrue(new CompoundCommand.Builder("x").isEmpty());
    }

    @Test
    public void isEmpty_afterAdd_returnsFalse() {
        CompoundCommand.Builder builder = new CompoundCommand.Builder("x");
        builder.add(new AddCategoryCommand(CreditsDocument.empty(), new DocumentCategory("a")));
        assertFalse(builder.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Execute and undo
    // -----------------------------------------------------------------------

    @Test
    public void execute_appliesAllChildrenInOrder() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory catA = new DocumentCategory("a");
        DocumentCategory catB = new DocumentCategory("b");

        Command compound = new CompoundCommand.Builder("add two").add(new AddCategoryCommand(doc, catA))
            .add(new AddCategoryCommand(doc, catB))
            .build();

        compound.execute();

        assertEquals(2, doc.categories.size());
        assertEquals("a", doc.categories.get(0).id);
        assertEquals("b", doc.categories.get(1).id);
    }

    @Test
    public void undo_reversesAllChildrenInReverseOrder() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentPerson alice = new DocumentPerson("Alice");
        DocumentPerson bob = new DocumentPerson("Bob");

        Command compound = new CompoundCommand.Builder("add two persons").add(new AddPersonCommand(doc, alice))
            .add(new AddPersonCommand(doc, bob))
            .build();

        compound.execute();
        assertEquals(2, doc.persons.size());

        compound.undo();
        assertTrue(doc.persons.isEmpty());
    }

    @Test
    public void execute_afterUndo_reapplies() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("team");
        DocumentPerson person = new DocumentPerson("Alice");

        Command compound = new CompoundCommand.Builder("add both").add(new AddCategoryCommand(doc, cat))
            .add(new AddPersonCommand(doc, person))
            .build();

        compound.execute();
        compound.undo();
        compound.execute();

        assertEquals(1, doc.categories.size());
        assertEquals(1, doc.persons.size());
    }

    @Test
    public void displayName_matchesBuilder() {
        Command compound = new CompoundCommand.Builder("Import TSV (5 persons)")
            .add(new AddPersonCommand(CreditsDocument.empty(), new DocumentPerson("a")))
            .add(new AddPersonCommand(CreditsDocument.empty(), new DocumentPerson("b")))
            .build();

        assertEquals("Import TSV (5 persons)", compound.getDisplayName());
    }

    @Test
    public void isLightEdit_returnsFalse() {
        Command compound = new CompoundCommand.Builder("x")
            .add(new AddPersonCommand(CreditsDocument.empty(), new DocumentPerson("a")))
            .add(new AddPersonCommand(CreditsDocument.empty(), new DocumentPerson("b")))
            .build();

        assertFalse(compound.isLightEdit());
    }

    // -----------------------------------------------------------------------
    // Mixed structural commands
    // -----------------------------------------------------------------------

    @Test
    public void compound_withMixedCommands_undoesCorrectly() {
        CreditsDocument doc = CreditsDocument.empty();
        DocumentCategory cat = new DocumentCategory("dev");
        doc.categories.add(cat);
        DocumentPerson person = new DocumentPerson("Alice");
        doc.persons.add(person);

        // Compound: add membership + add role
        DocumentMembership membership = new DocumentMembership("dev");
        Command compound = new CompoundCommand.Builder("assign with role")
            .add(new AddMembershipCommand(person, membership))
            .add(new AddPersonRoleCommand(membership, "lead"))
            .build();

        compound.execute();
        assertEquals(1, person.memberships.size());
        assertEquals(List.of("lead"), person.memberships.getFirst().roles);

        compound.undo();
        assertTrue(person.memberships.isEmpty());
    }
}
