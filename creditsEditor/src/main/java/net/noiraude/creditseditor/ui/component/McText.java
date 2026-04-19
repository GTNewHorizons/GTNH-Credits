package net.noiraude.creditseditor.ui.component;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Plain-text utilities for Minecraft {@code §x}-formatted strings and lang file encoding.
 *
 * <p>
 * Provides lightweight string operations that do not require the full
 * {@link McFormatCode} enum. Display-only code (list renderers, title
 * labels) should prefer this class so it avoids a dependency on the
 * richer formatting machinery.
 */
public final class McText {

    @Contract(pure = true)
    private McText() {}

    /**
     * Returns a plain-text copy of {@code raw} with all {@code §x} sequences removed.
     */
    @Contract(pure = true)
    public static @NotNull String strip(@NotNull String raw) {
        if (raw.isEmpty()) return raw;
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '§' && i + 1 < raw.length()) {
                i++;
            } else {
                sb.append(raw.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Decodes a Minecraft lang file value to display format.
     *
     * <p>
     * Converts {@code \\} to a single backslash and {@code \n} (backslash-n) to an actual
     * newline character. Other {@code \x} sequences are passed through unchanged.
     */
    @Contract(pure = true)
    public static @NotNull String decodeLang(@NotNull String langValue) {
        if (!langValue.contains("\\")) return langValue;
        StringBuilder sb = new StringBuilder(langValue.length());
        int i = 0;
        while (i < langValue.length()) {
            char c = langValue.charAt(i);
            if (c == '\\' && i + 1 < langValue.length()) {
                char next = langValue.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                    i += 2;
                } else if (next == '\\') {
                    sb.append('\\');
                    i += 2;
                } else {
                    sb.append(c);
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Encodes a display-format string to Minecraft lang file format.
     *
     * <p>
     * Converts actual newline characters to {@code \n} (backslash-n) and backslashes to
     * {@code \\}.
     */
    @Contract(pure = true)
    public static @NotNull String encodeLang(@NotNull String displayValue) {
        if (!displayValue.contains("\\") && !displayValue.contains("\n")) return displayValue;
        StringBuilder sb = new StringBuilder(displayValue.length() + 4);
        for (int i = 0; i < displayValue.length(); i++) {
            char c = displayValue.charAt(i);
            if (c == '\n') sb.append("\\n");
            else if (c == '\\') sb.append("\\\\");
            else sb.append(c);
        }
        return sb.toString();
    }
}
