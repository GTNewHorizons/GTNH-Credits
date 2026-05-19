package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.noiraude.creditseditor.bus.DocumentBus;

import org.jetbrains.annotations.NotNull;

/** Opens the manage-locales window. */
final class ManageLocalesAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    ManageLocalesAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.manage_locales"));
        this.bus = bus;
        String mnemonic = I18n.get("action.manage_locales.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(Action.SHORT_DESCRIPTION, I18n.get("action.manage_locales"));
        setEnabled(bus.hasSession());
        PropertyChangeListener sessionListener = e -> setEnabled(bus.hasSession());
        bus.addListener(DocumentBus.TOPIC_SESSION, sessionListener);
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireManageLocalesRequested();
    }
}
