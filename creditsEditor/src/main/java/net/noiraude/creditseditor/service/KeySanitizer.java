package net.noiraude.creditseditor.service;

/**
 * Converts a category id or role string into its lang key suffix.
 *
 * <p>
 * The algorithm is an independent reimplementation that must produce the same output as
 * the GTNH Credits mod at runtime, so that lang keys written by this editor are found by
 * the game. The algorithm is:
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

    private KeySanitizer() {}

    /** Returns the lang key suffix for {@code value}. */
    public static String sanitize(String value) {
        return value.replace(".", "")
            .replace("-", "")
            .replaceAll(" +", "_")
            .toLowerCase();
    }
}
