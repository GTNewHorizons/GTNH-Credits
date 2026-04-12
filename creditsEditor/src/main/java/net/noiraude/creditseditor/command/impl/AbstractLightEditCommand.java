package net.noiraude.creditseditor.command.impl;

/**
 * Base class for commands that edit a single field value (text, key, etc.).
 *
 * <p>
 * Light-edit commands only need list cells repainted, not a full structural refresh.
 * {@link #isLightEdit()} returns {@code true}.
 */
abstract class AbstractLightEditCommand extends AbstractCommand {

    @Override
    public boolean isLightEdit() {
        return true;
    }
}
