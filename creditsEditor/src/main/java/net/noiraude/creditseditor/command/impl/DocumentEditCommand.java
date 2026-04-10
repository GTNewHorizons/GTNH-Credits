package net.noiraude.creditseditor.command.impl;

import javax.swing.undo.UndoableEdit;

import net.noiraude.creditseditor.command.Command;

/**
 * Command that wraps a {@link UndoableEdit} captured from a text component's document.
 *
 * <p>
 * The edit is already applied when this command is constructed (the user typed it). Undo
 * delegates to {@link UndoableEdit#undo()}; redo delegates to {@link UndoableEdit#redo()}.
 * Execute is a no-op on the first call (edit already applied) and calls
 * {@link UndoableEdit#redo()} on later calls (redo after undo).
 */
public final class DocumentEditCommand implements Command {

    private final String displayName;
    private final UndoableEdit edit;
    private boolean applied = true;

    public DocumentEditCommand(String displayName, UndoableEdit edit) {
        this.displayName = displayName;
        this.edit = edit;
    }

    @Override
    public void execute() {
        if (!applied) {
            edit.redo();
            applied = true;
        }
    }

    @Override
    public void undo() {
        edit.undo();
        applied = false;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}
