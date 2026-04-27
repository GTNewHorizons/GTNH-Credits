package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.model.DocumentCategory;

import org.jetbrains.annotations.NotNull;

/** Appends a new category to the end of the category list. */
public final class AddCategoryCommand extends AbstractCommand {

    private final @NotNull DocumentBus bus;
    private final @NotNull DocumentCategory category;

    public AddCategoryCommand(@NotNull DocumentBus bus, @NotNull DocumentCategory category) {
        this.bus = bus;
        this.category = category;
    }

    @Override
    public void execute() {
        bus.creditsDoc().categories.add(category);
        bus.fireCategoriesChanged();
    }

    @Override
    public void undo() {
        bus.creditsDoc().categories.remove(category);
        bus.fireCategoriesChanged();
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Add category " + category.id;
    }
}
