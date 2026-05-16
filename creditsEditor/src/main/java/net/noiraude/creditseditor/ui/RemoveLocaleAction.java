package net.noiraude.creditseditor.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.service.LangResolver;

import org.jetbrains.annotations.NotNull;

/** Removes the active editing locale from the session. */
final class RemoveLocaleAction extends AbstractAction {

    private final @NotNull DocumentBus bus;

    RemoveLocaleAction(@NotNull DocumentBus bus) {
        super(I18n.get("action.remove_locale"));
        this.bus = bus;
        String mnemonic = I18n.get("action.remove_locale.mnemonic");
        if (mnemonic.length() == 1) {
            putValue(Action.MNEMONIC_KEY, (int) Character.toUpperCase(mnemonic.charAt(0)));
        }
        putValue(Action.SHORT_DESCRIPTION, I18n.get("action.remove_locale"));
        refreshEnabled();
        PropertyChangeListener refresh = e -> refreshEnabled();
        bus.addListener(DocumentBus.TOPIC_SESSION, refresh);
        bus.addListener(DocumentBus.TOPIC_LOCALE, refresh);
    }

    private void refreshEnabled() {
        setEnabled(bus.hasSession() && !LangResolver.DEFAULT_LOCALE.equals(bus.activeLocale()));
    }

    @Override
    public void actionPerformed(@NotNull ActionEvent e) {
        bus.fireRemoveLocaleRequested();
    }
}
