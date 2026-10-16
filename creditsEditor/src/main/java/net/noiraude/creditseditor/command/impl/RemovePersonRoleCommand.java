package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentMembership;
import net.noiraude.libcredits.model.DocumentPerson;
import net.noiraude.libcredits.model.DocumentRole;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Removes a local role or external reference from a category membership's role list.
 *
 * <p>
 * Undo restores the role at its original position.
 */
public final class RemovePersonRoleCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentPerson person;
    private final @NotNull DocumentMembership membership;
    private final @NotNull DocumentRole role;
    private int savedIndex;

    public RemovePersonRoleCommand(@NotNull DocumentBus bus, @NotNull DocumentPerson person,
        @NotNull DocumentMembership membership, @NotNull DocumentRole role) {
        this.bus = bus;
        this.person = person;
        this.membership = membership;
        this.role = role;
    }

    @Override
    public void execute() {
        savedIndex = membership.roles.indexOf(role);
        membership.roles.remove(savedIndex);
        bus.firePersonChanged(person);
    }

    @Override
    public void undo() {
        membership.roles.add(savedIndex, role);
        bus.firePersonChanged(person);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return "Remove role " + role + " from " + membership.categoryId;
    }
}
