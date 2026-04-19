package net.noiraude.creditseditor.ui.component;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import net.noiraude.creditseditor.mc.McFormatCode;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Swing-side palette: maps the 16 {@link McFormatCode} colour codes to their
 * {@link java.awt.Color} equivalents.
 *
 * <p>
 * Sole owner of the domain-to-AWT color conversion. Every Swing site that needs a
 * {@code java.awt.Color} for a palette code goes through this class.
 */
public final class McPalette {

    private static final @NotNull Map<McFormatCode, Color> COLORS;

    static {
        EnumMap<McFormatCode, Color> m = new EnumMap<>(McFormatCode.class);
        for (McFormatCode c : McFormatCode.PALETTE) m.put(c, new Color(c.rgb()));
        COLORS = Collections.unmodifiableMap(m);
    }

    @Contract(pure = true)
    private McPalette() {}

    /**
     * Returns the {@link Color} for a palette color code.
     *
     * @throws IllegalArgumentException if {@code code} is not a color code
     */
    @Contract(pure = true)
    public static @NotNull Color colorOf(@NotNull McFormatCode code) {
        Color c = COLORS.get(code);
        if (c == null) throw new IllegalArgumentException(code.name() + " is not a colour code");
        return c;
    }
}
