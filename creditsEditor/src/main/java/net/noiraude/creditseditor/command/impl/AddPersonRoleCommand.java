package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;

import org.jetbrains.annotations.NotNull;

/** Appends a role string to a category membership's role list. */
public final class AddPersonRoleCommand extends AbstractStructuralCommand {

    private final @NotNull DocumentMembership membership;
    private final @NotNull String role;

    public AddPersonRoleCommand(@NotNull DocumentMembership membership, @NotNull String role) {
        this.membership = membership;
        this.role = role;
    }

    @Override
    public void execute() {
        membership.roles.add(role);
    }

    @Override
    public void undo() {
        membership.roles.remove(role);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Add role " + role + " in " + membership.categoryId;
    }
}
