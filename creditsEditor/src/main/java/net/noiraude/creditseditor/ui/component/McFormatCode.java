package net.noiraude.creditseditor.ui.component;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parser for Minecraft {@code §x} formatting codes.
 *
 * <p>
 * Splits a raw string into a list of {@link Segment} instances, each carrying the text
 * content and the active style at that position. This class has no Swing dependency and can be
 * unit-tested independently.
 *
 * <p>
 * Supported codes:
 * <ul>
 * <li>{@code §0}–{@code §9}, {@code §a}–{@code §f}: set foreground colour (Minecraft palette);
 * also resets all active format modifiers.
 * <li>{@code §l}: bold on
 * <li>{@code §o}: italic on
 * <li>{@code §n}: underline on
 * <li>{@code §m}: strikethrough on
 * <li>{@code §k}: obfuscated on
 * <li>{@code §r}: reset all formatting (colour and modifiers) to default
 * </ul>
 *
 * <p>
 * Format modifier codes are additive. Colour codes reset all modifiers before setting the new
 * colour. {@code §r} resets everything including colour.
 */
public final class McFormatCode {

    /**
     * The 16-colour Minecraft palette.
     *
     * <p>
     * Index {@code i} corresponds to {@code §0}–{@code §9} (i = 0–9) and {@code §a}–{@code
     * §f} (i = 10–15).
     */
    public static final Color[] PALETTE = { new Color(0x000000), // §0 black
        new Color(0x0000AA), // §1 dark blue
        new Color(0x00AA00), // §2 dark green
        new Color(0x00AAAA), // §3 dark aqua
        new Color(0xAA0000), // §4 dark red
        new Color(0xAA00AA), // §5 dark purple
        new Color(0xFFAA00), // §6 gold
        new Color(0xAAAAAA), // §7 gray
        new Color(0x555555), // §8 dark gray
        new Color(0x5555FF), // §9 blue
        new Color(0x55FF55), // §a green
        new Color(0x55FFFF), // §b aqua
        new Color(0xFF5555), // §c red
        new Color(0xFF55FF), // §d light purple
        new Color(0xFFFF55), // §e yellow
        new Color(0xFFFFFF), // §f white
    };

    /**
     * A contiguous run of plain text that shares the same visual style.
     *
     * <p>
     * {@code color == null} means "inherit the component's default foreground colour".
     */
    public static final class Segment {

        /** Plain text content, with all {@code §x} escape sequences removed. */
        public final String text;

        /**
         * The foreground colour, or {@code null} to inherit the component's default foreground
         * colour.
         */
        public final Color color;

        public final boolean bold;
        public final boolean italic;
        public final boolean underline;
        public final boolean strikethrough;
        public final boolean obfuscated;

        Segment(String text, Color color, boolean bold, boolean italic, boolean underline, boolean strikethrough,
            boolean obfuscated) {
            this.text = text;
            this.color = color;
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.strikethrough = strikethrough;
            this.obfuscated = obfuscated;
        }
    }

    /**
     * Parses {@code raw} into an immutable list of styled segments.
     *
     * <p>
     * Empty text runs (adjacent codes with no characters between them) are omitted from the
     * result.
     *
     * @param raw the string to parse; {@code null} and empty are both accepted
     * @return ordered list of segments; never {@code null}
     */
    public static List<Segment> parse(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<Segment> result = new ArrayList<>();
        Color color = null;
        boolean bold = false, italic = false, underline = false, strikethrough = false, obfuscated = false;
        StringBuilder text = new StringBuilder();

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(i + 1));
                if (text.length() > 0) {
                    result.add(new Segment(text.toString(), color, bold, italic, underline, strikethrough, obfuscated));
                    text.setLength(0);
                }
                int colorIdx = paletteIndex(code);
                if (colorIdx >= 0) {
                    color = PALETTE[colorIdx];
                    bold = italic = underline = strikethrough = obfuscated = false;
                } else {
                    switch (code) {
                        case 'l':
                            bold = true;
                            break;
                        case 'o':
                            italic = true;
                            break;
                        case 'n':
                            underline = true;
                            break;
                        case 'm':
                            strikethrough = true;
                            break;
                        case 'k':
                            obfuscated = true;
                            break;
                        case 'r':
                            color = null;
                            bold = italic = underline = strikethrough = obfuscated = false;
                            break;
                        default:
                            // Unknown code: emit both characters as literal text
                            text.append(c)
                                .append(raw.charAt(i + 1));
                            i += 2;
                            continue;
                    }
                }
                i += 2;
            } else {
                text.append(c);
                i++;
            }
        }
        if (text.length() > 0) {
            result.add(new Segment(text.toString(), color, bold, italic, underline, strikethrough, obfuscated));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns a plain-text copy of {@code raw} with all {@code §x} sequences removed.
     *
     * @param raw the string to strip; {@code null} is returned as-is
     * @return stripped string, or {@code null} if the input is {@code null}
     */
    public static String strip(String raw) {
        if (raw == null) return null;
        if (raw.isEmpty()) return raw;
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '§' && i + 1 < raw.length()) {
                i++; // skip § and its code character
            } else {
                sb.append(raw.charAt(i));
            }
        }
        return sb.toString();
    }

    /** Returns the palette index (0–15) for {@code code}, or -1 if not a colour code. */
    private static int paletteIndex(char code) {
        if (code >= '0' && code <= '9') return code - '0';
        if (code >= 'a' && code <= 'f') return 10 + (code - 'a');
        return -1;
    }

    private McFormatCode() {}
}
