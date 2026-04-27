package net.noiraude.creditseditor.command;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Undo/redo stack with dirty-state tracking.
 *
 * <p>
 * The stack is "clean" at the position corresponding to the last save. Executing,
 * undoing, or redoing commands may move the current position away from or back to the
 * clean mark, which is reflected by {@link #isDirty()}.
 *
 * <p>
 * Executing a new command after one or more undoing clears the redo stack.
 */
public final class CommandStack {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    /**
     * The size of the undo stack at the last call to {@link #markClean()},
     * or {@code -1} if the clean mark has been invalidated (i.e., the redo
     * stack was cleared after the mark was set).
     */
    private int cleanMark = 0;

    // -----------------------------------------------------------------------
    // Execute / undo / redo
    // -----------------------------------------------------------------------

    /** Executes {@code command}, pushes it onto the undo stack, and clears the redo stack. */
    public void execute(@NotNull Command command) {
        command.execute();
        undoStack.push(command);
        if (!redoStack.isEmpty()) {
            redoStack.clear();
            // Clearing the redo stack destroys the path back to the clean mark when
            // the mark was at a depth >= the current undo stack size (i.e., the mark
            // was ahead of where we were before this executing).
            if (cleanMark >= undoStack.size()) {
                cleanMark = -1;
            }
        }
    }

    /**
     * Undoes the most recently executed command.
     *
     * @throws IllegalStateException if there is nothing to undo
     */
    public void undo() {
        if (undoStack.isEmpty()) throw new IllegalStateException("Nothing to undo");
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
    }

    /**
     * Re-executes the most recently undone command.
     *
     * @throws IllegalStateException if there is nothing to redo
     */
    public void redo() {
        if (redoStack.isEmpty()) throw new IllegalStateException("Nothing to redo");
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
    }

    // -----------------------------------------------------------------------
    // Dirty state
    // -----------------------------------------------------------------------

    /**
     * Records the current stack position as the clean (saved) state.
     * Call this immediately after a successful save.
     */
    @Contract(mutates = "this")
    public void markClean() {
        cleanMark = undoStack.size();
    }

    /**
     * Returns {@code true} if the model has been modified since the last
     * {@link #markClean()} call (or since the stack was created if never marked clean).
     */
    @Contract(pure = true)
    public boolean isDirty() {
        if (cleanMark < 0) return true;
        return undoStack.size() != cleanMark;
    }

    // -----------------------------------------------------------------------
    // State inspection
    // -----------------------------------------------------------------------

    /** Returns {@code true} if there is at least one command that can be undone. */
    @Contract(pure = true)
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /** Returns {@code true} if there is at least one command that can be redone. */
    @Contract(pure = true)
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Returns the display name of the command that would be undone by the next
     * {@link #undo()} call, or {@code null} if the undo stack is empty.
     */
    public @Nullable String peekUndoName() {
        Command top = undoStack.peek();
        return top != null ? top.getDisplayName() : null;
    }

    /**
     * Returns the display name of the command that would be redone by the next
     * {@link #redo()} call, or {@code null} if the redo stack is empty.
     */
    public @Nullable String peekRedoName() {
        Command top = redoStack.peek();
        return top != null ? top.getDisplayName() : null;
    }

    /** Clears both the undo and redo stacks and resets the clean mark to zero. */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        cleanMark = 0;
    }
}
