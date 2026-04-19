package net.noiraude.creditseditor.ui.component;

import java.util.EnumSet;

import javax.swing.event.CaretListener;

import net.noiraude.creditseditor.mc.McFormatCode;
import net.noiraude.creditseditor.mc.McSelectionPresence;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by styled text panes that can be connected to a {@link McFormatToolbar}.
 *
 * <p>
 * Provides the toolbar with the information it needs to reflect the current formatting state
 * and routes toolbar actions back to the pane. All formatting states are exchanged in pure-domain
 * types ({@link McFormatCode} / {@link McSelectionPresence}); Swing attribute sets are a concern
 * of the implementing class.
 */
interface McFormatTarget {

    /** Returns {@code true} if there is a non-empty selection. */
    @Contract(pure = true)
    boolean hasSelection();

    /**
     * Computes which codes are active across the current selection. Call only when
     * {@link #hasSelection()} is {@code true}.
     */
    @NotNull
    McSelectionPresence computeSelectionPresence();

    /**
     * Returns the formatting codes to reflect in the toolbar when there is no selection (or as a
     * fallback).
     */
    @NotNull
    EnumSet<McFormatCode> getCaretStyle();

    /** Applies or removes a formatting code on the current selection or input style. */
    void applyCode(@NotNull McFormatCode mc, boolean active);

    /** Removes all formatting from the selection or input style (equivalent to {@code §r}). */
    void applyReset();

    /**
     * Removes the color attribute from the selection (or input style) without touching modifier
     * attributes. This restores the default (no-color) state for the affected characters.
     */
    void applyColorReset();

    /** Registers a listener notified when the caret or selection changes. */
    void addCaretListener(@NotNull CaretListener l);
}
