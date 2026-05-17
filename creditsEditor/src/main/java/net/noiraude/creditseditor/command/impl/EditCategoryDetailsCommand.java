package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.LangFieldWriter;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Edit of the details lang key for a category. */
public final class EditCategoryDetailsCommand extends AbstractEditLangFieldCommand {

    private static final @NotNull String DISPLAY_NAME = I18n.get("command.edit.details");

    @Contract(pure = true)
    private EditCategoryDetailsCommand(@NotNull LangFieldWriter writer, @NotNull String oldValue,
        @NotNull String newValue) {
        super(writer, oldValue, newValue);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Command create(@NotNull LangFieldWriter writer, @NotNull String oldValue,
        @NotNull String newValue) {
        return new EditCategoryDetailsCommand(writer, oldValue, newValue);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }
}
