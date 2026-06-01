package net.noiraude.creditseditor.command.impl;

import java.util.function.IntConsumer;

import net.noiraude.creditseditor.command.LangFieldWriter;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Base for locale-targeted edits of a single lang field. */
abstract class AbstractEditLangFieldCommand extends AbstractCommand {

    private final @NotNull LangFieldWriter writer;
    private final @NotNull String oldValue;
    private final @NotNull String newValue;
    private final @NotNull IntConsumer caretSink;
    private final int caretAfter;

    @Contract(pure = true)
    protected AbstractEditLangFieldCommand(@NotNull LangFieldWriter writer, @NotNull String oldValue,
        @NotNull String newValue, @NotNull IntConsumer caretSink, int caretAfter) {
        this.writer = writer;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.caretSink = caretSink;
        this.caretAfter = caretAfter;
    }

    @Override
    public final void execute() {
        writer.write(newValue);
        caretSink.accept(Math.min(caretAfter, newValue.length()));
    }

    @Override
    public final void undo() {
        writer.write(oldValue);
        caretSink.accept(oldValue.length() - commonSuffixLength(oldValue, newValue));
    }

    private static int commonSuffixLength(@NotNull String a, @NotNull String b) {
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while (i < n && a.charAt(a.length() - 1 - i) == b.charAt(b.length() - 1 - i)) i++;
        return i;
    }
}
