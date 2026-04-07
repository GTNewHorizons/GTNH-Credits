package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorMembership;

/**
 * Removes a role string from a category membership's role list.
 *
 * <p>
 * Undo restores the role at its original position.
 */
public final class RemovePersonRoleCommand implements Command {

    private final EditorMembership membership;
    private final String role;
    private int savedIndex;

    public RemovePersonRoleCommand(EditorMembership membership, String role) {
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
    public String getDisplayName() {
        return "Remove role " + role + " from " + membership.categoryId;
    }
}
