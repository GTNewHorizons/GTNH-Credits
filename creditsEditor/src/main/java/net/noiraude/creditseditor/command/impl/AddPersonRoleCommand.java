package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Appends a local role or external reference to a category membership's ordered role list. */
public final class AddPersonRoleCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentPerson person;
    private final @NotNull DocumentMembership membership;
    private final @NotNull DocumentRole role;

    public AddPersonRoleCommand(@NotNull DocumentBus bus, @NotNull DocumentPerson person,
        @NotNull DocumentMembership membership, @NotNull DocumentRole role) {
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

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return "Add role " + role + " in " + membership.categoryId;
    }
}
