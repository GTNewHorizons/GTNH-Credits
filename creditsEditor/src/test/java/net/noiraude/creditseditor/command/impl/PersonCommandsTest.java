package net.noiraude.creditseditor.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.lang.LangParser;
import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PersonCommandsTest {

    private DocumentBus bus;
    private CreditsDocument creditsDoc;
    private DocumentPerson alice;

    @BeforeEach
    public void setUp() {
        creditsDoc = CreditsDocument.empty();
        bus = new DocumentBus();
        bus.setSession(creditsDoc, LangParser.empty());
        alice = new DocumentPerson("Alice");
        DocumentPerson bob = new DocumentPerson("Bob");
        creditsDoc.persons.add(alice);
        creditsDoc.persons.add(bob);
    }

    // -----------------------------------------------------------------------
    // AddPersonCommand
    // -----------------------------------------------------------------------

    @Test
    public void addPerson_execute_appendsToList() {
        DocumentPerson carol = new DocumentPerson("Carol");
        new AddPersonCommand(bus, carol).execute();
        assertEquals(3, creditsDoc.persons.size());
        assertEquals("Carol", creditsDoc.persons.get(2).name);
    }

    @Test
    public void addPerson_undo_removesFromList() {
        DocumentPerson carol = new DocumentPerson("Carol");
        AddPersonCommand cmd = new AddPersonCommand(bus, carol);
        cmd.execute();
        cmd.undo();
        assertEquals(2, creditsDoc.persons.size());
        assertFalse(creditsDoc.persons.contains(carol));
    }

    // -----------------------------------------------------------------------
    // RemovePersonCommand
    // -----------------------------------------------------------------------

    @Test
    public void removePerson_execute_removesFromList() {
        new RemovePersonCommand(bus, alice).execute();
        assertEquals(1, creditsDoc.persons.size());
        assertFalse(creditsDoc.persons.contains(alice));
    }

    @Test
    public void removePerson_undo_restoresAtOriginalIndex() {
        RemovePersonCommand cmd = new RemovePersonCommand(bus, alice);
        cmd.execute();
        cmd.undo();
        assertEquals(2, creditsDoc.persons.size());
        assertEquals("Alice", creditsDoc.persons.getFirst().name);
    }

    @Test
    public void removePerson_membershipsPreservedOnUndo() {
        alice.memberships.add(new DocumentMembership("team"));
        RemovePersonCommand cmd = new RemovePersonCommand(bus, alice);
        cmd.execute();
        cmd.undo();
        assertEquals(1, creditsDoc.persons.getFirst().memberships.size());
    }

    // -----------------------------------------------------------------------
    // AddMembershipCommand
    // -----------------------------------------------------------------------

    @Test
    public void addMembership_execute_appendsMembership() {
        DocumentMembership m = new DocumentMembership("team");
        new AddMembershipCommand(bus, alice, m).execute();
        assertEquals(1, alice.memberships.size());
        assertEquals("team", alice.memberships.getFirst().categoryId);
    }

    @Test
    public void addMembership_undo_removesMembership() {
        DocumentMembership m = new DocumentMembership("team");
        AddMembershipCommand cmd = new AddMembershipCommand(bus, alice, m);
        cmd.execute();
        cmd.undo();
        assertTrue(alice.memberships.isEmpty());
    }

    // -----------------------------------------------------------------------
    // RemoveMembershipCommand
    // -----------------------------------------------------------------------

    @Test
    public void removeMembership_execute_removesFromList() {
        DocumentMembership dev = new DocumentMembership("dev");
        alice.memberships.add(new DocumentMembership("team"));
        alice.memberships.add(dev);
        new RemoveMembershipCommand(bus, alice, dev).execute();
        assertEquals(1, alice.memberships.size());
        assertEquals("team", alice.memberships.getFirst().categoryId);
    }

    @Test
    public void removeMembership_undo_restoresAtOriginalIndex() {
        DocumentMembership dev = new DocumentMembership("dev");
        alice.memberships.add(new DocumentMembership("team"));
        alice.memberships.add(dev);
        alice.memberships.add(new DocumentMembership("contrib"));
        RemoveMembershipCommand cmd = new RemoveMembershipCommand(bus, alice, dev);
        cmd.execute();
        cmd.undo();
        assertEquals(3, alice.memberships.size());
        assertEquals(dev, alice.memberships.get(1));
    }

    // -----------------------------------------------------------------------
    // AddPersonRoleCommand
    // -----------------------------------------------------------------------

    @Test
    public void addRole_execute_appendsRole() {
        DocumentMembership m = new DocumentMembership("team");
        alice.memberships.add(m);
        new AddPersonRoleCommand(bus, alice, m, "lead").execute();
        assertEquals(1, m.roles.size());
        assertEquals("lead", m.roles.getFirst());
    }

    @Test
    public void addRole_undo_removesFirstOccurrence() {
        DocumentMembership m = new DocumentMembership("team");
        alice.memberships.add(m);
        m.roles.add("lead");
        AddPersonRoleCommand cmd = new AddPersonRoleCommand(bus, alice, m, "infra");
        cmd.execute();
        cmd.undo();
        assertEquals(1, m.roles.size());
        assertEquals("lead", m.roles.getFirst());
    }

    // -----------------------------------------------------------------------
    // RemovePersonRoleCommand
    // -----------------------------------------------------------------------

    @Test
    public void removeRole_execute_removesFromList() {
        DocumentMembership m = new DocumentMembership("team");
        alice.memberships.add(m);
        m.roles.add("lead");
        m.roles.add("infra");
        new RemovePersonRoleCommand(bus, alice, m, "lead").execute();
        assertEquals(1, m.roles.size());
        assertEquals("infra", m.roles.getFirst());
    }

    @Test
    public void removeRole_undo_restoresAtOriginalIndex() {
        DocumentMembership m = new DocumentMembership("team");
        alice.memberships.add(m);
        m.roles.add("lead");
        m.roles.add("infra");
        m.roles.add("devops");
        RemovePersonRoleCommand cmd = new RemovePersonRoleCommand(bus, alice, m, "infra");
        cmd.execute();
        cmd.undo();
        assertEquals(3, m.roles.size());
        assertEquals("infra", m.roles.get(1));
    }
}
