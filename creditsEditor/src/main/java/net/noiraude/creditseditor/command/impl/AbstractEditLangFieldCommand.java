package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.LangFieldWriter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Base for locale-targeted edits of a single lang field. */
abstract class AbstractEditLangFieldCommand extends AbstractCommand {

    private final @NotNull LangFieldWriter writer;
    private final @NotNull String oldValue;
    private final @NotNull String newValue;

    @Contract(pure = true)
    protected AbstractEditLangFieldCommand(@NotNull LangFieldWriter writer, @NotNull String oldValue,
        @NotNull String newValue) {
        this.writer = writer;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public final void execute() {
        writer.write(newValue);
    }

    @Override
    public final void undo() {
        writer.write(oldValue);
    }
}
