package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorPerson;

/**
 * Removes a category membership from a person's membership list.
 *
 * <p>
 * Undo restores the membership at its original position.
 */
public final class RemoveMembershipCommand implements Command {

    private final EditorPerson person;
    private final EditorMembership membership;
    private int savedIndex;

    public RemoveMembershipCommand(EditorPerson person, EditorMembership membership) {
        this.person = person;
        this.membership = membership;
    }

    @Override
    public void execute() {
        savedIndex = person.memberships.indexOf(membership);
        person.memberships.remove(savedIndex);
    }

    @Override
    public void undo() {
        person.memberships.add(savedIndex, membership);
    }

    @Override
    public String getDisplayName() {
        return "Remove " + membership.categoryId + " from " + person.name;
    }
}
