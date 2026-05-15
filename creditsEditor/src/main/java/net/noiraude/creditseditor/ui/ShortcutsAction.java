package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Opens the keyboard-shortcuts reference dialog. */
final class ShortcutsAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    ShortcutsAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.shortcuts"));
        this.bus = bus;
        String mnemonic = I18n.get("action.shortcuts.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireShortcutsRequested();
    }
}
