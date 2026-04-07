package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

/**
 * Removes a person (and all their memberships) from the model.
 *
 * <p>
 * Undo restores the person at their original position. Their memberships are preserved
 * on the person object and come back with them.
 */
public final class RemovePersonCommand implements Command {

    private final EditorModel model;
    private final EditorPerson person;
    private int savedIndex;

    public RemovePersonCommand(EditorModel model, EditorPerson person) {
        this.model = model;
        this.person = person;
    }

    @Override
    public void execute() {
        savedIndex = model.persons.indexOf(person);
        model.persons.remove(savedIndex);
    }

    @Override
    public void undo() {
        model.persons.add(savedIndex, person);
    }

    @Override
    public String getDisplayName() {
        return "Remove person " + person.name;
    }
}
