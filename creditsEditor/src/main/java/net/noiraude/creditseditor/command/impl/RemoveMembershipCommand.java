package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/**
 * Removes a category membership from a person's membership list.
 *
 * <p>
 * Undo restores the membership at its original position.
 */
public final class RemoveMembershipCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentPerson person;
    private final @NotNull DocumentMembership membership;
    private int savedIndex;

    public RemoveMembershipCommand(@NotNull DocumentBus bus, @NotNull DocumentPerson person,
        @NotNull DocumentMembership membership) {
        this.bus = bus;
        this.person = person;
        this.membership = membership;
    }

    @Override
    public void execute() {
        savedIndex = person.memberships.indexOf(membership);
        person.memberships.remove(savedIndex);
        bus.firePersonChanged(person);
    }

    @Override
    public void undo() {
        person.memberships.add(savedIndex, membership);
        bus.firePersonChanged(person);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove " + membership.categoryId + " from " + person.name;
    }
}
