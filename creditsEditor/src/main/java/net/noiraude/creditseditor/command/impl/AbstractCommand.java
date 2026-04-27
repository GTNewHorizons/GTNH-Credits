package net.noiraude.creditseditor.command.impl;

import net.noiraude.creditseditor.command.Command;

/**
 * Package-private base class for all concrete command implementations.
 *
 * <p>
 * Exists solely to concentrate the direct dependency on {@link Command} into one place
 * so that the individual command classes in this package do not each import it.
 * All three {@link Command} methods remain abstract; subclasses supply the behavior.
 */
abstract class AbstractCommand implements Command {
}
