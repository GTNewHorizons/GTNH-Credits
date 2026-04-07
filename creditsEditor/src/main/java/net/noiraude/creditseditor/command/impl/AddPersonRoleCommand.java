package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorMembership;

/** Appends a role string to a category membership's role list. */
public final class AddPersonRoleCommand implements Command {

    private final EditorMembership membership;
    private final String role;

    public AddPersonRoleCommand(EditorMembership membership, String role) {
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
