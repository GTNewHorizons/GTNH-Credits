package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/** Appends a category membership to a person's membership list. */
public final class AddMembershipCommand extends AbstractStructuralCommand {

    private final @NotNull DocumentPerson person;
    private final @NotNull DocumentMembership membership;

    public AddMembershipCommand(@NotNull DocumentPerson person, @NotNull DocumentMembership membership) {
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
    public @NotNull String getDisplayName() {
        return "Add " + membership.categoryId + " to " + person.name;
    }
}
