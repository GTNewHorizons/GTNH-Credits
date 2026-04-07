package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.model.EditorCategory;
import net.noiraude.creditseditor.model.EditorModel;

/**
 * Moves a category to a new position in the ordered category list.
 *
 * <p>
 * {@code targetIndex} is the desired final position of the category in the result list
 * (0-based). The category's {@link #undo} restores it to its position before the move.
 */
public final class MoveCategoryOrderCommand implements Command {

    private final EditorModel model;
    private final EditorCategory category;
    private final int targetIndex;
    private int originalIndex;

    public MoveCategoryOrderCommand(EditorModel model, EditorCategory category, int targetIndex) {
        this.model = model;
        this.category = category;
        this.targetIndex = targetIndex;
    }

    @Override
    public void execute() {
        originalIndex = model.categories.indexOf(category);
        model.categories.remove(category);
        model.categories.add(targetIndex, category);
    }

    @Override
    public void undo() {
        model.categories.remove(category);
        model.categories.add(originalIndex, category);
    }

    @Override
    public String getDisplayName() {
        return "Move category " + category.id;
    }
}
