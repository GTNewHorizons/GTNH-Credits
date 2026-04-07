package net.noiraude.creditseditor.command;

/**
 * A reversible editor action.
 *
 * <p>
 * Implementations mutate the {@link net.noiraude.creditseditor.model.EditorModel} (and
 * optionally the {@link net.noiraude.creditseditor.service.LangDocument}) directly.
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
    String getDisplayName();
}
