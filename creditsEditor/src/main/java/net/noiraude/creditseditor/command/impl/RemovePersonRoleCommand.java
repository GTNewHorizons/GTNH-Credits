package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.NotNull;

/**
 * Removes a role string from a category membership's role list.
 *
 * <p>
 * Undo restores the role at its original position.
 */
public final class RemovePersonRoleCommand extends AbstractStructuralCommand {

    private final @NotNull DocumentMembership membership;
    private final @NotNull String role;
    private int savedIndex;

    public RemovePersonRoleCommand(@NotNull DocumentMembership membership, @NotNull String role) {
        this.membership = membership;
        this.role = role;
    }

    @Override
    public void execute() {
        savedIndex = membership.roles.indexOf(role);
        membership.roles.remove(savedIndex);
    }

    @Override
    public void undo() {
        membership.roles.add(savedIndex, role);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove role " + role + " from " + membership.categoryId;
    }
}
