package net.noiraude.creditseditor.command.impl;

/**
 * Base class for commands that modify list structure (add, remove, reorder).
 *
 * <p>
 * Structural commands trigger a full panel refresh on execute, undo, and redo.
 * They inherit the default {@link net.noiraude.creditseditor.command.Command#isLightEdit()
 * isLightEdit()} returning {@code false}.
 */
abstract class AbstractStructuralCommand extends AbstractCommand {
}
