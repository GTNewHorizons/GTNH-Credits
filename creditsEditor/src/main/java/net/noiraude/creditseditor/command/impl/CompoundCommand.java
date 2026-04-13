package net.noiraude.creditseditor.command.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.noiraude.creditseditor.command.Command;

/**
 * Groups multiple commands into a single undo/redo entry.
 *
 * <p>
 * On execute, children run in insertion order. On undo, they run in reverse order.
 * The compound is always structural ({@link #isLightEdit()} returns {@code false}).
 */
public final class CompoundCommand extends AbstractStructuralCommand {

    private final String displayName;
    private final List<Command> children;

    private CompoundCommand(String displayName, List<Command> children) {
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
    public String getDisplayName() {
        return displayName;
    }

    /** Returns the number of child commands in this compound. */
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

        private final String displayName;
        private final List<Command> children = new ArrayList<>();

        public Builder(String displayName) {
            this.displayName = displayName;
        }

        /** Appends a child command. */
        public Builder add(Command child) {
            children.add(child);
            return this;
        }

        /** Returns {@code true} if no child commands have been added. */
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
        public Command build() {
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
