package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.command.LangFieldWriter;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Edit of the display-name lang key for a role. */
public final class EditRoleDisplayNameCommand extends AbstractEditLangFieldCommand {

    private static final @NotNull String DISPLAY_NAME = I18n.get("command.edit.role_display_name");

    @Contract(pure = true)
    private EditRoleDisplayNameCommand(@NotNull LangFieldWriter writer, @NotNull String oldValue,
        @NotNull String newValue) {
        super(writer, oldValue, newValue);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Command create(@NotNull LangFieldWriter writer, @NotNull String oldValue,
        @NotNull String newValue) {
        return new EditRoleDisplayNameCommand(writer, oldValue, newValue);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }
}
