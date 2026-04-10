package net.noiraude.creditseditor.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

import org.junit.Before;
import org.junit.Test;

public class PersonCommandsTest {

    private EditorModel model;
    private EditorPerson alice;

    @Before
    public void setUp() {
        model = new EditorModel();
        alice = new EditorPerson("Alice");
        EditorPerson bob = new EditorPerson("Bob");
        model.persons.add(alice);
        model.persons.add(bob);
    }

    // -----------------------------------------------------------------------
    // AddPersonCommand
    // -----------------------------------------------------------------------

    @Test
    public void addPerson_execute_appendsToList() {
        EditorPerson carol = new EditorPerson("Carol");
        new AddPersonCommand(model, carol).execute();
        assertEquals(3, model.persons.size());
        assertEquals("Carol", model.persons.get(2).name);
    }

    @Test
    public void addPerson_undo_removesFromList() {
        EditorPerson carol = new EditorPerson("Carol");
        AddPersonCommand cmd = new AddPersonCommand(model, carol);
        cmd.execute();
        cmd.undo();
        assertEquals(2, model.persons.size());
        assertFalse(model.persons.contains(carol));
    }

    // -----------------------------------------------------------------------
    // RemovePersonCommand
    // -----------------------------------------------------------------------

    @Test
    public void removePerson_execute_removesFromList() {
        new RemovePersonCommand(model, alice).execute();
        assertEquals(1, model.persons.size());
        assertFalse(model.persons.contains(alice));
    }

    @Test
    public void removePerson_undo_restoresAtOriginalIndex() {
        RemovePersonCommand cmd = new RemovePersonCommand(model, alice);
        cmd.execute();
        cmd.undo();
        assertEquals(2, model.persons.size());
        assertEquals("Alice", model.persons.getFirst().name);
    }

    @Test
    public void removePerson_membershipsPreservedOnUndo() {
        alice.memberships.add(new EditorMembership("team"));
        RemovePersonCommand cmd = new RemovePersonCommand(model, alice);
        cmd.execute();
        cmd.undo();
        assertEquals(1, model.persons.getFirst().memberships.size());
    }

    // -----------------------------------------------------------------------
    // AddMembershipCommand
    // -----------------------------------------------------------------------

    @Test
    public void addMembership_execute_appendsMembership() {
        EditorMembership m = new EditorMembership("team");
        new AddMembershipCommand(alice, m).execute();
        assertEquals(1, alice.memberships.size());
        assertEquals("team", alice.memberships.getFirst().categoryId);
    }

    @Test
    public void addMembership_undo_removesMembership() {
        EditorMembership m = new EditorMembership("team");
        AddMembershipCommand cmd = new AddMembershipCommand(alice, m);
        cmd.execute();
        cmd.undo();
        assertTrue(alice.memberships.isEmpty());
    }

    // -----------------------------------------------------------------------
    // RemoveMembershipCommand
    // -----------------------------------------------------------------------

    @Test
    public void removeMembership_execute_removesFromList() {
        EditorMembership dev = new EditorMembership("dev");
        alice.memberships.add(new EditorMembership("team"));
        alice.memberships.add(dev);
        new RemoveMembershipCommand(alice, dev).execute();
        assertEquals(1, alice.memberships.size());
        assertEquals("team", alice.memberships.getFirst().categoryId);
    }

    @Test
    public void removeMembership_undo_restoresAtOriginalIndex() {
        EditorMembership dev = new EditorMembership("dev");
        alice.memberships.add(new EditorMembership("team"));
        alice.memberships.add(dev);
        alice.memberships.add(new EditorMembership("contrib"));
        RemoveMembershipCommand cmd = new RemoveMembershipCommand(alice, dev);
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
        EditorMembership m = new EditorMembership("team");
        new AddPersonRoleCommand(m, "lead").execute();
        assertEquals(1, m.roles.size());
        assertEquals("lead", m.roles.getFirst());
    }

    @Test
    public void addRole_undo_removesFirstOccurrence() {
        EditorMembership m = new EditorMembership("team");
        m.roles.add("lead");
        AddPersonRoleCommand cmd = new AddPersonRoleCommand(m, "infra");
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
        EditorMembership m = new EditorMembership("team");
        m.roles.add("lead");
        m.roles.add("infra");
        new RemovePersonRoleCommand(m, "lead").execute();
        assertEquals(1, m.roles.size());
        assertEquals("infra", m.roles.getFirst());
    }

    @Test
    public void removeRole_undo_restoresAtOriginalIndex() {
        EditorMembership m = new EditorMembership("team");
        m.roles.add("lead");
        m.roles.add("infra");
        m.roles.add("devops");
        RemovePersonRoleCommand cmd = new RemovePersonRoleCommand(m, "infra");
        cmd.execute();
        cmd.undo();
        assertEquals(3, m.roles.size());
        assertEquals("infra", m.roles.get(1));
    }
}
