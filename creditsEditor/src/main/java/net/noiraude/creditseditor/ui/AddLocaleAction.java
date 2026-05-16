package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Prompts for a new editing locale to add to the active session. */
final class AddLocaleAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    AddLocaleAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.add_locale"));
        this.bus = bus;
        String mnemonic = I18n.get("action.add_locale.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(Action.SHORT_DESCRIPTION, I18n.get("action.add_locale"));
        setEnabled(bus.hasSession());
        PropertyChangeListener sessionListener = e -> setEnabled(bus.hasSession());
        bus.addListener(DocumentBus.TOPIC_SESSION, sessionListener);
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireAddLocaleRequested();
    }
}
