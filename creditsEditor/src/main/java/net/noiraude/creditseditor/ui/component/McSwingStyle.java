package net.noiraude.creditseditor.ui.component;

import java.util.EnumSet;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.noiraude.creditseditor.mc.McFormatCode;
import net.noiraude.creditseditor.mc.McSelectionPresence;
import net.noiraude.creditseditor.mc.McText;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Bridge between the pure {@link McFormatCode} domain and Swing's
 * {@link javax.swing.text.StyledDocument} / {@link AttributeSet} machinery.
 *
 * <p>
 * Every {@code javax.swing.text.*} reference that deals with Minecraft formatting lives here.
 * The pure domain ({@link McFormatCode}, {@link McText}, {@link McSelectionPresence}) remains
 * free of Swing and AWT dependencies.
 *
 * <h3>Obfuscated storage</h3>
 *
 * <p>
 * {@link StyleConstants} has no slot for the Minecraft obfuscated ({@code §k}) effect, so this
 * class defines a private string key used solely to hang a boolean flag on an {@link AttributeSet}
 * for round-tripping. The key is a storage detail of the bridge, not a domain concept.
 */
public final class McSwingStyle {

    /** Private storage key for the obfuscated flag on Swing {@link AttributeSet}s. */
    private static final @NotNull Object ATTR_OBFUSCATED = "mc-obfuscated";

    @Contract(pure = true)
    private McSwingStyle() {}

    /**
     * Applies {@code mc}'s effect to {@code attrs}.
     *
     * <p>
     * Color codes set {@link StyleConstants#Foreground}. Modifier codes set their corresponding
     * {@link StyleConstants} attribute to {@code true} (or the obfuscated storage key for
     * {@link McFormatCode#OBFUSCATED}). Has no effect for {@link McFormatCode#RESET}.
     */
    public static void applyTo(@NotNull SimpleAttributeSet attrs, @NotNull McFormatCode mc) {
        switch (mc) {
            case BOLD -> StyleConstants.setBold(attrs, true);
            case ITALIC -> StyleConstants.setItalic(attrs, true);
            case UNDERLINE -> StyleConstants.setUnderline(attrs, true);
            case STRIKETHROUGH -> StyleConstants.setStrikeThrough(attrs, true);
            case OBFUSCATED -> attrs.addAttribute(ATTR_OBFUSCATED, Boolean.TRUE);
            default -> {
                if (mc.isColor()) StyleConstants.setForeground(attrs, McPalette.colorOf(mc));
            }
        }
    }

    /**
     * Removes {@code mc}'s attribute from {@code attrs}, turning it off.
     *
     * <p>
     * Has no effect for color codes or {@link McFormatCode#RESET}.
     */
    public static void removeFrom(@NotNull SimpleAttributeSet attrs, @NotNull McFormatCode mc) {
        switch (mc) {
            case BOLD -> StyleConstants.setBold(attrs, false);
            case ITALIC -> StyleConstants.setItalic(attrs, false);
            case UNDERLINE -> StyleConstants.setUnderline(attrs, false);
            case STRIKETHROUGH -> StyleConstants.setStrikeThrough(attrs, false);
            case OBFUSCATED -> attrs.addAttribute(ATTR_OBFUSCATED, Boolean.FALSE);
            default -> {}
        }
    }

    /**
     * Returns {@code true} if {@code mc} is currently active in {@code attrs}.
     *
     * <p>
     * Always returns {@code false} for {@link McFormatCode#RESET}.
     */
    public static boolean isActive(@NotNull AttributeSet attrs, @NotNull McFormatCode mc) {
        return switch (mc) {
            case BOLD -> StyleConstants.isBold(attrs);
            case ITALIC -> StyleConstants.isItalic(attrs);
            case UNDERLINE -> StyleConstants.isUnderline(attrs);
            case STRIKETHROUGH -> StyleConstants.isStrikeThrough(attrs);
            case OBFUSCATED -> Boolean.TRUE.equals(attrs.getAttribute(ATTR_OBFUSCATED));
            default -> mc.isColor() && attrs.isDefined(StyleConstants.Foreground)
                && McPalette.colorOf(mc)
                    .equals(StyleConstants.getForeground(attrs));
        };
    }

    /** Builds an {@link EnumSet} of all codes active in {@code attrs}. */
    public static @NotNull EnumSet<McFormatCode> fromAttributes(@NotNull AttributeSet attrs) {
        EnumSet<McFormatCode> result = EnumSet.noneOf(McFormatCode.class);
        for (McFormatCode c : McFormatCode.values()) {
            if (c != McFormatCode.RESET && isActive(attrs, c)) result.add(c);
        }
        return result;
    }

    /** Builds a fresh {@link SimpleAttributeSet} populated from {@code codes}. */
    public static @NotNull SimpleAttributeSet toAttributes(@NotNull EnumSet<McFormatCode> codes) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        for (McFormatCode mc : codes) applyTo(attrs, mc);
        return attrs;
    }

    /**
     * Computes which codes are active across the characters of {@code doc} over
     * {@code [start, end)}, excluding paragraph separator ({@code '\n'}) characters.
     */
    @Contract("_, _, _ -> new")
    public static @NotNull McSelectionPresence computePresence(@NotNull StyledDocument doc, int start, int end) {
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
            if (McText.containsNonNewline(text)) {
                EnumSet<McFormatCode> active = fromAttributes(elem.getAttributes());
                if (McFormatCode.activeColor(active) == null) anyLacksColor = true;
                if (all == null) {
                    all = EnumSet.copyOf(active);
                } else {
                    all.retainAll(active); // intersection: keep only codes present everywhere
                }
                any.addAll(active); // union: accumulate codes present anywhere
            }
            offset = runEnd;
        }
        return new McSelectionPresence(all != null ? all : EnumSet.noneOf(McFormatCode.class), any, anyLacksColor);
    }
}
