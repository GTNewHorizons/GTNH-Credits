package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/**
 * Removes a category membership from a person's membership list.
 *
 * <p>
 * Undo restores the membership at its original position.
 */
public final class RemoveMembershipCommand extends AbstractStructuralCommand {

    private final @NotNull DocumentPerson person;
    private final @NotNull DocumentMembership membership;
    private int savedIndex;

    public RemoveMembershipCommand(@NotNull DocumentPerson person, @NotNull DocumentMembership membership) {
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
    public @NotNull String getDisplayName() {
        return "Remove " + membership.categoryId + " from " + person.name;
    }
}
