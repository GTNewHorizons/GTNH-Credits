package net.noiraude.creditseditor.command;

import java.util.Optional;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Immutable snapshot of a {@link CommandStack}'s observable state. */
public record CommandStackSnapshot(boolean canUndo, boolean canRedo, @NotNull Optional<String> undoName,
    @NotNull Optional<String> redoName) {

    public static final @NotNull CommandStackSnapshot EMPTY = new CommandStackSnapshot(
        false,
        false,
        Optional.empty(),
        Optional.empty());

    @Contract("_ -> new")
    public static @NotNull CommandStackSnapshot of(@NotNull CommandStack stack) {
        return new CommandStackSnapshot(
            stack.canUndo(),
            stack.canRedo(),
            Optional.ofNullable(stack.peekUndoName()),
            Optional.ofNullable(stack.peekRedoName()));
    }
}
