package net.noiraude.creditseditor.ui.component;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * {@link DocumentListener} that calls the same action on any document change (insert, remove, or
 * attribute change).
 */
public final class AnyChangeListener implements DocumentListener {

    private final @NotNull Runnable action;

    @Contract(pure = true)
    public AnyChangeListener(@NotNull Runnable action) {
        this.action = action;
    }

    @Override
    public void insertUpdate(@NotNull DocumentEvent e) {
        action.run();
    }

    @Override
    public void removeUpdate(@NotNull DocumentEvent e) {
        action.run();
    }

    @Override
    public void changedUpdate(@NotNull DocumentEvent e) {
        action.run();
    }
}
