package net.noiraude.creditseditor.command;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.libcredits.lang.LangDocument;
import net.noiraude.libcredits.lang.LangKey;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/** Write surface bound to one lang key in one lang document, notifying observers on each write. */
public interface LangFieldWriter {

    void write(@NotNull String value);

    @Contract(pure = true)
    static @NotNull LangFieldWriter ofBus(@NotNull DocumentBus bus, @NotNull LangDocument target,
        @NotNull LangKey key) {
        return value -> {
            if (value.isEmpty()) key.remove(target);
            else key.write(target, value);
            bus.fireLangChanged(key.key());
        };
    }
}
