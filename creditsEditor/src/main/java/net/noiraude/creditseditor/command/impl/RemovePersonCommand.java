package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/**
 * Removes a person (and all their memberships) from the document.
 *
 * <p>
 * Undo restores the person at their original position. Their memberships are preserved
 * on the person object and come back with them.
 */
public final class RemovePersonCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentPerson person;
    private int savedIndex;

    public RemovePersonCommand(@NotNull DocumentBus bus, @NotNull DocumentPerson person) {
        this.bus = bus;
        this.person = person;
    }

    @Override
    public void execute() {
        savedIndex = bus.creditsDoc().persons.indexOf(person);
        bus.creditsDoc().persons.remove(savedIndex);
        bus.firePersonsChanged();
    }

    @Override
    public void undo() {
        bus.creditsDoc().persons.add(savedIndex, person);
        bus.firePersonsChanged();
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove person " + person.name;
    }
}
