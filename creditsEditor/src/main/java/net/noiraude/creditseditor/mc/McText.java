package net.noiraude.creditseditor.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Plain-text utilities for Minecraft {@code §x}-formatted strings and lang file encoding.
 *
 * <p>
 * Pure: no Swing, no AWT. Owns {@code §x} parsing into {@link Segment}s and the minimal-codes
 * rendering transition helper used when walking a styled document back into a raw string.
 */
public final class McText {

    @Contract(pure = true)
    private McText() {}

    // ------------------------------------------------------------------
    // Stripping and lang encoding
    // ------------------------------------------------------------------

    /**
     * Returns a plain-text copy of {@code raw} with all {@code §x} sequences removed.
     *
     * <p>
     * {@code null} is returned as-is so call sites that thread optional strings can chain through.
     */
    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static @Nullable String strip(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return raw;
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

    /** Returns {@code true} if {@code text} contains any character other than {@code '\n'}. */
    @Contract(pure = true)
    public static boolean containsNonNewline(@NotNull String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) != '\n') return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Segment + parse
    // ------------------------------------------------------------------

    /**
     * A contiguous run of plain text sharing the same visual style.
     *
     * <p>
     * {@code codes} holds the active color code (if any) and any modifier codes. An empty set
     * means the run inherits the component's default style. Never contains
     * {@link McFormatCode#RESET}.
     */
    public static final class Segment {

        /** Plain text content, with all {@code §x} sequences removed. */
        public final @NotNull String text;

        /**
         * Active formatting codes: at most one color plus any subset of modifiers. Never contains
         * {@link McFormatCode#RESET}.
         */
        public final @NotNull EnumSet<McFormatCode> codes;

        Segment(@NotNull String text, @NotNull EnumSet<McFormatCode> codes) {
            this.text = text;
            this.codes = codes.isEmpty() ? EnumSet.noneOf(McFormatCode.class) : EnumSet.copyOf(codes);
        }
    }

    /**
     * Parses {@code raw} into an immutable list of styled segments.
     *
     * <p>
     * Empty text runs are omitted. Unknown {@code §x} sequences are emitted as literal text.
     *
     * @param raw the string to parse; {@code null} and empty are both accepted
     * @return ordered list of segments; never {@code null}
     */
    public static @NotNull @UnmodifiableView List<Segment> parse(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        List<Segment> result = new ArrayList<>();
        EnumSet<McFormatCode> state = EnumSet.noneOf(McFormatCode.class);
        StringBuilder text = new StringBuilder();

        int i = 0;
        while (i < raw.length()) {
            char c = raw.charAt(i);
            if (c == '§' && i + 1 < raw.length()) {
                McFormatCode mc = McFormatCode.fromChar(Character.toLowerCase(raw.charAt(i + 1)));
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

    private static void flushSegment(@NotNull StringBuilder text, @NotNull EnumSet<McFormatCode> state,
        @NotNull List<Segment> result) {
        if (!text.isEmpty()) {
            result.add(new Segment(text.toString(), state));
            text.setLength(0);
        }
    }

    private static void applyStateChange(@NotNull McFormatCode mc, @NotNull EnumSet<McFormatCode> state) {
        if (mc == McFormatCode.RESET || mc.isColor()) state.clear();
        if (mc != McFormatCode.RESET) state.add(mc);
    }

    // ------------------------------------------------------------------
    // Rendering: minimal-codes transition
    // ------------------------------------------------------------------

    /**
     * Appends the minimal set of {@code §x} codes required to transition from {@code prev} style
     * to {@code curr} style.
     *
     * <p>
     * When a modifier is turned off or the color changes, a reset point (color code or
     * {@code §r}) is emitted followed by all currently active modifier codes. When only new
     * modifiers are added, only those new codes are emitted.
     */
    public static void appendTransitionCodes(@NotNull StringBuilder sb, @NotNull Set<McFormatCode> prev,
        @NotNull Set<McFormatCode> curr) {
        if (McFormatCode.needsResetBefore(prev, curr)) {
            McFormatCode color = McFormatCode.activeColor(curr);
            if (color != null) color.appendTo(sb);
            else sb.append("§r");
            for (McFormatCode mc : curr) {
                if (mc.isModifier()) mc.appendTo(sb);
            }
        } else {
            for (McFormatCode mc : curr) {
                if (mc.isModifier() && !prev.contains(mc)) mc.appendTo(sb);
            }
        }
    }
}
