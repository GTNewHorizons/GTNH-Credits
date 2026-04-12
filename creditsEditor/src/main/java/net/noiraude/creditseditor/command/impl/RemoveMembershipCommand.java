package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Removes a category membership from a person's membership list.
 *
 * <p>
 * Undo restores the membership at its original position.
 */
public final class RemoveMembershipCommand extends AbstractStructuralCommand {

    private final DocumentPerson person;
    private final DocumentMembership membership;
    private int savedIndex;

    public RemoveMembershipCommand(DocumentPerson person, DocumentMembership membership) {
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
