package net.noiraude.creditseditor.ui.component;

import static net.noiraude.creditseditor.ui.ScaledMetrics.gapSmall;
import static net.noiraude.creditseditor.ui.ScaledMetrics.gapTiny;

import java.awt.BorderLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.noiraude.creditseditor.bus.DocumentBus;
import net.noiraude.creditseditor.service.LangResolver;
import net.noiraude.creditseditor.ui.I18n;

import org.jetbrains.annotations.NotNull;

/** Toolbar group for the active editing locale of the current session. */
public final class LocaleControls extends JPanel {

    private final @NotNull DocumentBus bus;
    private final @NotNull JButton addButton = new JButton("+");
    private final @NotNull JButton removeButton = new JButton("−");

    public LocaleControls(@NotNull DocumentBus bus) {
        super(new BorderLayout(gapTiny, 0));
        this.bus = bus;

        JLabel label = new JLabel(I18n.get("toolbar.locale.label"));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, gapSmall));
        LocaleSelector selector = new LocaleSelector(bus);

        addButton.setMargin(new Insets(0, gapTiny, 0, gapTiny));
        addButton.setToolTipText(I18n.get("action.add_locale"));
        addButton.addActionListener(e -> bus.fireAddLocaleRequested());

        removeButton.setMargin(new Insets(0, gapTiny, 0, gapTiny));
        removeButton.setToolTipText(I18n.get("action.remove_locale"));
        removeButton.addActionListener(e -> bus.fireRemoveLocaleRequested());

        JPanel buttons = new JPanel(new BorderLayout(gapTiny, 0));
        buttons.add(addButton, BorderLayout.WEST);
        buttons.add(removeButton, BorderLayout.EAST);

        add(label, BorderLayout.WEST);
        add(selector, BorderLayout.CENTER);
        add(buttons, BorderLayout.EAST);

        bus.addListener(DocumentBus.TOPIC_SESSION, e -> refreshButtons());
        bus.addListener(DocumentBus.TOPIC_LOCALE, e -> refreshButtons());
        refreshButtons();
    }

    private void refreshButtons() {
        boolean hasSession = bus.hasSession();
        addButton.setEnabled(hasSession);
        removeButton.setEnabled(hasSession && !LangResolver.DEFAULT_LOCALE.equals(bus.activeLocale()));
    }
}
