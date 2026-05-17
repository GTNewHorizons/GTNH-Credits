package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.LocaleEditor;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Marks a locale's credits-owned keys for removal and falls back to the default locale. */
public final class RemoveLocaleCommand extends AbstractLocaleCommand {

    private static final @NotNull String DISPLAY_NAME = I18n.get("command.remove_locale");

    @Contract(pure = true)
    private RemoveLocaleCommand(@NotNull LocaleEditor editor, @NotNull DocumentBus bus, @NotNull String code) {
        super(editor, bus, code);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Command create(@NotNull LocaleEditor editor, @NotNull DocumentBus bus,
        @NotNull String code) {
        return new RemoveLocaleCommand(editor, bus, code);
    }

    @Override
    public void execute() {
        editor.removeLocale(code);
        bus.setActiveLocale(LangResolver.DEFAULT_LOCALE);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }
}
