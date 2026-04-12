package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentPerson;

/** Appends a new person to the end of the person list. */
public final class AddPersonCommand extends AbstractStructuralCommand {

    private final CreditsDocument creditsDoc;
    private final DocumentPerson person;

    public AddPersonCommand(CreditsDocument creditsDoc, DocumentPerson person) {
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
    public String getDisplayName() {
        return "Add person " + person.name;
    }
}
