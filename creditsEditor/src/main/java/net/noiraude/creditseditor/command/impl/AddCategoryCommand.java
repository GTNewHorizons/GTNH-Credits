package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;

/** Appends a new category to the end of the category list. */
public final class AddCategoryCommand implements Command {

    private final EditorModel model;
    private final EditorCategory category;

    public AddCategoryCommand(EditorModel model, EditorCategory category) {
        this.model = model;
        this.category = category;
    }

    @Override
    public void execute() {
        model.categories.add(category);
    }

    @Override
    public void undo() {
        model.categories.remove(category);
    }

    @Override
    public String getDisplayName() {
        return "Add category " + category.id;
    }
}
