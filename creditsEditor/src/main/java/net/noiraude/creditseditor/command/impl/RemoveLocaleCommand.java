package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.LocaleEditor;
import net.noiraude.creditseditor.command.Command;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Removes a locale from the editing session. */
public final class RemoveLocaleCommand extends AbstractLocaleCommand {

    private static final @NotNull String DISPLAY_NAME = I18n.get("command.uninstall_locale");

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
        boolean removingActive = code.equals(bus.activeLocale());
        editor.removeLocale(code);
        if (removingActive) {
            String fallback = bus.availableLocales()
                .stream()
                .findFirst()
                .orElse("");
            bus.setActiveLocale(fallback);
        } else {
            bus.fireAvailableLocalesChanged();
        }
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getDisplayName() {
        return DISPLAY_NAME;
    }
}
