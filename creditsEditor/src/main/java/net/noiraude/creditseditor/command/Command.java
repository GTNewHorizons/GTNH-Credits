package net.noiraude.creditseditor.command;

import org.jetbrains.annotations.NotNull;

/**
 * A reversible editor action.
 *
 * <p>
 * Implementations mutate the {@link net.noiraude.libcredits.model.CreditsDocument} and/or
 * the {@link net.noiraude.libcredits.lang.LangDocument} directly and publish the
 * corresponding {@link net.noiraude.creditseditor.bus.DocumentBus} topic so that widgets
 * can react.
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
}
