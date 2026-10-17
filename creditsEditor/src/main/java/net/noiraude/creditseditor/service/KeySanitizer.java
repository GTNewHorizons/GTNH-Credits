package net.noiraude.creditseditor.service;

import net.noiraude.libcredits.model.CreditsKeys;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Converts a category id or role string into its lang key suffix.
 *
 * <p>
 * The algorithm is shared with the mod through libCredits.
 * The algorithm is:
 * <ol>
 * <li>Delete all {@code .} characters.</li>
 * <li>Delete all {@code -} characters.</li>
 * <li>Replace runs of one or more spaces with a single {@code _}.</li>
 * <li>Lowercase the result.</li>
 * </ol>
 *
 * <p>
 * Examples:
 * <ul>
 * <li>{@code "team"} → {@code "team"}</li>
 * <li>{@code "Core.Mod-Team"} → {@code "coremodteam"}</li>
 * <li>{@code "gtnh-creator"} → {@code "gtnhcreator"}</li>
 * <li>{@code "Key Test"} → {@code "key_test"}</li>
 * </ul>
 */
public final class KeySanitizer {

    @Contract(pure = true)
    private KeySanitizer() {}

    /** Returns the lang key suffix for {@code value}. */
    public static @NotNull String sanitize(@NotNull String value) {
        return CreditsKeys.sanitize(value);
    }
}
