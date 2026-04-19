package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;

/** Appends a new category to the end of the category list. */
public final class AddCategoryCommand extends AbstractStructuralCommand {

    private final @NotNull CreditsDocument creditsDoc;
    private final @NotNull DocumentCategory category;

    public AddCategoryCommand(@NotNull CreditsDocument creditsDoc, @NotNull DocumentCategory category) {
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
    public @NotNull String getDisplayName() {
        return "Add category " + category.id;
    }
}
