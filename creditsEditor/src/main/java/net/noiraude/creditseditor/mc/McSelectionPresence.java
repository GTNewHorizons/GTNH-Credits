package net.noiraude.creditseditor.mc;

import java.util.EnumSet;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Result of a selection-wide formatting scan.
 *
 * <p>
 * {@link #all} contains every {@link McFormatCode} active on every character in the selection.
 * {@link #any} contains every code active on at least one character. A code in {@link #any} but
 * not in {@link #all} is in a mixed (indeterminate) state. {@link #anyLacksColor} is {@code true}
 * when at least one character in the selection carries no color attribute.
 *
 * <p>
 * Pure data: produced by the Swing bridge when walking a {@code StyledDocument} and consumed by
 * the toolbar to render tri-state buttons, but the type itself carries no Swing or AWT
 * dependency.
 *
 * @param all           Codes active on every character in the selection.
 * @param any           Codes active on at least one character in the selection.
 * @param anyLacksColor {@code true} when at least one selected character has no color attribute (default color).
 */
public record McSelectionPresence(@NotNull EnumSet<McFormatCode> all, @NotNull EnumSet<McFormatCode> any,
                                  boolean anyLacksColor) {

    @Contract(pure = true)
    public McSelectionPresence {
    }
}
