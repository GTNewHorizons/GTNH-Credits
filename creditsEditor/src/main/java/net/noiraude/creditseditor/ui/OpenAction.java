package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Loads an existing credits resource from disk into the editor. */
final class OpenAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    OpenAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.open"));
        this.bus = bus;
        String mnemonic = I18n.get("action.open.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        putValue(Action.SHORT_DESCRIPTION, I18n.get("action.open"));
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireOpenRequested();
    }
}
