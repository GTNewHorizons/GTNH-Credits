package net.noiraude.creditseditor.ui.component;

import javax.swing.event.CaretListener;
import javax.swing.text.AttributeSet;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Interface implemented by styled text panes that can be connected to a {@link McFormatToolbar}.
 *
 * <p>
 * Provides the toolbar with the information it needs to reflect the current formatting state
 * and routes toolbar actions back to the pane.
 */
interface McFormatTarget {

    /** Returns {@code true} if there is a non-empty selection. */
    @Contract(pure = true)
    boolean hasSelection();

    /**
     * Computes which codes are active across the current selection. Call only when
     * {@link #hasSelection()} is {@code true}.
     */
    McFormatCode.@NotNull SelectionPresence computeSelectionPresence();

    /**
     * Returns the character attributes to reflect in the toolbar when there is no selection
     * (or as a fallback).
     */
    @NotNull
    AttributeSet getCaretAttributes();

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
