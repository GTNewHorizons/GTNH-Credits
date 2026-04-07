package net.noiraude.creditseditor.command.impl;

import static org.junit.Assert.assertEquals;

import net.noiraude.creditseditor.model.EditorCategory;

import org.junit.Test;

public class EditFieldCommandTest {

    @Test
    public void execute_setsNewValue() {
        EditorCategory cat = new EditorCategory("team");
        cat.displayName = "Old";

        new EditFieldCommand<>("edit", () -> cat.displayName, v -> cat.displayName = v, "New").execute();

        assertEquals("New", cat.displayName);
    }

    @Test
    public void undo_restoresOldValue() {
        EditorCategory cat = new EditorCategory("team");
        cat.displayName = "Old";

        EditFieldCommand<String> cmd = new EditFieldCommand<>(
            "edit",
            () -> cat.displayName,
            v -> cat.displayName = v,
            "New");
        cmd.execute();
        cmd.undo();

        assertEquals("Old", cat.displayName);
    }

    @Test
    public void executeUndoExecute_cyclesCorrectly() {
        EditorCategory cat = new EditorCategory("team");
        cat.displayName = "Old";

        EditFieldCommand<String> cmd = new EditFieldCommand<>(
            "edit",
            () -> cat.displayName,
            v -> cat.displayName = v,
            "New");
        cmd.execute();
        cmd.undo();
        cmd.execute();

        assertEquals("New", cat.displayName);
    }

    @Test
    public void getDisplayName_returnsProvidedName() {
        EditFieldCommand<String> cmd = new EditFieldCommand<>("Edit display name", () -> "", v -> {}, "x");
        assertEquals("Edit display name", cmd.getDisplayName());
    }
}
