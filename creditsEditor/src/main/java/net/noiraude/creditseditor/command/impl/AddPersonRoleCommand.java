package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/** Appends a role string to a category membership's role list. */
public final class AddPersonRoleCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentPerson person;
    private final @NotNull DocumentMembership membership;
    private final @NotNull String role;

    public AddPersonRoleCommand(@NotNull DocumentBus bus, @NotNull DocumentPerson person,
        @NotNull DocumentMembership membership, @NotNull String role) {
        this.bus = bus;
        this.person = person;
        this.membership = membership;
        this.role = role;
    }

    @Override
    public void execute() {
        membership.roles.add(role);
        bus.firePersonChanged(person);
    }

    @Override
    public void undo() {
        membership.roles.remove(role);
        bus.firePersonChanged(person);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Add role " + role + " in " + membership.categoryId;
    }
}
