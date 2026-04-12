package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;

/** Appends a role string to a category membership's role list. */
public final class AddPersonRoleCommand extends AbstractStructuralCommand {

    private final DocumentMembership membership;
    private final String role;

    public AddPersonRoleCommand(DocumentMembership membership, String role) {
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
    public String getDisplayName() {
        return "Add role " + role + " in " + membership.categoryId;
    }
}
