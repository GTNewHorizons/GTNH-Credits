package net.noiraude.creditseditor.ui.detail;

import java.awt.*;
import java.util.function.Consumer;

import javax.swing.*;

import net.noiraude.creditseditor.command.Command;

/**
 * Base panel for detail form views.
 *
 * <p>
 * Holds the command executor, the current item reference, and the loading guard common to all
 * detail views. Also provides the standard two-column {@link GridBagLayout} row-constraint
 * factories shared by every subclass.
 *
 * @param <T> the type of the model item being edited
 */
abstract class DetailView<T> extends JPanel {

    protected final Consumer<Command> onCommand;
    protected T current;
    protected boolean loading;

    protected DetailView(Consumer<Command> onCommand) {
        this.onCommand = onCommand;
        setLayout(new GridBagLayout());
    }

    /** Returns a fresh {@link GridBagConstraints} for the left-column label cell. */
    protected static GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(4, 6, 4, 4);
        return c;
    }

    /** Returns a fresh {@link GridBagConstraints} for the right-column field cell. */
    protected static GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0;
        c.insets = new Insets(4, 0, 4, 6);
        return c;
    }
}
