package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;

/** Appends a new category to the end of the category list. */
public final class AddCategoryCommand extends AbstractStructuralCommand {

    private final CreditsDocument creditsDoc;
    private final DocumentCategory category;

    public AddCategoryCommand(CreditsDocument creditsDoc, DocumentCategory category) {
        this.creditsDoc = creditsDoc;
        this.category = category;
    }

    @Override
    public void execute() {
        creditsDoc.categories.add(category);
    }

    @Override
    public void undo() {
        creditsDoc.categories.remove(category);
    }

    @Override
    public String getDisplayName() {
        return "Add category " + category.id;
    }
}
