package net.noiraude.creditseditor.ui.component;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * {@link DocumentListener} that calls the same action on any document change (insert, remove, or
 * attribute change).
 */
final class AnyChangeListener implements DocumentListener {

    private final Runnable action;

    AnyChangeListener(Runnable action) {
        this.action = action;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        action.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        action.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        action.run();
    }
}
