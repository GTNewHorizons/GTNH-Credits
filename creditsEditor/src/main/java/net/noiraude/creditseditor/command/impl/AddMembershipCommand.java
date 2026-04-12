package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

/** Appends a category membership to a person's membership list. */
public final class AddMembershipCommand extends AbstractStructuralCommand {

    private final DocumentPerson person;
    private final DocumentMembership membership;

    public AddMembershipCommand(DocumentPerson person, DocumentMembership membership) {
        this.person = person;
        this.membership = membership;
    }

    @Override
    public void execute() {
        person.memberships.add(membership);
    }

    @Override
    public void undo() {
        person.memberships.remove(membership);
    }

    @Override
    public String getDisplayName() {
        return "Add " + membership.categoryId + " to " + person.name;
    }
}
