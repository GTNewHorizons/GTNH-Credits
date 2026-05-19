package net.noiraude.creditseditor.ui.component;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.ui.ActionIcons;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;

/** Toolbar group for the active editing locale of the current session. */
public final class LocaleControls extends JPanel {

    private final @NotNull DocumentBus bus;
    private final @NotNull JButton manageButton = new JButton();

    public LocaleControls(@NotNull DocumentBus bus) {
        super(new BorderLayout(gapTiny, 0));
        this.bus = bus;

        JLabel label = new JLabel(I18n.get("toolbar.locale.label"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, gapSmall));
        LocaleSelector selector = new LocaleSelector(bus);

        manageButton.setIcon(ActionIcons.manageLocales());
        manageButton.setToolTipText(I18n.get("action.manage_locales"));
        manageButton.addActionListener(e -> bus.fireManageLocalesRequested());

        add(label, BorderLayout.WEST);
        add(selector, BorderLayout.CENTER);
        add(manageButton, BorderLayout.EAST);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> refreshEnabled());
        refreshEnabled();
    }

    private void refreshEnabled() {
        manageButton.setEnabled(bus.hasSession());
    }
}
