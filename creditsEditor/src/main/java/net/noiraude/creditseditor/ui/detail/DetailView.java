package net.noiraude.creditseditor.ui.detail;

import static net.noiraude.creditseditor.ui.UiScale.scaled;

import java.awt.*;

import javax.swing.*;

import net.noiraude.creditseditor.command.CommandExecutor;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
public abstract class DetailView<T> extends JPanel {

    protected final @NotNull CommandExecutor onCommand;
    protected @Nullable T current;
    protected boolean loading;

    protected DetailView(@NotNull CommandExecutor onCommand) {
        this.onCommand = onCommand;
        setLayout(new GridBagLayout());
    }

    /** Returns a fresh {@link GridBagConstraints} for the left-column label cell. */
    @Contract(value = " -> new", pure = true)
    protected static @NotNull GridBagConstraints labelConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(scaled(4), scaled(6), scaled(4), scaled(4));
        return c;
    }

    /** Returns a fresh {@link GridBagConstraints} for the right-column field cell. */
    @Contract(value = " -> new", pure = true)
    protected static @NotNull GridBagConstraints fieldConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;
        c.weighty = 0;
        c.insets = new Insets(scaled(4), 0, scaled(4), scaled(6));
        return c;
    }
}
