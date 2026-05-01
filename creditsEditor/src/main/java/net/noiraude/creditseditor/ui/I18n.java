package net.noiraude.creditseditor.ui;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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
     * Returns the localized message for {@code key}, formatted with {@code args} via
     * {@link MessageFormat#format(String, Object...)} when any are supplied. Returns {@code key}
     * verbatim when the bundle is missing or the key is unknown.
     */
    public static @NotNull String get(@NotNull String key, @NotNull Object... args) {
        ResourceBundle bundle = Holder.BUNDLE;
        if (bundle == null) return key;
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
        if (args.length == 0) return pattern;
        return MessageFormat.format(pattern, args);
    }
}
