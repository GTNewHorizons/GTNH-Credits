package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.LocaleEditor;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Registers a new editing locale and activates it; undo restores the prior state. */
public final class AddLocaleCommand extends AbstractLocaleCommand {

    private static final @NotNull String DISPLAY_NAME = I18n.get("command.add_locale");

    @Contract(pure = true)
    private AddLocaleCommand(@NotNull LocaleEditor editor, @NotNull DocumentBus bus, @NotNull String code) {
        super(editor, bus, code);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Command create(@NotNull LocaleEditor editor, @NotNull DocumentBus bus,
        @NotNull String code) {
        return new AddLocaleCommand(editor, bus, code);
    }

    @Override
    public void execute() {
        editor.addLocale(code);
        bus.setActiveLocale(code);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }
}
