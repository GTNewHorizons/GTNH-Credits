package net.noiraude.creditseditor.ui;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Closed set of typed values that can be substituted into an {@link I18n} message pattern.
 */
public sealed interface MsgArg {

    /**
     * Returns the raw value for the JDK's formatting boundary.
     * Named 'unwrap' to avoid collisions with record component accessors.
     */
    @NotNull
    Object unwrap();

    @Contract("_ -> new")
    static @NotNull MsgArg text(@NotNull String text) {
        return new Text(text);
    }

    @Contract("_ -> new")
    static @NotNull MsgArg count(long count) {
        return new Count(count);
    }

    /**
     * Efficiently adapts the typed array to an {@code Object[]} without Stream overhead.
     */
    static Object @NotNull [] unwrapArgs(@NotNull MsgArg @NotNull [] args) {
        var out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            out[i] = args[i].unwrap();
        }
        return out;
    }

    /** Plain text substitution. */
    record Text(@NotNull String text) implements MsgArg {

        @Contract(pure = true)
        @Override
        public @NotNull Object unwrap() {
            return text;
        }
    }

    /** Count substitution. */
    record Count(long count) implements MsgArg {

        @Contract(pure = true)
        @Override
        public @NotNull Object unwrap() {
            return count;
        }
    }
}
