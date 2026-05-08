package net.noiraude.creditseditor.service;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.noiraude.libcredits.lang.LangDocument;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Read-only locale fallback for credits lang keys.
 *
 * <p>
 * {@link #resolve(String, String)} returns the first non-empty value found in:
 * <ol>
 * <li>the active locale's {@link LangDocument}, if loaded;
 * <li>the {@link #DEFAULT_LOCALE} document, if loaded.
 * </ol>
 *
 * <p>
 * When neither tier has a non-empty value, the resolver returns an empty
 * {@link Optional}; the caller is responsible for choosing a placeholder
 * (e.g. the raw category id, role string, or empty UI text).
 *
 * <p>
 * Empty-string values are treated as absent at every tier, so a translator
 * leaving an entry blank in a non-default locale still falls through to the
 * default locale rather than rendering as empty text.
 */
public final class LangResolver {

    /**
     * Authoritative fallback locale tag for the credits lang resolution chain. Sourced
     * from {@link Locale#US} so the literal {@code "en_US"} is not hardcoded across the
     * codebase. Every other component referencing a default locale should read it here.
     */
    public static final @NotNull String DEFAULT_LOCALE = Locale.US.toString();

    private final @NotNull Map<String, LangDocument> locales;

    /**
     * @param locales live map keyed by locale tag (e.g. {@code en_US}, {@code fr_FR}).
     *                The resolver reads through the reference, so changes to the map
     *                or the documents are observed by subsequent {@link #resolve} calls.
     */
    public LangResolver(@NotNull Map<String, LangDocument> locales) {
        this.locales = locales;
    }

    /**
     * Returns the resolved value for {@code key} in {@code activeLocale}, falling back
     * to the default locale, or {@link Optional#empty()} if neither carries a non-empty
     * value.
     */
    @Contract(pure = true)
    public @NotNull Optional<String> resolve(@NotNull String key, @NotNull String activeLocale) {
        Optional<String> active = lookup(activeLocale, key);
        if (active.isPresent()) return active;
        if (DEFAULT_LOCALE.equals(activeLocale)) return Optional.empty();
        return lookup(DEFAULT_LOCALE, key);
    }

    private @NotNull Optional<String> lookup(@NotNull String locale, @NotNull String key) {
        LangDocument doc = locales.get(locale);
        if (doc == null) return Optional.empty();
        String value = doc.get(key);
        if (value == null || value.isEmpty()) return Optional.empty();
        return Optional.of(value);
    }
}
