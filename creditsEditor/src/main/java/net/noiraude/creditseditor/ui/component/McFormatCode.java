package net.noiraude.creditseditor.ui.component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Minecraft {@code §x} formatting codes.
 *
 * <p>
 * Serves as the unified bridge between raw {@code §}-encoded strings, {@code StyledDocument}
 * character attributes, and toolbar state. State combinations are represented as
 * {@link EnumSet}{@code <McFormatCode>}: at most one color code plus any subset of modifier codes
 * may be active at once.
 *
 * <p>
 * Supported codes:
 * <ul>
 * <li>{@code §0}-{@code §f}: set foreground color (Minecraft palette); also resets all active
 * modifiers.
 * <li>{@code §l}: bold on
 * <li>{@code §o}: italic on
 * <li>{@code §n}: underline on
 * <li>{@code §m}: strikethrough on
 * <li>{@code §k}: obfuscated on
 * <li>{@code §r}: reset all formatting
 * </ul>
 */
public enum McFormatCode {

    // @formatter:off
    BLACK         ('0', new Color(0x000000)),
    DARK_BLUE     ('1', new Color(0x0000AA)),
    DARK_GREEN    ('2', new Color(0x00AA00)),
    DARK_AQUA     ('3', new Color(0x00AAAA)),
    DARK_RED      ('4', new Color(0xAA0000)),
    DARK_PURPLE   ('5', new Color(0xAA00AA)),
    GOLD          ('6', new Color(0xFFAA00)),
    GRAY          ('7', new Color(0xAAAAAA)),
    DARK_GRAY     ('8', new Color(0x555555)),
    BLUE          ('9', new Color(0x5555FF)),
    GREEN         ('a', new Color(0x55FF55)),
    AQUA          ('b', new Color(0x55FFFF)),
    RED           ('c', new Color(0xFF5555)),
    LIGHT_PURPLE  ('d', new Color(0xFF55FF)),
    YELLOW        ('e', new Color(0xFFFF55)),
    WHITE         ('f', new Color(0xFFFFFF)),
    BOLD          ('l'),
    ITALIC        ('o'),
    UNDERLINE     ('n'),
    STRIKETHROUGH ('m'),
    OBFUSCATED    ('k'),
    RESET         ('r');
    // @formatter:on

    /**
     * Document attribute key for the obfuscated ({@code §k}) effect.
     *
     * <p>
     * Stored as {@link Boolean#TRUE} when active. {@link StyleConstants} has no built-in support
     * for this effect.
     */
    public static final Object ATTR_OBFUSCATED = "mc-obfuscated";

    /** The 16 color codes in palette order ({@code §0}-{@code §f}). */
    public static final List<McFormatCode> COLORS;

    static {
        List<McFormatCode> colors = new ArrayList<>(16);
        for (McFormatCode c : values()) {
            if (c.color != null) colors.add(c);
        }
        COLORS = Collections.unmodifiableList(colors);
    }

    /** The § code character (the char after {@code §}). */
    public final char code;

    /**
     * The foreground color for color codes, or {@code null} for modifier/reset codes.
     */
    public final Color color;

    McFormatCode(char code) {
        this(code, null);
    }

    McFormatCode(char code, Color color) {
        this.code = code;
        this.color = color;
    }

    /** Returns {@code true} if this is one of the 16 color codes ({@code §0}-{@code §f}). */
    public boolean isColor() {
        return color != null;
    }

    /**
     * Returns {@code true} if this is one of the 5 modifier codes ({@code §l §o §n §m §k}).
     */
    public boolean isModifier() {
        return color == null && this != RESET;
    }

    /**
     * Returns a human-readable display name derived from the enum constant name, e.g.
     * {@code DARK_BLUE} → {@code "Dark Blue (§1)"}, {@code BOLD} → {@code "Bold (§l)"}.
     */
    public String displayName() {
        String[] parts = name().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0)));
            sb.append(
                part.substring(1)
                    .toLowerCase());
        }
        sb.append(" (§")
            .append(code)
            .append(')');
        return sb.toString();
    }

    /** Appends the {@code §x} escape sequence for this code to {@code sb}. */
    public void appendTo(StringBuilder sb) {
        sb.append('§')
            .append(code);
    }

    /**
     * Applies this code's effect to {@code attrs}.
     *
     * <p>
     * Color codes set {@link StyleConstants#Foreground}. Modifier codes set their corresponding
     * {@link StyleConstants} attribute to {@code true} (or {@link #ATTR_OBFUSCATED} for
     * {@link #OBFUSCATED}). Has no effect for {@link #RESET}.
     */
    public void applyTo(SimpleAttributeSet attrs) {
        switch (this) {
            case BOLD -> StyleConstants.setBold(attrs, true);
            case ITALIC -> StyleConstants.setItalic(attrs, true);
            case UNDERLINE -> StyleConstants.setUnderline(attrs, true);
            case STRIKETHROUGH -> StyleConstants.setStrikeThrough(attrs, true);
            case OBFUSCATED -> attrs.addAttribute(ATTR_OBFUSCATED, Boolean.TRUE);
            default -> {
                if (color != null) StyleConstants.setForeground(attrs, color);
            }
        }
    }

    /**
     * Removes this modifier's attribute from {@code attrs}, turning it off.
     *
     * <p>
     * Has no effect for color codes or {@link #RESET}.
     */
    public void removeFrom(SimpleAttributeSet attrs) {
        switch (this) {
            case BOLD -> StyleConstants.setBold(attrs, false);
            case ITALIC -> StyleConstants.setItalic(attrs, false);
            case UNDERLINE -> StyleConstants.setUnderline(attrs, false);
            case STRIKETHROUGH -> StyleConstants.setStrikeThrough(attrs, false);
            case OBFUSCATED -> attrs.addAttribute(ATTR_OBFUSCATED, Boolean.FALSE);
            default -> {}
        }
    }

    /**
     * Returns {@code true} if this code is currently active in {@code attrs}.
     *
     * <p>
     * Always returns {@code false} for {@link #RESET}.
     */
    public boolean isActive(AttributeSet attrs) {
        return switch (this) {
            case BOLD -> StyleConstants.isBold(attrs);
            case ITALIC -> StyleConstants.isItalic(attrs);
            case UNDERLINE -> StyleConstants.isUnderline(attrs);
            case STRIKETHROUGH -> StyleConstants.isStrikeThrough(attrs);
            case OBFUSCATED -> Boolean.TRUE.equals(attrs.getAttribute(ATTR_OBFUSCATED));
            default -> color != null && attrs.isDefined(StyleConstants.Foreground)
                && color.equals(StyleConstants.getForeground(attrs));
        };
    }

    /**
     * Builds an {@link EnumSet} of all codes active in {@code attrs}: at most one color plus any
     * modifier codes currently set.
     */
    public static EnumSet<McFormatCode> fromAttributes(AttributeSet attrs) {
        EnumSet<McFormatCode> result = EnumSet.noneOf(McFormatCode.class);
        for (McFormatCode c : values()) {
            if (c != RESET && c.isActive(attrs)) result.add(c);
        }
        return result;
    }

    /**
     * Computes which codes are active across the characters in {@code doc} over {@code [start,
     * end)}, excluding paragraph separator ({@code '\n'}) characters.
     *
     * @return a {@link SelectionPresence} whose {@code all} set contains codes active on every
     *         character and whose {@code any} set contains codes active on at least one character
     */
    public static SelectionPresence computePresence(StyledDocument doc, int start, int end) {
        EnumSet<McFormatCode> all = null; // null until the first non-newline run is seen
        EnumSet<McFormatCode> any = EnumSet.noneOf(McFormatCode.class);
        boolean anyLacksColor = false;
        int offset = start;
        while (offset < end) {
            Element elem = doc.getCharacterElement(offset);
            int runEnd = Math.min(elem.getEndOffset(), end);
            String text;
            try {
                text = doc.getText(offset, runEnd - offset);
            } catch (BadLocationException ignored) {
                offset = runEnd;
                continue;
            }
            if (containsNonNewline(text)) {
                EnumSet<McFormatCode> active = fromAttributes(elem.getAttributes());
                if (activeColor(active) == null) anyLacksColor = true;
                if (all == null) {
                    all = EnumSet.copyOf(active.isEmpty() ? EnumSet.noneOf(McFormatCode.class) : active);
                } else {
                    all.retainAll(active); // intersection: keep only codes present everywhere
                }
                any.addAll(active); // union: accumulate codes present anywhere
            }
            offset = runEnd;
        }
        return new SelectionPresence(all != null ? all : EnumSet.noneOf(McFormatCode.class), any, anyLacksColor);
    }

    static boolean containsNonNewline(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\n') return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // SelectionPresence
    // ------------------------------------------------------------------

    /**
     * Holds the result of a selection-wide formatting scan.
     *
     * <p>
     * {@code all} contains every {@link McFormatCode} active on every character in the selection.
     * {@code any} contains every code active on at least one character. A code in {@code any} but
     * not in {@code all} is in a mixed (indeterminate) state. {@code anyLacksColor} is
     * {@code true} when at least one character in the selection carries no color attribute.
     */
    public static final class SelectionPresence {

        /** Codes active on every character in the selection. */
        public final EnumSet<McFormatCode> all;

        /** Codes active on at least one character in the selection. */
        public final EnumSet<McFormatCode> any;

        /**
         * {@code true} when at least one selected character has no color attribute (default
         * color).
         */
        public final boolean anyLacksColor;

        SelectionPresence(EnumSet<McFormatCode> all, EnumSet<McFormatCode> any, boolean anyLacksColor) {
            this.all = all;
            this.any = any;
            this.anyLacksColor = anyLacksColor;
        }
    }

    /**
     * Returns the active color code from {@code codes}, or {@code null} if none is present.
     */
    public static McFormatCode activeColor(EnumSet<McFormatCode> codes) {
        for (McFormatCode c : codes) {
            if (c.isColor()) return c;
        }
        return null;
    }

    /**
     * Returns {@code true} if transitioning from {@code prev} to {@code next} requires a reset
     * point (a color code or {@code §r}): either the color changed or a modifier was turned off.
     */
    public static boolean needsResetBefore(EnumSet<McFormatCode> prev, EnumSet<McFormatCode> next) {
        if (activeColor(prev) != activeColor(next)) return true;
        for (McFormatCode c : prev) {
            if (c.isModifier() && !next.contains(c)) return true;
        }
        return false;
    }

    /**
     * Returns the {@link McFormatCode} for the given code character, or {@code null} if not
     * recognized.
     */
    public static McFormatCode fromChar(char c) {
        for (McFormatCode mc : values()) {
            if (mc.code == c) return mc;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Segment
    // ------------------------------------------------------------------

    /**
     * A contiguous run of plain text sharing the same visual style.
     *
     * <p>
     * {@code codes} holds the active color code (if any) and any modifier codes. An empty set
     * means the run inherits the component's default style. Never contains {@link #RESET}.
     */
    public static final class Segment {

        /** Plain text content, with all {@code §x} sequences removed. */
        public final String text;

        /**
         * Active formatting codes: at most one color plus any subset of modifiers. Never contains
         * {@link McFormatCode#RESET}.
         */
        public final EnumSet<McFormatCode> codes;

        Segment(String text, EnumSet<McFormatCode> codes) {
            this.text = text;
            this.codes = codes.isEmpty() ? EnumSet.noneOf(McFormatCode.class) : EnumSet.copyOf(codes);
        }
    }

    // ------------------------------------------------------------------
    // Parsing and stripping
    // ------------------------------------------------------------------

    /**
     * Parses {@code raw} into an immutable list of styled segments.
     *
     * <p>
     * Empty text runs are omitted. Unknown {@code §x} sequences are emitted as literal text.
     *
     * @param raw the string to parse; {@code null} and empty are both accepted
     * @return ordered list of segments; never {@code null}
     */
    public static List<Segment> parse(String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<Segment> result = new ArrayList<>();
        EnumSet<McFormatCode> state = EnumSet.noneOf(McFormatCode.class);
        StringBuilder text = new StringBuilder();

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                McFormatCode mc = fromChar(Character.toLowerCase(raw.charAt(i + 1)));
                if (mc == null) {
                    text.append(c)
                        .append(raw.charAt(i + 1));
                    i += 2;
                    continue;
                }
                flushSegment(text, state, result);
                applyStateChange(mc, state);
                i += 2;
            } else {
                text.append(c);
                i++;
            }
        }
        flushSegment(text, state, result);
        return Collections.unmodifiableList(result);
    }

    private static void flushSegment(StringBuilder text, EnumSet<McFormatCode> state, List<Segment> result) {
        if (!text.isEmpty()) {
            result.add(new Segment(text.toString(), state));
            text.setLength(0);
        }
    }

    private static void applyStateChange(McFormatCode mc, EnumSet<McFormatCode> state) {
        if (mc == RESET || mc.isColor()) state.clear();
        if (mc != RESET) state.add(mc);
    }

    /**
     * Returns a plain-text copy of {@code raw} with all {@code §x} sequences removed.
     *
     * @param raw the string to strip; {@code null} is returned as-is
     * @see McText#strip(String)
     */
    public static String strip(String raw) {
        return McText.strip(raw);
    }
}
