package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.bus.LocaleEditor;
import net.noiraude.creditseditor.bus.LocaleSnapshot;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Base for undoable locale lifecycle commands that swap the active locale and snapshot state. */
abstract class AbstractLocaleCommand extends AbstractCommand {

    protected final @NotNull LocaleEditor editor;
    protected final @NotNull DocumentBus bus;
    protected final @NotNull String code;
    private final @NotNull String previousActiveLocale;
    private final @NotNull LocaleSnapshot beforeSnapshot;

    @Contract(pure = true)
    protected AbstractLocaleCommand(@NotNull LocaleEditor editor, @NotNull DocumentBus bus, @NotNull String code) {
        this.editor = editor;
        this.bus = bus;
        this.code = code;
        this.previousActiveLocale = bus.activeLocale();
        this.beforeSnapshot = editor.snapshotLocale(code);
    }

    @Override
    public final void undo() {
        editor.applyLocaleSnapshot(code, beforeSnapshot);
        bus.setActiveLocale(previousActiveLocale);
        bus.fireAvailableLocalesChanged();
    }
}
