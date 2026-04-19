package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentPerson;

import org.jetbrains.annotations.NotNull;

/**
 * Removes a person (and all their memberships) from the document.
 *
 * <p>
 * Undo restores the person at their original position. Their memberships are preserved
 * on the person object and come back with them.
 */
public final class RemovePersonCommand extends AbstractStructuralCommand {

    private final @NotNull CreditsDocument creditsDoc;
    private final @NotNull DocumentPerson person;
    private int savedIndex;

    public RemovePersonCommand(@NotNull CreditsDocument creditsDoc, @NotNull DocumentPerson person) {
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
    public @NotNull String getDisplayName() {
        return "Remove person " + person.name;
    }
}
