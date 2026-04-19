package net.noiraude.creditseditor.command.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unused")
public class CategoryCommandsTest {

    private CreditsDocument creditsDoc;
    private DocumentCategory team, dev, contrib;

    @Before
    public void setUp() {
        creditsDoc = CreditsDocument.empty();
        team = new DocumentCategory("team");
        dev = new DocumentCategory("dev");
        contrib = new DocumentCategory("contrib");
        creditsDoc.categories.add(team);
        creditsDoc.categories.add(dev);
        creditsDoc.categories.add(contrib);
    }

    // -----------------------------------------------------------------------
    // AddCategoryCommand
    // -----------------------------------------------------------------------

    @Test
    public void addCategory_execute_appendsToList() {
        DocumentCategory extra = new DocumentCategory("extra");
        new AddCategoryCommand(creditsDoc, extra).execute();
        assertEquals(4, creditsDoc.categories.size());
        assertEquals("extra", creditsDoc.categories.get(3).id);
    }

    @Test
    public void addCategory_undo_removesFromList() {
        DocumentCategory extra = new DocumentCategory("extra");
        AddCategoryCommand cmd = new AddCategoryCommand(creditsDoc, extra);
        cmd.execute();
        cmd.undo();
        assertEquals(3, creditsDoc.categories.size());
        assertFalse(creditsDoc.categories.contains(extra));
    }

    @Test
    public void addCategory_getDisplayName_containsId() {
        assertTrue(
            new AddCategoryCommand(creditsDoc, team).getDisplayName()
                .contains("team"));
    }

    // -----------------------------------------------------------------------
    // RemoveCategoryCommand
    // -----------------------------------------------------------------------

    @Test
    public void removeCategory_execute_removesFromList() {
        new RemoveCategoryCommand(creditsDoc, dev).execute();
        assertEquals(2, creditsDoc.categories.size());
        assertFalse(creditsDoc.categories.contains(dev));
    }

    @Test
    public void removeCategory_undo_restoresAtOriginalIndex() {
        RemoveCategoryCommand cmd = new RemoveCategoryCommand(creditsDoc, dev);
        cmd.execute();
        cmd.undo();
        assertEquals(3, creditsDoc.categories.size());
        assertEquals("dev", creditsDoc.categories.get(1).id);
    }

    @Test
    public void removeCategory_execute_stripsMembershipsFromPersons() {
        DocumentPerson alice = new DocumentPerson("Alice");
        alice.memberships.add(new DocumentMembership("dev"));
        alice.memberships.add(new DocumentMembership("team"));
        creditsDoc.persons.add(alice);

        new RemoveCategoryCommand(creditsDoc, dev).execute();

        assertEquals(1, alice.memberships.size());
        assertEquals("team", alice.memberships.getFirst().categoryId);
    }

    @Test
    public void removeCategory_undo_restoresMembershipsAtOriginalIndex() {
        DocumentPerson alice = new DocumentPerson("Alice");
        DocumentMembership devMembership = new DocumentMembership("dev");
        alice.memberships.add(new DocumentMembership("team"));
        alice.memberships.add(devMembership);
        alice.memberships.add(new DocumentMembership("contrib"));
        creditsDoc.persons.add(alice);

        RemoveCategoryCommand cmd = new RemoveCategoryCommand(creditsDoc, dev);
        cmd.execute();
        cmd.undo();

        assertEquals(3, alice.memberships.size());
        assertEquals(devMembership, alice.memberships.get(1));
    }

    @Test
    public void removeCategory_execute_personWithoutMembership_unaffected() {
        DocumentPerson bob = new DocumentPerson("Bob");
        bob.memberships.add(new DocumentMembership("team"));
        creditsDoc.persons.add(bob);

        new RemoveCategoryCommand(creditsDoc, dev).execute();

        assertEquals(1, bob.memberships.size());
    }

    // -----------------------------------------------------------------------
    // MoveCategoriesOrderCommand
    // -----------------------------------------------------------------------

    @Test
    public void moveCategories_singleForward_endsAtDropIndex() {
        // Move team (index 0) past contrib: dropIndex=3 → [dev, contrib, team]
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 0 }, 3).execute();
        assertEquals("dev", creditsDoc.categories.getFirst().id);
        assertEquals("contrib", creditsDoc.categories.get(1).id);
        assertEquals("team", creditsDoc.categories.get(2).id);
    }

    @Test
    public void moveCategories_singleBackward_endsAtDropIndex() {
        // Move contrib (index 2) before team: dropIndex=0 → [contrib, team, dev]
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 2 }, 0).execute();
        assertEquals("contrib", creditsDoc.categories.getFirst().id);
        assertEquals("team", creditsDoc.categories.get(1).id);
        assertEquals("dev", creditsDoc.categories.get(2).id);
    }

    @Test
    public void moveCategories_undo_restoresOriginalOrder() {
        MoveCategoriesOrderCommand cmd = new MoveCategoriesOrderCommand(creditsDoc, new int[] { 0 }, 3);
        cmd.execute();
        cmd.undo();
        assertEquals("team", creditsDoc.categories.getFirst().id);
        assertEquals("dev", creditsDoc.categories.get(1).id);
        assertEquals("contrib", creditsDoc.categories.get(2).id);
    }

    @Test
    public void moveCategories_adjacentDown_swapsWithNext() {
        // Move team (0) to dropIndex=2 (between dev and contrib) → [dev, team, contrib]
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 0 }, 2).execute();
        assertEquals("dev", creditsDoc.categories.getFirst().id);
        assertEquals("team", creditsDoc.categories.get(1).id);
    }

    @Test
    public void moveCategories_adjacentUp_swapsWithPrevious() {
        // Move dev (1) to dropIndex=0 → [dev, team, contrib]
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 1 }, 0).execute();
        assertEquals("dev", creditsDoc.categories.getFirst().id);
        assertEquals("team", creditsDoc.categories.get(1).id);
    }

    @Test
    public void moveCategories_discontinuousSelection_preservesRelativeOrder() {
        // Given [team, dev, contrib], move {team(0), contrib(2)} to dropIndex=1 (between team and dev)
        // extracted = [team, contrib] (relative order preserved)
        // remaining = [dev], insertAt = 1 - 1 = 0 → [team, contrib, dev]
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 0, 2 }, 1).execute();
        assertEquals("team", creditsDoc.categories.getFirst().id);
        assertEquals("contrib", creditsDoc.categories.get(1).id);
        assertEquals("dev", creditsDoc.categories.get(2).id);
    }

    @Test
    public void moveCategories_contiguousBlock_movesTogether() {
        // Move {team(0), dev(1)} past contrib: dropIndex=3 → [contrib, team, dev]
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 0, 1 }, 3).execute();
        assertEquals("contrib", creditsDoc.categories.getFirst().id);
        assertEquals("team", creditsDoc.categories.get(1).id);
        assertEquals("dev", creditsDoc.categories.get(2).id);
    }

    @Test
    public void moveCategories_unsortedInput_isSorted() {
        // Same as discontinuous test, but pass indices in reverse order
        new MoveCategoriesOrderCommand(creditsDoc, new int[] { 2, 0 }, 1).execute();
        assertEquals("team", creditsDoc.categories.getFirst().id);
        assertEquals("contrib", creditsDoc.categories.get(1).id);
        assertEquals("dev", creditsDoc.categories.get(2).id);
    }

    @Test
    public void moveCategories_undo_afterDiscontinuousMove_restoresOriginalOrder() {
        MoveCategoriesOrderCommand cmd = new MoveCategoriesOrderCommand(creditsDoc, new int[] { 0, 2 }, 1);
        cmd.execute();
        cmd.undo();
        assertEquals("team", creditsDoc.categories.getFirst().id);
        assertEquals("dev", creditsDoc.categories.get(1).id);
        assertEquals("contrib", creditsDoc.categories.get(2).id);
    }
}
