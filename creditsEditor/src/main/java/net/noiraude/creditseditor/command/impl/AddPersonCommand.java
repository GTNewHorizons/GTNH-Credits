package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/** Appends a new person to the end of the person list. */
public final class AddPersonCommand extends AbstractStructuralCommand {

    private final @NotNull CreditsDocument creditsDoc;
    private final @NotNull DocumentPerson person;

    public AddPersonCommand(@NotNull CreditsDocument creditsDoc, @NotNull DocumentPerson person) {
        this.creditsDoc = creditsDoc;
        this.person = person;
    }

    @Override
    public void execute() {
        creditsDoc.persons.add(person);
    }

    @Override
    public void undo() {
        creditsDoc.persons.remove(person);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Add person " + person.name;
    }
}
