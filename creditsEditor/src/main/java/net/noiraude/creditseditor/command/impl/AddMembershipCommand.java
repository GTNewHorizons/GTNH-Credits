package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorMembership;
import net.noiraude.creditseditor.model.EditorPerson;

/** Appends a category membership to a person's membership list. */
public final class AddMembershipCommand implements Command {

    private final EditorPerson person;
    private final EditorMembership membership;

    public AddMembershipCommand(EditorPerson person, EditorMembership membership) {
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
