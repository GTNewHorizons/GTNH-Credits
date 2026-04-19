package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.NotNull;

/**
 * Moves a role to a new position within a membership's role list.
 *
 * <p>
 * {@code fromIndex} is the source position and {@code dropIndex} is the insertion point as
 * reported by a {@code DropMode.INSERT} drop location (i.e., the index in the original list
 * before the drag item is removed). The actual insertion position is adjusted internally.
 */
public final class MoveRoleOrderCommand extends AbstractStructuralCommand {

    private final @NotNull DocumentMembership membership;
    private final int fromIndex;
    private final int insertAt;

    public MoveRoleOrderCommand(@NotNull DocumentMembership membership, int fromIndex, int dropIndex) {
        this.membership = membership;
        this.fromIndex = fromIndex;
        this.insertAt = dropIndex > fromIndex ? dropIndex - 1 : dropIndex;
    }

    @Override
    public void execute() {
        String role = membership.roles.remove(fromIndex);
        membership.roles.add(insertAt, role);
    }

    @Override
    public void undo() {
        String role = membership.roles.remove(insertAt);
        membership.roles.add(fromIndex, role);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Move role in " + membership.categoryId;
    }
}
