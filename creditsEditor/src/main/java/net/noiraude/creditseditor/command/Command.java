package net.noiraude.creditseditor.command;

import org.jetbrains.annotations.NotNull;

/**
 * A reversible editor action.
 *
 * <p>
 * Implementations mutate the {@link net.noiraude.libcredits.model.CreditsDocument} and/or
 * the {@link net.noiraude.libcredits.lang.LangDocument} directly.
 * No Swing code belongs in implementations of this interface.
 */
public interface Command {

    /** Applies this command. Called once when the command is first executed. */
    void execute();

    /** Reverses the effect of {@link #execute()}. */
    void undo();

    /**
     * Human-readable name shown in the Undo/Redo menu items.
     * Example: {@code "Assign to category dev"}.
     */
    @NotNull
    String getDisplayName();

    /**
     * Returns {@code true} if this command only mutates field values or document text
     * and does not add or remove items from the document structure.
     *
     * <p>
     * When {@code true}, the UI only needs a targeted list repaint after execute/undo rather
     * than a full panel rebuild. Returns {@code false} by default.
     */
    default boolean isLightEdit() {
        return false;
    }
}
