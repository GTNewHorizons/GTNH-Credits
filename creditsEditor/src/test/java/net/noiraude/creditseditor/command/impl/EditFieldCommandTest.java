package net.noiraude.creditseditor.command.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.noiraude.libcredits.model.DocumentCategory;

import org.junit.jupiter.api.Test;

public class EditFieldCommandTest {

    @Test
    public void execute_setsNewValue() {
        DocumentCategory cat = new DocumentCategory("team");
        cat.id = "old";

        new EditFieldCommand<>("edit", () -> cat.id, v -> cat.id = v, "new").execute();

        assertEquals("new", cat.id);
    }

    @Test
    public void undo_restoresOldValue() {
        DocumentCategory cat = new DocumentCategory("team");
        cat.id = "old";

        EditFieldCommand<String> cmd = new EditFieldCommand<>("edit", () -> cat.id, v -> cat.id = v, "new");
        cmd.execute();
        cmd.undo();

        assertEquals("old", cat.id);
    }

    @Test
    public void executeUndoExecute_cyclesCorrectly() {
        DocumentCategory cat = new DocumentCategory("team");
        cat.id = "old";

        EditFieldCommand<String> cmd = new EditFieldCommand<>("edit", () -> cat.id, v -> cat.id = v, "new");
        cmd.execute();
        cmd.undo();
        cmd.execute();

        assertEquals("new", cat.id);
    }

    @Test
    public void getDisplayName_returnsProvidedName() {
        EditFieldCommand<String> cmd = new EditFieldCommand<>("Edit display name", () -> "", v -> {}, "x");
        assertEquals("Edit display name", cmd.getDisplayName());
    }
}
