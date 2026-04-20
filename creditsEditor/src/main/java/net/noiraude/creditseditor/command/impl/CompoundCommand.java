package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.noiraude.creditseditor.command.Command;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Groups multiple commands into a single undo/redo entry.
 *
 * <p>
 * On execute, children run in insertion order. On undo, they run in reverse order.
 * Each child publishes its own bus topics; no extra events are fired by the compound.
 */
public final class CompoundCommand extends AbstractCommand {

    private final @NotNull String displayName;
    private final @NotNull List<Command> children;

    private CompoundCommand(@NotNull String displayName, @NotNull List<Command> children) {
        this.displayName = displayName;
        this.children = children;
    }

    @Override
    public void execute() {
        for (Command child : children) {
            child.execute();
        }
    }

    @Override
    public void undo() {
        List<Command> reversed = new ArrayList<>(children);
        Collections.reverse(reversed);
        for (Command child : reversed) {
            child.undo();
        }
    }

    @Override
    public @NotNull String getDisplayName() {
        return displayName;
    }

    /** Returns the number of child commands in this compound. */
    @Contract(pure = true)
    public int size() {
        return children.size();
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    /**
     * Fluent builder that accumulates child commands before creating the compound.
     *
     * <p>
     * If only one child is added, {@link #build()} returns that child directly
     * instead of wrapping it in a compound.
     */
    public static final class Builder {

        private final @NotNull String displayName;
        private final @NotNull List<Command> children = new ArrayList<>();

        public Builder(@NotNull String displayName) {
            this.displayName = displayName;
        }

        /** Appends a child command. */
        @Contract(value = "_ -> this", mutates = "this")
        public @NotNull Builder add(@NotNull Command child) {
            children.add(child);
            return this;
        }

        /** Returns {@code true} if no child commands have been added. */
        @Contract(pure = true)
        public boolean isEmpty() {
            return children.isEmpty();
        }

        /**
         * Builds the resulting command.
         *
         * @return the single child if only one was added, or a {@link CompoundCommand}
         *         wrapping all children
         * @throws IllegalStateException if no children were added
         */
        public @NotNull Command build() {
            if (children.isEmpty()) {
                throw new IllegalStateException("CompoundCommand requires at least one child");
            }
            if (children.size() == 1) {
                return children.getFirst();
            }
            return new CompoundCommand(displayName, List.copyOf(children));
        }
    }
}
