package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;

/**
 * Removes a role string from a category membership's role list.
 *
 * <p>
 * Undo restores the role at its original position.
 */
public final class RemovePersonRoleCommand extends AbstractStructuralCommand {

    private final DocumentMembership membership;
    private final String role;
    private int savedIndex;

    public RemovePersonRoleCommand(DocumentMembership membership, String role) {
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
