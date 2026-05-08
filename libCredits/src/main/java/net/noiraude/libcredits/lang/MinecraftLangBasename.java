package net.noiraude.libcredits.lang;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Utilities for the Minecraft 1.7.10 lang file basename convention ({@code aa_BB.lang}).
 *
 * <p>
 * Minecraft uses an underscore-separated lowercase-language plus uppercase-country form for
 * its lang file stems. That form is not the same as {@link Locale#toLanguageTag()}, which uses
 * a hyphen, so a small adapter is needed whenever the editor's UI {@link Locale} has to be
 * resolved against the lang files actually present in a credits resource pack.
 *
 * <p>
 * This class lives in {@code libCredits} because the {@code aa_BB} basename format is a
 * property of the resource pack, not of the editor's UI.
 */
public final class MinecraftLangBasename {

    @Contract(pure = true)
    private MinecraftLangBasename() {}

    /**
     * Resolves a JVM {@link Locale} to a basename present in {@code available}.
     *
     * <p>
     * Tries an exact {@code language_COUNTRY} match first ({@code fr_FR}), then any entry
     * sharing the language tag ({@code fr_*}). Returns {@link Optional#empty()} when
     * {@code jvmLocale} carries no language tag or when no entry in {@code available} matches;
     * the caller decides what default to apply.
     *
     * @param jvmLocale the runtime locale, typically {@link Locale#getDefault()}
     * @param available the lang basenames present in the resource pack, e.g. {@code en_US}
     */
    @Contract(pure = true)
    public static @NotNull Optional<String> forJavaLocale(@NotNull Locale jvmLocale, @NotNull Set<String> available) {
        String lang = jvmLocale.getLanguage();
        if (lang.isEmpty()) return Optional.empty();
        String country = jvmLocale.getCountry();
        if (!country.isEmpty()) {
            String exact = lang + "_" + country;
            if (available.contains(exact)) return Optional.of(exact);
        }
        String prefix = lang + "_";
        for (String basename : available) {
            if (basename.startsWith(prefix)) return Optional.of(basename);
        }
        return Optional.empty();
    }
}
