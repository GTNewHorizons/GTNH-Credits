package net.noiraude.creditseditor.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

import org.junit.Before;
import org.junit.Test;

public class CategoryCommandsTest {

    private EditorModel model;
    private EditorCategory team, dev, contrib;

    @Before
    public void setUp() {
        model = new EditorModel();
        team = new EditorCategory("team");
        dev = new EditorCategory("dev");
        contrib = new EditorCategory("contrib");
        model.categories.add(team);
        model.categories.add(dev);
        model.categories.add(contrib);
    }

    // -----------------------------------------------------------------------
    // AddCategoryCommand
    // -----------------------------------------------------------------------

    @Test
    public void addCategory_execute_appendsToList() {
        EditorCategory extra = new EditorCategory("extra");
        new AddCategoryCommand(model, extra).execute();
        assertEquals(4, model.categories.size());
        assertEquals("extra", model.categories.get(3).id);
    }

    @Test
    public void addCategory_undo_removesFromList() {
        EditorCategory extra = new EditorCategory("extra");
        AddCategoryCommand cmd = new AddCategoryCommand(model, extra);
        cmd.execute();
        cmd.undo();
        assertEquals(3, model.categories.size());
        assertFalse(model.categories.contains(extra));
    }

    @Test
    public void addCategory_getDisplayName_containsId() {
        assertTrue(
            new AddCategoryCommand(model, team).getDisplayName()
                .contains("team"));
    }

    // -----------------------------------------------------------------------
    // RemoveCategoryCommand
    // -----------------------------------------------------------------------

    @Test
    public void removeCategory_execute_removesFromList() {
        new RemoveCategoryCommand(model, dev).execute();
        assertEquals(2, model.categories.size());
        assertFalse(model.categories.contains(dev));
    }

    @Test
    public void removeCategory_undo_restoresAtOriginalIndex() {
        RemoveCategoryCommand cmd = new RemoveCategoryCommand(model, dev);
        cmd.execute();
        cmd.undo();
        assertEquals(3, model.categories.size());
        assertEquals("dev", model.categories.get(1).id);
    }

    @Test
    public void removeCategory_execute_stripsMembershipsFromPersons() {
        EditorPerson alice = new EditorPerson("Alice");
        alice.memberships.add(new EditorMembership("dev"));
        alice.memberships.add(new EditorMembership("team"));
        model.persons.add(alice);

        new RemoveCategoryCommand(model, dev).execute();

        assertEquals(1, alice.memberships.size());
        assertEquals("team", alice.memberships.getFirst().categoryId);
    }

    @Test
    public void removeCategory_undo_restoresMembershipsAtOriginalIndex() {
        EditorPerson alice = new EditorPerson("Alice");
        EditorMembership devMembership = new EditorMembership("dev");
        alice.memberships.add(new EditorMembership("team"));
        alice.memberships.add(devMembership);
        alice.memberships.add(new EditorMembership("contrib"));
        model.persons.add(alice);

        RemoveCategoryCommand cmd = new RemoveCategoryCommand(model, dev);
        cmd.execute();
        cmd.undo();

        assertEquals(3, alice.memberships.size());
        assertEquals(devMembership, alice.memberships.get(1));
    }

    @Test
    public void removeCategory_execute_personWithoutMembership_unaffected() {
        EditorPerson bob = new EditorPerson("Bob");
        bob.memberships.add(new EditorMembership("team"));
        model.persons.add(bob);

        new RemoveCategoryCommand(model, dev).execute();

        assertEquals(1, bob.memberships.size());
    }

    // -----------------------------------------------------------------------
    // MoveCategoryOrderCommand
    // -----------------------------------------------------------------------

    @Test
    public void moveCategory_forward_endsAtTargetIndex() {
        // Move team (index 0) to index 2 → [dev, contrib, team]
        new MoveCategoryOrderCommand(model, team, 2).execute();
        assertEquals("dev", model.categories.getFirst().id);
        assertEquals("contrib", model.categories.get(1).id);
        assertEquals("team", model.categories.get(2).id);
    }

    @Test
    public void moveCategory_backward_endsAtTargetIndex() {
        // Move contrib (index 2) to index 0 → [contrib, team, dev]
        new MoveCategoryOrderCommand(model, contrib, 0).execute();
        assertEquals("contrib", model.categories.getFirst().id);
        assertEquals("team", model.categories.get(1).id);
        assertEquals("dev", model.categories.get(2).id);
    }

    @Test
    public void moveCategory_undo_restoresOriginalOrder() {
        MoveCategoryOrderCommand cmd = new MoveCategoryOrderCommand(model, team, 2);
        cmd.execute();
        cmd.undo();
        assertEquals("team", model.categories.getFirst().id);
        assertEquals("dev", model.categories.get(1).id);
        assertEquals("contrib", model.categories.get(2).id);
    }

    @Test
    public void moveCategory_adjacentDown_swapsWithNext() {
        // Move team (0) to index 1 → [dev, team, contrib]
        new MoveCategoryOrderCommand(model, team, 1).execute();
        assertEquals("dev", model.categories.getFirst().id);
        assertEquals("team", model.categories.get(1).id);
    }

    @Test
    public void moveCategory_adjacentUp_swapsWithPrevious() {
        // Move dev (1) to index 0 → [dev, team, contrib]
        new MoveCategoryOrderCommand(model, dev, 0).execute();
        assertEquals("dev", model.categories.getFirst().id);
        assertEquals("team", model.categories.get(1).id);
    }
}
