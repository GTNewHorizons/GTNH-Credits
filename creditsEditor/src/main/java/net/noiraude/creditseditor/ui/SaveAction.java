package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Persists the active session to disk. */
final class SaveAction extends AbstractAction {

    private final @NotNull Runnable callback;

    SaveAction(@NotNull DocumentBus bus, @NotNull Runnable callback) {
        super(I18n.get("menu.file.save"));
        this.callback = callback;
        String mnemonic = I18n.get("menu.file.save.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        setEnabled(false);
        bus.addListener(DocumentBus.TOPIC_DIRTY, e -> setEnabled((Boolean) e.getNewValue()));
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        callback.run();
    }
}
