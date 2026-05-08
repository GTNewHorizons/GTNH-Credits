package net.noiraude.creditseditor.ui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Localized string lookup for the credits editor UI.
 *
 * <p>
 * Resolves keys against {@code messages.properties} resource bundles for {@link Locale#getDefault()},
 * formatted with {@link MessageFormat} when arguments are passed. Returns the key itself when the
 * bundle has no entry for it (or no bundle loaded at all), so missing translations surface in the
 * UI without crashing.
 *
 * <p>
 * The bundle is loaded lazily on first lookup and cached for the JVM lifetime; runtime locale
 * changes are not picked up.
 */
public final class I18n {

    private static final @NotNull String BUNDLE_BASE = "messages";

    private static final class Holder {

        private static final @Nullable ResourceBundle BUNDLE = load();

        private static @Nullable ResourceBundle load() {
            try {
                return ResourceBundle.getBundle(BUNDLE_BASE, Locale.getDefault());
            } catch (MissingResourceException ex) {
                try {
                    return ResourceBundle.getBundle(BUNDLE_BASE, Locale.ENGLISH);
                } catch (MissingResourceException ex2) {
                    return null;
                }
            }
        }
    }

    @Contract(pure = true)
    private I18n() {}

    /**
     * Returns the localized message for {@code key}, with each {@link MsgArg} substituted into
     * the matching {@code {n}} placeholder via {@link MessageFormat}. Returns {@code key}
     * verbatim when the bundle is missing or the key is unknown.
     *
     * <p>
     * The conversion to the {@link Object} array required by {@link MessageFormat} happens
     * inside {@link MsgArg#unwrapArgs}, so the public API exposes only the typed surface.
     */
    public static @NotNull String get(@NotNull String key, @NotNull MsgArg... args) {
        ResourceBundle bundle = Holder.BUNDLE;
        if (bundle == null) return key;
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
        if (args.length == 0) return pattern;
        return MessageFormat.format(pattern, MsgArg.unwrapArgs(args));
    }

    /**
     * Resolves a JVM {@link Locale} to a basename present in {@code available}.
     *
     * <p>
     * Tries an exact {@code language_COUNTRY} match first ({@code fr_FR}), then any entry sharing
     * the language tag ({@code fr_*}). Returns {@link Optional#empty()} when {@code jvmLocale}
     * carries no language tag or when no entry in {@code available} matches; the caller decides
     * what default to apply.
     */
    @Contract(pure = true)
    public static @NotNull Optional<String> resolveLangBasename(@NotNull Locale jvmLocale,
        @NotNull Set<String> available) {
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
