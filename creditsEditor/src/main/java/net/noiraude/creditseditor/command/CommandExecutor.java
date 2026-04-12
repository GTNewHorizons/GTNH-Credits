package net.noiraude.creditseditor.command;

/**
 * Callback that receives and executes a {@link Command}.
 *
 * <p>
 * Used throughout the UI layer in place of {@code Consumer<Command>} so that UI classes
 * depend only on this type and do not directly import {@link Command}.
 */
@FunctionalInterface
public interface CommandExecutor {

    /** Executes {@code cmd} through the editor's command stack. */
    void execute(Command cmd);
}
