package net.noiraude.creditseditor.mc;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minecraft {@code §x} formatting codes as a pure domain concept.
 *
 * <p>
 * Carries no presentation-layer types: color codes expose their RGB value as an {@code int}
 * rather than a {@code java.awt.Color}, and no {@code javax.swing.text} attribute conversion
 * lives here. The Swing bridge is {@code McSwingStyle} plus {@code McPalette} in the editor UI
 * package.
 *
 * <p>
 * State combinations are represented as {@link EnumSet}{@code <McFormatCode>}: at most one color
 * code plus any subset of modifier codes may be active at once. {@link #RESET} is never kept in
 * an {@link EnumSet}; it is applied by clearing the set.
 */
public enum McFormatCode {

    // @formatter:off
    BLACK         ('0', 0x000000),
    DARK_BLUE     ('1', 0x0000AA),
    DARK_GREEN    ('2', 0x00AA00),
    DARK_AQUA     ('3', 0x00AAAA),
    DARK_RED      ('4', 0xAA0000),
    DARK_PURPLE   ('5', 0xAA00AA),
    GOLD          ('6', 0xFFAA00),
    GRAY          ('7', 0xAAAAAA),
    DARK_GRAY     ('8', 0x555555),
    BLUE          ('9', 0x5555FF),
    GREEN         ('a', 0x55FF55),
    AQUA          ('b', 0x55FFFF),
    RED           ('c', 0xFF5555),
    LIGHT_PURPLE  ('d', 0xFF55FF),
    YELLOW        ('e', 0xFFFF55),
    WHITE         ('f', 0xFFFFFF),
    BOLD          ('l'),
    ITALIC        ('o'),
    UNDERLINE     ('n'),
    STRIKETHROUGH ('m'),
    OBFUSCATED    ('k'),
    RESET         ('r');
    // @formatter:on

    /** The 16-color palette codes in order ({@code §0}-{@code §f}). */
    public static final McFormatCode[] PALETTE = { BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD,
        GRAY, DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE };

    /** The § code character (the char after {@code §}). */
    public final char code;

    private final int rgb; // -1 for non-colour codes

    @Contract(pure = true)
    McFormatCode(char code) {
        this(code, -1);
    }

    @Contract(pure = true)
    McFormatCode(char code, int rgb) {
        this.code = code;
        this.rgb = rgb;
    }

    /** Returns {@code true} if this is one of the 16 color codes ({@code §0}-{@code §f}). */
    @Contract(pure = true)
    public boolean isColor() {
        return rgb >= 0;
    }

    /** Returns {@code true} if this is one of the 5 modifier codes ({@code §l §o §n §m §k}). */
    @Contract(pure = true)
    public boolean isModifier() {
        return rgb < 0 && this != RESET;
    }

    /**
     * Returns the 24-bit packed RGB value of this color code.
     *
     * @throws IllegalStateException if this is not a color code
     */
    @Contract(pure = true)
    public int rgb() {
        if (rgb < 0) throw new IllegalStateException(name() + " is not a colour code");
        return rgb;
    }

    /** Appends the {@code §x} escape sequence for this code to {@code sb}. */
    public void appendTo(@NotNull StringBuilder sb) {
        sb.append('§')
            .append(code);
    }

    /**
     * Returns the {@link McFormatCode} for the given code character, or {@code null} if not
     * recognized.
     */
    @Contract(pure = true)
    public static @Nullable McFormatCode fromChar(char c) {
        for (McFormatCode mc : values()) {
            if (mc.code == c) return mc;
        }
        return null;
    }

    /** Returns the active color code in {@code codes}, or {@code null} if none is present. */
    @Contract(pure = true)
    public static @Nullable McFormatCode activeColor(@NotNull Set<McFormatCode> codes) {
        for (McFormatCode c : codes) {
            if (c.isColor()) return c;
        }
        return null;
    }

    /**
     * Returns {@code true} if transitioning from {@code prev} to {@code next} requires a reset
     * point (a color code or {@code §r}): either the color changed or a modifier was turned off.
     */
    @Contract(pure = true)
    public static boolean needsResetBefore(@NotNull Set<McFormatCode> prev, @NotNull Set<McFormatCode> next) {
        if (activeColor(prev) != activeColor(next)) return true;
        for (McFormatCode c : prev) {
            if (c.isModifier() && !next.contains(c)) return true;
        }
        return false;
    }
}
