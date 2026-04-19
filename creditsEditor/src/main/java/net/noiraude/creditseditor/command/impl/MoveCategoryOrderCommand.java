package net.noiraude.creditseditor.command.impl;

import net.noiraude.libcredits.model.CreditsDocument;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;

/**
 * Moves a category to a new position in the ordered category list.
 *
 * <p>
 * {@code targetIndex} is the desired final position of the category in the result list
 * (0-based). The category's {@link #undo} restores it to its position before the move.
 */
public final class MoveCategoryOrderCommand extends AbstractStructuralCommand {

    private final @NotNull CreditsDocument creditsDoc;
    private final @NotNull DocumentCategory category;
    private final int targetIndex;
    private int originalIndex;

    public MoveCategoryOrderCommand(@NotNull CreditsDocument creditsDoc, @NotNull DocumentCategory category,
        int targetIndex) {
        this.creditsDoc = creditsDoc;
        this.category = category;
        this.targetIndex = targetIndex;
    }

    @Override
    public void execute() {
        originalIndex = creditsDoc.categories.indexOf(category);
        creditsDoc.categories.remove(category);
        creditsDoc.categories.add(targetIndex, category);
    }

    @Override
    public void undo() {
        creditsDoc.categories.remove(category);
        creditsDoc.categories.add(originalIndex, category);
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Move category " + category.id;
    }
}
