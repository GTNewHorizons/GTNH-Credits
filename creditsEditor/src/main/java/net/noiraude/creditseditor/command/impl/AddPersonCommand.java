package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/** Appends a new person to the end of the person list. */
public final class AddPersonCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentPerson person;

    public AddPersonCommand(@NotNull DocumentBus bus, @NotNull DocumentPerson person) {
        this.bus = bus;
        this.person = person;
    }

    @Override
    public void execute() {
        bus.creditsDoc().persons.add(person);
        bus.firePersonsChanged();
    }

    @Override
    public void undo() {
        bus.creditsDoc().persons.remove(person);
        bus.firePersonsChanged();
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Add person " + person.name;
    }
}
