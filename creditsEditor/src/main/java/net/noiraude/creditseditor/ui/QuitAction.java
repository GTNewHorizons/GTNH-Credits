package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Closes the editor after prompting for unsaved changes. */
final class QuitAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    QuitAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.quit"));
        this.bus = bus;
        String mnemonic = I18n.get("action.quit.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireQuitRequested();
    }
}
