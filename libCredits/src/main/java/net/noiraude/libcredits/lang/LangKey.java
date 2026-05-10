package net.noiraude.libcredits.lang;

import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Logical access path into a {@link LangDocument} for a single physical lang key.
 *
 * <p>
 * Subclasses replace the access semantics for keys whose logical content spans multiple
 * physical keys (see {@link DetailLangKey}). Callers commit to a key kind at construction
 * time so the document never branches on key shape.
 */
public class LangKey {

    /** The physical key string this logical entry maps to. */
    protected final @NotNull String key;

    @Contract(pure = true)
    public LangKey(@NotNull String key) {
        this.key = Objects.requireNonNull(key, "key");
    }

    /** Returns the underlying lang key string. */
    @Contract(pure = true)
    public final @NotNull String key() {
        return key;
    }

    /** Returns the value stored under this entry, or empty if no value exists. */
    @Contract(pure = true)
    public @NotNull Optional<String> read(@NotNull LangDocument doc) {
        return Optional.ofNullable(doc.get(key));
    }

    /** Stores {@code value} under this entry. */
    public void write(@NotNull LangDocument doc, @NotNull String value) {
        doc.set(key, value);
    }

    /** Removes every physical representation of this entry. */
    public void remove(@NotNull LangDocument doc) {
        doc.remove(key);
    }
}
