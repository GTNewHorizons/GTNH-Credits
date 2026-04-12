package net.noiraude.creditseditor.ui.component;

/**
 * Plain-text utilities for Minecraft {@code §x}-formatted strings.
 *
 * <p>
 * Provides lightweight string operations that do not require the full
 * {@link McFormatCode} enum. Display-only code (list renderers, title
 * labels) should prefer this class so it avoids a dependency on the
 * richer formatting machinery.
 */
public final class McText {

    private McText() {}

    /**
     * Returns a plain-text copy of {@code raw} with all {@code §x} sequences removed.
     *
     * @param raw the string to strip; {@code null} is returned as-is
     */
    public static String strip(String raw) {
        if (raw == null) return null;
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
}
