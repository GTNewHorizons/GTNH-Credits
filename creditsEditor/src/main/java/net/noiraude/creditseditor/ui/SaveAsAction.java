package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Persists the active session to a user-chosen location. */
final class SaveAsAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    SaveAsAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.save_as"));
        this.bus = bus;
        String mnemonic = I18n.get("action.save_as.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(
            Action.ACCELERATOR_KEY,
            KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        putValue(Action.SHORT_DESCRIPTION, I18n.get("action.save_as"));
        setEnabled(false);
        bus.addListener(DocumentBus.TOPIC_SESSION, e -> setEnabled((Boolean) e.getNewValue()));
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireSaveAsRequested();
    }
}
