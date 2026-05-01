package net.noiraude.creditseditor.command.impl;

import javax.swing.undo.UndoableEdit;

import org.jetbrains.annotations.NotNull;

/**
 * Command that wraps a {@link UndoableEdit} captured from a text component's document.
 *
 * <p>
 * The edit is already applied when this command is constructed (the user typed it). Undo
 * delegates to {@link UndoableEdit#undo()}; redo delegates to {@link UndoableEdit#redo()}.
 * Execute is a no-op on the first call (edit already applied) and calls
 * {@link UndoableEdit#redo()} on later calls (redo after undo).
 *
 * <p>
 * No bus events are fired from here. The owning text editor's {@code "text"} property
 * change listener runs on every undo and redo path and is responsible for writing the
 * new value into the model and publishing the appropriate topic.
 */
public final class DocumentEditCommand extends AbstractCommand {

    private final @NotNull String displayName;
    private final @NotNull UndoableEdit edit;
    private boolean applied = true;

    public DocumentEditCommand(@NotNull String displayName, @NotNull UndoableEdit edit) {
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
    public @NotNull String getDisplayName() {
        return displayName;
    }
}
