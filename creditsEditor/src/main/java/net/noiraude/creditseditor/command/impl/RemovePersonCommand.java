package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentPerson;

/**
 * Removes a person (and all their memberships) from the document.
 *
 * <p>
 * Undo restores the person at their original position. Their memberships are preserved
 * on the person object and come back with them.
 */
public final class RemovePersonCommand extends AbstractStructuralCommand {

    private final CreditsDocument creditsDoc;
    private final DocumentPerson person;
    private int savedIndex;

    public RemovePersonCommand(CreditsDocument creditsDoc, DocumentPerson person) {
        this.creditsDoc = creditsDoc;
        this.person = person;
    }

    @Override
    public void execute() {
        savedIndex = creditsDoc.persons.indexOf(person);
        creditsDoc.persons.remove(savedIndex);
    }

    @Override
    public void undo() {
        creditsDoc.persons.add(savedIndex, person);
    }

    @Override
    public String getDisplayName() {
        return "Remove person " + person.name;
    }
}
