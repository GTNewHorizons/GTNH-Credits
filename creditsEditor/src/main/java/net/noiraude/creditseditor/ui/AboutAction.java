package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Opens the editor's about dialog. */
final class AboutAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    AboutAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.about", MsgArg.text(AppInfo.name())));
        this.bus = bus;
        String mnemonic = I18n.get("action.about.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireAboutRequested();
    }
}
