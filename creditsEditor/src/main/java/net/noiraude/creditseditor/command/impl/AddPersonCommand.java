package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorModel;
import net.noiraude.creditseditor.model.EditorPerson;

/** Appends a new person to the end of the person list. */
public final class AddPersonCommand implements Command {

    private final EditorModel model;
    private final EditorPerson person;

    public AddPersonCommand(EditorModel model, EditorPerson person) {
        this.model = model;
        this.person = person;
    }

    @Override
    public void execute() {
        model.persons.add(person);
    }

    @Override
    public void undo() {
        model.persons.remove(person);
    }

    @Override
    public String getDisplayName() {
        return "Add person " + person.name;
    }
}
